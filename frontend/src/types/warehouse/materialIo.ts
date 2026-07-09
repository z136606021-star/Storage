import type { MaterialIoPurposeCode } from '@/constants/materialIoPurpose'

export type IoType = 'IN' | 'OUT'

export interface MaterialIoRecord {
  id: number
  materialLedgerId: number
  ioType: IoType
  quantity: number
  unitPrice: number | null
  remark: string | null
  purpose: MaterialIoPurposeCode | string | null
  purposeLabel?: string | null
  projectRef?: string | null
  operatorUserId: number
  operatorUsername: string | null
  operatorDisplayName: string | null
  operatedAt: string
  category: string
  genericName: string
  brand: string | null
  name: string
  model: string
  binLocation: string
  stockQuantity: number | null
  createdAt?: string
  updatedAt?: string
}

export interface MaterialIoQuery {
  category?: string
  genericName?: string
  brand?: string
  name?: string
  model?: string
  binLocation?: string
  ioType?: string
  purpose?: string
  projectRef?: string
  operatedAtStart?: string
  operatedAtEnd?: string
  materialLedgerId?: number
  ids?: number[]
  page?: number
  pageSize?: number
}

export interface MaterialIoBatchItemPayload {
  materialLedgerId?: number
  bomId?: number
  binLocation?: string
  quantity: number
  unitPrice?: number | null
  remark?: string | null
  purpose?: string | null
  projectRef?: string | null
}

export interface MaterialIoBatchSavePayload {
  ioType: IoType
  operatedAt?: string | null
  items: MaterialIoBatchItemPayload[]
}

export interface MaterialIoUpdatePayload {
  quantity: number
  unitPrice?: number | null
  remark?: string | null
  purpose?: string | null
  projectRef?: string | null
}

export interface MaterialIoSafetyHint {
  materialLedgerId: number
  currentStock: number | null
  safetyQuantity: number | null
  warningEnabled: boolean | null
}

export interface MaterialIoSavePayload {
  materialLedgerId: number
  ioType: IoType
  quantity: number
  unitPrice?: number | null
  remark?: string | null
  purpose?: string | null
  projectRef?: string | null
}

export type MaterialIoExportQuery = Omit<MaterialIoQuery, 'page' | 'pageSize'>

export interface MaterialIoFormRow {
  key: string
  materialLedgerId?: number
  bomId?: number
  category?: string
  genericName?: string
  brand?: string | null
  name?: string
  model?: string
  binLocation?: string
  stockQuantity?: number
  quantity?: number
  unitPrice?: number | null
  purpose?: string
  projectRef?: string
  remark?: string
}
