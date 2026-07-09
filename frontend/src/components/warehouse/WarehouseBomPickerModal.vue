<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import { fetchWarehouseBomPage } from '@/api/warehouse/warehouseBom'
import { defaultTablePagination } from '@/constants/pagination'
import type { WarehouseBom } from '@/types/warehouse/warehouseBom'
import { displayValue } from '@/utils/format'
import { getTableRowIndex } from '@/utils/tableIndex'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  select: [record: WarehouseBom]
}>()

const loading = ref(false)
const dataSource = ref<WarehouseBom[]>([])
const selectedRowKeys = ref<number[]>([])
const queryForm = reactive({
  category: '',
  genericName: '',
  brand: '',
  name: '',
  model: '',
})
const pagination = reactive<TablePaginationConfig>({
  ...defaultTablePagination,
})

const columns = [
  { title: '序号', key: 'index', width: 64, align: 'center' as const },
  { title: '品类', dataIndex: 'category', key: 'category', width: 100, ellipsis: true },
  { title: '统称', dataIndex: 'genericName', key: 'genericName', width: 100, ellipsis: true },
  { title: '品牌', dataIndex: 'brand', key: 'brand', width: 90, ellipsis: true },
  { title: '名称', dataIndex: 'name', key: 'name', width: 140, ellipsis: true },
  { title: '规格', dataIndex: 'model', key: 'model', width: 120, ellipsis: true },
]

const rowSelection = computed(() => ({
  type: 'radio' as const,
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[]) => {
    selectedRowKeys.value = keys.map((key) => Number(key))
  },
}))

function buildQueryParams() {
  return {
    category: queryForm.category.trim() || undefined,
    genericName: queryForm.genericName.trim() || undefined,
    brand: queryForm.brand.trim() || undefined,
    name: queryForm.name.trim() || undefined,
    model: queryForm.model.trim() || undefined,
    page: pagination.current,
    pageSize: pagination.pageSize,
  }
}

async function loadData() {
  loading.value = true
  try {
    const page = await fetchWarehouseBomPage(buildQueryParams())
    dataSource.value = page.records
    pagination.total = page.total
    pagination.current = page.current
    pagination.pageSize = page.size
  } catch {
    dataSource.value = []
    message.error('加载物料清单失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  selectedRowKeys.value = []
  void loadData()
}

function handleReset() {
  Object.assign(queryForm, {
    category: '',
    genericName: '',
    brand: '',
    name: '',
    model: '',
  })
  handleSearch()
}

function handleTableChange(page: TablePaginationConfig) {
  pagination.current = page.current
  pagination.pageSize = page.pageSize
  void loadData()
}

function handleClose() {
  emit('update:open', false)
}

function confirmSelect(record: WarehouseBom) {
  emit('select', record)
  emit('update:open', false)
}

function handleConfirm() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择一条物料清单')
    return
  }
  const selected = dataSource.value.find((item) => item.id === selectedRowKeys.value[0])
  if (!selected) {
    message.warning('所选记录无效，请重新选择')
    return
  }
  confirmSelect(selected)
}

function customRow(record: WarehouseBom) {
  return {
    onDblclick: () => {
      selectedRowKeys.value = [record.id]
      confirmSelect(record)
    },
  }
}

watch(
  () => props.open,
  (open) => {
    if (!open) {
      return
    }
    selectedRowKeys.value = []
    pagination.current = 1
    void loadData()
  },
)
</script>

<template>
  <a-modal
    :open="open"
    title="从物料清单选择"
    width="980px"
    destroy-on-close
    @cancel="handleClose"
  >
    <a-form layout="inline" class="picker-filter">
      <a-row :gutter="[12, 8]" class="filter-row">
        <a-col :xs="24" :sm="12" :md="8">
          <a-form-item label="品类" class="filter-item">
            <a-input v-model:value="queryForm.category" allow-clear @press-enter="handleSearch" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :md="8">
          <a-form-item label="统称" class="filter-item">
            <a-input v-model:value="queryForm.genericName" allow-clear @press-enter="handleSearch" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :md="8">
          <a-form-item label="品牌" class="filter-item">
            <a-input v-model:value="queryForm.brand" allow-clear @press-enter="handleSearch" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :md="8">
          <a-form-item label="名称" class="filter-item">
            <a-input v-model:value="queryForm.name" allow-clear @press-enter="handleSearch" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :md="8">
          <a-form-item label="规格" class="filter-item">
            <a-input v-model:value="queryForm.model" allow-clear @press-enter="handleSearch" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :md="8" class="filter-actions-col">
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
    </a-form>

    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="(record: WarehouseBom) => record.id"
      :row-selection="rowSelection"
      :custom-row="customRow"
      size="small"
      :scroll="{ x: 780, y: 320 }"
      class="picker-table"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'index'">
          {{ getTableRowIndex(index, pagination) }}
        </template>
        <template v-else-if="column.key === 'brand'">
          {{ displayValue(record.brand) }}
        </template>
      </template>
    </a-table>

    <p class="picker-hint">提示：双击行可快速选中</p>

    <template #footer>
      <a-button @click="handleClose">取消</a-button>
      <a-button type="primary" @click="handleConfirm">确定</a-button>
    </template>
  </a-modal>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.picker-filter {
  margin-bottom: @spacing-md;
  width: 100%;
}

.filter-row {
  width: 100%;
}

.filter-item {
  margin-bottom: 0;
}

.filter-actions-col {
  display: flex;
  align-items: flex-start;
}

.picker-table {
  margin-top: @spacing-xs;
}

.picker-hint {
  margin: @spacing-sm 0 0;
  font-size: 12px;
  color: @color-text-tertiary;
}
</style>
