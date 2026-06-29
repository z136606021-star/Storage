export interface WarehouseBin {
  id: number
  binCode: string
  rowNo: number
  colNo: number
  levelNo: number
  remark: string | null
  createdAt: string
  updatedAt: string
}

export interface WarehouseBinQuery {
  binCode?: string
  page?: number
  pageSize?: number
}

export interface WarehouseBinSavePayload {
  rowNo: number
  colNo: number
  levelNo: number
  remark?: string | null
}

export type WarehouseBinExportQuery = Omit<WarehouseBinQuery, 'page' | 'pageSize'> & {
  ids?: number[]
}
