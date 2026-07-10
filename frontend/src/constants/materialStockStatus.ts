import type { MaterialStockStatus } from '@/types/warehouse/materialLedger'

export const MATERIAL_STOCK_STATUS_OPTIONS: Array<{
  label: string
  value: MaterialStockStatus
}> = [
  { label: '有库存', value: 'IN_STOCK' },
  { label: '无库存', value: 'ZERO_STOCK' },
]

export const DEFAULT_LEDGER_STOCK_STATUS: MaterialStockStatus = 'IN_STOCK'

export const DEFAULT_PICKER_STOCK_STATUS: MaterialStockStatus = 'IN_STOCK'
