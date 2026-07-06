import type { SafetyStockRecord } from '@/types/warehouse/safetyStock'

export interface WarehouseStatsOverview {
  recentDays: number
  totalLedgerCount: number
  totalStockQuantity: number
  warningMaterialCount: number
  inboundRecordCount: number
  outboundRecordCount: number
  inboundQuantitySum: number
  outboundQuantitySum: number
  warningMaterials: SafetyStockRecord[]
}
