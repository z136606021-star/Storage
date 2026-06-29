import type { TablePaginationConfig } from 'ant-design-vue'

export function getTableRowIndex(
  index: number,
  pagination: TablePaginationConfig,
  pageSizeFallback = 10,
) {
  const current = pagination.current ?? 1
  const pageSize = pagination.pageSize ?? pageSizeFallback
  return (current - 1) * pageSize + index + 1
}
