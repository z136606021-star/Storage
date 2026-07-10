import { http } from '@/api/http'
import type { FileUploadResult } from '@/types/file'
import type { FileUploadPolicy } from '@/types/uploadPolicy'

const UPLOAD_TIMEOUT_MS = 300_000

export async function fetchUploadPolicy(): Promise<FileUploadPolicy> {
  const { data } = await http.get<FileUploadPolicy>('/files/upload-policy')
  return data
}

export async function uploadFile(file: File): Promise<FileUploadResult> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<FileUploadResult>('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: UPLOAD_TIMEOUT_MS,
  })
  return data
}

export async function downloadFile(objectKey: string): Promise<Blob> {
  const { data } = await http.get<Blob>('/files/download', {
    params: { objectKey },
    responseType: 'blob',
  })
  return data
}
