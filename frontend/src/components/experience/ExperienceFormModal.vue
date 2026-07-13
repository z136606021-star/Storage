<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, FileOutlined, ReloadOutlined, SettingOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { createExperienceRecord, updateExperienceRecord } from '@/api/experience'
import { downloadFile, fetchUploadPolicy } from '@/api/file'
import { getErrorMessage } from '@/api/http'
import {
  useControlledFileUpload,
  type ControlledUploadItem,
} from '@/composables/useControlledFileUpload'
import type {
  ExperienceAttachment,
  ExperienceRecordDetail,
  ExperienceRecordSavePayload,
  ExperienceType,
} from '@/types/experience'
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

const formRef = ref<FormInstance>()
const submitting = ref(false)
const downloadingObjectKey = ref<string | null>(null)

const {
  items,
  isUploading,
  canAddMore,
  maxCount,
  setPolicy,
  setItems,
  clearItems,
  beforeUpload,
  retryUpload,
  removeItem,
  resolveObjectKeys,
} = useControlledFileUpload()

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

function toControlledItems(attachments: ExperienceAttachment[]): ControlledUploadItem[] {
  return attachments.map((item, index) => ({
    uid: `${item.objectKey}-${index}`,
    name: item.originalName,
    status: 'done',
    url: item.url,
    objectKey: item.objectKey,
    contentType: item.contentType,
    sizeBytes: item.sizeBytes,
  }))
}

function syncAttachmentKeys() {
  formState.attachmentObjectKeys = resolveObjectKeys()
}

async function loadUploadPolicy() {
  try {
    const policy = await fetchUploadPolicy()
    setPolicy(policy)
  } catch {
    // keep composable defaults
  }
}

watch(
  () => props.open,
  async (open) => {
    if (!open) {
      return
    }
    await loadUploadPolicy()
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
      setItems(toControlledItems(props.record.attachments))
    } else {
      Object.assign(formState, defaultForm())
      clearItems()
    }
    syncAttachmentKeys()
  },
)

watch(
  items,
  () => {
    syncAttachmentKeys()
  },
  { deep: true },
)

function isPreviewable(item: ControlledUploadItem) {
  return Boolean(item.contentType?.startsWith('image/') && item.url)
}

async function handleDownloadAttachment(item: ControlledUploadItem) {
  if (!item.objectKey) {
    return
  }
  downloadingObjectKey.value = item.objectKey
  try {
    const blob = await downloadFile(item.objectKey)
    downloadBlob(blob, item.name)
  } catch (error) {
    message.error(getErrorMessage(error, '下载失败'))
  } finally {
    downloadingObjectKey.value = null
  }
}

function formatFileSize(size?: number) {
  if (!size) {
    return '—'
  }
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
  clearItems()
}

async function handleSubmit() {
  if (isUploading.value) {
    message.warning('附件仍在上传中，请稍候')
    return
  }

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
    attachmentObjectKeys: resolveObjectKeys(),
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
            :disabled="!canAddMore || isUploading"
            accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.txt"
            multiple
          >
            <a-button :loading="isUploading" :disabled="!canAddMore">
              <template #icon><UploadOutlined /></template>
              上传
            </a-button>
          </a-upload>
          <div class="upload-hint">最多 {{ maxCount }} 个，支持多选上传</div>

          <div v-if="items.length" class="attachment-list">
            <div v-for="file in items" :key="file.uid" class="attachment-item">
              <FileOutlined />
              <template v-if="file.status === 'uploading'">
                <span class="attachment-name">{{ file.name }}</span>
                <span class="attachment-status">上传中...</span>
              </template>
              <template v-else-if="file.status === 'error'">
                <span class="attachment-name attachment-error">{{ file.name }}</span>
                <span class="attachment-status attachment-error">{{ file.errorMessage }}</span>
                <a-button type="link" size="small" @click="retryUpload(file.uid)">
                  <template #icon><ReloadOutlined /></template>
                  重试
                </a-button>
              </template>
              <template v-else>
                <a
                  v-if="isPreviewable(file)"
                  :href="file.url ?? undefined"
                  target="_blank"
                  rel="noreferrer"
                  class="attachment-name"
                >
                  {{ file.name }}
                </a>
                <a-button
                  v-else
                  type="link"
                  class="attachment-link"
                  :loading="downloadingObjectKey === file.objectKey"
                  @click="handleDownloadAttachment(file)"
                >
                  {{ file.name }}
                </a-button>
              </template>
              <span class="attachment-size">{{ formatFileSize(file.sizeBytes) }}</span>
              <a-button
                type="link"
                size="small"
                danger
                class="remove-button"
                @click="removeItem(file.uid)"
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

.upload-hint {
  font-size: 12px;
  color: @color-text-tertiary;
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

.attachment-name {
  color: @color-text;
}

.attachment-link {
  height: auto;
  padding: 0;
  line-height: 1.4;
  text-align: left;
}

.attachment-status {
  color: @color-text-tertiary;
  font-size: @font-size-sm;
}

.attachment-error {
  color: #ff4d4f;
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
