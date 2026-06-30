import type { WarehouseMaterialQuery } from '@/composables/useWarehouseMaterialFilters'

export interface MaterialQueryParams {
  category?: string
  genericName?: string
  brand?: string
  name?: string
  model?: string
  binLocation?: string
}

const IDENTITY_FIELD_META = [
  { key: 'category', title: '品类' },
  { key: 'genericName', title: '统称' },
  { key: 'brand', title: '品牌' },
  { key: 'name', title: '名称' },
  { key: 'model', title: '型号' },
  { key: 'binLocation', title: 'Bin位' },
] as const

type IdentityPreset = 'ledgerList' | 'ioList' | 'picker' | 'batchForm' | 'statsWarning'

const IDENTITY_WIDTHS: Record<IdentityPreset, Record<string, number>> = {
  ledgerList: {
    category: 100,
    genericName: 100,
    brand: 80,
    name: 140,
    model: 100,
    binLocation: 80,
  },
  ioList: {
    category: 100,
    genericName: 100,
    brand: 80,
    name: 140,
    model: 100,
    binLocation: 80,
  },
  picker: {
    category: 90,
    genericName: 90,
    brand: 80,
    name: 120,
    model: 90,
    binLocation: 80,
  },
  batchForm: {
    category: 90,
    genericName: 90,
    brand: 80,
    name: 120,
    model: 90,
    binLocation: 80,
  },
  statsWarning: {
    category: 90,
    genericName: 90,
    brand: 80,
    name: 120,
    model: 90,
    binLocation: 80,
  },
}

export function buildMaterialQueryParams(queryForm: WarehouseMaterialQuery): MaterialQueryParams {
  return {
    category: queryForm.category,
    genericName: queryForm.genericName,
    brand: queryForm.brand,
    name: queryForm.name.trim() || undefined,
    model: queryForm.model,
    binLocation: queryForm.binLocation,
  }
}

export function materialIdentityColumns(preset: IdentityPreset) {
  const widths = IDENTITY_WIDTHS[preset]
  const withDataIndex = preset !== 'batchForm'
  const ellipsis = preset !== 'batchForm'

  return IDENTITY_FIELD_META.map(({ key, title }) => ({
    title,
    key,
    ...(withDataIndex ? { dataIndex: key } : {}),
    width: widths[key],
    ...(ellipsis ? { ellipsis: true } : {}),
    ...(key === 'binLocation' ? { align: 'center' as const } : {}),
  }))
}
