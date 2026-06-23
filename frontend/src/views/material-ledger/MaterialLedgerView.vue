<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import type { Key } from 'ant-design-vue/es/table/interface'
import { ExportOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { fetchFilterOptions, fetchMaterialLedgerPage } from '@/api/materialLedger'
import type { FilterOptions, MaterialLedger } from '@/types/materialLedger'

const ALL_OPTION = '全部'

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
const dataSource = ref<MaterialLedger[]>([])
const selectedRowKeys = ref<Key[]>([])
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

const filterOptions = computed(() => ({
  category: [ALL_OPTION, ...filterOptionsRaw.value.categories],
  genericName: [ALL_OPTION, ...filterOptionsRaw.value.genericNames],
  brand: [ALL_OPTION, ...filterOptionsRaw.value.brands],
  model: [ALL_OPTION, ...filterOptionsRaw.value.models],
  binLocation: [ALL_OPTION, ...filterOptionsRaw.value.binLocations],
}))

const columns = [
  { title: '序号', key: 'index', width: 70, align: 'center' as const },
  { title: '品类', dataIndex: 'category', key: 'category', width: 110 },
  { title: '统称', dataIndex: 'genericName', key: 'genericName', width: 110 },
  { title: '品牌', dataIndex: 'brand', key: 'brand', width: 90 },
  { title: '名称', dataIndex: 'name', key: 'name', width: 150 },
  { title: '型号', dataIndex: 'model', key: 'model', width: 100 },
  { title: 'Bin位', dataIndex: 'binLocation', key: 'binLocation', width: 90 },
  { title: '库存数量', dataIndex: 'stockQuantity', key: 'stockQuantity', width: 100, align: 'center' as const },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 90, align: 'center' as const },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 80, align: 'center' as const, fixed: 'right' as const },
]

const rowSelection = {
  selectedRowKeys,
  onChange: (keys: Key[]) => {
    selectedRowKeys.value = keys
  },
}

async function loadFilterOptions() {
  filterOptionsRaw.value = await fetchFilterOptions()
}

async function loadData() {
  loading.value = true
  try {
    const result = await fetchMaterialLedgerPage({
      category: queryForm.category,
      genericName: queryForm.genericName,
      brand: queryForm.brand,
      name: queryForm.name.trim() || undefined,
      model: queryForm.model,
      binLocation: queryForm.binLocation,
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

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  Object.assign(queryForm, defaultQuery)
  pagination.current = 1
  selectedRowKeys.value = []
  loadData()
}

function handleTableChange(pageConfig: TablePaginationConfig) {
  pagination.current = pageConfig.current ?? 1
  pagination.pageSize = pageConfig.pageSize ?? 10
  loadData()
}

function handleExport() {
  message.info('导出功能开发中，敬请期待')
}

function handleView(_record: MaterialLedger) {
  message.info('查看详情功能开发中，敬请期待')
}

function formatUnitPrice(value: number | null) {
  return value == null ? '' : value.toFixed(2)
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
      <a-form layout="vertical" class="filter-form">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-item label="品类">
              <a-select
                v-model:value="queryForm.category"
                :options="filterOptions.category.map((v) => ({ label: v, value: v }))"
              />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="统称">
              <a-select
                v-model:value="queryForm.genericName"
                :options="filterOptions.genericName.map((v) => ({ label: v, value: v }))"
              />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="品牌">
              <a-select
                v-model:value="queryForm.brand"
                :options="filterOptions.brand.map((v) => ({ label: v, value: v }))"
              />
            </a-form-item>
          </a-col>
          <a-col :span="6" class="filter-actions">
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
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-item label="名称">
              <a-input v-model:value="queryForm.name" placeholder="关键字查找" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="型号">
              <a-select
                v-model:value="queryForm.model"
                :options="filterOptions.model.map((v) => ({ label: v, value: v }))"
              />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="Bin位">
              <a-select
                v-model:value="queryForm.binLocation"
                :options="filterOptions.binLocation.map((v) => ({ label: v, value: v }))"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <a-card :bordered="false" class="table-card">
      <div class="toolbar">
        <a-button type="primary" @click="handleExport">
          <template #icon><ExportOutlined /></template>
          导出
        </a-button>
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
        }"
        :row-key="(record: MaterialLedger) => record.id"
        :row-selection="rowSelection"
        :scroll="{ x: 1200 }"
        size="middle"
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
            <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<style scoped>
.material-ledger-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-card,
.table-card {
  border-radius: 4px;
}

.filter-form :deep(.ant-form-item) {
  margin-bottom: 12px;
}

.filter-actions {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  padding-bottom: 12px;
}

.toolbar {
  margin-bottom: 12px;
}

.table-card :deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 500;
}

.table-card :deep(.ant-pagination) {
  margin-top: 16px;
}
</style>
