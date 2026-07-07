import type { Router, RouteRecordRaw } from 'vue-router'
import type { NavMenuNode } from '@/types/system'
import { resolveRouteComponent } from '@/router/routeComponentRegistry'

const DYNAMIC_ROUTE_PARENT = 'AppRoot'

const dynamicRouteNames = new Set<string>()

const STABLE_ROUTE_NAMES: Record<string, string> = {
  SystemManageLayout: 'SystemManage',
  UserManage: 'UserManage',
  RoleManagePanel: 'RoleManage',
  MenuManagePanel: 'MenuManage',
}

function normalizeChildPath(path: string) {
  return path.replace(/^\//, '')
}

function routeNameFromNode(node: NavMenuNode) {
  if (node.componentKey && STABLE_ROUTE_NAMES[node.componentKey]) {
    return STABLE_ROUTE_NAMES[node.componentKey]
  }
  return `DynamicMenu${node.key}`
}

function buildChildRoutes(nodes: NavMenuNode[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  for (const node of nodes) {
    const route = buildRoute(node, true)
    if (route) {
      routes.push(route)
    }
  }
  return routes
}

function buildRoute(node: NavMenuNode, nested = false): RouteRecordRaw | null {
  if (!node.componentKey) {
    return null
  }
  const component = resolveRouteComponent(node.componentKey)
  if (!component) {
    return null
  }

  const routableChildren = (node.children ?? []).filter(
    (child) => child.componentKey && child.path !== undefined && child.path !== null,
  )
  const childRoutes = routableChildren.length ? buildChildRoutes(routableChildren) : undefined

  if (!nested && !node.path && !childRoutes?.length) {
    return null
  }

  const routePath = node.path !== undefined && node.path !== null
    ? (nested ? node.path : normalizeChildPath(node.path))
    : ''

  if (!nested && !routePath && !childRoutes?.length) {
    return null
  }

  const route: RouteRecordRaw = {
    path: routePath,
    name: routeNameFromNode(node),
    component,
    meta: {
      title: node.label,
      requiresAuth: true,
      permission: node.permission ?? undefined,
    },
    ...(childRoutes?.length ? { children: childRoutes } : {}),
  }

  return route
}

function collectRoutes(nodes: NavMenuNode[], routes: RouteRecordRaw[] = []) {
  for (const node of nodes) {
    const route = buildRoute(node)
    if (route) {
      routes.push(route)
    } else if (node.children?.length) {
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
