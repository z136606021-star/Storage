import { http } from '@/api/http'
import type { ImportResult } from '@/types/materialLedger'
import type { SysRole, SysRoleSave } from '@/types/system'

export function fetchRoles() {
  return http.get<SysRole[]>('/system/roles')
}

export function fetchRoleDetail(id: number) {
  return http.get<SysRole>(`/system/roles/${id}`)
}

export function createRole(data: SysRoleSave) {
  return http.post<SysRole>('/system/roles', data)
}

export function updateRole(id: number, data: SysRoleSave) {
  return http.put<SysRole>(`/system/roles/${id}`, data)
}

export function deleteRole(id: number) {
  return http.delete<void>(`/system/roles/${id}`)
}

export async function exportRoles(): Promise<Blob> {
  const { data } = await http.get<Blob>('/system/roles/export', {
    responseType: 'blob',
  })
  return data
}

export async function downloadRoleImportTemplate(): Promise<Blob> {
  const { data } = await http.get<Blob>('/system/roles/import-template', {
    responseType: 'blob',
  })
  return data
}

export async function importRoles(file: File): Promise<ImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ImportResult>('/system/roles/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
