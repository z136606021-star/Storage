import type { Ref } from 'vue'
import type { IoType, MaterialIoFormRow, MaterialIoRecord } from '@/types/materialIo'

export interface MaterialIoStockContext {
  ioType: Ref<IoType>
  rows: Ref<MaterialIoFormRow[]>
  isEdit: Ref<boolean>
  editingRecord: Ref<MaterialIoRecord | null | undefined>
}

export function availableStockForRow(
  row: MaterialIoFormRow,
  rowIndex: number,
  context: MaterialIoStockContext,
): number | null {
  if (row.stockQuantity == null) {
    return null
  }

  const { ioType, rows, isEdit, editingRecord } = context

  if (isEdit.value && editingRecord.value && row.materialLedgerId === editingRecord.value.materialLedgerId) {
    let available = row.stockQuantity
    if (editingRecord.value.ioType === 'OUT') {
      available += editingRecord.value.quantity
    } else {
      available -= editingRecord.value.quantity
    }
    return available
  }

  if (ioType.value !== 'OUT') {
    return row.stockQuantity
  }

  const simulated = new Map<number, number>()
  for (let i = 0; i < rowIndex; i += 1) {
    const prev = rows.value[i]
    if (!prev.materialLedgerId || prev.stockQuantity == null || !prev.quantity) {
      continue
    }
    const ledgerId = prev.materialLedgerId
    if (!simulated.has(ledgerId)) {
      simulated.set(ledgerId, prev.stockQuantity)
    }
    const stock = simulated.get(ledgerId)!
    simulated.set(ledgerId, stock - prev.quantity)
  }

  if (row.materialLedgerId && simulated.has(row.materialLedgerId)) {
    return simulated.get(row.materialLedgerId)!
  }

  return row.stockQuantity
}

export function useMaterialIoStock(context: MaterialIoStockContext) {
  function getDisplayStock(row: MaterialIoFormRow, rowIndex: number): number | null {
    return availableStockForRow(row, rowIndex, context)
  }

  function getStockColumnTitle(): string {
    return context.ioType.value === 'OUT' ? '可用库存' : '库存'
  }

  return {
    availableStockForRow: (row: MaterialIoFormRow, rowIndex: number) =>
      availableStockForRow(row, rowIndex, context),
    getDisplayStock,
    getStockColumnTitle,
  }
}
