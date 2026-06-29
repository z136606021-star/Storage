<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, UploadProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { getErrorMessage } from '@/api/http'
import { uploadFile } from '@/api/file'
import { createWarehouseBom, updateWarehouseBom } from '@/api/warehouseBom'
import type { WarehouseBom, WarehouseBomSavePayload } from '@/types/warehouseBom'

const props = defineProps<{
  open: boolean
  record: WarehouseBom | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024

const formRef = ref<FormInstance>()
const submitting = ref(false)
const uploading = ref(false)
const previewUrl = ref<string | null>(null)

const defaultForm = (): WarehouseBomSavePayload => ({
  category: '',
  genericName: '',
  brand: null,
  name: '',
  remark: null,
  imageObjectKey: null,
})

const formState = reactive<WarehouseBomSavePayload>(defaultForm())

const isEdit = computed(() => props.record != null)
const modalTitle = computed(() => (isEdit.value ? '编辑物料清单' : '新增物料清单'))
const hasImage = computed(() => Boolean(previewUrl.value))

const rules = {
  category: [{ required: true, message: '请输入品类', trigger: 'blur' }],
  genericName: [{ required: true, message: '请输入统称', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
}

watch(
  () => props.open,
  (open) => {
    if (!open) {
      return
    }
    if (props.record) {
      Object.assign(formState, {
        category: props.record.category,
        genericName: props.record.genericName,
        brand: props.record.brand,
        name: props.record.name,
        remark: props.record.remark,
        imageObjectKey: props.record.imageObjectKey,
      })
      previewUrl.value = props.record.imageUrl
    } else {
      Object.assign(formState, defaultForm())
      previewUrl.value = null
    }
  },
)

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    message.error('仅支持 JPG、PNG、WebP、GIF 格式图片')
    return false
  }
  if (file.size > MAX_IMAGE_SIZE_BYTES) {
    message.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const customUpload: UploadProps['customRequest'] = async (options) => {
  const file = options.file as File
  uploading.value = true
  try {
    const result = await uploadFile(file)
    formState.imageObjectKey = result.objectKey
    previewUrl.value = result.url
    options.onSuccess?.(result)
    message.success('图片上传成功')
  } catch (error) {
    options.onError?.(error as Error)
    message.error(getErrorMessage(error, '图片上传失败'))
  } finally {
    uploading.value = false
  }
}

function clearImage() {
  formState.imageObjectKey = null
  previewUrl.value = null
}

function handleCancel() {
  emit('update:open', false)
  formRef.value?.resetFields()
  previewUrl.value = null
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  const payload: WarehouseBomSavePayload = {
    category: formState.category.trim(),
    genericName: formState.genericName.trim(),
    brand: formState.brand?.trim() || null,
    name: formState.name.trim(),
    remark: formState.remark?.trim() || null,
    imageObjectKey: formState.imageObjectKey ?? null,
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
      <a-form-item label="备注" name="remark">
        <a-textarea
          v-model:value="formState.remark"
          :rows="3"
          placeholder="请输入……"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="图片">
        <div class="image-upload-row">
          <a-upload
            list-type="picture-card"
            accept="image/*"
            :show-upload-list="false"
            :before-upload="beforeUpload"
            :custom-request="customUpload"
            :disabled="uploading"
          >
            <div v-if="hasImage" class="image-preview-wrap">
              <img :src="previewUrl!" alt="物料图片" class="image-preview" />
            </div>
            <div v-else class="upload-placeholder">
              <PlusOutlined />
              <div class="upload-text">上传</div>
            </div>
          </a-upload>
          <a-button v-if="hasImage" type="link" danger @click="clearImage">清除图片</a-button>
        </div>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.image-upload-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.image-preview-wrap,
.upload-placeholder {
  width: 104px;
  height: 104px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.upload-text {
  margin-top: 8px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
