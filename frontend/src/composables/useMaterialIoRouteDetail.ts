import type { RouteLocationNormalizedLoaded, Router } from 'vue-router'
import { useCrudRouteDetail } from '@/composables/useCrudRouteDetail'
import { parseIoRecordIdFromQuery } from '@/utils/materialIoRouteQuery'

export interface MaterialIoRouteDetailConfig {
  route: RouteLocationNormalizedLoaded
  router: Router
  openDetail: (record: { id: number }) => void | Promise<void>
  onRouteIdChange?: (id: number | null) => void | Promise<void>
}

export function useMaterialIoRouteDetail(config: MaterialIoRouteDetailConfig) {
  return useCrudRouteDetail({
    route: config.route,
    router: config.router,
    openDetail: config.openDetail,
    onRouteIdChange: config.onRouteIdChange,
    queryKey: 'id',
    parseId: parseIoRecordIdFromQuery,
    rowHighlightClass: 'io-row-highlight',
    clearQueryMode: 'omitKey',
  })
}
