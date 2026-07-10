import { describe, expect, it } from 'vitest'
import {
  materialIoFormValidationMessage,
  validateMaterialIoFormRows,
} from '@/composables/useMaterialIoFormValidation'
import type { MaterialIoFormRow } from '@/types/warehouse/materialIo'

function inboundRow(overrides: Partial<MaterialIoFormRow> = {}): MaterialIoFormRow {
  return {
    key: 'row-1',
    bomId: 1,
    binLocation: '1-1-1',
    quantity: 1,
    ...overrides,
  }
}

function outboundRow(overrides: Partial<MaterialIoFormRow> = {}): MaterialIoFormRow {
  return {
    key: 'row-1',
    materialLedgerId: 10,
    quantity: 1,
    stockQuantity: 5,
    ...overrides,
  }
}

describe('validateMaterialIoFormRows', () => {
  it('allows editing inbound record without bomId', () => {
    const error = validateMaterialIoFormRows({
      ioType: 'IN',
      isEdit: true,
      rows: [
        inboundRow({
          bomId: undefined,
          materialLedgerId: 10,
          category: '耗材',
          genericName: '密封圈',
          name: 'O型密封圈',
          model: 'OR-10',
          binLocation: '1-1-4',
          quantity: 2,
        }),
      ],
    })

    expect(error).toBeNull()
  })

  it('still requires bomId when creating inbound rows', () => {
    const error = validateMaterialIoFormRows({
      ioType: 'IN',
      isEdit: false,
      rows: [inboundRow({ bomId: undefined })],
    })

    expect(error).toEqual({
      rowIndex: 0,
      rowNo: 1,
      code: 'MISSING_BOM',
    })
    expect(materialIoFormValidationMessage(error!)).toBe('第 1 行请选择物料清单')
  })

  it('still requires bin location when creating inbound rows', () => {
    const error = validateMaterialIoFormRows({
      ioType: 'IN',
      isEdit: false,
      rows: [inboundRow({ binLocation: undefined })],
    })

    expect(error?.code).toBe('MISSING_BIN')
  })

  it('still requires ledger when creating outbound rows', () => {
    const error = validateMaterialIoFormRows({
      ioType: 'OUT',
      isEdit: false,
      rows: [outboundRow({ materialLedgerId: undefined })],
    })

    expect(error?.code).toBe('MISSING_LEDGER')
  })

  it('validates quantity for edit and create modes', () => {
    expect(
      validateMaterialIoFormRows({
        ioType: 'IN',
        isEdit: true,
        rows: [inboundRow({ bomId: undefined, quantity: 0 })],
      })?.code,
    ).toBe('INVALID_QUANTITY')

    expect(
      validateMaterialIoFormRows({
        ioType: 'IN',
        isEdit: false,
        rows: [inboundRow({ quantity: 0 })],
      })?.code,
    ).toBe('INVALID_QUANTITY')
  })

  it('keeps outbound stock validation in edit mode', () => {
    const error = validateMaterialIoFormRows({
      ioType: 'OUT',
      isEdit: true,
      rows: [outboundRow({ quantity: 8 })],
      availableStockForRow: () => 5,
    })

    expect(error).toEqual({
      rowIndex: 0,
      rowNo: 1,
      code: 'EXCEEDS_STOCK',
      availableStock: 5,
    })
  })
})
