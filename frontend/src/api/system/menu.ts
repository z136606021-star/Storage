import { http } from '@/api/http'
import type { SysMenu, SysMenuSave, NavMenuNode } from '@/types/system'

export function fetchMenuTree() {
  return http.get<SysMenu[]>('/system/menus/tree')
}

export function createMenu(data: SysMenuSave) {
  return http.post<SysMenu>('/system/menus', data)
}

export function updateMenu(id: number, data: SysMenuSave) {
  return http.put<SysMenu>(`/system/menus/${id}`, data)
}

export function deleteMenu(id: number) {
  return http.delete<void>(`/system/menus/${id}`)
}

export function fetchNavTree() {
  return http.get<NavMenuNode[]>('/menus/nav-tree')
}
