export type { ImportResult, PageResult } from '@/types/common'

export interface MaterialLedger {
  id: number
  category: string
  genericName: string
  brand: string | null
  name: string
  model: string
  binLocation: string
  stockQuantity: number
  unitPrice: number | null
  remark: string | null
  createdAt?: string
  updatedAt?: string
}

export interface MaterialSavePayload {
  category: string
  genericName: string
  brand?: string | null
  name: string
  model: string
  binLocation: string
  unitPrice?: number | null
  remark?: string | null
}

export interface MaterialQuery {
  category?: string
  genericName?: string
  brand?: string
  name?: string
  model?: string
  binLocation?: string
  ids?: number[]
  page?: number
  pageSize?: number
}

export interface FilterLinkageQuery {
  category?: string
  genericName?: string
  brand?: string
}

export interface FilterOptions {
  categories: string[]
  genericNames: string[]
  brands: string[]
  models: string[]
  binLocations: string[]
}

export interface BomCatalogItem {
  id: number
  category: string
  genericName: string
  brand: string | null
  name: string
  displayLabel: string
}

