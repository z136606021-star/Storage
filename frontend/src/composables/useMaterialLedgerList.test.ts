import { describe, expect, it } from 'vitest'
import {
  buildMaterialLedgerQueryParams,
  defaultMaterialLedgerQuery,
} from '@/composables/useMaterialLedgerList'

describe('buildMaterialLedgerQueryParams', () => {
  it('omits undefined stock status from API params', () => {
    expect(
      buildMaterialLedgerQueryParams({
        ...defaultMaterialLedgerQuery(),
        stockStatus: undefined,
        category: undefined,
        name: '测试',
      }),
    ).toEqual({
      category: undefined,
      genericName: undefined,
      brand: undefined,
      name: '测试',
      model: undefined,
      binLocation: undefined,
      stockStatus: undefined,
    })
  })

  it('keeps IN_STOCK and ZERO_STOCK in API params', () => {
    expect(buildMaterialLedgerQueryParams(defaultMaterialLedgerQuery('IN_STOCK')).stockStatus).toBe(
      'IN_STOCK',
    )
    expect(
      buildMaterialLedgerQueryParams(defaultMaterialLedgerQuery('ZERO_STOCK')).stockStatus,
    ).toBe('ZERO_STOCK')
  })
})

describe('defaultMaterialLedgerQuery', () => {
  it('uses caller-specific initial stock status', () => {
    expect(defaultMaterialLedgerQuery('IN_STOCK').stockStatus).toBe('IN_STOCK')
    expect(defaultMaterialLedgerQuery('ZERO_STOCK').stockStatus).toBe('ZERO_STOCK')
  })
})
