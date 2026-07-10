<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import {
  batchDeleteWarehouseBins,
  deleteWarehouseBin,
  downloadWarehouseBinImportTemplate,
  exportWarehouseBins,
  fetchWarehouseBinPage,
  importWarehouseBins,
} from '@/api/warehouse/warehouseBin'
import CrudListPage from '@/components/common/CrudListPage.vue'
import CrudRowActions from '@/components/common/CrudRowActions.vue'
import BinFormModal from '@/components/warehouse/BinFormModal.vue'
import { useExcelImportExport } from '@/composables/useExcelImportExport'
import { usePaginatedCrudList } from '@/composables/usePaginatedCrudList'
import { useWritePermission } from '@/composables/useWritePermission'
import type { WarehouseBin, WarehouseBinExportQuery } from '@/types/warehouse/warehouseBin'
import { confirmBatchDelete, confirmDelete } from '@/utils/confirmDelete'
import { displayValue, formatDateTime } from '@/utils/format'
import { getTableRowIndex } from '@/utils/tableIndex'

const { canWrite } = useWritePermission('warehouse:bin:write')

const queryForm = reactive({
  binCode: '',
})

const formOpen = ref(false)
const editingRecord = ref<WarehouseBin | null>(null)

function buildQueryParams() {
  return {
    binCode: queryForm.binCode.trim() || undefined,
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
} = usePaginatedCrudList<WarehouseBin, ReturnType<typeof buildQueryParams>>({
  fetchPage: fetchWarehouseBinPage,
  buildQueryParams,
  loadErrorMessage: '加载 Bin 位列表失败',
  enableRowSelection: true,
})

const {
  exporting,
  importing,
  batchExporting,
  handleExport,
  handleBatchExport,
  handleImport,
  handleDownloadTemplate,
} = useExcelImportExport<ReturnType<typeof buildQueryParams>, WarehouseBinExportQuery>({
  exportFn: (params) => exportWarehouseBins(params ?? buildQueryParams()),
  batchExportFn: (params) => exportWarehouseBins(params),
  importFn: importWarehouseBins,
  templateFn: downloadWarehouseBinImportTemplate,
  buildExportParams: buildQueryParams,
  buildBatchExportParams: (ids) => ({ ...buildQueryParams(), ids }),
  getExportFilename: () => `Bin位-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  getBatchExportFilename: () => `Bin位-勾选-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  getTemplateFilename: () => 'Bin位导入模板.xlsx',
  exportSuccessMessage: '导出成功',
  showFirstImportError: true,
  onAfterImport: loadData,
  onAfterBatchExport: clearSelection,
})

const columns = [
  { title: '序号', key: 'index', width: 64, align: 'center' as const },
  { title: 'Bin位', dataIndex: 'binCode', key: 'binCode', width: 100, align: 'center' as const },
  { title: '排', dataIndex: 'rowNo', key: 'rowNo', width: 72, align: 'center' as const },
  { title: '列', dataIndex: 'colNo', key: 'colNo', width: 72, align: 'center' as const },
  { title: '层', dataIndex: 'levelNo', key: 'levelNo', width: 72, align: 'center' as const },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true, minWidth: 120 },
  { title: '更新人', dataIndex: 'operatorName', key: 'operatorName', width: 120, ellipsis: true },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 170 },
  { title: '操作', key: 'action', width: 140, align: 'center' as const, fixed: 'right' as const },
]

function handleReset() {
  queryForm.binCode = ''
  handleResetQuery()
  clearSelection()
  loadData()
}

function handleCreate() {
  editingRecord.value = null
  formOpen.value = true
}

function handleEdit(record: WarehouseBin) {
  editingRecord.value = record
  formOpen.value = true
}

function handleDelete(record: WarehouseBin) {
  confirmDelete({
    title: '确认删除',
    content: `确定删除 Bin 位「${record.binCode}」吗？`,
    successMessage: '删除成功',
    onDelete: async () => {
      await deleteWarehouseBin(record.id)
    },
    onSuccess: async () => {
      removeFromSelection(record.id)
      await loadData()
    },
  })
}

function handleBatchDelete() {
  if (!hasSelection.value) {
    return
  }
  confirmBatchDelete({
    title: '批量删除',
    count: selectedRowKeys.value.length,
    content: `确定删除选中的 ${selectedRowKeys.value.length} 个 Bin 位吗？`,
    okText: '确定',
    onDelete: async () => {
      const ids = selectedRowKeys.value.map((key) => Number(key))
      await batchDeleteWarehouseBins(ids)
    },
    onSuccess: async () => {
      clearSelection()
      await loadData()
    },
  })
}

function onBatchExport() {
  if (!hasSelection.value) {
    return
  }
  handleBatchExport(selectedRowKeys.value.map((key) => Number(key)))
}

async function onFormSuccess() {
  pagination.current = 1
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <CrudListPage
    table-key="warehouse.bin"
    :columns="columns"
    :loading="loading"
    :data-source="dataSource"
    :pagination="pagination"
    :row-key="(record: WarehouseBin) => record.id"
    :row-selection="canWrite ? rowSelection : undefined"
    :scroll="{ x: 1180 }"
    toolbar-create-green
    toolbar-export-icon="download"
    :toolbar-show-export="true"
    toolbar-show-batch-export
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
    <template #toolbarPrepend>
      <div class="bin-toolbar-filters">
        <span class="bin-toolbar-label">Bin位</span>
        <a-input
          v-model:value="queryForm.binCode"
          allow-clear
          placeholder="关键字查找"
          class="bin-toolbar-input"
          @press-enter="handleSearch"
        />
        <a-button type="primary" @click="handleSearch">
          <template #icon><SearchOutlined /></template>
          查询
        </a-button>
        <a-button @click="handleReset">
          <template #icon><ReloadOutlined /></template>
          重置
        </a-button>
      </div>
    </template>

    <template #bodyCell="{ column, record, index }">
      <template v-if="column.key === 'index'">
        {{ getTableRowIndex(index, pagination) }}
      </template>
      <template v-else-if="column.key === 'colNo'">
        {{ displayValue(record.colNo) }}
      </template>
      <template v-else-if="column.key === 'levelNo'">
        {{ displayValue(record.levelNo) }}
      </template>
      <template v-else-if="column.key === 'remark'">
        {{ displayValue(record.remark) }}
      </template>
      <template v-else-if="column.key === 'operatorName'">
        {{ displayValue(record.operatorName) }}
      </template>
      <template v-else-if="column.key === 'updatedAt'">
        {{ formatDateTime(record.updatedAt) }}
      </template>
      <template v-else-if="column.key === 'action'">
        <CrudRowActions
          :can-write="canWrite"
          :show-view="false"
          @edit="handleEdit(record)"
          @delete="handleDelete(record)"
        />
      </template>
    </template>
  </CrudListPage>

  <BinFormModal v-model:open="formOpen" :record="editingRecord" @success="onFormSuccess" />
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.bin-toolbar-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: @spacing-sm;
  margin-right: @spacing-md;
}

.bin-toolbar-label {
  flex-shrink: 0;
  color: @color-text-secondary;
  font-size: @font-size-sm;
}

.bin-toolbar-input {
  width: 200px;
}
</style>
