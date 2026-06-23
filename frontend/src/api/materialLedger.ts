import axios from 'axios'
import type {
  FilterOptions,
  MaterialLedger,
  MaterialQuery,
  PageResult,
} from '@/types/materialLedger'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

export async function fetchMaterialLedgerPage(
  query: MaterialQuery,
): Promise<PageResult<MaterialLedger>> {
  const { data } = await http.get<PageResult<MaterialLedger>>('/materials', {
    params: query,
  })
  return data
}

export async function fetchFilterOptions(): Promise<FilterOptions> {
  const { data } = await http.get<FilterOptions>('/materials/filter-options')
  return data
}
