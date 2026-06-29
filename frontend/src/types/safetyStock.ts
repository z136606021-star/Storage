import type { MaterialQuery } from '@/types/materialLedger'

export interface SafetyStockRecord {
  safetyStockId?: number | null
  materialLedgerId: number
  category: string
  genericName: string
  brand?: string | null
  name: string
  model: string
  binLocation: string
  stockQuantity: number
  safetyQuantity: number
  warningEnabled: boolean
  inWarningPeriod: boolean
  createdAt?: string | null
  updatedAt?: string | null
}

export interface SafetyStockQuery extends MaterialQuery {
  safetyQuantityKeyword?: string
  warningPeriod?: string
}

export interface SafetyStockExportQuery extends SafetyStockQuery {
  ids?: number[]
}

export interface SafetyStockUpdatePayload {
  safetyQuantity: number
  warningEnabled: boolean
}
