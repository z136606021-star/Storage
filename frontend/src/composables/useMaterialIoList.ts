import { reactive, ref } from 'vue'
import type { Dayjs } from 'dayjs'
import { fetchMaterialIoFilterOptions, fetchMaterialIoPage } from '@/api/warehouse/materialIo'
import {
  assignDefaultMaterialFields,
  defaultMaterialQuery,
  useWarehouseMaterialFilters,
} from '@/composables/useWarehouseMaterialFilters'
import { usePaginatedCrudList } from '@/composables/usePaginatedCrudList'
import { ALL_OPTION } from '@/constants/filter'
import type { MaterialIoRecord } from '@/types/warehouse/materialIo'
import { buildMaterialQueryParams } from '@/utils/warehouseMaterialTable'

export function defaultIoQuery() {
  return {
    ...defaultMaterialQuery(),
    ioType: ALL_OPTION,
    purpose: ALL_OPTION,
    projectRef: '',
  }
}

export interface UseMaterialIoListOptions {
  loadErrorMessage?: string
  paginationDefaults?: Record<string, unknown>
  enableRowSelection?: boolean
  getMaterialLedgerId?: () => number | undefined
  getRouteRecordIds?: () => number[] | undefined
}

export function useMaterialIoList(options: UseMaterialIoListOptions = {}) {
  const {
    loadErrorMessage = '加载物料出入库列表失败',
    paginationDefaults = { showSizeChanger: false, position: ['bottomCenter'] },
    enableRowSelection = true,
    getMaterialLedgerId,
    getRouteRecordIds,
  } = options

  const queryForm = reactive(defaultIoQuery())
  const operatedAtRange = ref<[Dayjs, Dayjs] | null>(null)

  const {
    filterOptions,
    reloadFilterOptions,
    handleCategoryChange,
    handleGenericNameChange,
    handleBrandChange,
  } = useWarehouseMaterialFilters(queryForm, fetchMaterialIoFilterOptions)

  function buildQueryParams() {
    const routeIds = getRouteRecordIds?.()
    return {
      ...buildMaterialQueryParams(queryForm),
      ioType: queryForm.ioType === ALL_OPTION ? undefined : queryForm.ioType,
      purpose: queryForm.purpose === ALL_OPTION ? undefined : queryForm.purpose,
      projectRef: queryForm.projectRef?.trim() || undefined,
      operatedAtStart: operatedAtRange.value?.[0]?.format('YYYY-MM-DD'),
      operatedAtEnd: operatedAtRange.value?.[1]?.format('YYYY-MM-DD'),
      materialLedgerId: getMaterialLedgerId?.(),
      ids: routeIds?.length ? routeIds : undefined,
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
  } = usePaginatedCrudList<MaterialIoRecord, ReturnType<typeof buildQueryParams>>({
    fetchPage: fetchMaterialIoPage,
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
    queryForm.ioType = ALL_OPTION
    queryForm.purpose = ALL_OPTION
    queryForm.projectRef = ''
    operatedAtRange.value = null
  }

  function handleReset() {
    resetQueryForm()
    handleResetQuery()
    clearSelection()
    refreshAll()
  }

  return {
    queryForm,
    operatedAtRange,
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
