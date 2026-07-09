export interface DesignProductType {
  id: number
  typeCode: string
  typeName: string
  enabled: number
  operatorUserId: number | null
  operatorName: string | null
  operatedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface DesignStage {
  id: number
  sortOrder: number
  stageName: string
  enabled: number
  operatorUserId: number | null
  operatorName: string | null
  operatedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface DesignGuide {
  id: number
  productTypeId: number
  productTypeCode: string
  productTypeName: string
  stageId: number
  stageName: string
  scope: string
  checkItem: string
  remark: string | null
  recorderUserId: number | null
  recorderName: string | null
  recordedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface DesignProductTypeOption {
  id: number
  typeCode: string
  typeName: string
  label: string
}

export interface DesignStageOption {
  id: number
  sortOrder: number
  stageName: string
  label: string
}

export interface DesignGuideFilterOptions {
  productTypes: DesignProductTypeOption[]
  stages: DesignStageOption[]
  scopes: string[]
}

export interface DesignGuideQuery {
  productTypeId?: number
  stageId?: number
  scope?: string
  checkItem?: string
  page?: number
  pageSize?: number
}

export interface DesignGuideSavePayload {
  productTypeId: number
  stageId: number
  scope: string
  checkItem: string
  remark?: string | null
}

export type DesignGuideExportQuery = Omit<DesignGuideQuery, 'page' | 'pageSize'> & {
  ids?: number[]
}

export interface DesignProductTypeQuery {
  typeCode?: string
  typeName?: string
  enabled?: number
  page?: number
  pageSize?: number
}

export interface DesignProductTypeSavePayload {
  typeCode: string
  typeName: string
  enabled: boolean
}

export type DesignProductTypeExportQuery = Omit<DesignProductTypeQuery, 'page' | 'pageSize'> & {
  ids?: number[]
}

export interface DesignStageQuery {
  stageName?: string
  enabled?: number
  page?: number
  pageSize?: number
}

export interface DesignStageSavePayload {
  sortOrder: number
  stageName: string
  enabled: boolean
}

export type DesignStageExportQuery = Omit<DesignStageQuery, 'page' | 'pageSize'> & {
  ids?: number[]
}
