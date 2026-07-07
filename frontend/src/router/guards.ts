import type { Router } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useMenuStore } from '@/stores/menu'

const LOGIN_PATH = '/login'

function isPublicRoute(path: string): boolean {
  return path === LOGIN_PATH || path.startsWith(`${LOGIN_PATH}?`)
}

function buildLoginRedirect(targetPath: string) {
  return {
    path: LOGIN_PATH,
    query: targetPath && targetPath !== '/' && targetPath !== LOGIN_PATH
      ? { redirect: targetPath }
      : undefined,
    replace: true,
  }
}

export function setupAuthGuard(router: Router) {
  router.beforeEach(async (to) => {
    const auth = useAuth()
    await auth.initialize()
    const menu = useMenuStore()
    const registeredRoutes = auth.isAuthenticated()
      ? await menu.ensureDynamicRoutes(router)
      : false

    if (isPublicRoute(to.path)) {
      if (auth.isAuthenticated()) {
        const redirect = typeof to.query.redirect === 'string'
          ? to.query.redirect
          : menu.getDefaultRoute()?.path
        return redirect ?? true
      }
      return true
    }

    const needsAuth = to.matched.some((record) => record.meta.requiresAuth)
    if ((needsAuth || !isPublicRoute(to.path)) && !auth.isAuthenticated()) {
      return buildLoginRedirect(to.fullPath)
    }

    if (auth.isAuthenticated()) {
      if (registeredRoutes && to.path !== '/') {
        return { path: to.fullPath, replace: true }
      }
    }

    const requiredPermission = to.matched
      .map((record) => record.meta.permission)
      .filter((permission): permission is string => Boolean(permission))
      .at(-1)

    if (requiredPermission && !auth.hasPermission(requiredPermission)) {
      const defaultRoute = menu.getDefaultRoute()
      return defaultRoute ? { path: defaultRoute.path, replace: true } : false
    }

    return true
  })
}
