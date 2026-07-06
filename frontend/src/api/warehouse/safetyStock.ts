import { http } from '@/api/http'
import type { PageResult } from '@/types/common'
import type { FilterLinkageQuery, FilterOptions } from '@/types/warehouse/materialLedger'
import type {
  SafetyStockExportQuery,
  SafetyStockQuery,
  SafetyStockRecord,
  SafetyStockUpdatePayload,
} from '@/types/warehouse/safetyStock'

export async function fetchSafetyStockPage(
  query: SafetyStockQuery,
): Promise<PageResult<SafetyStockRecord>> {
  const { data } = await http.get<PageResult<SafetyStockRecord>>('/safety-stock', {
    params: query,
  })
  return data
}

export async function fetchSafetyStockFilterOptions(
  query: FilterLinkageQuery,
): Promise<FilterOptions> {
  const { data } = await http.get<FilterOptions>('/safety-stock/filter-options', {
    params: query,
  })
  return data
}

export async function fetchSafetyStockDetail(
  materialLedgerId: number,
): Promise<SafetyStockRecord> {
  const { data } = await http.get<SafetyStockRecord>(`/safety-stock/${materialLedgerId}`)
  return data
}

export async function updateSafetyStock(
  materialLedgerId: number,
  payload: SafetyStockUpdatePayload,
): Promise<SafetyStockRecord> {
  const { data } = await http.put<SafetyStockRecord>(`/safety-stock/${materialLedgerId}`, payload)
  return data
}

export async function exportSafetyStock(query: SafetyStockExportQuery): Promise<Blob> {
  const { data } = await http.get<Blob>('/safety-stock/export', {
    params: query,
    paramsSerializer: {
      indexes: null,
    },
    responseType: 'blob',
  })
  return data
}
