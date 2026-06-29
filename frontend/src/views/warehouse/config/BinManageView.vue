<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import {
  batchDeleteWarehouseBins,
  deleteWarehouseBin,
  downloadWarehouseBinImportTemplate,
  exportWarehouseBins,
  fetchWarehouseBinDetail,
  fetchWarehouseBinPage,
  importWarehouseBins,
} from '@/api/warehouseBin'
import CrudDetailDrawer from '@/components/common/CrudDetailDrawer.vue'
import CrudListPage from '@/components/common/CrudListPage.vue'
import CrudRowActions from '@/components/common/CrudRowActions.vue'
import BinFormModal from '@/components/warehouse/BinFormModal.vue'
import { useCrudDetailDrawer } from '@/composables/useCrudDetailDrawer'
import { useExcelImportExport } from '@/composables/useExcelImportExport'
import { usePaginatedCrudList } from '@/composables/usePaginatedCrudList'
import { useWritePermission } from '@/composables/useWritePermission'
import type { WarehouseBin } from '@/types/warehouseBin'
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
  paginationDefaults: { showSizeChanger: true },
  enableRowSelection: true,
})

const {
  drawerOpen,
  detailLoading,
  detailRecord,
  openDetail,
  closeDetail,
} = useCrudDetailDrawer(fetchWarehouseBinDetail, '加载 Bin 位详情失败')

const {
  exporting,
  importing,
  handleExport,
  handleImport,
  handleDownloadTemplate,
} = useExcelImportExport({
  exportFn: (params) => exportWarehouseBins(params ?? buildQueryParams()),
  importFn: importWarehouseBins,
  templateFn: downloadWarehouseBinImportTemplate,
  buildExportParams: buildQueryParams,
  getExportFilename: () => `Bin位-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  getTemplateFilename: () => 'Bin位导入模板.xlsx',
  exportSuccessMessage: '导出成功',
  showFirstImportError: true,
  onAfterImport: loadData,
})

const columns = [
  { title: '序号', key: 'index', width: 64, align: 'center' as const },
  { title: 'Bin位', dataIndex: 'binCode', key: 'binCode', width: 100, align: 'center' as const },
  { title: '排', dataIndex: 'rowNo', key: 'rowNo', width: 72, align: 'center' as const },
  { title: '列', dataIndex: 'colNo', key: 'colNo', width: 72, align: 'center' as const },
  { title: '层', dataIndex: 'levelNo', key: 'levelNo', width: 72, align: 'center' as const },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true, minWidth: 120 },
  { title: '更新日期', dataIndex: 'updatedAt', key: 'updatedAt', width: 170 },
  { title: '操作', key: 'action', width: 160, align: 'center' as const, fixed: 'right' as const },
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

function handleEditFromDrawer() {
  if (!detailRecord.value) {
    return
  }
  editingRecord.value = detailRecord.value
  drawerOpen.value = false
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

async function onFormSuccess() {
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <CrudListPage
    :columns="columns"
    :loading="loading"
    :data-source="dataSource"
    :pagination="pagination"
    :row-key="(record: WarehouseBin) => record.id"
    :row-selection="canWrite ? rowSelection : undefined"
    :scroll="{ x: 960 }"
    toolbar-create-green
    toolbar-export-icon="download"
    :toolbar-show-batch-delete="canWrite"
    :toolbar-can-write="canWrite"
    :toolbar-importing="importing"
    :toolbar-exporting="exporting"
    :toolbar-has-selection="hasSelection"
    @change="handleTableChange"
    @toolbar-create="handleCreate"
    @toolbar-export="handleExport"
    @toolbar-batch-delete="handleBatchDelete"
    @toolbar-import="handleImport"
    @toolbar-download-template="handleDownloadTemplate"
  >
    <template #filters>
      <a-form layout="inline" class="filter-form">
        <a-form-item label="Bin位编号">
          <a-input
            v-model:value="queryForm.binCode"
            allow-clear
            placeholder="关键字查找"
            style="width: 200px"
            @press-enter="handleSearch"
          />
        </a-form-item>
        <a-form-item>
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
      </a-form>
    </template>

    <template #bodyCell="{ column, record, index }">
      <template v-if="column.key === 'index'">
        {{ getTableRowIndex(index, pagination) }}
      </template>
      <template v-else-if="column.key === 'remark'">
        {{ displayValue(record.remark) }}
      </template>
      <template v-else-if="column.key === 'updatedAt'">
        {{ formatDateTime(record.updatedAt) }}
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

  <BinFormModal v-model:open="formOpen" :record="editingRecord" @success="onFormSuccess" />

  <CrudDetailDrawer
    v-model:open="drawerOpen"
    title="Bin 位详情"
    :loading="detailLoading"
    :show-edit="canWrite"
    @close="closeDetail"
    @edit="handleEditFromDrawer"
  >
    <a-descriptions v-if="detailRecord" :column="1" bordered size="small">
      <a-descriptions-item label="Bin位编号">
        {{ detailRecord.binCode }}
      </a-descriptions-item>
      <a-descriptions-item label="排">{{ detailRecord.rowNo }}</a-descriptions-item>
      <a-descriptions-item label="列">{{ detailRecord.colNo }}</a-descriptions-item>
      <a-descriptions-item label="层">{{ detailRecord.levelNo }}</a-descriptions-item>
      <a-descriptions-item label="备注">
        {{ displayValue(detailRecord.remark) }}
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">
        {{ formatDateTime(detailRecord.createdAt) }}
      </a-descriptions-item>
      <a-descriptions-item label="更新时间">
        {{ formatDateTime(detailRecord.updatedAt) }}
      </a-descriptions-item>
    </a-descriptions>
  </CrudDetailDrawer>
</template>

<style scoped>
.filter-form {
  row-gap: 8px;
}
</style>
