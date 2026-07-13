export interface SysUser {
  id: number
  username: string
  displayName: string
  email?: string
  phone?: string
  status: number
  roleIds: number[]
  roleCodes: string[]
  roleNames?: string[]
  createdAt?: string
  updatedAt?: string
}

export interface SysUserQuery {
  page?: number
  pageSize?: number
  keyword?: string
  username?: string
  displayName?: string
  email?: string
  roleId?: number
  status?: number
}

export interface SysUserSave {
  username: string
  displayName: string
  email?: string
  phone?: string
  password?: string
  status: number
  roleIds: number[]
}

export interface SysRole {
  id: number
  code: string
  name: string
  status: number
  menuIds: number[]
  permissions?: string[]
  createdAt?: string
}

export interface SysRoleSave {
  code: string
  name: string
  status: number
  menuIds: number[]
}

export interface SysMenu {
  id: number
  parentId: number | null
  menuType: 'TOP' | 'SUB' | 'BUTTON' | 'CATALOG' | 'MENU'
  name: string
  permission: string | null
  path: string | null
  componentKey?: string | null
  icon: string | null
  visible: number
  sortOrder: number
  children?: SysMenu[]
}

export interface SysMenuSave {
  parentId: number | null
  menuType: 'TOP' | 'SUB' | 'BUTTON' | 'CATALOG' | 'MENU'
  name: string
  permission?: string
  path?: string
  componentKey?: string
  icon?: string
  visible: number
  sortOrder: number
}

export interface NavMenuNode {
  key: string
  label: string
  path?: string
  permission?: string | null
  componentKey?: string | null
  icon?: string
  visible?: number
  children?: NavMenuNode[]
}

export interface UserPermissions {
  menuTree: SysMenu[]
  checkedMenuIds: number[]
}

export interface RegisterRequest {
  username: string
  password: string
  displayName: string
  email: string
}
