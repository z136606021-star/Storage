export interface FileUploadPolicy {
  maxSizeBytes: number
  maxFilesPerRecord: number
}

export const DEFAULT_UPLOAD_POLICY: FileUploadPolicy = {
  maxSizeBytes: 5 * 1024 * 1024 * 1024,
  maxFilesPerRecord: 20,
}
