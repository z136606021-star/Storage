export interface FileUploadPolicy {
  maxSizeBytes: number
}

export const DEFAULT_UPLOAD_POLICY: FileUploadPolicy = {
  maxSizeBytes: 5 * 1024 * 1024 * 1024,
}
