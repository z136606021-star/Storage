import { http } from '@/api/http'
import type { PageResult } from '@/types/common'
import type { ImportResult } from '@/types/common'
import type { SysUser, SysUserQuery, SysUserSave, UserPermissions } from '@/types/system'

export type SysUserExportQuery = Omit<SysUserQuery, 'page' | 'pageSize'>

export function fetchUserPage(params: SysUserQuery) {
  return http.get<PageResult<SysUser>>('/system/users', { params })
}

export function fetchUserDetail(id: number) {
  return http.get<SysUser>(`/system/users/${id}`)
}

export function fetchUserPermissions(id: number) {
  return http.get<UserPermissions>(`/system/users/${id}/permissions`)
}

export function createUser(data: SysUserSave) {
  return http.post<SysUser>('/system/users', data)
}

export function updateUser(id: number, data: SysUserSave) {
  return http.put<SysUser>(`/system/users/${id}`, data)
}

export function deleteUser(id: number) {
  return http.delete<void>(`/system/users/${id}`)
}

export function resetUserPassword(id: number, password: string) {
  return http.put<void>(`/system/users/${id}/password`, { password })
}

export function updateUserStatus(id: number, status: number) {
  return http.put<void>(`/system/users/${id}/status`, { status })
}

export async function exportUsers(query: SysUserExportQuery = {}): Promise<Blob> {
  const { data } = await http.get<Blob>('/system/users/export', {
    params: query,
    responseType: 'blob',
  })
  return data
}

export async function downloadUserImportTemplate(): Promise<Blob> {
  const { data } = await http.get<Blob>('/system/users/import-template', {
    responseType: 'blob',
  })
  return data
}

export async function importUsers(file: File): Promise<ImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ImportResult>('/system/users/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
