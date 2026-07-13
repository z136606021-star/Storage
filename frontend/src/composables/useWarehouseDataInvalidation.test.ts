import { beforeEach, describe, expect, it } from 'vitest'
import {
  consumeMaterialLedgerDirty,
  markMaterialLedgerDirty,
  resetMaterialLedgerDirty,
} from '@/composables/useWarehouseDataInvalidation'

describe('useWarehouseDataInvalidation', () => {
  beforeEach(() => {
    resetMaterialLedgerDirty()
  })

  it('marks and consumes ledger dirty state once', () => {
    expect(consumeMaterialLedgerDirty()).toBe(false)

    markMaterialLedgerDirty()
    expect(consumeMaterialLedgerDirty()).toBe(true)
    expect(consumeMaterialLedgerDirty()).toBe(false)
  })
})
