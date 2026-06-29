import { Modal, message } from 'ant-design-vue'
import { getErrorMessage } from '@/api/http'

export interface ConfirmDeleteOptions {
  title: string
  content: string
  okText?: string
  cancelText?: string
  successMessage?: string
  errorMessage?: string
  onDelete: () => Promise<void>
  onSuccess?: () => void | Promise<void>
}

export function confirmDelete(options: ConfirmDeleteOptions) {
  Modal.confirm({
    title: options.title,
    content: options.content,
    okText: options.okText ?? '删除',
    okType: 'danger',
    cancelText: options.cancelText ?? '取消',
    async onOk() {
      try {
        await options.onDelete()
        if (options.successMessage) {
          message.success(options.successMessage)
        }
        await options.onSuccess?.()
      } catch (error) {
        message.error(getErrorMessage(error, options.errorMessage ?? '删除失败，请稍后重试'))
      }
    },
  })
}

export interface ConfirmBatchDeleteOptions {
  count: number
  entityLabel?: string
  content?: string
  title?: string
  okText?: string
  cancelText?: string
  successMessage?: string
  errorMessage?: string
  onDelete: () => Promise<void>
  onSuccess?: () => void | Promise<void>
}

export function confirmBatchDelete(options: ConfirmBatchDeleteOptions) {
  const label = options.entityLabel ?? '记录'
  confirmDelete({
    title: options.title ?? '确认批量删除',
    content: options.content ?? `确定删除选中的 ${options.count} 条${label}吗？`,
    okText: options.okText ?? '删除',
    cancelText: options.cancelText ?? '取消',
    successMessage: options.successMessage ?? '批量删除成功',
    errorMessage: options.errorMessage ?? '批量删除失败，请稍后重试',
    onDelete: options.onDelete,
    onSuccess: options.onSuccess,
  })
}
