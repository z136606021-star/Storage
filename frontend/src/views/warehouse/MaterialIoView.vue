<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DownOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  batchDeleteMaterialIo,
  deleteMaterialIo,
  downloadMaterialIoImportTemplate,
  exportMaterialIo,
  fetchMaterialIoDetail,
  importMaterialIo,
} from '@/api/warehouse/materialIo'
import CrudDetailDrawer from '@/components/common/CrudDetailDrawer.vue'
import CrudListPage from '@/components/common/CrudListPage.vue'
import CrudRowActions from '@/components/common/CrudRowActions.vue'
import MaterialIoBatchFormModal from '@/components/warehouse/MaterialIoBatchFormModal.vue'
import MaterialIoContextBar from '@/components/warehouse/MaterialIoContextBar.vue'
import MaterialIoDetailDescriptions from '@/components/warehouse/MaterialIoDetailDescriptions.vue'
import MaterialIoFilterPanel from '@/components/warehouse/MaterialIoFilterPanel.vue'
import MaterialIdentityDescriptions from '@/components/warehouse/MaterialIdentityDescriptions.vue'
import { useCrudDetailDrawer } from '@/composables/useCrudDetailDrawer'
import { useExcelImportExport } from '@/composables/useExcelImportExport'
import { useMaterialLedgerDeepLink } from '@/composables/useMaterialLedgerDeepLink'
import { useMaterialIoList } from '@/composables/useMaterialIoList'
import { useMaterialIoRouteDetail } from '@/composables/useMaterialIoRouteDetail'
import { useWritePermission } from '@/composables/useWritePermission'
import { useAuth } from '@/composables/useAuth'
import type { IoType, MaterialIoRecord } from '@/types/warehouse/materialIo'
import { confirmBatchDelete, confirmDelete } from '@/utils/confirmDelete'
import { formatDateTime } from '@/utils/format'
import {
  formatIoTypeLabel,
  formatOperator,
  formatPurposeLabel,
  getIoTypeTagColor,
} from '@/utils/materialIo'
import { parseIoRecordIdFromQuery } from '@/utils/materialIoRouteQuery'
import { getTableRowIndex } from '@/utils/tableIndex'
import { materialIdentityColumns } from '@/utils/warehouseMaterialTable'

const { canWrite } = useWritePermission('warehouse:material-io:write')
const { hasPermission } = useAuth()
const canViewMaterialLedger = computed(() => hasPermission('warehouse:material-ledger:read'))

const route = useRoute()
const router = useRouter()

const formOpen = ref(false)
const editingRecord = ref<MaterialIoRecord | null>(null)
const pendingInitialIoType = ref<IoType>('IN')

const {
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
} = useMaterialIoList({
  loadErrorMessage: '加载物料出入库列表失败',
  enableRowSelection: true,
  getMaterialLedgerId: () => materialLedgerIdParam(),
  getRouteRecordIds: () => {
    const id = parseIoRecordIdFromQuery(route.query.id)
    return id != null ? [id] : undefined
  },
})

const {
  materialContext,
  materialLedgerIdParam,
  clearMaterialLedgerFilter,
  initFromRoute,
  setupRouteWatch,
  clearDeepLinkOnReset,
} = useMaterialLedgerDeepLink({ route, router, pagination, loadData })

const {
  drawerOpen,
  detailLoading,
  detailRecord,
  openDetail,
  closeDetail,
} = useCrudDetailDrawer(fetchMaterialIoDetail, '加载出入库详情失败')

const {
  initFromRoute: initIoRouteDetail,
  setupRouteWatch: setupIoRouteWatch,
  clearRouteDetailQuery,
  customRow,
} = useMaterialIoRouteDetail({
  route,
  router,
  openDetail,
  onRouteIdChange: async () => {
    await loadData()
  },
})

async function handleCloseDetail() {
  closeDetail()
  await clearRouteDetailQuery()
}

function handleViewDetail(record: MaterialIoRecord) {
  router.replace({
    path: route.path,
    query: { ...route.query, id: String(record.id) },
  })
}

function handleReset() {
  resetQueryForm()
  clearDeepLinkOnReset()
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
  exportFn: (params) => exportMaterialIo(params ?? buildQueryParams()),
  batchExportFn: (params) => exportMaterialIo(params),
  importFn: importMaterialIo,
  templateFn: downloadMaterialIoImportTemplate,
  buildExportParams: buildQueryParams,
  buildBatchExportParams: (ids) => ({ ids }),
  getExportFilename: () => `物料出入库-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  getBatchExportFilename: () => `物料出入库-勾选-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  getTemplateFilename: () => '物料出入库导入模板.xlsx',
  exportSuccessMessage: '导出成功',
  showFirstImportError: true,
  onAfterImport: refreshAll,
})

const columns = [
  { title: '序号', key: 'index', width: 64, align: 'center' as const },
  ...materialIdentityColumns('ioList'),
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 72, align: 'center' as const },
  { title: '用途', dataIndex: 'purpose', key: 'purpose', width: 110, align: 'center' as const },
  { title: '项目编号', dataIndex: 'projectRef', key: 'projectRef', width: 100, ellipsis: true },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 140, ellipsis: true },
  { title: '操作类型', dataIndex: 'ioType', key: 'ioType', width: 88, align: 'center' as const },
  { title: '操作人', dataIndex: 'operatorDisplayName', key: 'operator', width: 88, ellipsis: true },
  { title: '操作时间', dataIndex: 'operatedAt', key: 'operatedAt', width: 168 },
  { title: '操作', key: 'action', width: 160, align: 'center' as const, fixed: 'right' as const },
]

function handleMoreMenuClick({ key }: { key: string }) {
  if (key === 'template') {
    handleDownloadTemplate()
  } else if (key === 'batch-export') {
    onBatchExport()
  } else if (key === 'batch-delete') {
    handleBatchDelete()
  }
}

function handleCreateWithType(type: IoType) {
  editingRecord.value = null
  pendingInitialIoType.value = type
  formOpen.value = true
}

function handleCreateMenuClick({ key }: { key: string }) {
  handleCreateWithType(key === 'OUT' ? 'OUT' : 'IN')
}

function handleEdit(record: MaterialIoRecord) {
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

function handleDelete(record: MaterialIoRecord) {
  confirmDelete({
    title: '确认删除',
    content: `确定删除该条${formatIoTypeLabel(record.ioType)}记录吗？`,
    successMessage: '删除成功',
    onDelete: async () => {
      await deleteMaterialIo(record.id)
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
    entityLabel: '出入库记录',
    onDelete: async () => {
      const ids = selectedRowKeys.value.map((key) => Number(key))
      await batchDeleteMaterialIo(ids)
    },
    onSuccess: async () => {
      clearSelection()
      await refreshAll()
    },
  })
}

function viewMaterialLedger() {
  if (!detailRecord.value?.materialLedgerId) {
    return
  }
  router.push({
    path: '/warehouse/material-ledger',
    query: { materialLedgerId: String(detailRecord.value.materialLedgerId) },
  })
}

onMounted(async () => {
  try {
    await reloadFilterOptions()
  } catch {
    message.warning('筛选选项加载失败，将使用默认选项')
  }
  const applied = await initFromRoute()
  if (!applied) {
    await loadData()
  }
  await initIoRouteDetail()
})

setupRouteWatch()
setupIoRouteWatch()
</script>

<template>
  <CrudListPage
    :columns="columns"
    :loading="loading"
    :data-source="dataSource"
    :pagination="pagination"
    :row-key="(record: MaterialIoRecord) => record.id"
    :row-selection="canWrite ? rowSelection : undefined"
    :custom-row="customRow"
    :scroll="{ x: 1580 }"
    :toolbar-show-create="false"
    :toolbar-show-batch-export="false"
    :toolbar-show-template="false"
    :toolbar-show-batch-delete="false"
    :toolbar-can-write="canWrite"
    :toolbar-importing="importing"
    :toolbar-exporting="exporting"
    :toolbar-batch-exporting="batchExporting"
    :toolbar-has-selection="hasSelection"
    @change="handleTableChange"
    @toolbar-export="handleExport"
    @toolbar-import="handleImport"
  >
    <template #toolbarPrepend>
      <MaterialIoContextBar
        v-if="materialContext"
        :material="materialContext"
        :can-write="canWrite"
        @clear="clearMaterialLedgerFilter"
        @create="handleCreateWithType"
      />
      <a-dropdown v-if="canWrite">
        <a-button type="primary" class="btn-add-io">
          <template #icon><PlusOutlined /></template>
          新增
          <DownOutlined />
        </a-button>
        <template #overlay>
          <a-menu @click="handleCreateMenuClick">
            <a-menu-item key="IN">新增入库</a-menu-item>
            <a-menu-item key="OUT">新增出库</a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </template>

    <template #toolbarAppend>
      <a-dropdown>
        <a-button>
          更多
          <DownOutlined />
        </a-button>
        <template #overlay>
          <a-menu @click="handleMoreMenuClick">
            <a-menu-item key="template">下载模板</a-menu-item>
            <a-menu-item key="batch-export" :disabled="!hasSelection">批量导出</a-menu-item>
            <a-menu-item v-if="canWrite" key="batch-delete" :disabled="!hasSelection">
              批量删除
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </template>

    <template #filters>
      <MaterialIoFilterPanel
        v-model:operated-at-range="operatedAtRange"
        :query-form="queryForm"
        :filter-options="filterOptions"
        @category-change="handleCategoryChange"
        @generic-name-change="handleGenericNameChange"
        @brand-change="handleBrandChange"
        @search="handleSearch"
        @reset="handleReset"
      />
    </template>

    <template #bodyCell="{ column, record, index }">
      <template v-if="column.key === 'index'">
        {{ getTableRowIndex(index, pagination) }}
      </template>
      <template v-else-if="column.key === 'brand'">
        {{ record.brand ?? '' }}
      </template>
      <template v-else-if="column.key === 'remark'">
        {{ record.remark ?? '' }}
      </template>
      <template v-else-if="column.key === 'purpose'">
        <a-tag v-if="formatPurposeLabel(record.purpose, record.purposeLabel)">
          {{ formatPurposeLabel(record.purpose, record.purposeLabel) }}
        </a-tag>
        <span v-else>-</span>
      </template>
      <template v-else-if="column.key === 'projectRef'">
        {{ record.projectRef ?? '-' }}
      </template>
      <template v-else-if="column.key === 'ioType'">
        <a-tag :color="getIoTypeTagColor(record.ioType)">
          {{ formatIoTypeLabel(record.ioType) }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'operator'">
        {{ formatOperator(record) }}
      </template>
      <template v-else-if="column.key === 'operatedAt'">
        {{ formatDateTime(record.operatedAt) }}
      </template>
      <template v-else-if="column.key === 'action'">
        <CrudRowActions
          :can-write="canWrite"
          @view="handleViewDetail(record)"
          @edit="handleEdit(record)"
          @delete="handleDelete(record)"
        />
      </template>
    </template>
  </CrudListPage>

  <MaterialIoBatchFormModal
    v-model:open="formOpen"
    :record="editingRecord"
    :initial-ledger="editingRecord ? null : materialContext"
    :initial-io-type="pendingInitialIoType"
    @success="refreshAll"
  />

  <CrudDetailDrawer
    v-model:open="drawerOpen"
    title="出入库详情"
    :width="520"
    :loading="detailLoading"
    :show-edit="canWrite"
    @close="handleCloseDetail"
    @edit="handleEditFromDrawer"
  >
    <template v-if="detailRecord">
      <MaterialIdentityDescriptions :record="detailRecord" />
      <MaterialIoDetailDescriptions
        :record="detailRecord"
        :show-ledger-link="canViewMaterialLedger"
        show-copy-link
        @view-ledger="viewMaterialLedger"
      />
    </template>
  </CrudDetailDrawer>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';
@import '@/styles/mixins.less';

.btn-add-io {
  .btn-success-primary();
}

:deep(.io-row-highlight) td {
  .row-highlight();
}
</style>
