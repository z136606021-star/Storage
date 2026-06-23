import axios from 'axios'
import type {
  FilterLinkageQuery,
  FilterOptions,
  ImportResult,
  MaterialLedger,
  MaterialQuery,
  MaterialSavePayload,
  PageResult,
} from '@/types/materialLedger'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

export type MaterialExportQuery = Omit<MaterialQuery, 'page' | 'pageSize'>

function getErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error) && error.response?.data?.message) {
    return String(error.response.data.message)
  }
  return fallback
}

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

export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

export { getErrorMessage }
