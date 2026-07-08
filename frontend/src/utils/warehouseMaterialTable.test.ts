import { describe, expect, it } from 'vitest'
import { ALL_OPTION } from '@/constants/filter'
import { buildMaterialQueryParams } from '@/utils/warehouseMaterialTable'

describe('buildMaterialQueryParams', () => {
  it('omits all-option filters so empty params mean all records', () => {
    expect(buildMaterialQueryParams({
      category: ALL_OPTION,
      genericName: ALL_OPTION,
      brand: ALL_OPTION,
      name: '  ',
      model: ALL_OPTION,
      binLocation: ALL_OPTION,
    })).toEqual({
      category: undefined,
      genericName: undefined,
      brand: undefined,
      name: undefined,
      model: undefined,
      binLocation: undefined,
    })
  })

  it('keeps concrete filter values and trims fuzzy name', () => {
    expect(buildMaterialQueryParams({
      category: '电子',
      genericName: '电阻',
      brand: 'YAGEO',
      name: '  贴片电阻  ',
      model: '0805-10K',
      binLocation: '1-2-3',
    })).toEqual({
      category: '电子',
      genericName: '电阻',
      brand: 'YAGEO',
      name: '贴片电阻',
      model: '0805-10K',
      binLocation: '1-2-3',
    })
  })
})
