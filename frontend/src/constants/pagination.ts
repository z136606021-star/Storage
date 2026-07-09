export const DEFAULT_PAGE_SIZE = 10
export const PAGE_SIZE_OPTIONS = ['10', '20', '50', '100'] as const

export const defaultTablePagination = {
  current: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: [...PAGE_SIZE_OPTIONS],
  showTotal: (total: number) => `共 ${total} 条`,
}
