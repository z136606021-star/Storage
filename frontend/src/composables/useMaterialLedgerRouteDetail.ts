import type { RouteLocationNormalizedLoaded, Router } from 'vue-router'
import { useCrudRouteDetail } from '@/composables/useCrudRouteDetail'
import { parseMaterialLedgerIdFromQuery } from '@/utils/materialLedgerRouteQuery'

export interface MaterialLedgerRouteDetailConfig {
  route: RouteLocationNormalizedLoaded
  router: Router
  openDetail: (record: { id: number }) => void | Promise<void>
}

export function useMaterialLedgerRouteDetail(config: MaterialLedgerRouteDetailConfig) {
  const result = useCrudRouteDetail({
    route: config.route,
    router: config.router,
    openDetail: config.openDetail,
    queryKey: 'materialLedgerId',
    parseId: parseMaterialLedgerIdFromQuery,
    rowHighlightClass: 'ledger-row-highlight',
    clearQueryMode: 'clearAll',
  })

  return {
    highlightLedgerId: result.highlightRecordId,
    initFromRoute: result.initFromRoute,
    setupRouteWatch: result.setupRouteWatch,
    clearRouteDetailQuery: result.clearRouteDetailQuery,
    customRow: result.customRow,
  }
}
