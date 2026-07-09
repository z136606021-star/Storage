import { http } from '@/api/http'
import type { ImportResult, PageResult } from '@/types/common'
import type {
  DesignGuide,
  DesignGuideExportQuery,
  DesignGuideFilterOptions,
  DesignGuideQuery,
  DesignGuideSavePayload,
  DesignProductType,
  DesignProductTypeExportQuery,
  DesignProductTypeQuery,
  DesignProductTypeSavePayload,
  DesignStage,
  DesignStageExportQuery,
  DesignStageQuery,
  DesignStageSavePayload,
} from '@/types/design/designGuide'

export async function fetchDesignGuidePage(
  query: DesignGuideQuery,
): Promise<PageResult<DesignGuide>> {
  const { data } = await http.get<PageResult<DesignGuide>>('/design-guides', { params: query })
  return data
}

export async function fetchDesignGuideDetail(id: number): Promise<DesignGuide> {
  const { data } = await http.get<DesignGuide>(`/design-guides/${id}`)
  return data
}

export async function createDesignGuide(payload: DesignGuideSavePayload): Promise<DesignGuide> {
  const { data } = await http.post<DesignGuide>('/design-guides', payload)
  return data
}

export async function updateDesignGuide(
  id: number,
  payload: DesignGuideSavePayload,
): Promise<DesignGuide> {
  const { data } = await http.put<DesignGuide>(`/design-guides/${id}`, payload)
  return data
}

export async function deleteDesignGuide(id: number): Promise<void> {
  await http.delete(`/design-guides/${id}`)
}

export async function batchDeleteDesignGuides(ids: number[]): Promise<void> {
  await http.delete('/design-guides/batch', { data: { ids } })
}

export async function exportDesignGuides(query: DesignGuideExportQuery): Promise<Blob> {
  const { data } = await http.get<Blob>('/design-guides/export', {
    params: query,
    paramsSerializer: { indexes: null },
    responseType: 'blob',
  })
  return data
}

export async function downloadDesignGuideImportTemplate(): Promise<Blob> {
  const { data } = await http.get<Blob>('/design-guides/import-template', {
    responseType: 'blob',
  })
  return data
}

export async function importDesignGuides(file: File): Promise<ImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ImportResult>('/design-guides/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}

export async function fetchDesignGuideFilterOptions(): Promise<DesignGuideFilterOptions> {
  const { data } = await http.get<DesignGuideFilterOptions>('/design-guides/filter-options')
  return data
}

export async function fetchDesignProductTypePage(
  query: DesignProductTypeQuery,
): Promise<PageResult<DesignProductType>> {
  const { data } = await http.get<PageResult<DesignProductType>>('/design/product-types', {
    params: query,
  })
  return data
}

export async function createDesignProductType(
  payload: DesignProductTypeSavePayload,
): Promise<DesignProductType> {
  const { data } = await http.post<DesignProductType>('/design/product-types', payload)
  return data
}

export async function updateDesignProductType(
  id: number,
  payload: DesignProductTypeSavePayload,
): Promise<DesignProductType> {
  const { data } = await http.put<DesignProductType>(`/design/product-types/${id}`, payload)
  return data
}

export async function deleteDesignProductType(id: number): Promise<void> {
  await http.delete(`/design/product-types/${id}`)
}

export async function exportDesignProductTypes(
  query: DesignProductTypeExportQuery,
): Promise<Blob> {
  const { data } = await http.get<Blob>('/design/product-types/export', {
    params: query,
    paramsSerializer: { indexes: null },
    responseType: 'blob',
  })
  return data
}

export async function fetchDesignStagePage(
  query: DesignStageQuery,
): Promise<PageResult<DesignStage>> {
  const { data } = await http.get<PageResult<DesignStage>>('/design/stages', { params: query })
  return data
}

export async function createDesignStage(payload: DesignStageSavePayload): Promise<DesignStage> {
  const { data } = await http.post<DesignStage>('/design/stages', payload)
  return data
}

export async function updateDesignStage(
  id: number,
  payload: DesignStageSavePayload,
): Promise<DesignStage> {
  const { data } = await http.put<DesignStage>(`/design/stages/${id}`, payload)
  return data
}

export async function deleteDesignStage(id: number): Promise<void> {
  await http.delete(`/design/stages/${id}`)
}

export async function exportDesignStages(query: DesignStageExportQuery): Promise<Blob> {
  const { data } = await http.get<Blob>('/design/stages/export', {
    params: query,
    paramsSerializer: { indexes: null },
    responseType: 'blob',
  })
  return data
}
