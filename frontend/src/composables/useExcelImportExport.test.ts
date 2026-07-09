import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useExcelImportExport } from '@/composables/useExcelImportExport'

vi.mock('ant-design-vue', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))

vi.mock('@/utils/download', () => ({
  downloadBlob: vi.fn(),
}))

describe('useExcelImportExport', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('builds batch export params from selected ids', async () => {
    const batchExportFn = vi.fn().mockResolvedValue(new Blob(['xlsx']))
    const buildBatchExportParams = vi.fn((ids: number[]) => ({ ids }))

    const { handleBatchExport } = useExcelImportExport({
      exportFn: vi.fn(),
      batchExportFn,
      buildBatchExportParams,
      getExportFilename: () => 'export.xlsx',
    })

    await handleBatchExport([1, 2, 3])

    expect(buildBatchExportParams).toHaveBeenCalledWith([1, 2, 3])
    expect(batchExportFn).toHaveBeenCalledWith({ ids: [1, 2, 3] })
  })

  it('runs onAfterBatchExport after successful batch export', async () => {
    const onAfterBatchExport = vi.fn()

    const { handleBatchExport } = useExcelImportExport({
      exportFn: vi.fn(),
      batchExportFn: vi.fn().mockResolvedValue(new Blob(['xlsx'])),
      buildBatchExportParams: (ids) => ({ ids }),
      getExportFilename: () => 'export.xlsx',
      onAfterBatchExport,
    })

    await handleBatchExport([9])

    expect(onAfterBatchExport).toHaveBeenCalledTimes(1)
  })
})
