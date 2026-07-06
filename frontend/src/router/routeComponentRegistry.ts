import type { Component } from 'vue'

type RouteComponentLoader = () => Promise<Component>

export const routeComponentRegistry: Record<string, RouteComponentLoader> = {
  MaterialLedger: () => import('@/views/warehouse/MaterialLedgerView.vue'),
  MaterialIo: () => import('@/views/warehouse/MaterialIoView.vue'),
  SafetyStock: () => import('@/views/warehouse/SafetyStockView.vue'),
  InventoryStats: () => import('@/views/warehouse/InventoryStatsView.vue'),
  BinManage: () => import('@/views/warehouse/config/BinManageView.vue'),
  BomManage: () => import('@/views/warehouse/config/BomManageView.vue'),
  SystemManageLayout: () => import('@/views/system/SystemManageLayout.vue'),
  UserManage: () => import('@/views/system/UserManageView.vue'),
  RoleManagePanel: () => import('@/components/system/RoleManagePanel.vue'),
  MenuManagePanel: () => import('@/components/system/MenuManagePanel.vue'),
  CustomerManage: () => import('@/views/system/CustomerManageView.vue'),
  ShellPlaceholder: () => import('@/views/platform/ShellPlaceholderView.vue'),
}

export function resolveRouteComponent(componentKey?: string | null) {
  if (!componentKey) {
    return null
  }
  return routeComponentRegistry[componentKey] ?? null
}
