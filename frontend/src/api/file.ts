import { http } from '@/api/http'
import type { FileUploadResult } from '@/types/file'

export async function uploadFile(file: File): Promise<FileUploadResult> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await http.post<FileUploadResult>('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
