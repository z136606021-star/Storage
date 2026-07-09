<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, UploadProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, FileOutlined, SettingOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { createExperienceRecord, updateExperienceRecord } from '@/api/experience'
import { downloadFile, uploadFile } from '@/api/file'
import { getErrorMessage } from '@/api/http'
import type {
  ExperienceAttachment,
  ExperienceRecordDetail,
  ExperienceRecordSavePayload,
  ExperienceType,
} from '@/types/experience'
import type { FileUploadResult } from '@/types/file'
import { downloadBlob } from '@/utils/download'

const props = defineProps<{
  open: boolean
  record: ExperienceRecordDetail | null
  types: ExperienceType[]
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
  'manage-types': []
}>()

const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024
const ALLOWED_TYPES = [
  'image/jpeg',
  'image/png',
  'image/webp',
  'image/gif',
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'text/plain',
]

const formRef = ref<FormInstance>()
const submitting = ref(false)
const uploading = ref(false)
const downloadingObjectKey = ref<string | null>(null)
const attachments = ref<ExperienceAttachment[]>([])

const defaultForm = (): ExperienceRecordSavePayload => ({
  typeId: null,
  description: '',
  impact: null,
  suggestion: null,
  actionPlan: null,
  recordedAt: null,
  projectNames: [],
  attachmentObjectKeys: [],
})

const formState = reactive<ExperienceRecordSavePayload>(defaultForm())

const isEdit = computed(() => props.record != null)
const modalTitle = computed(() => (isEdit.value ? '编辑经验' : '新增'))
const typeOptions = computed(() =>
  props.types
    .filter((item) => item.status === 1 || item.id === props.record?.typeId)
    .map((item) => ({ label: item.name, value: item.id })),
)

const rules = {
  typeId: [{ required: true, message: '请选择类型', trigger: 'change' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
}

watch(
  () => props.open,
  (open) => {
    if (!open) {
      return
    }
    if (props.record) {
      Object.assign(formState, {
        typeId: props.record.typeId,
        description: props.record.description,
        impact: props.record.impact,
        suggestion: props.record.suggestion,
        actionPlan: props.record.actionPlan,
        recordedAt: props.record.recordedAt,
        projectNames: [...props.record.projectNames],
        attachmentObjectKeys: props.record.attachments.map((item) => item.objectKey),
      })
      attachments.value = [...props.record.attachments]
    } else {
      Object.assign(formState, defaultForm())
      attachments.value = []
    }
  },
)

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  if (!ALLOWED_TYPES.includes(file.type)) {
    message.error('仅支持图片、PDF、Word、Excel、文本文件')
    return false
  }
  if (file.size > MAX_FILE_SIZE_BYTES) {
    message.error('文件大小不能超过 5MB')
    return false
  }
  return true
}

const customUpload: UploadProps['customRequest'] = async (options) => {
  const file = options.file as File
  uploading.value = true
  try {
    const result = await uploadFile(file)
    attachments.value.push(toAttachment(result))
    syncAttachmentKeys()
    options.onSuccess?.(result)
    message.success('上传成功')
  } catch (error) {
    options.onError?.(error as Error)
    message.error(getErrorMessage(error, '上传失败'))
  } finally {
    uploading.value = false
  }
}

function toAttachment(file: FileUploadResult): ExperienceAttachment {
  const previewable = Boolean(file.contentType?.startsWith('image/'))
  return {
    id: file.id,
    objectKey: file.objectKey,
    originalName: file.originalName,
    contentType: file.contentType,
    sizeBytes: file.sizeBytes,
    url: previewable ? file.url : null,
    previewable,
  }
}

function syncAttachmentKeys() {
  formState.attachmentObjectKeys = attachments.value.map((item) => item.objectKey)
}

function removeAttachment(objectKey: string) {
  attachments.value = attachments.value.filter((item) => item.objectKey !== objectKey)
  syncAttachmentKeys()
}

async function handleDownloadAttachment(file: ExperienceAttachment) {
  downloadingObjectKey.value = file.objectKey
  try {
    const blob = await downloadFile(file.objectKey)
    downloadBlob(blob, file.originalName)
  } catch (error) {
    message.error(getErrorMessage(error, '下载失败'))
  } finally {
    downloadingObjectKey.value = null
  }
}

function formatFileSize(size: number) {
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function trimToNull(value?: string | null) {
  const text = value?.trim()
  return text || null
}

function handleCancel() {
  emit('update:open', false)
  formRef.value?.resetFields()
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (formState.typeId == null) {
    return
  }

  submitting.value = true
  const payload: ExperienceRecordSavePayload = {
    typeId: formState.typeId,
    description: formState.description.trim(),
    impact: trimToNull(formState.impact),
    suggestion: trimToNull(formState.suggestion),
    actionPlan: trimToNull(formState.actionPlan),
    recordedAt: formState.recordedAt ?? null,
    projectNames: formState.projectNames.map((item) => item.trim()).filter(Boolean),
    attachmentObjectKeys: formState.attachmentObjectKeys,
  }

  try {
    if (isEdit.value && props.record) {
      await updateExperienceRecord(props.record.id, payload)
      message.success('保存成功')
    } else {
      await createExperienceRecord(payload)
      message.success('提交成功')
    }
    emit('update:open', false)
    emit('success')
  } catch (error) {
    message.error(getErrorMessage(error, '保存失败'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <a-modal
    :open="open"
    :title="modalTitle"
    width="1120px"
    ok-text="提交"
    cancel-text="关闭"
    :confirm-loading="submitting"
    destroy-on-close
    @cancel="handleCancel"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="formState" :rules="rules" class="experience-form">
      <a-form-item label="类型" name="typeId" class="type-row">
        <a-space>
          <a-select
            v-model:value="formState.typeId"
            :options="typeOptions"
            placeholder="请选择类型"
            show-search
            option-filter-prop="label"
            style="width: 240px"
          />
          <a-tooltip title="类型管理">
            <a-button @click="emit('manage-types')">
              <template #icon><SettingOutlined /></template>
            </a-button>
          </a-tooltip>
        </a-space>
      </a-form-item>

      <a-form-item label="描述" name="description">
        <a-textarea v-model:value="formState.description" :rows="4" />
      </a-form-item>

      <a-form-item label="影响" name="impact">
        <a-textarea v-model:value="formState.impact" :rows="4" />
      </a-form-item>

      <a-form-item label="建议" name="suggestion">
        <a-textarea v-model:value="formState.suggestion" :rows="4" />
      </a-form-item>

      <a-form-item label="改善行动" name="actionPlan">
        <a-textarea v-model:value="formState.actionPlan" :rows="4" />
      </a-form-item>

      <a-form-item label="关联项目">
        <a-select
          v-model:value="formState.projectNames"
          mode="tags"
          placeholder="输入项目名称后回车"
          class="project-tags"
          :token-separators="[',', '，', ';', '；']"
        />
      </a-form-item>

      <a-form-item label="文件/图片">
        <div class="attachment-area">
          <a-upload
            :show-upload-list="false"
            :before-upload="beforeUpload"
            :custom-request="customUpload"
            :disabled="uploading"
            accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.txt"
          >
            <a-button :loading="uploading">
              <template #icon><UploadOutlined /></template>
              上传
            </a-button>
          </a-upload>

          <div v-if="attachments.length" class="attachment-list">
            <div v-for="file in attachments" :key="file.objectKey" class="attachment-item">
              <FileOutlined />
              <a v-if="file.previewable && file.url" :href="file.url" target="_blank" rel="noreferrer">
                {{ file.originalName }}
              </a>
              <a-button
                v-else
                type="link"
                class="attachment-link"
                :loading="downloadingObjectKey === file.objectKey"
                @click="handleDownloadAttachment(file)"
              >
                {{ file.originalName }}
              </a-button>
              <span class="attachment-size">{{ formatFileSize(file.sizeBytes) }}</span>
              <a-button
                type="link"
                size="small"
                danger
                class="remove-button"
                @click="removeAttachment(file.objectKey)"
              >
                <template #icon><DeleteOutlined /></template>
              </a-button>
            </div>
          </div>
        </div>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.experience-form {
  max-width: 960px;
  margin: 0 auto;

  :deep(.ant-form-item) {
    margin-bottom: @spacing-md;
  }

  :deep(.ant-form-item-label) {
    width: 96px;
    text-align: right;
  }

  :deep(.ant-form-item-control) {
    flex: 1;
  }
}

.project-tags {
  max-width: 560px;
}

.attachment-area {
  display: flex;
  flex-direction: column;
  gap: @spacing-sm;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: @spacing-xs;
  max-width: 560px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: @spacing-xs;
  min-height: 32px;
  padding: 0 @spacing-sm;
  border: 1px solid @color-border;
  border-radius: @radius-sm;
}

.attachment-link {
  height: auto;
  padding: 0;
  line-height: 1.4;
  text-align: left;
}

.attachment-size {
  color: @color-text-tertiary;
  margin-left: auto;
  font-size: @font-size-sm;
}

.remove-button {
  padding-inline: 0;
}
</style>
