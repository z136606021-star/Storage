import { http } from '@/api/http'
import type { WarehouseStatsOverview } from '@/types/warehouseStats'

export function fetchWarehouseStatsOverview(recentDays?: number) {
  return http.get<WarehouseStatsOverview>('/warehouse-stats/overview', {
    params: recentDays != null ? { recentDays } : undefined,
  })
}
