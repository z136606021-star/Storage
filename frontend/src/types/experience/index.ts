export interface ExperienceType {
  id: number
  name: string
  status: number
  sortOrder: number
  createdAt?: string
  updatedAt?: string
}

export interface ExperienceAttachment {
  id: number
  objectKey: string
  originalName: string
  contentType: string | null
  sizeBytes: number
  url: string | null
  previewable: boolean
}

export interface ExperienceRecord {
  id: number
  typeId: number
  typeName: string | null
  description: string
  impact: string | null
  suggestion: string | null
  actionPlan: string | null
  recorderUserId: number | null
  recorderName: string
  recordedAt: string
  createdAt: string
  updatedAt: string
  projectNames: string[]
  attachmentCount: number
}

export interface ExperienceRecordDetail extends ExperienceRecord {
  attachments: ExperienceAttachment[]
}

export interface ExperienceRecordQuery {
  page?: number
  pageSize?: number
  typeId?: number
  recorderName?: string
  keyword?: string
  recordedStart?: string
  recordedEnd?: string
  ids?: number[]
}

export interface ExperienceRecordSavePayload {
  typeId: number | null
  description: string
  impact?: string | null
  suggestion?: string | null
  actionPlan?: string | null
  recordedAt?: string | null
  projectNames: string[]
  attachmentObjectKeys: string[]
}

export interface ExperienceTypeSavePayload {
  name: string
  status: number
  sortOrder: number
}

export interface ExperienceFilterOptions {
  types: ExperienceType[]
  recorderNames: string[]
}
