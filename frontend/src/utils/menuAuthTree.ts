import type { SysMenu } from '@/types/system'

export type MenuType = 'TOP' | 'SUB' | 'BUTTON' | 'CATALOG' | 'MENU'

const MENU_TYPE_LABELS: Record<string, string> = {
  TOP: '一级菜单',
  SUB: '子菜单',
  BUTTON: '按钮权限',
  CATALOG: '一级菜单',
  MENU: '子菜单',
}

export function menuTypeLabel(menuType: string): string {
  return MENU_TYPE_LABELS[menuType] ?? menuType
}

export function isTopMenu(menuType: string): boolean {
  return menuType === 'TOP' || menuType === 'CATALOG'
}

export function isSubMenu(menuType: string): boolean {
  return menuType === 'SUB' || menuType === 'MENU'
}

export function isButtonMenu(menuType: string): boolean {
  return menuType === 'BUTTON'
}

export function buildMenuAuthTreeNodes(
  menus: SysMenu[],
): Array<{ title: string; key: number; children?: ReturnType<typeof buildMenuAuthTreeNodes> }> {
  return menus.map((menu) => ({
    title: `${menu.name} [${menuTypeLabel(menu.menuType)}]${menu.permission ? ` (${menu.permission})` : ''}`,
    key: menu.id,
    children: menu.children?.length ? buildMenuAuthTreeNodes(menu.children) : undefined,
  }))
}

export function collectDescendantIds(menus: SysMenu[], rootId: number): Set<number> {
  const ids = new Set<number>()
  const collectChildren = (node: SysMenu) => {
    ids.add(node.id)
    for (const child of node.children ?? []) {
      collectChildren(child)
    }
  }
  const findAndCollect = (nodes: SysMenu[]): boolean => {
    for (const node of nodes) {
      if (node.id === rootId) {
        collectChildren(node)
        return true
      }
      if (node.children?.length && findAndCollect(node.children)) {
        return true
      }
    }
    return false
  }
  findAndCollect(menus)
  return ids
}

export function buildParentOptions(
  menus: SysMenu[],
  excludeIds: Set<number>,
  allowedParentTypes: Set<string>,
): Array<{ title: string; value: number; disabled?: boolean; children?: ReturnType<typeof buildParentOptions> }> {
  return menus
    .filter((menu) => !excludeIds.has(menu.id))
    .map((menu) => ({
      title: `${menu.name} [${menuTypeLabel(menu.menuType)}]`,
      value: menu.id,
      disabled: !allowedParentTypes.has(menu.menuType),
      children: menu.children?.length
        ? buildParentOptions(menu.children, excludeIds, allowedParentTypes)
        : undefined,
    }))
}

export function allowedParentTypesFor(menuType: MenuType): Set<string> {
  if (menuType === 'SUB') {
    return new Set(['TOP', 'SUB', 'CATALOG', 'MENU'])
  }
  if (menuType === 'BUTTON') {
    return new Set(['SUB', 'MENU'])
  }
  return new Set()
}
