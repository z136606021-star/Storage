import { computed, reactive, ref } from 'vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import type { Key } from 'ant-design-vue/es/table/interface'
import { message } from 'ant-design-vue'
import { getErrorMessage } from '@/api/http'
import type { PageResult } from '@/types/common'

export interface PaginatedCrudListOptions<T, Q> {
  fetchPage: (params: Q & { page: number; pageSize: number }) => Promise<PageResult<T>>
  buildQueryParams: () => Q
  paginationDefaults?: Partial<TablePaginationConfig>
  loadErrorMessage?: string
  onAfterLoad?: (records: T[]) => void | Promise<void>
  enableRowSelection?: boolean
}

export function usePaginatedCrudList<T, Q>(options: PaginatedCrudListOptions<T, Q>) {
  const loading = ref(false)
  const dataSource = ref<T[]>([])
  const selectedRowKeys = ref<Key[]>([])

  const pagination = reactive<TablePaginationConfig>({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: false,
    showTotal: (total: number) => `共 ${total} 条`,
    ...options.paginationDefaults,
  })

  const hasSelection = computed(() => selectedRowKeys.value.length > 0)

  const rowSelection = options.enableRowSelection
    ? {
        selectedRowKeys,
        onChange: (keys: Key[]) => {
          selectedRowKeys.value = keys
        },
      }
    : undefined

  async function loadData() {
    loading.value = true
    try {
      const result = await options.fetchPage({
        ...options.buildQueryParams(),
        page: pagination.current ?? 1,
        pageSize: pagination.pageSize ?? 10,
      })
      dataSource.value = result.records
      pagination.total = result.total
      pagination.current = result.current
      pagination.pageSize = result.size
      await options.onAfterLoad?.(result.records)
    } catch (error) {
      message.error(getErrorMessage(error, options.loadErrorMessage ?? '加载列表失败'))
    } finally {
      loading.value = false
    }
  }

  function handleSearch() {
    pagination.current = 1
    loadData()
  }

  function handleResetQuery() {
    pagination.current = 1
  }

  function handleTableChange(pageConfig: TablePaginationConfig) {
    pagination.current = pageConfig.current ?? 1
    pagination.pageSize = pageConfig.pageSize ?? pagination.pageSize ?? 10
    loadData()
  }

  function clearSelection() {
    selectedRowKeys.value = []
  }

  function removeFromSelection(id: Key) {
    selectedRowKeys.value = selectedRowKeys.value.filter((key) => key !== id)
  }

  return {
    loading,
    dataSource,
    pagination,
    loadData,
    handleSearch,
    handleResetQuery,
    handleTableChange,
    selectedRowKeys,
    rowSelection,
    hasSelection,
    clearSelection,
    removeFromSelection,
  }
}
