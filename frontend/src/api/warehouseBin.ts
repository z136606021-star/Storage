import { http } from '@/api/http'
import type { PageResult, ImportResult } from '@/types/common'
import type {
  WarehouseBin,
  WarehouseBinExportQuery,
  WarehouseBinQuery,
  WarehouseBinSavePayload,
} from '@/types/warehouseBin'

export async function fetchWarehouseBinPage(
  query: WarehouseBinQuery,
): Promise<PageResult<WarehouseBin>> {
  const { data } = await http.get<PageResult<WarehouseBin>>('/warehouse-bins', {
    params: query,
  })
  return data
}

export async function fetchWarehouseBinCodes(): Promise<string[]> {
  const { data } = await http.get<string[]>('/warehouse-bins/codes')
  return data
}

export async function fetchWarehouseBinDetail(id: number): Promise<WarehouseBin> {
  const { data } = await http.get<WarehouseBin>(`/warehouse-bins/${id}`)
  return data
}

export async function createWarehouseBin(
  payload: WarehouseBinSavePayload,
): Promise<WarehouseBin> {
  const { data } = await http.post<WarehouseBin>('/warehouse-bins', payload)
  return data
}

export async function updateWarehouseBin(
  id: number,
  payload: WarehouseBinSavePayload,
): Promise<WarehouseBin> {
  const { data } = await http.put<WarehouseBin>(`/warehouse-bins/${id}`, payload)
  return data
}

export async function deleteWarehouseBin(id: number): Promise<void> {
  await http.delete(`/warehouse-bins/${id}`)
}

export async function batchDeleteWarehouseBins(ids: number[]): Promise<void> {
  await http.delete('/warehouse-bins/batch', { data: { ids } })
}

export async function exportWarehouseBins(query: WarehouseBinExportQuery): Promise<Blob> {
  const { data } = await http.get<Blob>('/warehouse-bins/export', {
    params: query,
    paramsSerializer: {
      indexes: null,
    },
    responseType: 'blob',
  })
  return data
}

export async function downloadWarehouseBinImportTemplate(): Promise<Blob> {
  const { data } = await http.get<Blob>('/warehouse-bins/import-template', {
    responseType: 'blob',
  })
  return data
}

export async function importWarehouseBins(file: File): Promise<ImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ImportResult>('/warehouse-bins/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
