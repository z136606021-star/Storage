import { http } from '@/api/http'
import type { PageResult, ImportResult } from '@/types/common'
import type {
  SysCustomer,
  SysCustomerExportQuery,
  SysCustomerQuery,
  SysCustomerSavePayload,
} from '@/types/system/customer'

export async function fetchCustomerPage(query: SysCustomerQuery): Promise<PageResult<SysCustomer>> {
  const { data } = await http.get<PageResult<SysCustomer>>('/system/customers', { params: query })
  return data
}

export async function fetchCustomerDetail(id: number): Promise<SysCustomer> {
  const { data } = await http.get<SysCustomer>(`/system/customers/${id}`)
  return data
}

export async function createCustomer(payload: SysCustomerSavePayload): Promise<SysCustomer> {
  const { data } = await http.post<SysCustomer>('/system/customers', payload)
  return data
}

export async function updateCustomer(
  id: number,
  payload: SysCustomerSavePayload,
): Promise<SysCustomer> {
  const { data } = await http.put<SysCustomer>(`/system/customers/${id}`, payload)
  return data
}

export async function deleteCustomer(id: number): Promise<void> {
  await http.delete(`/system/customers/${id}`)
}

export async function batchDeleteCustomers(ids: number[]): Promise<void> {
  await http.delete('/system/customers/batch', { data: { ids } })
}

export async function exportCustomers(query: SysCustomerExportQuery): Promise<Blob> {
  const { data } = await http.get<Blob>('/system/customers/export', {
    params: query,
    paramsSerializer: { indexes: null },
    responseType: 'blob',
  })
  return data
}

export async function downloadCustomerImportTemplate(): Promise<Blob> {
  const { data } = await http.get<Blob>('/system/customers/import-template', {
    responseType: 'blob',
  })
  return data
}

export async function importCustomers(file: File): Promise<ImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ImportResult>('/system/customers/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
