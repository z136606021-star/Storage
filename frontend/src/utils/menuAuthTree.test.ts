import { describe, expect, it } from 'vitest'
import type { SysMenu } from '@/types/system'
import {
  buildMenuAuthTreeNodes,
  buildParentOptions,
  menuTypeLabel,
} from '@/utils/menuAuthTree'

const sampleTree: SysMenu[] = [
  {
    id: 110,
    parentId: null,
    menuType: 'TOP',
    name: '仓库管理',
    permission: null,
    path: null,
    icon: 'InboxOutlined',
    visible: 1,
    sortOrder: 10,
    children: [
      {
        id: 111,
        parentId: 110,
        menuType: 'SUB',
        name: '物料台账',
        permission: 'warehouse:material-ledger:read',
        path: '/warehouse/material-ledger',
        icon: null,
        visible: 1,
        sortOrder: 10,
        children: [
          {
            id: 2,
            parentId: 111,
            menuType: 'BUTTON',
            name: '物料台账写',
            permission: 'warehouse:material-ledger:write',
            path: null,
            icon: null,
            visible: 0,
            sortOrder: 11,
          },
        ],
      },
    ],
  },
]

describe('menuAuthTree', () => {
  it('labels menu types in Chinese', () => {
    expect(menuTypeLabel('TOP')).toBe('一级菜单')
    expect(menuTypeLabel('BUTTON')).toBe('按钮权限')
  })

  it('maps auth tree with type and permission', () => {
    const nodes = buildMenuAuthTreeNodes(sampleTree)
    expect(nodes[0]?.title).toContain('仓库管理 [一级菜单]')
    expect(nodes[0]?.children?.[0]?.title).toContain('物料台账 [子菜单]')
    expect(nodes[0]?.children?.[0]?.children?.[0]?.title).toContain('物料台账写 [按钮权限]')
    expect(nodes[0]?.children?.[0]?.children?.[0]?.title).toContain('warehouse:material-ledger:write')
  })

  it('builds parent options for button under sub menu only', () => {
    const options = buildParentOptions(sampleTree, new Set(), new Set(['SUB', 'MENU']))
    expect(options[0]?.disabled).toBe(true)
    expect(options[0]?.children?.[0]?.disabled).toBe(false)
  })
})
