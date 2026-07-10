import type { IoType, MaterialIoFormRow } from '@/types/warehouse/materialIo'

export type MaterialIoFormValidationErrorCode =
  | 'EMPTY_ROWS'
  | 'MISSING_BOM'
  | 'MISSING_BIN'
  | 'MISSING_LEDGER'
  | 'INVALID_QUANTITY'
  | 'EXCEEDS_STOCK'

export interface MaterialIoFormValidationError {
  rowIndex: number
  rowNo: number
  code: MaterialIoFormValidationErrorCode
  availableStock?: number
}

export interface ValidateMaterialIoFormRowsOptions {
  ioType: IoType
  isEdit: boolean
  rows: MaterialIoFormRow[]
  availableStockForRow?: (row: MaterialIoFormRow, rowIndex: number) => number | null
}

export function materialIoFormValidationMessage(error: MaterialIoFormValidationError): string {
  switch (error.code) {
    case 'EMPTY_ROWS':
      return '请至少添加一行物料'
    case 'MISSING_BOM':
      return `第 ${error.rowNo} 行请选择物料清单`
    case 'MISSING_BIN':
      return `第 ${error.rowNo} 行请选择Bin位`
    case 'MISSING_LEDGER':
      return `第 ${error.rowNo} 行请选择物料台账`
    case 'INVALID_QUANTITY':
      return `第 ${error.rowNo} 行数量必须大于 0`
    case 'EXCEEDS_STOCK':
      return `第 ${error.rowNo} 行出库数量不能超过可用库存 ${error.availableStock ?? 0}`
    default:
      return '表单校验失败'
  }
}

export function validateMaterialIoFormRows(
  options: ValidateMaterialIoFormRowsOptions,
): MaterialIoFormValidationError | null {
  const { ioType, isEdit, rows, availableStockForRow } = options

  if (rows.length === 0) {
    return { rowIndex: -1, rowNo: 0, code: 'EMPTY_ROWS' }
  }

  for (let i = 0; i < rows.length; i += 1) {
    const row = rows[i]
    const rowNo = i + 1

    if (!isEdit) {
      if (ioType === 'IN') {
        if (!row.bomId) {
          return { rowIndex: i, rowNo, code: 'MISSING_BOM' }
        }
        if (!row.binLocation) {
          return { rowIndex: i, rowNo, code: 'MISSING_BIN' }
        }
      } else if (!row.materialLedgerId) {
        return { rowIndex: i, rowNo, code: 'MISSING_LEDGER' }
      }
    }

    if (!row.quantity || row.quantity < 1) {
      return { rowIndex: i, rowNo, code: 'INVALID_QUANTITY' }
    }

    if (ioType === 'OUT' && availableStockForRow) {
      const available = availableStockForRow(row, i)
      if (available != null && row.quantity > available) {
        return { rowIndex: i, rowNo, code: 'EXCEEDS_STOCK', availableStock: available }
      }
    }
  }

  return null
}
