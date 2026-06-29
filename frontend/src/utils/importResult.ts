import { message } from 'ant-design-vue'
import type { ImportResult } from '@/types/common'

export interface ShowImportResultOptions {
  showFirstError?: boolean
}

export function showImportResultMessage(result: ImportResult, options: ShowImportResultOptions = {}) {
  if (result.failCount === 0 && result.successCount === 0) {
    message.warning('无有效数据行')
    return
  }

  if (result.failCount === 0) {
    message.success(`导入成功，共 ${result.successCount} 条`)
    return
  }

  if (options.showFirstError) {
    const firstError = result.errors[0]
    const detail = firstError ? `第 ${firstError.row} 行：${firstError.message}` : ''
    message.warning(
      `导入完成：成功 ${result.successCount} 条，失败 ${result.failCount} 条。${detail}`,
    )
    return
  }

  message.warning(`导入完成：成功 ${result.successCount} 条，失败 ${result.failCount} 条`)
}
