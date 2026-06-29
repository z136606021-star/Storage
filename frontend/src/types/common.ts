export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface ImportResult {
  successCount: number
  failCount: number
  errors: Array<{ row: number; message: string }>
}
