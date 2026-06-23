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
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface MaterialQuery {
  category?: string
  genericName?: string
  brand?: string
  name?: string
  model?: string
  binLocation?: string
  page?: number
  pageSize?: number
}

export interface FilterOptions {
  categories: string[]
  genericNames: string[]
  brands: string[]
  models: string[]
  binLocations: string[]
}
