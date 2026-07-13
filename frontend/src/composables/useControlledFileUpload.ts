import { computed, ref } from 'vue'
import type { UploadFile, UploadProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { getErrorMessage } from '@/api/http'
import { uploadFile } from '@/api/file'
import type { FileUploadResult } from '@/types/file'
import type { FileUploadPolicy } from '@/types/uploadPolicy'
import { DEFAULT_UPLOAD_POLICY } from '@/types/uploadPolicy'

export interface ControlledUploadItem {
  uid: string
  name: string
  status: 'uploading' | 'done' | 'error'
  url?: string | null
  objectKey?: string
  contentType?: string | null
  sizeBytes?: number
  errorMessage?: string
}

interface PendingUpload {
  uid: string
  file: File
}

export interface UseControlledFileUploadOptions {
  policy?: Partial<FileUploadPolicy>
  allowedTypes?: string[]
  uploadFn?: (file: File) => Promise<FileUploadResult>
  mapResult?: (result: FileUploadResult) => Partial<ControlledUploadItem>
}

function createUid(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

export function useControlledFileUpload(options: UseControlledFileUploadOptions = {}) {
  const policy = ref<FileUploadPolicy>({
    ...DEFAULT_UPLOAD_POLICY,
    ...options.policy,
  })
  const items = ref<ControlledUploadItem[]>([])
  const originFiles = new Map<string, File>()
  const pendingQueue: PendingUpload[] = []
  let activeUploads = 0

  const allowedTypes = computed(
    () => options.allowedTypes ?? policy.value.allowedContentTypes,
  )
  const maxCount = computed(() => policy.value.maxFilesPerRecord)
  const maxSizeBytes = computed(() => policy.value.maxSizeBytes)
  const concurrency = computed(() => policy.value.uploadConcurrency)
  const isUploading = computed(() =>
    items.value.some((item) => item.status === 'uploading') || pendingQueue.length > 0,
  )
  const canAddMore = computed(() => items.value.length < maxCount.value)

  const fileList = computed<UploadFile[]>(() =>
    items.value.map((item) => ({
      uid: item.uid,
      name: item.name,
      status: item.status,
      url: item.url ?? undefined,
      thumbUrl: item.url ?? undefined,
      objectKey: item.objectKey,
      error: item.errorMessage ? new Error(item.errorMessage) : undefined,
    })),
  )

  function setPolicy(nextPolicy: Partial<FileUploadPolicy>) {
    policy.value = {
      ...policy.value,
      ...nextPolicy,
    }
  }

  function validateFile(file: File): string | null {
    if (!allowedTypes.value.includes(file.type)) {
      return '文件类型不支持'
    }
    return null
  }

  function updateItem(uid: string, patch: Partial<ControlledUploadItem>) {
    items.value = items.value.map((item) => (item.uid === uid ? { ...item, ...patch } : item))
  }

  function removeItem(uid: string) {
    items.value = items.value.filter((item) => item.uid !== uid)
    originFiles.delete(uid)
  }

  function clearItems() {
    items.value = []
    originFiles.clear()
    pendingQueue.length = 0
  }

  function setItems(nextItems: ControlledUploadItem[]) {
    items.value = [...nextItems]
    originFiles.clear()
  }

  function resolveObjectKeys(): string[] {
    return items.value
      .filter((item) => item.status === 'done' && item.objectKey)
      .map((item) => item.objectKey as string)
  }

  async function uploadOne(task: PendingUpload) {
    const upload = options.uploadFn ?? uploadFile
    updateItem(task.uid, { status: 'uploading', errorMessage: undefined })
    try {
      const result = await upload(task.file)
      const mapped = options.mapResult?.(result) ?? {}
      updateItem(task.uid, {
        status: 'done',
        name: result.originalName || task.file.name,
        url: result.url,
        objectKey: result.objectKey,
        contentType: result.contentType,
        sizeBytes: result.sizeBytes,
        errorMessage: undefined,
        ...mapped,
      })
    } catch (error) {
      const errorMessage = getErrorMessage(error, '上传失败')
      updateItem(task.uid, {
        status: 'error',
        errorMessage,
      })
      message.error(`${task.file.name} 上传失败：${errorMessage}`)
    }
  }

  function pumpQueue() {
    while (activeUploads < concurrency.value && pendingQueue.length > 0) {
      const task = pendingQueue.shift()
      if (!task) {
        return
      }
      activeUploads += 1
      void uploadOne(task).finally(() => {
        activeUploads -= 1
        pumpQueue()
      })
    }
  }

  function enqueueFile(file: File): boolean {
    const validationError = validateFile(file)
    if (validationError) {
      message.error(`${file.name}：${validationError}`)
      return false
    }
    if (!canAddMore.value) {
      message.error(`最多只能上传 ${maxCount.value} 个文件`)
      return false
    }

    const uid = createUid()
    originFiles.set(uid, file)
    items.value = [
      ...items.value,
      {
        uid,
        name: file.name,
        status: 'uploading',
        sizeBytes: file.size,
        contentType: file.type,
      },
    ]
    pendingQueue.push({ uid, file })
    pumpQueue()
    return true
  }

  const beforeUpload: UploadProps['beforeUpload'] = (file) => {
    enqueueFile(file as File)
    return false
  }

  function retryUpload(uid: string) {
    const file = originFiles.get(uid)
    if (!file) {
      message.error('无法重试：原始文件已丢失，请重新选择')
      return
    }
    const validationError = validateFile(file)
    if (validationError) {
      message.error(validationError)
      return
    }
    pendingQueue.push({ uid, file })
    pumpQueue()
  }

  function handleRemove(file: UploadFile) {
    removeItem(file.uid)
  }

  return {
    policy,
    items,
    fileList,
    isUploading,
    canAddMore,
    maxCount,
    maxSizeBytes,
    allowedTypes,
    setPolicy,
    setItems,
    clearItems,
    beforeUpload,
    enqueueFile,
    retryUpload,
    handleRemove,
    removeItem,
    resolveObjectKeys,
    validateFile,
  }
}
