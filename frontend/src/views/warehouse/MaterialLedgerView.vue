<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  exportMaterialLedger,
  fetchMaterialLedgerDetail,
} from '@/api/warehouse/materialLedger'
import CrudDetailDrawer from '@/components/common/CrudDetailDrawer.vue'
import CrudListPage from '@/components/common/CrudListPage.vue'
import MaterialIdentityDescriptions from '@/components/warehouse/MaterialIdentityDescriptions.vue'
import WarehouseMaterialFilterPanel from '@/components/warehouse/WarehouseMaterialFilterPanel.vue'
import { useCrudDetailDrawer } from '@/composables/useCrudDetailDrawer'
import { useExcelImportExport } from '@/composables/useExcelImportExport'
import { useMaterialLedgerList } from '@/composables/useMaterialLedgerList'
import { useMaterialLedgerRouteDetail } from '@/composables/useMaterialLedgerRouteDetail'
import { defaultMaterialQuery } from '@/composables/useWarehouseMaterialFilters'
import { useAuth } from '@/composables/useAuth'
import { useMenuStore } from '@/stores/menu'
import type { MaterialLedger } from '@/types/warehouse/materialLedger'
import { displayValue, formatDateTime, formatUnitPrice } from '@/utils/format'
import { getTableRowIndex } from '@/utils/tableIndex'
import { materialIdentityColumns } from '@/utils/warehouseMaterialTable'

const { hasPermission } = useAuth()
const menu = useMenuStore()
const route = useRoute()
const router = useRouter()

const canViewMaterialIo = computed(() => hasPermission('warehouse:material-io:read'))

const defaultQuery = {
  ...defaultMaterialQuery(),
}

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
  batchExporting,
  handleExport,
  handleBatchExport,
} = useExcelImportExport<ReturnType<typeof buildQueryParams>, { ids: number[] }>({
  exportFn: (params) => exportMaterialLedger(params ?? buildQueryParams()),
  batchExportFn: (params) => exportMaterialLedger(params),
  buildExportParams: buildQueryParams,
  buildBatchExportParams: (ids) => ({ ids }),
  getExportFilename: () => `物料台账-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  getBatchExportFilename: () => `物料台账-勾选-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  exportSuccessMessage: '导出成功',
  onAfterBatchExport: clearSelection,
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

function viewMaterialIoHistory() {
  if (!detailRecord.value) {
    return
  }
  const target = menu.findRouteByPermission('warehouse:material-io:read')
  if (!target) {
    return
  }
  router.push({
    path: target.path,
    query: { materialLedgerId: String(detailRecord.value.id) },
  })
}

function onBatchExport() {
  if (!hasSelection.value) {
    return
  }
  handleBatchExport(selectedRowKeys.value.map((key) => Number(key)))
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
    :row-selection="rowSelection"
    :custom-row="customRow"
    :scroll="{ x: 1180 }"
    :toolbar-show-create="false"
    :toolbar-show-import="false"
    :toolbar-show-export="true"
    :toolbar-show-template="false"
    toolbar-show-batch-export
    :toolbar-show-batch-delete="false"
    :toolbar-can-write="false"
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
      <template v-else-if="column.key === 'brand'">
        {{ displayValue(record.brand) }}
      </template>
      <template v-else-if="column.key === 'unitPrice'">
        {{ formatUnitPrice(record.unitPrice) || '-' }}
      </template>
      <template v-else-if="column.key === 'remark'">
        {{ displayValue(record.remark) }}
      </template>
      <template v-else-if="column.key === 'action'">
        <a-button type="link" size="small" @click="openDetail(record)">查看</a-button>
      </template>
    </template>
  </CrudListPage>

  <CrudDetailDrawer
    v-model:open="drawerOpen"
    title="物料详情"
    :width="480"
    :loading="detailLoading"
    :show-edit="false"
    @close="handleCloseDetail"
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

<style scoped lang="less">
@import '@/styles/variables.less';
@import '@/styles/mixins.less';

:deep(.ledger-row-highlight) td {
  .row-highlight();
}
</style>
