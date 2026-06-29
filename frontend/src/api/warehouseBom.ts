import { http } from '@/api/http'
import type { PageResult, ImportResult } from '@/types/common'
import type {
  BomFilterLinkageQuery,
  BomFilterOptions,
  WarehouseBom,
  WarehouseBomExportQuery,
  WarehouseBomQuery,
  WarehouseBomSavePayload,
} from '@/types/warehouseBom'

export async function fetchWarehouseBomPage(
  query: WarehouseBomQuery,
): Promise<PageResult<WarehouseBom>> {
  const { data } = await http.get<PageResult<WarehouseBom>>('/warehouse-boms', {
    params: query,
  })
  return data
}

export async function fetchBomFilterOptions(
  query: BomFilterLinkageQuery = {},
): Promise<BomFilterOptions> {
  const { data } = await http.get<BomFilterOptions>('/warehouse-boms/filter-options', {
    params: query,
  })
  return data
}

export async function fetchWarehouseBomDetail(id: number): Promise<WarehouseBom> {
  const { data } = await http.get<WarehouseBom>(`/warehouse-boms/${id}`)
  return data
}

export async function createWarehouseBom(
  payload: WarehouseBomSavePayload,
): Promise<WarehouseBom> {
  const { data } = await http.post<WarehouseBom>('/warehouse-boms', payload)
  return data
}

export async function updateWarehouseBom(
  id: number,
  payload: WarehouseBomSavePayload,
): Promise<WarehouseBom> {
  const { data } = await http.put<WarehouseBom>(`/warehouse-boms/${id}`, payload)
  return data
}

export async function deleteWarehouseBom(id: number): Promise<void> {
  await http.delete(`/warehouse-boms/${id}`)
}

export async function batchDeleteWarehouseBoms(ids: number[]): Promise<void> {
  await http.delete('/warehouse-boms/batch', { data: { ids } })
}

export async function exportWarehouseBoms(query: WarehouseBomExportQuery): Promise<Blob> {
  const { data } = await http.get<Blob>('/warehouse-boms/export', {
    params: query,
    paramsSerializer: {
      indexes: null,
    },
    responseType: 'blob',
  })
  return data
}

export async function downloadWarehouseBomImportTemplate(): Promise<Blob> {
  const { data } = await http.get<Blob>('/warehouse-boms/import-template', {
    responseType: 'blob',
  })
  return data
}

export async function importWarehouseBoms(file: File): Promise<ImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ImportResult>('/warehouse-boms/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
