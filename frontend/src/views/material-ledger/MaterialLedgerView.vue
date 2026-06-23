<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import type { Key } from 'ant-design-vue/es/table/interface'
import {
  DeleteOutlined,
  ExportOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import { Modal, message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { getErrorMessage } from '@/api/http'
import {
  batchDeleteMaterialLedger,
  deleteMaterialLedger,
  downloadImportTemplate,
  exportMaterialLedger,
  fetchFilterOptions,
  fetchMaterialLedgerDetail,
  fetchMaterialLedgerPage,
  importMaterialLedger,
} from '@/api/materialLedger'
import MaterialLedgerFormModal from '@/components/warehouse/MaterialLedgerFormModal.vue'
import { ALL_OPTION } from '@/constants/filter'
import type { FilterOptions, MaterialLedger } from '@/types/materialLedger'
import { downloadBlob } from '@/utils/download'
import { displayValue, formatDateTime, formatUnitPrice } from '@/utils/format'
import { toSelectOptions, withAllOption } from '@/utils/selectOptions'

const defaultQuery = {
  category: ALL_OPTION,
  genericName: ALL_OPTION,
  brand: ALL_OPTION,
  name: '',
  model: ALL_OPTION,
  binLocation: ALL_OPTION,
}

const queryForm = reactive({ ...defaultQuery })
const loading = ref(false)
const exporting = ref(false)
const batchExporting = ref(false)
const importing = ref(false)
const detailLoading = ref(false)
const drawerOpen = ref(false)
const formOpen = ref(false)
const dataSource = ref<MaterialLedger[]>([])
const selectedRowKeys = ref<Key[]>([])
const detailRecord = ref<MaterialLedger | null>(null)
const editingRecord = ref<MaterialLedger | null>(null)
const filterOptionsRaw = ref<FilterOptions>({
  categories: [],
  genericNames: [],
  brands: [],
  models: [],
  binLocations: [],
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: false,
})

const hasSelection = computed(() => selectedRowKeys.value.length > 0)

const filterOptions = computed(() => ({
  category: withAllOption(filterOptionsRaw.value.categories),
  genericName: withAllOption(filterOptionsRaw.value.genericNames),
  brand: withAllOption(filterOptionsRaw.value.brands),
  model: withAllOption(filterOptionsRaw.value.models),
  binLocation: withAllOption(filterOptionsRaw.value.binLocations),
}))

const columns = [
  { title: '序号', key: 'index', width: 64, align: 'center' as const },
  { title: '品类', dataIndex: 'category', key: 'category', width: 100, ellipsis: true },
  { title: '统称', dataIndex: 'genericName', key: 'genericName', width: 100, ellipsis: true },
  { title: '品牌', dataIndex: 'brand', key: 'brand', width: 80, ellipsis: true },
  { title: '名称', dataIndex: 'name', key: 'name', width: 140, ellipsis: true },
  { title: '型号', dataIndex: 'model', key: 'model', width: 100, ellipsis: true },
  { title: 'Bin位', dataIndex: 'binLocation', key: 'binLocation', width: 80, align: 'center' as const },
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

const rowSelection = {
  selectedRowKeys,
  onChange: (keys: Key[]) => {
    selectedRowKeys.value = keys
  },
}

function buildLinkageParams() {
  return {
    category: queryForm.category !== ALL_OPTION ? queryForm.category : undefined,
    genericName: queryForm.genericName !== ALL_OPTION ? queryForm.genericName : undefined,
    brand: queryForm.brand !== ALL_OPTION ? queryForm.brand : undefined,
  }
}

function buildQueryParams() {
  return {
    category: queryForm.category,
    genericName: queryForm.genericName,
    brand: queryForm.brand,
    name: queryForm.name.trim() || undefined,
    model: queryForm.model,
    binLocation: queryForm.binLocation,
  }
}

function ensureOptionValue(field: keyof typeof queryForm, options: string[]) {
  const value = queryForm[field]
  if (typeof value === 'string' && value !== ALL_OPTION && !options.includes(value)) {
    queryForm[field] = ALL_OPTION as never
  }
}

async function loadFilterOptions() {
  filterOptionsRaw.value = await fetchFilterOptions(buildLinkageParams())
  ensureOptionValue('genericName', filterOptions.value.genericName)
  ensureOptionValue('brand', filterOptions.value.brand)
  ensureOptionValue('model', filterOptions.value.model)
  ensureOptionValue('binLocation', filterOptions.value.binLocation)
}

async function loadData() {
  loading.value = true
  try {
    const result = await fetchMaterialLedgerPage({
      ...buildQueryParams(),
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    dataSource.value = result.records
    pagination.total = result.total
    pagination.current = result.current
  } catch {
    message.error('加载物料台账失败，请确认后端服务已启动')
  } finally {
    loading.value = false
  }
}

async function refreshAll() {
  await loadFilterOptions()
  await loadData()
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  Object.assign(queryForm, defaultQuery)
  pagination.current = 1
  selectedRowKeys.value = []
  refreshAll()
}

function handleTableChange(pageConfig: TablePaginationConfig) {
  pagination.current = pageConfig.current ?? 1
  pagination.pageSize = pageConfig.pageSize ?? 10
  loadData()
}

async function handleCategoryChange() {
  queryForm.genericName = ALL_OPTION
  queryForm.brand = ALL_OPTION
  queryForm.model = ALL_OPTION
  queryForm.binLocation = ALL_OPTION
  await loadFilterOptions()
}

async function handleGenericNameChange() {
  queryForm.brand = ALL_OPTION
  queryForm.model = ALL_OPTION
  queryForm.binLocation = ALL_OPTION
  await loadFilterOptions()
}

async function handleBrandChange() {
  queryForm.model = ALL_OPTION
  queryForm.binLocation = ALL_OPTION
  await loadFilterOptions()
}

function handleCreate() {
  editingRecord.value = null
  formOpen.value = true
}

function handleEdit(record: MaterialLedger) {
  editingRecord.value = record
  formOpen.value = true
}

async function handleExport() {
  exporting.value = true
  try {
    const blob = await exportMaterialLedger(buildQueryParams())
    downloadBlob(blob, `物料台账-${dayjs().format('YYYY-MM-DD')}.xlsx`)
    message.success('导出成功')
  } catch {
    message.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

async function handleBatchExport() {
  if (!hasSelection.value) {
    return
  }
  batchExporting.value = true
  try {
    const ids = selectedRowKeys.value.map((key) => Number(key))
    const blob = await exportMaterialLedger({ ids })
    downloadBlob(blob, `物料台账-勾选-${dayjs().format('YYYY-MM-DD')}.xlsx`)
    message.success('批量导出成功')
  } catch {
    message.error('批量导出失败，请稍后重试')
  } finally {
    batchExporting.value = false
  }
}

function handleDelete(record: MaterialLedger) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除物料「${record.name}」吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deleteMaterialLedger(record.id)
        message.success('删除成功')
        selectedRowKeys.value = selectedRowKeys.value.filter((key) => key !== record.id)
        await refreshAll()
      } catch (error) {
        message.error(getErrorMessage(error, '删除失败，请稍后重试'))
      }
    },
  })
}

function handleBatchDelete() {
  if (!hasSelection.value) {
    return
  }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '确认批量删除',
    content: `确定删除选中的 ${count} 条物料吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        const ids = selectedRowKeys.value.map((key) => Number(key))
        await batchDeleteMaterialLedger(ids)
        message.success('批量删除成功')
        selectedRowKeys.value = []
        await refreshAll()
      } catch (error) {
        message.error(getErrorMessage(error, '批量删除失败，请稍后重试'))
      }
    },
  })
}

async function handleDownloadTemplate() {
  try {
    const blob = await downloadImportTemplate()
    downloadBlob(blob, '物料台账导入模板.xlsx')
  } catch {
    message.error('下载模板失败，请稍后重试')
  }
}

async function handleImport(file: File) {
  importing.value = true
  try {
    const result = await importMaterialLedger(file)
    if (result.failCount === 0) {
      message.success(`导入成功，共 ${result.successCount} 条`)
    } else {
      const firstError = result.errors[0]
      const detail = firstError ? `第 ${firstError.row} 行：${firstError.message}` : ''
      message.warning(
        `导入完成：成功 ${result.successCount} 条，失败 ${result.failCount} 条。${detail}`,
      )
    }
    await refreshAll()
  } catch (error) {
    message.error(getErrorMessage(error, '导入失败，请稍后重试'))
  } finally {
    importing.value = false
  }
  return false
}

async function handleView(record: MaterialLedger) {
  drawerOpen.value = true
  detailLoading.value = true
  detailRecord.value = null
  try {
    detailRecord.value = await fetchMaterialLedgerDetail(record.id)
  } catch {
    message.error('加载物料详情失败')
    drawerOpen.value = false
  } finally {
    detailLoading.value = false
  }
}

function handleDrawerClose() {
  drawerOpen.value = false
  detailRecord.value = null
}

function getRowIndex(index: number) {
  return (pagination.current - 1) * pagination.pageSize + index + 1
}

onMounted(async () => {
  try {
    await loadFilterOptions()
  } catch {
    message.warning('筛选选项加载失败，将使用默认选项')
  }
  await loadData()
})
</script>

<template>
  <div class="material-ledger-page">
    <a-card :bordered="false" class="filter-card">
      <a-form layout="inline" class="filter-form">
        <a-row :gutter="[12, 8]" class="filter-row">
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="品类" class="filter-item">
              <a-select
                v-model:value="queryForm.category"
                :options="toSelectOptions(filterOptions.category)"
                class="filter-control"
                @change="handleCategoryChange"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="统称" class="filter-item">
              <a-select
                v-model:value="queryForm.genericName"
                :options="toSelectOptions(filterOptions.genericName)"
                class="filter-control"
                @change="handleGenericNameChange"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="品牌" class="filter-item">
              <a-select
                v-model:value="queryForm.brand"
                :options="toSelectOptions(filterOptions.brand)"
                class="filter-control"
                @change="handleBrandChange"
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

        <a-row :gutter="[12, 8]" class="filter-row">
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="名称" class="filter-item">
              <a-input
                v-model:value="queryForm.name"
                placeholder="关键字查找"
                allow-clear
                class="filter-control"
                @press-enter="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="型号" class="filter-item">
              <a-select
                v-model:value="queryForm.model"
                :options="toSelectOptions(filterOptions.model)"
                class="filter-control"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="Bin位" class="filter-item">
              <a-select
                v-model:value="queryForm.binLocation"
                :options="toSelectOptions(filterOptions.binLocation)"
                class="filter-control"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <a-card :bordered="false" class="table-card">
      <div class="toolbar">
        <a-space wrap>
          <a-button type="primary" @click="handleCreate">
            <template #icon><PlusOutlined /></template>
            新增
          </a-button>
          <a-upload
            :show-upload-list="false"
            accept=".xlsx,.xls"
            :before-upload="handleImport"
          >
            <a-button :loading="importing">
              <template #icon><UploadOutlined /></template>
              导入
            </a-button>
          </a-upload>
          <a-button @click="handleDownloadTemplate">下载模板</a-button>
          <a-button type="primary" :loading="exporting" @click="handleExport">
            <template #icon><ExportOutlined /></template>
            导出
          </a-button>
          <a-button :disabled="!hasSelection" :loading="batchExporting" @click="handleBatchExport">
            <template #icon><ExportOutlined /></template>
            批量导出
          </a-button>
          <a-button danger :disabled="!hasSelection" @click="handleBatchDelete">
            <template #icon><DeleteOutlined /></template>
            批量删除
          </a-button>
        </a-space>
      </div>

      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          showSizeChanger: false,
          position: ['bottomCenter'],
          showTotal: (total: number) => `共 ${total} 条`,
        }"
        :row-key="(record: MaterialLedger) => record.id"
        :row-selection="rowSelection"
        :scroll="{ x: 1180 }"
        size="small"
        bordered
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'index'">
            {{ getRowIndex(index) }}
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
            <a-space :size="0">
              <a-button type="link" size="small" class="action-link" @click="handleView(record)">
                查看
              </a-button>
              <a-button type="link" size="small" class="action-link" @click="handleEdit(record)">
                编辑
              </a-button>
              <a-button
                type="link"
                size="small"
                danger
                class="action-link"
                @click="handleDelete(record)"
              >
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <MaterialLedgerFormModal
      v-model:open="formOpen"
      :record="editingRecord"
      :category-options="filterOptions.category"
      :generic-name-options="filterOptions.genericName"
      :brand-options="filterOptions.brand"
      :model-options="filterOptions.model"
      :bin-location-options="filterOptions.binLocation"
      @success="refreshAll"
    />

    <a-drawer
      v-model:open="drawerOpen"
      title="物料详情"
      placement="right"
      :width="480"
      destroy-on-close
      @close="handleDrawerClose"
    >
      <a-spin :spinning="detailLoading">
        <template v-if="detailRecord">
          <a-descriptions :column="1" bordered size="small" class="detail-block">
            <a-descriptions-item label="品类">
              {{ displayValue(detailRecord.category) }}
            </a-descriptions-item>
            <a-descriptions-item label="统称">
              {{ displayValue(detailRecord.genericName) }}
            </a-descriptions-item>
            <a-descriptions-item label="品牌">
              {{ displayValue(detailRecord.brand) }}
            </a-descriptions-item>
            <a-descriptions-item label="名称">
              {{ displayValue(detailRecord.name) }}
            </a-descriptions-item>
            <a-descriptions-item label="型号">
              {{ displayValue(detailRecord.model) }}
            </a-descriptions-item>
            <a-descriptions-item label="Bin位">
              {{ displayValue(detailRecord.binLocation) }}
            </a-descriptions-item>
          </a-descriptions>

          <a-descriptions
            title="库存信息"
            :column="1"
            bordered
            size="small"
            class="detail-block"
          >
            <a-descriptions-item label="库存数量">
              {{ displayValue(detailRecord.stockQuantity) }}
            </a-descriptions-item>
            <a-descriptions-item label="单价">
              {{ formatUnitPrice(detailRecord.unitPrice) || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="备注">
              {{ displayValue(detailRecord.remark) }}
            </a-descriptions-item>
          </a-descriptions>

          <a-descriptions
            title="系统信息"
            :column="1"
            bordered
            size="small"
            class="detail-block"
          >
            <a-descriptions-item label="创建时间">
              {{ formatDateTime(detailRecord.createdAt) }}
            </a-descriptions-item>
            <a-descriptions-item label="更新时间">
              {{ formatDateTime(detailRecord.updatedAt) }}
            </a-descriptions-item>
          </a-descriptions>
        </template>
      </a-spin>
    </a-drawer>
  </div>
</template>

<style scoped>
.material-ledger-page {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-card,
.table-card {
  border-radius: 2px;
}

.filter-card :deep(.ant-card-body),
.table-card :deep(.ant-card-body) {
  padding: 12px 16px;
}

.filter-form {
  width: 100%;
}

.filter-row {
  width: 100%;
}

.filter-item {
  width: 100%;
  margin-inline-end: 0;
  margin-bottom: 0;
}

.filter-item :deep(.ant-form-item-row) {
  flex-wrap: nowrap;
}

.filter-item :deep(.ant-form-item-label) {
  width: 52px;
  flex: none;
  text-align: right;
}

.filter-item :deep(.ant-form-item-label > label) {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.filter-item :deep(.ant-form-item-control) {
  flex: 1;
  min-width: 0;
}

.filter-control {
  width: 100%;
}

.filter-actions-col {
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
}

.toolbar {
  margin-bottom: 8px;
}

.table-card :deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 500;
  padding: 8px;
  font-size: 13px;
}

.table-card :deep(.ant-table-tbody > tr > td) {
  padding: 6px 8px;
  font-size: 13px;
}

.table-card :deep(.ant-pagination) {
  margin: 12px 0 4px;
}

.table-card :deep(.ant-table-cell) {
  vertical-align: middle;
}

.action-link {
  padding: 0 4px;
  height: auto;
}

.detail-block + .detail-block {
  margin-top: 16px;
}

.detail-block :deep(.ant-descriptions-header) {
  margin-bottom: 8px;
}

.detail-block :deep(.ant-descriptions-item-label) {
  width: 96px;
  background: #fafafa;
}
</style>
