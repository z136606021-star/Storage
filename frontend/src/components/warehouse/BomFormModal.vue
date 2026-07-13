<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { getErrorMessage } from '@/api/http'
import { fetchUploadPolicy } from '@/api/file'
import { createWarehouseBom, updateWarehouseBom } from '@/api/warehouse/warehouseBom'
import {
  useControlledFileUpload,
  type ControlledUploadItem,
} from '@/composables/useControlledFileUpload'
import type { WarehouseBom, WarehouseBomSavePayload } from '@/types/warehouse/warehouseBom'

const props = defineProps<{
  open: boolean
  record: WarehouseBom | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const IMAGE_CONTENT_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

const {
  fileList,
  isUploading,
  canAddMore,
  maxCount,
  setPolicy,
  setItems,
  clearItems,
  beforeUpload,
  handleRemove,
  resolveObjectKeys,
} = useControlledFileUpload({ allowedTypes: IMAGE_CONTENT_TYPES })

const defaultForm = (): WarehouseBomSavePayload => ({
  category: '',
  genericName: '',
  brand: null,
  name: '',
  model: null,
  remark: null,
  imageObjectKeys: [],
})

const formState = reactive<WarehouseBomSavePayload>(defaultForm())

const isEdit = computed(() => props.record != null)
const modalTitle = computed(() => (isEdit.value ? '编辑物料清单' : '新增物料清单'))

const rules = {
  category: [{ required: true, message: '请输入品类', trigger: 'blur' }],
  genericName: [{ required: true, message: '请输入统称', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
}

function buildItems(record: WarehouseBom | null): ControlledUploadItem[] {
  if (!record) {
    return []
  }
  const keys = record.imageObjectKeys?.length
    ? record.imageObjectKeys
    : record.imageObjectKey
      ? [record.imageObjectKey]
      : []
  const urls = record.imageUrls?.length
    ? record.imageUrls
    : record.imageUrl
      ? [record.imageUrl]
      : []
  return keys.map((objectKey, index) => ({
    uid: `${objectKey}-${index}`,
    name: objectKey.split('/').pop() ?? `image-${index + 1}`,
    status: 'done' as const,
    url: urls[index] ?? null,
    objectKey,
  }))
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
        category: props.record.category,
        genericName: props.record.genericName,
        brand: props.record.brand,
        name: props.record.name,
        model: props.record.model,
        remark: props.record.remark,
        imageObjectKeys: props.record.imageObjectKeys ?? [],
      })
      setItems(buildItems(props.record))
    } else {
      Object.assign(formState, defaultForm())
      clearItems()
    }
  },
)

function handleCancel() {
  emit('update:open', false)
  formRef.value?.resetFields()
  clearItems()
}

async function handleSubmit() {
  if (isUploading.value) {
    message.warning('图片仍在上传中，请稍候')
    return
  }

  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  const imageObjectKeys = resolveObjectKeys()
  const payload: WarehouseBomSavePayload = {
    category: formState.category.trim(),
    genericName: formState.genericName.trim(),
    brand: formState.brand?.trim() || null,
    name: formState.name.trim(),
    model: formState.model?.trim() || null,
    remark: formState.remark?.trim() || null,
    imageObjectKeys,
    imageObjectKey: imageObjectKeys[0] ?? null,
  }

  try {
    if (isEdit.value && props.record) {
      await updateWarehouseBom(props.record.id, payload)
      message.success('更新成功')
    } else {
      await createWarehouseBom(payload)
      message.success('创建成功')
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
    :confirm-loading="submitting"
    ok-text="提交"
    cancel-text="关闭"
    destroy-on-close
    @cancel="handleCancel"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="formState" :rules="rules" layout="vertical">
      <a-form-item label="品类" name="category">
        <a-input v-model:value="formState.category" placeholder="请输入品类" allow-clear />
      </a-form-item>
      <a-form-item label="统称" name="genericName">
        <a-input v-model:value="formState.genericName" placeholder="请输入统称" allow-clear />
      </a-form-item>
      <a-form-item label="品牌" name="brand">
        <a-input v-model:value="formState.brand" placeholder="请输入品牌" allow-clear />
      </a-form-item>
      <a-form-item label="名称" name="name" required>
        <a-input v-model:value="formState.name" placeholder="请输入名称" allow-clear />
      </a-form-item>
      <a-form-item label="规格" name="model">
        <a-input v-model:value="formState.model" placeholder="请输入规格（选填）" allow-clear />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea
          v-model:value="formState.remark"
          :rows="3"
          placeholder="请输入……"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="图片">
        <a-upload
          :file-list="fileList"
          list-type="picture-card"
          accept="image/*"
          multiple
          :before-upload="beforeUpload"
          @remove="handleRemove"
        >
          <div v-if="canAddMore" class="upload-placeholder">
            <PlusOutlined />
            <div class="upload-text">上传</div>
          </div>
        </a-upload>
        <div class="upload-hint">最多 {{ maxCount }} 张，支持多选上传</div>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.upload-placeholder {
  width: 104px;
  height: 104px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.upload-text {
  margin-top: @spacing-sm;
  font-size: 12px;
  color: @color-text-tertiary;
}

.upload-hint {
  margin-top: @spacing-xs;
  font-size: 12px;
  color: @color-text-tertiary;
}
</style>
