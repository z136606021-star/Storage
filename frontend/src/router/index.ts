import { createRouter, createWebHistory } from 'vue-router'
import { setupAuthGuard } from './guards'
import { routes } from './routes'
import { useAuth } from '@/composables/useAuth'
import { useWorkbenchTabs } from '@/composables/useWorkbenchTabs'

const router = createRouter({
  history: createWebHistory(),
  routes,
})

setupAuthGuard(router)

router.afterEach((to) => {
  const auth = useAuth()
  if (!auth.isAuthenticated()) {
    return
  }
  const workbenchTabs = useWorkbenchTabs()
  workbenchTabs.initPresets(auth.hasPermission)
  workbenchTabs.syncTabFromRoute(to)
})

export default router
