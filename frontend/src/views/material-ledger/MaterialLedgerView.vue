<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  batchDeleteMaterialLedger,
  deleteMaterialLedger,
  downloadImportTemplate,
  exportMaterialLedger,
  fetchMaterialLedgerDetail,
  importMaterialLedger,
} from '@/api/materialLedger'
import CrudDetailDrawer from '@/components/common/CrudDetailDrawer.vue'
import CrudListPage from '@/components/common/CrudListPage.vue'
import CrudRowActions from '@/components/common/CrudRowActions.vue'
import MaterialLedgerFormModal from '@/components/warehouse/MaterialLedgerFormModal.vue'
import MaterialIdentityDescriptions from '@/components/warehouse/MaterialIdentityDescriptions.vue'
import WarehouseMaterialFilterPanel from '@/components/warehouse/WarehouseMaterialFilterPanel.vue'
import { useCrudDetailDrawer } from '@/composables/useCrudDetailDrawer'
import { useExcelImportExport } from '@/composables/useExcelImportExport'
import { useMaterialLedgerList } from '@/composables/useMaterialLedgerList'
import { useMaterialLedgerRouteDetail } from '@/composables/useMaterialLedgerRouteDetail'
import { defaultMaterialQuery } from '@/composables/useWarehouseMaterialFilters'
import { useWritePermission } from '@/composables/useWritePermission'
import { useAuth } from '@/composables/useAuth'
import type { MaterialLedger } from '@/types/materialLedger'
import { confirmBatchDelete, confirmDelete } from '@/utils/confirmDelete'
import { displayValue, formatDateTime, formatUnitPrice } from '@/utils/format'
import { getTableRowIndex } from '@/utils/tableIndex'
import { materialIdentityColumns } from '@/utils/warehouseMaterialTable'

const { canWrite } = useWritePermission('warehouse:material-ledger:write')
const { hasPermission } = useAuth()
const route = useRoute()
const router = useRouter()

const canViewMaterialIo = computed(() => hasPermission('warehouse:material-io:read'))

const defaultQuery = {
  ...defaultMaterialQuery(),
}

const formOpen = ref(false)
const editingRecord = ref<MaterialLedger | null>(null)

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
  handleResetQuery,
  handleTableChange,
  selectedRowKeys,
  rowSelection,
  hasSelection,
  clearSelection,
  removeFromSelection,
} = useMaterialLedgerList({
  loadErrorMessage: '加载物料台账失败，请确认后端服务已启动',
  enableRowSelection: true,
})

const {
  drawerOpen,
  detailLoading,
  detailRecord,
  openDetail,
  closeDetail,
} = useCrudDetailDrawer(fetchMaterialLedgerDetail, '加载物料详情失败')

const {
  initFromRoute,
  setupRouteWatch,
  clearRouteDetailQuery,
  customRow,
} = useMaterialLedgerRouteDetail({ route, router, openDetail })

function handleCloseDetail() {
  closeDetail()
  clearRouteDetailQuery()
}

function handleReset() {
  Object.assign(queryForm, defaultQuery)
  handleResetQuery()
  clearSelection()
  refreshAll()
}

const {
  exporting,
  importing,
  batchExporting,
  handleExport,
  handleBatchExport,
  handleImport,
  handleDownloadTemplate,
} = useExcelImportExport<ReturnType<typeof buildQueryParams>, { ids: number[] }>({
  exportFn: (params) => exportMaterialLedger(params ?? buildQueryParams()),
  batchExportFn: (params) => exportMaterialLedger(params),
  importFn: importMaterialLedger,
  templateFn: downloadImportTemplate,
  buildExportParams: buildQueryParams,
  buildBatchExportParams: (ids) => ({ ids }),
  getExportFilename: () => `物料台账-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  getBatchExportFilename: () => `物料台账-勾选-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  getTemplateFilename: () => '物料台账导入模板.xlsx',
  exportSuccessMessage: '导出成功',
  showFirstImportError: true,
  onAfterImport: refreshAll,
})

const columns = [
  { title: '序号', key: 'index', width: 64, align: 'center' as const },
  ...materialIdentityColumns('ledgerList'),
  {
    title: '库存数量',
    dataIndex: 'stockQuantity',
    key: 'stockQuantity',
    width: 90,
    align: 'center' as const,
  },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 80, align: 'center' as const },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true, minWidth: 100 },
  { title: '操作', key: 'action', width: 140, align: 'center' as const, fixed: 'right' as const },
]

function handleCreate() {
  editingRecord.value = null
  formOpen.value = true
}

function handleEdit(record: MaterialLedger) {
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

function viewMaterialIoHistory() {
  if (!detailRecord.value) {
    return
  }
  router.push({
    path: '/warehouse/material-io',
    query: { materialLedgerId: String(detailRecord.value.id) },
  })
}

function onBatchExport() {
  if (!hasSelection.value) {
    return
  }
  handleBatchExport(selectedRowKeys.value.map((key) => Number(key)))
}

function handleDelete(record: MaterialLedger) {
  confirmDelete({
    title: '确认删除',
    content: `确定删除物料「${record.name}」吗？`,
    successMessage: '删除成功',
    onDelete: async () => {
      await deleteMaterialLedger(record.id)
    },
    onSuccess: async () => {
      removeFromSelection(record.id)
      await refreshAll()
    },
  })
}

function handleBatchDelete() {
  if (!hasSelection.value) {
    return
  }
  confirmBatchDelete({
    count: selectedRowKeys.value.length,
    entityLabel: '物料',
    onDelete: async () => {
      const ids = selectedRowKeys.value.map((key) => Number(key))
      await batchDeleteMaterialLedger(ids)
    },
    onSuccess: async () => {
      clearSelection()
      await refreshAll()
    },
  })
}

onMounted(async () => {
  try {
    await reloadFilterOptions()
  } catch {
    message.warning('筛选选项加载失败，将使用默认选项')
  }
  await loadData()
  await initFromRoute()
})

setupRouteWatch()
</script>

<template>
  <CrudListPage
    :columns="columns"
    :loading="loading"
    :data-source="dataSource"
    :pagination="pagination"
    :row-key="(record: MaterialLedger) => record.id"
    :row-selection="canWrite ? rowSelection : undefined"
    :custom-row="customRow"
    :scroll="{ x: 1180 }"
    toolbar-create-green
    toolbar-show-batch-export
    :toolbar-template-requires-write="false"
    :toolbar-show-batch-delete="canWrite"
    :toolbar-can-write="canWrite"
    :toolbar-importing="importing"
    :toolbar-exporting="exporting"
    :toolbar-batch-exporting="batchExporting"
    :toolbar-has-selection="hasSelection"
    @change="handleTableChange"
    @toolbar-create="handleCreate"
    @toolbar-export="handleExport"
    @toolbar-batch-export="onBatchExport"
    @toolbar-batch-delete="handleBatchDelete"
    @toolbar-import="handleImport"
    @toolbar-download-template="handleDownloadTemplate"
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
        <template #actions>
          <a-form-item class="filter-item filter-actions">
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
          </a-form-item>
        </template>
      </WarehouseMaterialFilterPanel>
    </template>

    <template #bodyCell="{ column, record, index }">
      <template v-if="column.key === 'index'">
        {{ getTableRowIndex(index, pagination) }}
      </template>
      <template v-else-if="column.key === 'brand'">
        {{ record.brand ?? '' }}
      </template>
      <template v-else-if="column.key === 'unitPrice'">
        {{ formatUnitPrice(record.unitPrice) }}
      </template>
      <template v-else-if="column.key === 'remark'">
        {{ record.remark ?? '' }}
      </template>
      <template v-else-if="column.key === 'action'">
        <CrudRowActions
          :can-write="canWrite"
          @view="openDetail(record)"
          @edit="handleEdit(record)"
          @delete="handleDelete(record)"
        />
      </template>
    </template>
  </CrudListPage>

  <MaterialLedgerFormModal
    v-model:open="formOpen"
    :record="editingRecord"
    @success="refreshAll"
  />

  <CrudDetailDrawer
    v-model:open="drawerOpen"
    title="物料详情"
    :width="480"
    :loading="detailLoading"
    :show-edit="canWrite"
    @close="handleCloseDetail"
    @edit="handleEditFromDrawer"
  >
    <template v-if="detailRecord">
      <MaterialIdentityDescriptions :record="detailRecord" />

      <a-descriptions title="库存信息" :column="1" bordered size="small" class="detail-block">
        <a-descriptions-item label="库存数量">
          {{ displayValue(detailRecord.stockQuantity) }}
        </a-descriptions-item>
        <a-descriptions-item label="单价">
          {{ formatUnitPrice(detailRecord.unitPrice) || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="备注">
          {{ displayValue(detailRecord.remark) }}
        </a-descriptions-item>
        <a-descriptions-item v-if="canViewMaterialIo" label="出入库">
          <a-button type="link" class="io-history-link" @click="viewMaterialIoHistory">
            查看出入库记录
          </a-button>
        </a-descriptions-item>
      </a-descriptions>

      <a-descriptions title="系统信息" :column="1" bordered size="small" class="detail-block">
        <a-descriptions-item label="创建时间">
          {{ formatDateTime(detailRecord.createdAt) }}
        </a-descriptions-item>
        <a-descriptions-item label="更新时间">
          {{ formatDateTime(detailRecord.updatedAt) }}
        </a-descriptions-item>
      </a-descriptions>
    </template>
  </CrudDetailDrawer>
</template>

<style scoped>
:deep(.ledger-row-highlight) td {
  background-color: #e6f4ff;
}
</style>
