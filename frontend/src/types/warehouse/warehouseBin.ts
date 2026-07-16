export interface WarehouseBin {
  id: number
  binCode: string
  rowNo: string
  colNo: number | null
  levelNo: number | null
  remark: string | null
  operatorUserId: number | null
  operatorName: string | null
  createdAt: string
  updatedAt: string
}

export interface WarehouseBinQuery {
  binCode?: string
  rowNo?: string
  colNo?: number
  levelNo?: number
  page?: number
  pageSize?: number
}

export interface WarehouseBinSavePayload {
  rowNo: string
  colNo?: number | null
  levelNo?: number | null
  remark?: string | null
}

export type WarehouseBinExportQuery = Omit<WarehouseBinQuery, 'page' | 'pageSize'> & {
  ids?: number[]
}
