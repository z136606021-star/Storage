export interface FileUploadPolicy {
  maxSizeBytes: number
}

export const DEFAULT_UPLOAD_POLICY: FileUploadPolicy = {
  maxSizeBytes: 5_505_025_024,
}
