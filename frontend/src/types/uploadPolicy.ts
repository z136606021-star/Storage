export interface FileUploadPolicy {
  maxSizeBytes: number
  maxFilesPerRecord: number
  uploadConcurrency: number
  allowedContentTypes: string[]
  imageContentTypes: string[]
}

export const DEFAULT_UPLOAD_POLICY: FileUploadPolicy = {
  maxSizeBytes: 50 * 1024 * 1024,
  maxFilesPerRecord: 20,
  uploadConcurrency: 3,
  allowedContentTypes: [
    'image/jpeg',
    'image/png',
    'image/webp',
    'image/gif',
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'text/plain',
  ],
  imageContentTypes: ['image/jpeg', 'image/png', 'image/webp', 'image/gif'],
}
