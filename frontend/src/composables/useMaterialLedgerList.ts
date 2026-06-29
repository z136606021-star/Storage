import { reactive } from 'vue'
import { fetchFilterOptions, fetchMaterialLedgerPage } from '@/api/materialLedger'
import {
  assignDefaultMaterialFields,
  defaultMaterialQuery,
  useWarehouseMaterialFilters,
} from '@/composables/useWarehouseMaterialFilters'
import { usePaginatedCrudList } from '@/composables/usePaginatedCrudList'
import type { MaterialLedger } from '@/types/materialLedger'
import { buildMaterialQueryParams } from '@/utils/warehouseMaterialTable'

export interface UseMaterialLedgerListOptions {
  loadErrorMessage?: string
  paginationDefaults?: Record<string, unknown>
  enableRowSelection?: boolean
}

export function useMaterialLedgerList(options: UseMaterialLedgerListOptions = {}) {
  const {
    loadErrorMessage = '加载物料台账失败',
    paginationDefaults = { showSizeChanger: false, position: ['bottomCenter'] },
    enableRowSelection = false,
  } = options

  const queryForm = reactive({ ...defaultMaterialQuery() })

  const {
    filterOptions,
    reloadFilterOptions,
    handleCategoryChange,
    handleGenericNameChange,
    handleBrandChange,
  } = useWarehouseMaterialFilters(queryForm, fetchFilterOptions)

  function buildQueryParams() {
    return buildMaterialQueryParams(queryForm)
  }

  const {
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
  } = usePaginatedCrudList<MaterialLedger, ReturnType<typeof buildQueryParams>>({
    fetchPage: fetchMaterialLedgerPage,
    buildQueryParams,
    loadErrorMessage,
    paginationDefaults,
    enableRowSelection,
  })

  async function refreshAll() {
    await reloadFilterOptions()
    await loadData()
  }

  function resetQueryForm() {
    assignDefaultMaterialFields(queryForm)
  }

  function handleReset() {
    resetQueryForm()
    handleResetQuery()
    clearSelection()
    refreshAll()
  }

  return {
    queryForm,
    filterOptions,
    reloadFilterOptions,
    handleCategoryChange,
    handleGenericNameChange,
    handleBrandChange,
    buildQueryParams,
    refreshAll,
    resetQueryForm,
    handleReset,
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
