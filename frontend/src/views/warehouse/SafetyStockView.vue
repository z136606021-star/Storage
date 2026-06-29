<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { exportSafetyStock, fetchSafetyStockDetail } from '@/api/safetyStock'
import CrudDetailDrawer from '@/components/common/CrudDetailDrawer.vue'
import CrudListPage from '@/components/common/CrudListPage.vue'
import CrudRowActions from '@/components/common/CrudRowActions.vue'
import MaterialIdentityDescriptions from '@/components/warehouse/MaterialIdentityDescriptions.vue'
import SafetyStockDetailDescriptions from '@/components/warehouse/SafetyStockDetailDescriptions.vue'
import SafetyStockFormModal from '@/components/warehouse/SafetyStockFormModal.vue'
import WarehouseMaterialFilterPanel from '@/components/warehouse/WarehouseMaterialFilterPanel.vue'
import { useCrudDetailDrawer } from '@/composables/useCrudDetailDrawer'
import { useExcelImportExport } from '@/composables/useExcelImportExport'
import { useSafetyStockList } from '@/composables/useSafetyStockList'
import { useWritePermission } from '@/composables/useWritePermission'
import { useAuth } from '@/composables/useAuth'
import { ALL_OPTION } from '@/constants/filter'
import type { SafetyStockRecord } from '@/types/safetyStock'
import { displayValue } from '@/utils/format'
import { getTableRowIndex } from '@/utils/tableIndex'
import { materialIdentityColumns } from '@/utils/warehouseMaterialTable'

const { canWrite } = useWritePermission('warehouse:safety-stock:write')
const { hasPermission } = useAuth()

const canViewMaterialLedger = computed(() => hasPermission('warehouse:material-ledger:read'))
const canViewMaterialIo = computed(() => hasPermission('warehouse:material-io:read'))

const WARNING_PERIOD_OPTIONS = [
  { label: '全部', value: ALL_OPTION },
  { label: '是', value: '是' },
  { label: '否', value: '否' },
]

const formOpen = ref(false)
const editingRecord = ref<SafetyStockRecord | null>(null)

const {
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
} = useSafetyStockList({
  loadErrorMessage: '加载安全库存列表失败，请确认后端服务已启动',
  enableRowSelection: true,
})

const {
  drawerOpen,
  detailLoading,
  detailRecord,
  openDetail,
  closeDetail,
} = useCrudDetailDrawer(fetchSafetyStockDetailByLedgerId, '加载安全库存详情失败')

async function fetchSafetyStockDetailByLedgerId(materialLedgerId: number) {
  return fetchSafetyStockDetail(materialLedgerId)
}

const {
  exporting,
  batchExporting,
  handleExport,
  handleBatchExport,
} = useExcelImportExport<ReturnType<typeof buildQueryParams>, { ids: number[] }>({
  exportFn: (params) => exportSafetyStock(params ?? buildQueryParams()),
  batchExportFn: (params) => exportSafetyStock(params),
  importFn: async () => ({ successCount: 0, failCount: 0, errors: [] }),
  buildExportParams: buildQueryParams,
  buildBatchExportParams: (ids) => ({ ...buildQueryParams(), ids }),
  getExportFilename: () => `安全库存-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  getBatchExportFilename: () => `安全库存-勾选-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  exportSuccessMessage: '导出成功',
})

const columns = [
  { title: '序号', key: 'index', width: 64, align: 'center' as const },
  ...materialIdentityColumns('ledgerList'),
  {
    title: '库存数量',
    dataIndex: 'stockQuantity',
    key: 'stockQuantity',
    width: 96,
    align: 'center' as const,
  },
  {
    title: '安全库存数',
    dataIndex: 'safetyQuantity',
    key: 'safetyQuantity',
    width: 110,
    align: 'center' as const,
  },
  {
    title: '预警期',
    dataIndex: 'inWarningPeriod',
    key: 'inWarningPeriod',
    width: 88,
    align: 'center' as const,
  },
  { title: '操作', key: 'action', width: 140, align: 'center' as const, fixed: 'right' as const },
]

function customRow(record: SafetyStockRecord) {
  return {
    class: record.inWarningPeriod ? 'safety-stock-warning-row' : '',
  }
}

function formatWarningPeriod(inWarningPeriod: boolean) {
  return inWarningPeriod ? '是' : '否'
}

function handleView(record: SafetyStockRecord) {
  openDetail({ id: record.materialLedgerId })
}

function handleEdit(record: SafetyStockRecord) {
  editingRecord.value = record
  formOpen.value = true
}

function handleEditFromDrawer() {
  if (!detailRecord.value) {
    return
  }
  editingRecord.value = detailRecord.value
  drawerOpen.value = false
  formOpen.value = true
}

function onBatchExport() {
  if (!hasSelection.value) {
    return
  }
  handleBatchExport(selectedRowKeys.value.map((key) => Number(key)))
}

async function onFormSuccess() {
  await refreshAll()
  if (detailRecord.value) {
    await openDetail({ id: detailRecord.value.materialLedgerId })
  }
}

onMounted(async () => {
  try {
    await reloadFilterOptions()
  } catch {
    message.warning('筛选选项加载失败，将使用默认选项')
  }
  await loadData()
})
</script>

<template>
  <CrudListPage
    :columns="columns"
    :loading="loading"
    :data-source="dataSource"
    :pagination="pagination"
    :row-key="(record: SafetyStockRecord) => record.materialLedgerId"
    :row-selection="rowSelection"
    :custom-row="customRow"
    :scroll="{ x: 1400 }"
    toolbar-export-icon="download"
    :toolbar-show-create="false"
    :toolbar-show-import="false"
    :toolbar-show-template="false"
    toolbar-show-batch-export
    :toolbar-can-write="canWrite"
    :toolbar-exporting="exporting"
    :toolbar-batch-exporting="batchExporting"
    :toolbar-has-selection="hasSelection"
    @change="handleTableChange"
    @toolbar-export="handleExport"
    @toolbar-batch-export="onBatchExport"
  >
    <template #filters>
      <WarehouseMaterialFilterPanel
        :query-form="queryForm"
        :filter-options="filterOptions"
        @category-change="handleCategoryChange"
        @generic-name-change="handleGenericNameChange"
        @brand-change="handleBrandChange"
        @search="handleSearch"
      >
        <template #second-row-trailing>
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="安全库存数" class="filter-item">
              <a-input
                v-model:value="queryForm.safetyQuantityKeyword"
                placeholder="关键字查找"
                allow-clear
                class="filter-control"
                @press-enter="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="预警期" class="filter-item">
              <a-select
                v-model:value="queryForm.warningPeriod"
                :options="WARNING_PERIOD_OPTIONS"
                class="filter-control"
              />
            </a-form-item>
          </a-col>
        </template>

        <template #actions>
          <a-space>
            <a-button type="primary" @click="handleSearch">
              <template #icon><SearchOutlined /></template>
              查询
            </a-button>
            <a-button @click="handleReset">
              <template #icon><ReloadOutlined /></template>
              重置
            </a-button>
          </a-space>
        </template>
      </WarehouseMaterialFilterPanel>
    </template>

    <template #bodyCell="{ column, record, index }">
      <template v-if="column.key === 'index'">
        {{ getTableRowIndex(index, pagination) }}
      </template>
      <template v-else-if="column.key === 'inWarningPeriod'">
        <a-tag :color="record.inWarningPeriod ? 'warning' : 'default'">
          {{ formatWarningPeriod(record.inWarningPeriod) }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'brand'">
        {{ displayValue(record.brand) }}
      </template>
      <template v-else-if="column.key === 'action'">
        <CrudRowActions
          :can-write="canWrite"
          :show-delete="false"
          @view="handleView(record)"
          @edit="handleEdit(record)"
        />
      </template>
    </template>
  </CrudListPage>

  <CrudDetailDrawer
    v-model:open="drawerOpen"
    title="安全库存详情"
    :loading="detailLoading"
    :can-write="canWrite"
    @edit="handleEditFromDrawer"
    @close="closeDetail"
  >
    <template v-if="detailRecord">
      <MaterialIdentityDescriptions :record="detailRecord" />
      <SafetyStockDetailDescriptions
        :record="detailRecord"
        :show-ledger-link="canViewMaterialLedger"
        :show-material-io-link="canViewMaterialIo"
      />
    </template>
  </CrudDetailDrawer>

  <SafetyStockFormModal
    v-model:open="formOpen"
    :record="editingRecord"
    @success="onFormSuccess"
  />
</template>

<style scoped>
:deep(.safety-stock-warning-row) > td {
  background-color: #fffbe6 !important;
}

:deep(.safety-stock-warning-row:hover) > td {
  background-color: #fff1b8 !important;
}

.filter-item {
  margin-bottom: 0;
  width: 100%;
}

.filter-control {
  width: 100%;
}
</style>
