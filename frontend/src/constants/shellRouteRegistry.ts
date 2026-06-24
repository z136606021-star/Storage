export interface ShellRouteDef {
  menuId?: number
  path: string
  title: string
  permission: string
  routeName?: string
}

/** 壳层占位路由（与 sys_menu path / permission 对齐，供 routes.ts 与 DB 迁移共用） */
export const SHELL_ROUTE_REGISTRY: ShellRouteDef[] = [
  { menuId: 10, path: '/platform/personal', title: '个人中心', permission: 'platform:personal:read', routeName: 'PlatformPersonal' },
  { path: '/platform/project', title: '项目中心', permission: 'platform:project:read', routeName: 'PlatformProject' },
  { menuId: 21, path: '/platform/project/create', title: '新建项目', permission: 'platform:project:create', routeName: 'PlatformProjectCreate' },
  { menuId: 22, path: '/platform/project/set', title: '项目集', permission: 'platform:project:set', routeName: 'PlatformProjectSet' },
  { menuId: 121, path: '/platform/procurement', title: '采购管理', permission: 'procurement:read', routeName: 'PlatformProcurement' },
  { menuId: 122, path: '/platform/procurement/create', title: '新增采购需求', permission: 'procurement:create', routeName: 'PlatformProcurementCreate' },
  { menuId: 123, path: '/platform/procurement/mine', title: '我的采购需求', permission: 'procurement:mine', routeName: 'PlatformProcurementMine' },
  { menuId: 150, path: '/platform/design', title: '设计指引', permission: 'platform:design:read', routeName: 'PlatformDesign' },
  { menuId: 161, path: '/platform/skill/matrix', title: '技能矩阵', permission: 'skill:matrix:read', routeName: 'PlatformSkillMatrix' },
  { menuId: 162, path: '/platform/skill/talent', title: '人才画像', permission: 'skill:talent:read', routeName: 'PlatformSkillTalent' },
  { menuId: 163, path: '/platform/skill/training', title: '人才培训计划', permission: 'skill:training:read', routeName: 'PlatformSkillTraining' },
  { menuId: 170, path: '/platform/experience', title: '经验库', permission: 'platform:experience:read', routeName: 'PlatformExperience' },
  { menuId: 181, path: '/platform/finance/dashboard', title: '业务分析看板', permission: 'finance:dashboard:read', routeName: 'PlatformFinanceDashboard' },
  { menuId: 182, path: '/platform/finance/settlement', title: '财务结算中心', permission: 'finance:settlement:read', routeName: 'PlatformFinanceSettlement' },
  { menuId: 183, path: '/platform/finance/cost', title: '成本分析中心', permission: 'finance:cost:read', routeName: 'PlatformFinanceCost' },
]

export const DEFAULT_ADMIN_TAB_PRESETS: Array<{ path: string; title: string; permission: string }> = [
  { path: '/platform/personal', title: '个人中心', permission: 'platform:personal:read' },
  { path: '/platform/project', title: '项目中心', permission: 'platform:project:read' },
]

export const DEFAULT_USER_TAB_PRESET: { path: string; title: string; permission: string } = {
  path: '/warehouse/material-ledger',
  title: '物料台账',
  permission: 'warehouse:material-ledger:read',
}
