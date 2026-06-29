import { ref, watch, type Ref } from 'vue'
import type { RouteLocationNormalizedLoaded, Router } from 'vue-router'
import { message } from 'ant-design-vue'
import { fetchMaterialLedgerDetail } from '@/api/materialLedger'
import type { MaterialLedger } from '@/types/materialLedger'
import { parseMaterialLedgerIdFromQuery } from '@/utils/materialLedgerRouteQuery'

export interface MaterialLedgerDeepLinkConfig {
  route: RouteLocationNormalizedLoaded
  router?: Router
  pagination: { current?: number }
  loadData: () => Promise<void>
}

export function useMaterialLedgerDeepLink(config: MaterialLedgerDeepLinkConfig) {
  const materialLedgerIdFilter = ref<number | null>(null)
  const materialContext = ref<MaterialLedger | null>(null)

  function materialLedgerIdParam(): number | undefined {
    return materialLedgerIdFilter.value ?? undefined
  }

  async function applyMaterialLedgerFilter(id: number) {
    materialLedgerIdFilter.value = id
    try {
      materialContext.value = await fetchMaterialLedgerDetail(id)
    } catch {
      message.warning('物料台账信息加载失败')
      materialContext.value = null
    }
    config.pagination.current = 1
    await config.loadData()
  }

  function clearMaterialLedgerFilter() {
    materialLedgerIdFilter.value = null
    materialContext.value = null
    if (config.router && config.route.query.materialLedgerId) {
      config.router.replace({ path: config.route.path })
    }
    config.pagination.current = 1
    config.loadData()
  }

  function clearDeepLinkOnReset() {
    materialLedgerIdFilter.value = null
    materialContext.value = null
    if (config.router && config.route.query.materialLedgerId) {
      config.router.replace({ path: config.route.path })
    }
  }

  async function initFromRoute(): Promise<boolean> {
    const id = parseMaterialLedgerIdFromQuery(config.route.query.materialLedgerId)
    if (id != null) {
      await applyMaterialLedgerFilter(id)
      return true
    }
    return false
  }

  function setupRouteWatch() {
    watch(
      () => config.route.query.materialLedgerId,
      async (raw) => {
        const id = parseMaterialLedgerIdFromQuery(raw)
        if (id != null) {
          if (materialLedgerIdFilter.value !== id) {
            await applyMaterialLedgerFilter(id)
          }
          return
        }
        if (materialLedgerIdFilter.value != null) {
          materialLedgerIdFilter.value = null
          materialContext.value = null
          await config.loadData()
        }
      },
    )
  }

  return {
    materialLedgerIdFilter: materialLedgerIdFilter as Readonly<Ref<number | null>>,
    materialContext: materialContext as Readonly<Ref<MaterialLedger | null>>,
    materialLedgerIdParam,
    applyMaterialLedgerFilter,
    clearMaterialLedgerFilter,
    clearDeepLinkOnReset,
    initFromRoute,
    setupRouteWatch,
  }
}
