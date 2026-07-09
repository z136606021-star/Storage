export interface WarehouseBom {
  id: number
  category: string
  genericName: string
  brand: string | null
  name: string
  model: string
  remark: string | null
  imageObjectKey: string | null
  imageUrl: string | null
  imageObjectKeys: string[]
  imageUrls: string[]
  createdAt: string
  updatedAt: string
}

export interface WarehouseBomQuery {
  category?: string
  genericName?: string
  brand?: string
  name?: string
  model?: string
  page?: number
  pageSize?: number
}

export interface WarehouseBomSavePayload {
  category: string
  genericName: string
  brand?: string | null
  name: string
  model?: string | null
  remark?: string | null
  imageObjectKey?: string | null
  imageObjectKeys?: string[]
}

export interface BomFilterLinkageQuery {
  category?: string
  genericName?: string
  brand?: string
}

export interface BomFilterOptions {
  categories: string[]
  genericNames: string[]
  brands: string[]
}

export type WarehouseBomExportQuery = Omit<WarehouseBomQuery, 'page' | 'pageSize'> & {
  ids?: number[]
}
