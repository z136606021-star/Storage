import { reactive } from 'vue'
import { fetchSafetyStockFilterOptions, fetchSafetyStockPage } from '@/api/warehouse/safetyStock'
import {
  defaultMaterialQuery,
  useWarehouseMaterialFilters,
} from '@/composables/useWarehouseMaterialFilters'
import { usePaginatedCrudList } from '@/composables/usePaginatedCrudList'
import { ALL_OPTION } from '@/constants/filter'
import type { SafetyStockRecord } from '@/types/warehouse/safetyStock'
import { buildMaterialQueryParams } from '@/utils/warehouseMaterialTable'

export function defaultSafetyStockQuery() {
  return {
    ...defaultMaterialQuery(),
    safetyQuantityKeyword: '',
    warningPeriod: ALL_OPTION,
  }
}

export interface UseSafetyStockListOptions {
  loadErrorMessage?: string
  paginationDefaults?: Record<string, unknown>
  enableRowSelection?: boolean
}

export function useSafetyStockList(options: UseSafetyStockListOptions = {}) {
  const {
    loadErrorMessage = '加载安全库存列表失败',
    paginationDefaults = { showSizeChanger: false, position: ['bottomCenter'] },
    enableRowSelection = true,
  } = options

  const queryForm = reactive(defaultSafetyStockQuery())

  const {
    filterOptions,
    reloadFilterOptions,
    handleCategoryChange,
    handleGenericNameChange,
    handleBrandChange,
  } = useWarehouseMaterialFilters(queryForm, fetchSafetyStockFilterOptions)

  function buildQueryParams() {
    return {
      ...buildMaterialQueryParams(queryForm),
      safetyQuantityKeyword: queryForm.safetyQuantityKeyword.trim() || undefined,
      warningPeriod:
        queryForm.warningPeriod === ALL_OPTION ? undefined : queryForm.warningPeriod,
    }
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
  } = usePaginatedCrudList<SafetyStockRecord, ReturnType<typeof buildQueryParams>>({
    fetchPage: fetchSafetyStockPage,
    buildQueryParams,
    loadErrorMessage,
    paginationDefaults,
    enableRowSelection,
  })

  async function refreshAll() {
    await reloadFilterOptions()
    await loadData()
  }

  function handleReset() {
    Object.assign(queryForm, defaultSafetyStockQuery())
    handleResetQuery()
    clearSelection()
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
    loading,
    dataSource,
    pagination,
    loadData,
    handleSearch,
    handleReset,
    handleTableChange,
    selectedRowKeys,
    rowSelection,
    hasSelection,
    clearSelection,
    removeFromSelection,
  }
}
