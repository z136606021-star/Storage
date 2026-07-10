import { describe, expect, it } from 'vitest'
import { buildMaterialQueryParams } from '@/utils/warehouseMaterialTable'

describe('buildMaterialQueryParams', () => {
  it('omits blank filters so empty params mean all records', () => {
    expect(buildMaterialQueryParams({
      category: undefined,
      genericName: undefined,
      brand: undefined,
      name: '  ',
      model: undefined,
      binLocation: undefined,
    })).toEqual({
      category: undefined,
      genericName: undefined,
      brand: undefined,
      name: undefined,
      model: undefined,
      binLocation: undefined,
    })
  })

  it('treats legacy 全部 option as a concrete filter value', () => {
    expect(buildMaterialQueryParams({
      category: '全部',
      genericName: '全部',
      brand: '全部',
      name: '',
      model: '全部',
      binLocation: '全部',
    })).toEqual({
      category: '全部',
      genericName: '全部',
      brand: '全部',
      name: undefined,
      model: '全部',
      binLocation: '全部',
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
