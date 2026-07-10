import { reactive } from 'vue'
import { fetchFilterOptions, fetchMaterialLedgerPage } from '@/api/warehouse/materialLedger'
import {
  assignDefaultMaterialFields,
  defaultMaterialQuery,
  useWarehouseMaterialFilters,
} from '@/composables/useWarehouseMaterialFilters'
import { usePaginatedCrudList } from '@/composables/usePaginatedCrudList'
import { DEFAULT_LEDGER_STOCK_STATUS } from '@/constants/materialStockStatus'
import type { MaterialLedger, MaterialStockStatus } from '@/types/warehouse/materialLedger'
import { buildMaterialQueryParams } from '@/utils/warehouseMaterialTable'

export type MaterialLedgerQueryForm = ReturnType<typeof defaultMaterialQuery> & {
  stockStatus?: MaterialStockStatus
}

export interface UseMaterialLedgerListOptions {
  loadErrorMessage?: string
  paginationDefaults?: Record<string, unknown>
  enableRowSelection?: boolean
  initialStockStatus?: MaterialStockStatus
}

export function defaultMaterialLedgerQuery(
  initialStockStatus: MaterialStockStatus = DEFAULT_LEDGER_STOCK_STATUS,
): MaterialLedgerQueryForm {
  return {
    ...defaultMaterialQuery(),
    stockStatus: initialStockStatus,
  }
}

export function buildMaterialLedgerQueryParams(queryForm: MaterialLedgerQueryForm) {
  return {
    ...buildMaterialQueryParams(queryForm),
    stockStatus: queryForm.stockStatus,
  }
}

export function useMaterialLedgerList(options: UseMaterialLedgerListOptions = {}) {
  const {
    loadErrorMessage = '加载物料台账失败',
    paginationDefaults = { position: ['bottomCenter'] },
    enableRowSelection = false,
    initialStockStatus = DEFAULT_LEDGER_STOCK_STATUS,
  } = options

  const queryForm = reactive(defaultMaterialLedgerQuery(initialStockStatus))

  const {
    filterOptions,
    reloadFilterOptions,
    handleCategoryChange,
    handleGenericNameChange,
    handleBrandChange,
  } = useWarehouseMaterialFilters(queryForm, fetchFilterOptions)

  function buildQueryParams() {
    return buildMaterialLedgerQueryParams(queryForm)
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
    queryForm.stockStatus = initialStockStatus
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
    initialStockStatus,
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
