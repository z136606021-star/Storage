import type { RouteRecordRaw } from 'vue-router'

import AppLayout from '@/layouts/AppLayout.vue'

import LoginView from '@/views/auth/LoginView.vue'

import MaterialLedgerView from '@/views/material-ledger/MaterialLedgerView.vue'

import MaterialIoView from '@/views/warehouse/MaterialIoView.vue'
import InventoryStatsView from '@/views/warehouse/InventoryStatsView.vue'
import SafetyStockView from '@/views/warehouse/SafetyStockView.vue'
import BinManageView from '@/views/warehouse/config/BinManageView.vue'
import BomManageView from '@/views/warehouse/config/BomManageView.vue'

import ShellPlaceholderView from '@/views/platform/ShellPlaceholderView.vue'

import SystemManageLayout from '@/views/system/SystemManageLayout.vue'

import UserManageView from '@/views/system/UserManageView.vue'

import RoleManagePanel from '@/components/system/RoleManagePanel.vue'

import MenuManagePanel from '@/components/system/MenuManagePanel.vue'

import CustomerManageView from '@/views/system/CustomerManageView.vue'

import { SHELL_ROUTE_REGISTRY } from '@/constants/shellRouteRegistry'

function toChildPath(fullPath: string) {
  return fullPath.replace(/^\//, '')
}

const shellRoutes: RouteRecordRaw[] = SHELL_ROUTE_REGISTRY.map((def) => ({
  path: toChildPath(def.path),
  name: def.routeName,
  component: ShellPlaceholderView,
  meta: {
    title: def.title,
    requiresAuth: true,
    permission: def.permission,
  },
}))

export const routes: RouteRecordRaw[] = [

  {

    path: '/login',

    name: 'Login',

    component: LoginView,

    meta: { public: true, title: '登录', skipTab: true },

  },

  {

    path: '/',

    redirect: '/login',

  },

  {

    path: '/',

    component: AppLayout,

    meta: { requiresAuth: true },

    children: [

      ...shellRoutes,

      {

        path: 'warehouse/material-ledger',

        name: 'MaterialLedger',

        component: MaterialLedgerView,

        meta: { title: '物料台账', requiresAuth: true, permission: 'warehouse:material-ledger:read' },

      },

      {

        path: 'warehouse/material-io',

        name: 'MaterialIo',

        component: MaterialIoView,

        meta: { title: '物料出入库', requiresAuth: true, permission: 'warehouse:material-io:read' },

      },

      {

        path: 'warehouse/safety-stock',

        name: 'SafetyStock',

        component: SafetyStockView,

        meta: { title: '安全库存管理', requiresAuth: true, permission: 'warehouse:safety-stock:read' },

      },

      {

        path: 'warehouse/inventory-stats',

        name: 'InventoryStats',

        component: InventoryStatsView,

        meta: { title: '库存统计', requiresAuth: true, permission: 'warehouse:stats:read' },

      },

      {

        path: 'warehouse/config/bin',

        name: 'BinManage',

        component: BinManageView,

        meta: { title: 'Bin位管理', requiresAuth: true, permission: 'warehouse:bin:read' },

      },

      {

        path: 'warehouse/config/bom',

        name: 'BomManage',

        component: BomManageView,

        meta: { title: '物料清单管理', requiresAuth: true, permission: 'warehouse:bom:read' },

      },

      {

        path: 'system/users',

        component: SystemManageLayout,

        meta: { title: '用户管理', requiresAuth: true, permission: 'system:user:read' },

        children: [

          {

            path: '',

            name: 'UserManage',

            component: UserManageView,

            meta: { title: '用户管理', requiresAuth: true, permission: 'system:user:read' },

          },

          {

            path: 'roles',

            name: 'RoleManage',

            component: RoleManagePanel,

            meta: { title: '角色管理', requiresAuth: true, permission: 'system:role:read' },

          },

          {

            path: 'menus',

            name: 'MenuManage',

            component: MenuManagePanel,

            meta: { title: '菜单管理', requiresAuth: true, permission: 'system:menu:read' },

          },

        ],

      },

      {

        path: 'system/roles',

        redirect: '/system/users/roles',

      },

      {

        path: 'system/menus',

        redirect: '/system/users/menus',

      },

      {

        path: 'system/customers',

        name: 'CustomerManage',

        component: CustomerManageView,

        meta: { title: '客户管理', requiresAuth: true, permission: 'system:customer:read' },

      },

    ],

  },

]

