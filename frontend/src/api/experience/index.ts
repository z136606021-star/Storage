import { http } from '@/api/http'
import type { ImportResult, PageResult } from '@/types/common'
import type {
  ExperienceFilterOptions,
  ExperienceRecord,
  ExperienceRecordDetail,
  ExperienceRecordQuery,
  ExperienceRecordSavePayload,
  ExperienceType,
  ExperienceTypeSavePayload,
} from '@/types/experience'

export async function fetchExperiencePage(
  query: ExperienceRecordQuery,
): Promise<PageResult<ExperienceRecord>> {
  const { data } = await http.get<PageResult<ExperienceRecord>>('/experience/records', {
    params: query,
    paramsSerializer: { indexes: null },
  })
  return data
}

export async function fetchExperienceDetail(id: number): Promise<ExperienceRecordDetail> {
  const { data } = await http.get<ExperienceRecordDetail>(`/experience/records/${id}`)
  return data
}

export async function createExperienceRecord(
  payload: ExperienceRecordSavePayload,
): Promise<ExperienceRecordDetail> {
  const { data } = await http.post<ExperienceRecordDetail>('/experience/records', payload)
  return data
}

export async function updateExperienceRecord(
  id: number,
  payload: ExperienceRecordSavePayload,
): Promise<ExperienceRecordDetail> {
  const { data } = await http.put<ExperienceRecordDetail>(`/experience/records/${id}`, payload)
  return data
}

export async function deleteExperienceRecord(id: number): Promise<void> {
  await http.delete(`/experience/records/${id}`)
}

export async function batchDeleteExperienceRecords(ids: number[]): Promise<void> {
  await http.delete('/experience/records/batch', { data: { ids } })
}

export async function exportExperienceRecords(query: ExperienceRecordQuery): Promise<Blob> {
  const { data } = await http.get<Blob>('/experience/records/export', {
    params: query,
    paramsSerializer: { indexes: null },
    responseType: 'blob',
  })
  return data
}

export async function downloadExperienceImportTemplate(): Promise<Blob> {
  const { data } = await http.get<Blob>('/experience/records/import-template', {
    responseType: 'blob',
  })
  return data
}

export async function importExperienceRecords(file: File): Promise<ImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<ImportResult>('/experience/records/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}

export async function fetchExperienceFilterOptions(): Promise<ExperienceFilterOptions> {
  const { data } = await http.get<ExperienceFilterOptions>('/experience/records/filter-options')
  return data
}

export async function fetchExperienceTypes(): Promise<ExperienceType[]> {
  const { data } = await http.get<ExperienceType[]>('/experience/types')
  return data
}

export async function createExperienceType(
  payload: ExperienceTypeSavePayload,
): Promise<ExperienceType> {
  const { data } = await http.post<ExperienceType>('/experience/types', payload)
  return data
}

export async function updateExperienceType(
  id: number,
  payload: ExperienceTypeSavePayload,
): Promise<ExperienceType> {
  const { data } = await http.put<ExperienceType>(`/experience/types/${id}`, payload)
  return data
}

export async function deleteExperienceType(id: number): Promise<void> {
  await http.delete(`/experience/types/${id}`)
}
