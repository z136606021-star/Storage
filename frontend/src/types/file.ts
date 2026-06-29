export interface FileUploadResult {
  id: number
  objectKey: string
  originalName: string
  contentType: string | null
  sizeBytes: number
  url: string | null
}
