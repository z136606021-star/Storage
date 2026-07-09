import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import {
  projectedStockAfterOutbound,
  setSkipSafetyConfirm,
  shouldSkipSafetyConfirm,
  wouldTriggerSafetyWarning,
} from '@/composables/useMaterialIoSafetyHint'
import type { MaterialIoFormRow, MaterialIoSafetyHint } from '@/types/warehouse/materialIo'

describe('useMaterialIoSafetyHint helpers', () => {
  const storage = new Map<string, string>()

  beforeEach(() => {
    storage.clear()
    vi.stubGlobal('sessionStorage', {
      getItem: (key: string) => storage.get(key) ?? null,
      setItem: (key: string, value: string) => {
        storage.set(key, value)
      },
      removeItem: (key: string) => {
        storage.delete(key)
      },
    })
  })

  const stockContext = {
    ioType: ref<'IN' | 'OUT'>('OUT'),
    rows: ref<MaterialIoFormRow[]>([]),
    isEdit: ref(false),
    editingRecord: ref(null),
  }

  it('wouldTriggerSafetyWarning returns true when projected stock at or below safety', () => {
    const hint: MaterialIoSafetyHint = {
      materialLedgerId: 1,
      currentStock: 10,
      safetyQuantity: 5,
      warningEnabled: true,
    }
    expect(wouldTriggerSafetyWarning(hint, 4)).toBe(true)
    expect(wouldTriggerSafetyWarning(hint, 5)).toBe(true)
    expect(wouldTriggerSafetyWarning(hint, 6)).toBe(false)
  })

  it('wouldTriggerSafetyWarning does not warn when safety quantity is zero', () => {
    const hint: MaterialIoSafetyHint = {
      materialLedgerId: 1,
      currentStock: 10,
      safetyQuantity: 0,
      warningEnabled: false,
    }
    expect(wouldTriggerSafetyWarning(hint, 0)).toBe(false)
  })

  it('projectedStockAfterOutbound subtracts quantity from available stock', () => {
    stockContext.rows.value = [
      {
        key: '1',
        materialLedgerId: 1,
        stockQuantity: 10,
        quantity: 3,
      },
    ]
    const row = stockContext.rows.value[0]
    expect(projectedStockAfterOutbound(row, 0, stockContext)).toBe(7)
  })

  it('setSkipSafetyConfirm persists skip flag in sessionStorage', () => {
    expect(shouldSkipSafetyConfirm()).toBe(false)
    setSkipSafetyConfirm(true)
    expect(shouldSkipSafetyConfirm()).toBe(true)
    setSkipSafetyConfirm(false)
    expect(shouldSkipSafetyConfirm()).toBe(false)
  })
})
