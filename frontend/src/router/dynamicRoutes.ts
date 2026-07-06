import type { Router, RouteRecordRaw } from 'vue-router'
import type { NavMenuNode } from '@/types/system'
import { resolveRouteComponent } from '@/router/routeComponentRegistry'

const DYNAMIC_ROUTE_PARENT = 'AppRoot'
const SYSTEM_USERS_PATH = '/system/users'

const dynamicRouteNames = new Set<string>()

function normalizeChildPath(path: string) {
  return path.replace(/^\//, '')
}

function routeNameFromNode(node: NavMenuNode) {
  return `DynamicMenu${node.key}`
}

function systemManagementChildren(): RouteRecordRaw[] {
  return [
    {
      path: '',
      name: 'UserManage',
      component: resolveRouteComponent('UserManage')!,
      meta: { title: '用户管理', requiresAuth: true, permission: 'system:user:read' },
    },
    {
      path: 'roles',
      name: 'RoleManage',
      component: resolveRouteComponent('RoleManagePanel')!,
      meta: { title: '角色管理', requiresAuth: true, permission: 'system:role:read' },
    },
    {
      path: 'menus',
      name: 'MenuManage',
      component: resolveRouteComponent('MenuManagePanel')!,
      meta: { title: '菜单管理', requiresAuth: true, permission: 'system:menu:read' },
    },
  ]
}

function buildRoute(node: NavMenuNode): RouteRecordRaw | null {
  if (!node.path || !node.componentKey) {
    return null
  }
  const component = resolveRouteComponent(node.componentKey)
  if (!component) {
    return null
  }

  if (node.path === SYSTEM_USERS_PATH) {
    return {
      path: normalizeChildPath(node.path),
      name: 'SystemManage',
      component,
      meta: {
        title: node.label,
        requiresAuth: true,
        permission: node.permission ?? undefined,
      },
      children: systemManagementChildren(),
    }
  }

  return {
    path: normalizeChildPath(node.path),
    name: routeNameFromNode(node),
    component,
    meta: {
      title: node.label,
      requiresAuth: true,
      permission: node.permission ?? undefined,
    },
  }
}

function collectRoutes(nodes: NavMenuNode[], routes: RouteRecordRaw[] = []) {
  for (const node of nodes) {
    const route = buildRoute(node)
    if (route) {
      routes.push(route)
    }
    if (node.children?.length) {
      collectRoutes(node.children, routes)
    }
  }
  return routes
}

function collectRouteNames(route: RouteRecordRaw) {
  if (typeof route.name === 'string') {
    dynamicRouteNames.add(route.name)
  }
  for (const child of route.children ?? []) {
    collectRouteNames(child)
  }
}

export function clearDynamicRoutes(router: Router) {
  for (const name of dynamicRouteNames) {
    if (router.hasRoute(name)) {
      router.removeRoute(name)
    }
  }
  dynamicRouteNames.clear()
}

export function registerDynamicRoutes(router: Router, nodes: NavMenuNode[]) {
  clearDynamicRoutes(router)
  const routes = collectRoutes(nodes)
  for (const route of routes) {
    router.addRoute(DYNAMIC_ROUTE_PARENT, route)
    collectRouteNames(route)
  }
}
