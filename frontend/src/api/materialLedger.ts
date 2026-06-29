import { http } from '@/api/http'
import type { PageResult, ImportResult } from '@/types/common'
import type {
  BomCatalogItem,
  FilterLinkageQuery,
  FilterOptions,
  MaterialLedger,
  MaterialQuery,
  MaterialSavePayload,
} from '@/types/materialLedger'

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

export async function createMaterialLedger(
  payload: MaterialSavePayload,
): Promise<MaterialLedger> {
  const { data } = await http.post<MaterialLedger>('/materials', payload)
  return data
}

export async function updateMaterialLedger(
  id: number,
  payload: MaterialSavePayload,
): Promise<MaterialLedger> {
  const { data } = await http.put<MaterialLedger>(`/materials/${id}`, payload)
  return data
}

export async function deleteMaterialLedger(id: number): Promise<void> {
  await http.delete(`/materials/${id}`)
}

export async function batchDeleteMaterialLedger(ids: number[]): Promise<void> {
  await http.delete('/materials/batch', { data: { ids } })
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

export async function downloadImportTemplate(): Promise<Blob> {
  const { data } = await http.get<Blob>('/materials/import-template', {
    responseType: 'blob',
  })
  return data
}

export async function importMaterialLedger(file: File): Promise<ImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ImportResult>('/materials/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
