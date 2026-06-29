import { http } from '@/api/http'
import type { ImportResult, PageResult } from '@/types/common'
import type { FilterLinkageQuery, FilterOptions } from '@/types/materialLedger'
import type {
  MaterialIoBatchSavePayload,
  MaterialIoExportQuery,
  MaterialIoQuery,
  MaterialIoRecord,
  MaterialIoSafetyHint,
  MaterialIoUpdatePayload,
} from '@/types/materialIo'

export async function fetchMaterialIoPage(
  query: MaterialIoQuery,
): Promise<PageResult<MaterialIoRecord>> {
  const { data } = await http.get<PageResult<MaterialIoRecord>>('/material-io', {
    params: query,
  })
  return data
}

export async function fetchMaterialIoFilterOptions(
  query: FilterLinkageQuery = {},
): Promise<FilterOptions> {
  const { data } = await http.get<FilterOptions>('/material-io/filter-options', {
    params: query,
  })
  return data
}

export async function fetchMaterialIoDetail(id: number): Promise<MaterialIoRecord> {
  const { data } = await http.get<MaterialIoRecord>(`/material-io/${id}`)
  return data
}

export async function fetchMaterialIoSafetyHints(
  materialLedgerIds: number[],
): Promise<MaterialIoSafetyHint[]> {
  if (materialLedgerIds.length === 0) {
    return []
  }
  const { data } = await http.get<MaterialIoSafetyHint[]>('/material-io/safety-hints', {
    params: { materialLedgerIds },
    paramsSerializer: {
      indexes: null,
    },
  })
  return data
}

export async function batchCreateMaterialIo(
  payload: MaterialIoBatchSavePayload,
): Promise<MaterialIoRecord[]> {
  const { data } = await http.post<MaterialIoRecord[]>('/material-io/batch', payload)
  return data
}

export async function updateMaterialIo(
  id: number,
  payload: MaterialIoUpdatePayload,
): Promise<MaterialIoRecord> {
  const { data } = await http.put<MaterialIoRecord>(`/material-io/${id}`, payload)
  return data
}

export async function deleteMaterialIo(id: number): Promise<void> {
  await http.delete(`/material-io/${id}`)
}

export async function batchDeleteMaterialIo(ids: number[]): Promise<void> {
  await http.delete('/material-io/batch', { data: { ids } })
}

export async function exportMaterialIo(query: MaterialIoExportQuery): Promise<Blob> {
  const { data } = await http.get<Blob>('/material-io/export', {
    params: query,
    paramsSerializer: {
      indexes: null,
    },
    responseType: 'blob',
  })
  return data
}

export async function downloadMaterialIoImportTemplate(): Promise<Blob> {
  const { data } = await http.get<Blob>('/material-io/import-template', {
    responseType: 'blob',
  })
  return data
}

export async function importMaterialIo(file: File): Promise<ImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ImportResult>('/material-io/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
