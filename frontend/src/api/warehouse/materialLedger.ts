import { http } from '@/api/http'
import type { PageResult } from '@/types/common'
import type {
  BomCatalogItem,
  FilterLinkageQuery,
  FilterOptions,
  MaterialLedger,
  MaterialQuery,
} from '@/types/warehouse/materialLedger'

export type MaterialExportQuery = Omit<MaterialQuery, 'page' | 'pageSize'>

export async function fetchMaterialLedgerPage(
  query: MaterialQuery,
): Promise<PageResult<MaterialLedger>> {
  const { data } = await http.get<PageResult<MaterialLedger>>('/materials', {
    params: query,
  })
  return data
}

export async function fetchFilterOptions(
  query: FilterLinkageQuery = {},
): Promise<FilterOptions> {
  const { data } = await http.get<FilterOptions>('/materials/filter-options', {
    params: query,
  })
  return data
}

export async function fetchMaterialBinCodes(): Promise<string[]> {
  const { data } = await http.get<string[]>('/materials/bin-codes')
  return data
}

export async function fetchMaterialBomCatalog(): Promise<BomCatalogItem[]> {
  const { data } = await http.get<BomCatalogItem[]>('/materials/bom-catalog')
  return data
}

export async function fetchMaterialLedgerDetail(id: number): Promise<MaterialLedger> {
  const { data } = await http.get<MaterialLedger>(`/materials/${id}`)
  return data
}

export async function exportMaterialLedger(query: MaterialExportQuery): Promise<Blob> {
  const { data } = await http.get<Blob>('/materials/export', {
    params: query,
    paramsSerializer: {
      indexes: null,
    },
    responseType: 'blob',
  })
  return data
}
