import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { getErrorMessage } from '@/api/http'
import type { ImportResult } from '@/types/common'
import { downloadBlob } from '@/utils/download'
import { showImportResultMessage } from '@/utils/importResult'

export interface ExcelImportExportOptions<Q = unknown, BatchQ = Q> {
  exportFn: (params?: Q) => Promise<Blob>
  batchExportFn?: (params: BatchQ) => Promise<Blob>
  importFn: (file: File) => Promise<ImportResult>
  templateFn?: () => Promise<Blob>
  buildExportParams?: () => Q
  buildBatchExportParams?: (ids: number[]) => BatchQ
  getExportFilename: () => string
  getBatchExportFilename?: () => string
  getTemplateFilename?: () => string
  exportSuccessMessage?: string
  batchExportSuccessMessage?: string
  exportErrorMessage?: string
  batchExportErrorMessage?: string
  templateErrorMessage?: string
  importErrorMessage?: string
  onAfterImport?: () => void | Promise<void>
  onAfterExport?: () => void | Promise<void>
  showFirstImportError?: boolean
}

export function useExcelImportExport<Q = unknown, BatchQ = Q>(
  options: ExcelImportExportOptions<Q, BatchQ>,
) {
  const exporting = ref(false)
  const importing = ref(false)
  const batchExporting = ref(false)

  async function handleExport() {
    exporting.value = true
    try {
      const params = options.buildExportParams?.()
      const blob = await options.exportFn(params)
      downloadBlob(blob, options.getExportFilename())
      if (options.exportSuccessMessage) {
        message.success(options.exportSuccessMessage)
      }
      await options.onAfterExport?.()
    } catch (error) {
      message.error(getErrorMessage(error, options.exportErrorMessage ?? '导出失败，请稍后重试'))
    } finally {
      exporting.value = false
    }
  }

  async function handleBatchExport(ids: number[]) {
    if (!options.batchExportFn || !options.buildBatchExportParams) {
      return
    }
    batchExporting.value = true
    try {
      const blob = await options.batchExportFn(options.buildBatchExportParams(ids))
      downloadBlob(blob, options.getBatchExportFilename?.() ?? options.getExportFilename())
      message.success(options.batchExportSuccessMessage ?? '批量导出成功')
      await options.onAfterExport?.()
    } catch (error) {
      message.error(
        getErrorMessage(error, options.batchExportErrorMessage ?? '批量导出失败，请稍后重试'),
      )
    } finally {
      batchExporting.value = false
    }
  }

  async function handleDownloadTemplate() {
    if (!options.templateFn || !options.getTemplateFilename) {
      return
    }
    try {
      const blob = await options.templateFn()
      downloadBlob(blob, options.getTemplateFilename())
    } catch (error) {
      message.error(getErrorMessage(error, options.templateErrorMessage ?? '下载模板失败，请稍后重试'))
    }
  }

  async function handleImport(file: File) {
    importing.value = true
    try {
      const result = await options.importFn(file)
      showImportResultMessage(result, { showFirstError: options.showFirstImportError })
      await options.onAfterImport?.()
    } catch (error) {
      message.error(getErrorMessage(error, options.importErrorMessage ?? '导入失败，请稍后重试'))
    } finally {
      importing.value = false
    }
    return false
  }

  return {
    exporting,
    importing,
    batchExporting,
    handleExport,
    handleBatchExport,
    handleImport,
    handleDownloadTemplate,
  }
}
