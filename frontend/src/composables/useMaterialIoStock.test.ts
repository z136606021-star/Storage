import { describe, expect, it } from 'vitest'
import { ref } from 'vue'
import {
  availableStockForRow,
  useMaterialIoStock,
  type MaterialIoStockContext,
} from '@/composables/useMaterialIoStock'
import type { IoType, MaterialIoFormRow, MaterialIoRecord } from '@/types/warehouse/materialIo'

function createContext(
  ioType: IoType,
  rows: MaterialIoFormRow[],
  isEdit = false,
  editingRecord: MaterialIoRecord | null = null,
): MaterialIoStockContext {
  return {
    ioType: ref(ioType),
    rows: ref(rows),
    isEdit: ref(isEdit),
    editingRecord: ref(editingRecord),
  }
}

function row(
  key: string,
  ledgerId: number,
  stock: number | undefined,
  quantity: number,
): MaterialIoFormRow {
  return {
    key,
    materialLedgerId: ledgerId,
    stockQuantity: stock,
    quantity,
  }
}

describe('availableStockForRow', () => {
  it('returns null when stockQuantity is null', () => {
    const context = createContext('OUT', [row('a', 1, undefined, 1)])
    expect(availableStockForRow(context.rows.value[0], 0, context)).toBeNull()
  })

  it('returns stockQuantity in inbound mode', () => {
    const context = createContext('IN', [row('a', 1, 10, 3)])
    expect(availableStockForRow(context.rows.value[0], 0, context)).toBe(10)
  })

  it('deducts prior outbound rows for same ledger', () => {
    const context = createContext('OUT', [
      row('a', 1, 10, 3),
      row('b', 1, 10, 4),
    ])
    expect(availableStockForRow(context.rows.value[1], 1, context)).toBe(7)
  })

  it('adds back original outbound quantity when editing same ledger', () => {
    const editingRecord = {
      id: 99,
      materialLedgerId: 1,
      ioType: 'OUT' as const,
      quantity: 5,
    } as MaterialIoRecord
    const context = createContext(
      'OUT',
      [row('a', 1, 10, 3)],
      true,
      editingRecord,
    )
    expect(availableStockForRow(context.rows.value[0], 0, context)).toBe(15)
  })

  it('subtracts original inbound quantity when editing same ledger inbound record', () => {
    const editingRecord = {
      id: 99,
      materialLedgerId: 1,
      ioType: 'IN' as const,
      quantity: 4,
    } as MaterialIoRecord
    const context = createContext(
      'OUT',
      [row('a', 1, 10, 3)],
      true,
      editingRecord,
    )
    expect(availableStockForRow(context.rows.value[0], 0, context)).toBe(6)
  })

  it('uses outbound simulation when edit row targets different ledger', () => {
    const editingRecord = {
      id: 99,
      materialLedgerId: 1,
      ioType: 'OUT' as const,
      quantity: 5,
    } as MaterialIoRecord
    const context = createContext(
      'OUT',
      [row('a', 2, 8, 2)],
      true,
      editingRecord,
    )
    expect(availableStockForRow(context.rows.value[0], 0, context)).toBe(8)
  })
})

describe('useMaterialIoStock', () => {
  it('getStockColumnTitle reflects inbound vs outbound mode', () => {
    const inboundContext = createContext('IN', [row('a', 1, 10, 1)])
    const outboundContext = createContext('OUT', [row('a', 1, 10, 1)])

    expect(useMaterialIoStock(inboundContext).getStockColumnTitle()).toBe('库存')
    expect(useMaterialIoStock(outboundContext).getStockColumnTitle()).toBe('可用库存')
  })
})
