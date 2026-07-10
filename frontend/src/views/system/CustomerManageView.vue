<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import {
  batchDeleteCustomers,
  deleteCustomer,
  downloadCustomerImportTemplate,
  exportCustomers,
  fetchCustomerDetail,
  fetchCustomerPage,
  importCustomers,
} from '@/api/system/customer'
import CrudDetailDrawer from '@/components/common/CrudDetailDrawer.vue'
import CrudListPage from '@/components/common/CrudListPage.vue'
import CrudRowActions from '@/components/common/CrudRowActions.vue'
import CustomerFormModal from '@/components/system/CustomerFormModal.vue'
import { useCrudDetailDrawer } from '@/composables/useCrudDetailDrawer'
import { useExcelImportExport } from '@/composables/useExcelImportExport'
import { usePaginatedCrudList } from '@/composables/usePaginatedCrudList'
import { useWritePermission } from '@/composables/useWritePermission'
import type { SysCustomer } from '@/types/system/customer'
import { confirmBatchDelete, confirmDelete } from '@/utils/confirmDelete'
import { displayValue, formatDateTime } from '@/utils/format'
import { getTableRowIndex } from '@/utils/tableIndex'

const { canWrite } = useWritePermission('system:customer:write')

const queryForm = reactive({
  customerCode: '',
  name: '',
  contactName: '',
})

const formOpen = ref(false)
const editingRecord = ref<SysCustomer | null>(null)

function buildQueryParams() {
  return {
    customerCode: queryForm.customerCode.trim() || undefined,
    name: queryForm.name.trim() || undefined,
    contactName: queryForm.contactName.trim() || undefined,
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
} = usePaginatedCrudList<SysCustomer, ReturnType<typeof buildQueryParams>>({
  fetchPage: fetchCustomerPage,
  buildQueryParams,
  loadErrorMessage: '加载客户列表失败',
  enableRowSelection: true,
})

const {
  drawerOpen,
  detailLoading,
  detailRecord,
  openDetail,
  closeDetail,
} = useCrudDetailDrawer(fetchCustomerDetail, '加载客户详情失败')

const {
  exporting,
  importing,
  batchExporting,
  handleExport,
  handleBatchExport,
  handleImport,
  handleDownloadTemplate,
} = useExcelImportExport<ReturnType<typeof buildQueryParams>, { ids: number[] }>({
  exportFn: (params) => exportCustomers(params ?? buildQueryParams()),
  batchExportFn: (params) => exportCustomers(params),
  importFn: importCustomers,
  templateFn: downloadCustomerImportTemplate,
  buildExportParams: buildQueryParams,
  buildBatchExportParams: (ids) => ({ ids }),
  getExportFilename: () => `客户列表-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  getBatchExportFilename: () => `客户列表-勾选-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  getTemplateFilename: () => '客户导入模板.xlsx',
  exportSuccessMessage: '导出成功',
  showFirstImportError: true,
  onAfterImport: loadData,
})

const columns = [
  { title: '序号', key: 'index', width: 64, align: 'center' as const },
  { title: '客户编号', dataIndex: 'customerCode', key: 'customerCode', width: 120 },
  { title: '客户名称', dataIndex: 'name', key: 'name', width: 160, ellipsis: true },
  { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 100, ellipsis: true },
  { title: '电话', dataIndex: 'phone', key: 'phone', width: 120 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 160, ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80, align: 'center' as const },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 140, ellipsis: true },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 168 },
  { title: '操作', key: 'action', width: 160, align: 'center' as const, fixed: 'right' as const },
]

function formatStatus(status: number) {
  return status === 1 ? '启用' : '停用'
}

function handleReset() {
  queryForm.customerCode = ''
  queryForm.name = ''
  queryForm.contactName = ''
  handleResetQuery()
  clearSelection()
  loadData()
}

function handleCreate() {
  editingRecord.value = null
  formOpen.value = true
}

function handleEdit(record: SysCustomer) {
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

function handleDelete(record: SysCustomer) {
  confirmDelete({
    title: '确认删除',
    content: `确定删除客户「${record.name}」吗？`,
    successMessage: '删除成功',
    onDelete: async () => {
      await deleteCustomer(record.id)
    },
    onSuccess: async () => {
      removeFromSelection(record.id)
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

function handleBatchDelete() {
  if (!hasSelection.value) {
    return
  }
  confirmBatchDelete({
    title: '批量删除',
    count: selectedRowKeys.value.length,
    content: `确定删除选中的 ${selectedRowKeys.value.length} 个客户吗？`,
    onDelete: async () => {
      const ids = selectedRowKeys.value.map((key) => Number(key))
      await batchDeleteCustomers(ids)
    },
    onSuccess: async () => {
      clearSelection()
      await loadData()
    },
  })
}

onMounted(loadData)
</script>

<template>
  <CrudListPage
    table-key="system.customer"
    :columns="columns"
    :loading="loading"
    :data-source="dataSource"
    :pagination="pagination"
    :row-key="(record: SysCustomer) => record.id"
    :row-selection="canWrite ? rowSelection : undefined"
    :scroll="{ x: 1320 }"
    toolbar-create-green
    toolbar-export-icon="download"
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
    <template #filters>
      <a-form layout="inline" class="filter-form">
        <a-row :gutter="[12, 8]" class="filter-row">
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="客户编号" class="filter-item">
              <a-input
                v-model:value="queryForm.customerCode"
                allow-clear
                placeholder="关键字查找"
                class="filter-control"
                @press-enter="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="客户名称" class="filter-item">
              <a-input
                v-model:value="queryForm.name"
                allow-clear
                placeholder="关键字查找"
                class="filter-control"
                @press-enter="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="联系人" class="filter-item">
              <a-input
                v-model:value="queryForm.contactName"
                allow-clear
                placeholder="关键字查找"
                class="filter-control"
                @press-enter="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="24" :md="24" :lg="6" class="filter-actions-col">
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
          </a-col>
        </a-row>
      </a-form>
    </template>

    <template #bodyCell="{ column, record, index }">
      <template v-if="column.key === 'index'">
        {{ getTableRowIndex(index, pagination) }}
      </template>
      <template v-else-if="column.key === 'contactName'">
        {{ displayValue(record.contactName) }}
      </template>
      <template v-else-if="column.key === 'phone'">
        {{ displayValue(record.phone) }}
      </template>
      <template v-else-if="column.key === 'email'">
        {{ displayValue(record.email) }}
      </template>
      <template v-else-if="column.key === 'status'">
        <a-tag :color="record.status === 1 ? 'success' : 'default'">
          {{ formatStatus(record.status) }}
        </a-tag>
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

  <CustomerFormModal v-model:open="formOpen" :record="editingRecord" @success="loadData" />

  <CrudDetailDrawer
    v-model:open="drawerOpen"
    title="客户详情"
    :loading="detailLoading"
    :show-edit="canWrite"
    @close="closeDetail"
    @edit="handleEditFromDrawer"
  >
    <a-descriptions v-if="detailRecord" :column="1" bordered size="small">
      <a-descriptions-item label="客户编号">
        {{ detailRecord.customerCode }}
      </a-descriptions-item>
      <a-descriptions-item label="客户名称">
        {{ detailRecord.name }}
      </a-descriptions-item>
      <a-descriptions-item label="联系人">
        {{ displayValue(detailRecord.contactName) }}
      </a-descriptions-item>
      <a-descriptions-item label="电话">
        {{ displayValue(detailRecord.phone) }}
      </a-descriptions-item>
      <a-descriptions-item label="邮箱">
        {{ displayValue(detailRecord.email) }}
      </a-descriptions-item>
      <a-descriptions-item label="地址">
        {{ displayValue(detailRecord.address) }}
      </a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="detailRecord.status === 1 ? 'success' : 'default'">
          {{ formatStatus(detailRecord.status) }}
        </a-tag>
      </a-descriptions-item>
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

<style scoped lang="less">
@import '@/styles/variables.less';
</style>
