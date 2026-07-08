<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import { fetchWarehouseBinPage } from '@/api/warehouse/warehouseBin'
import type { WarehouseBin } from '@/types/warehouse/warehouseBin'
import { displayValue } from '@/utils/format'
import { getTableRowIndex } from '@/utils/tableIndex'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  select: [record: WarehouseBin]
}>()

const loading = ref(false)
const dataSource = ref<WarehouseBin[]>([])
const selectedRowKeys = ref<number[]>([])
const queryForm = reactive<{
  binCode: string
  rowNo?: number
  colNo?: number
  levelNo?: number
}>({
  binCode: '',
  rowNo: undefined,
  colNo: undefined,
  levelNo: undefined,
})
const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
})

const columns = [
  { title: '序号', key: 'index', width: 64, align: 'center' as const },
  { title: 'Bin位', dataIndex: 'binCode', key: 'binCode', width: 100, align: 'center' as const },
  { title: '排', dataIndex: 'rowNo', key: 'rowNo', width: 72, align: 'center' as const },
  { title: '列', dataIndex: 'colNo', key: 'colNo', width: 72, align: 'center' as const },
  { title: '层', dataIndex: 'levelNo', key: 'levelNo', width: 72, align: 'center' as const },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
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
    binCode: queryForm.binCode.trim() || undefined,
    rowNo: queryForm.rowNo,
    colNo: queryForm.colNo,
    levelNo: queryForm.levelNo,
    page: pagination.current,
    pageSize: pagination.pageSize,
  }
}

async function loadData() {
  loading.value = true
  try {
    const page = await fetchWarehouseBinPage(buildQueryParams())
    dataSource.value = page.records
    pagination.total = page.total
    pagination.current = page.current
    pagination.pageSize = page.size
  } catch {
    dataSource.value = []
    message.error('加载 Bin 位失败')
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
    binCode: '',
    rowNo: undefined,
    colNo: undefined,
    levelNo: undefined,
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

function confirmSelect(record: WarehouseBin) {
  emit('select', record)
  emit('update:open', false)
}

function handleConfirm() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择一个 Bin 位')
    return
  }
  const selected = dataSource.value.find((item) => item.id === selectedRowKeys.value[0])
  if (!selected) {
    message.warning('所选 Bin 位无效，请重新选择')
    return
  }
  confirmSelect(selected)
}

function customRow(record: WarehouseBin) {
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
    title="从 Bin 位配置选择"
    width="860px"
    destroy-on-close
    @cancel="handleClose"
  >
    <a-form layout="inline" class="picker-filter">
      <a-row :gutter="[12, 8]" class="filter-row">
        <a-col :xs="24" :sm="12" :md="8">
          <a-form-item label="Bin位" class="filter-item">
            <a-input v-model:value="queryForm.binCode" allow-clear @press-enter="handleSearch" />
          </a-form-item>
        </a-col>
        <a-col :xs="8" :sm="6" :md="4">
          <a-form-item label="排" class="filter-item">
            <a-input-number v-model:value="queryForm.rowNo" :min="1" :precision="0" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :xs="8" :sm="6" :md="4">
          <a-form-item label="列" class="filter-item">
            <a-input-number v-model:value="queryForm.colNo" :min="1" :precision="0" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :xs="8" :sm="6" :md="4">
          <a-form-item label="层" class="filter-item">
            <a-input-number v-model:value="queryForm.levelNo" :min="1" :precision="0" style="width: 100%" />
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
      :row-key="(record: WarehouseBin) => record.id"
      :row-selection="rowSelection"
      :custom-row="customRow"
      size="small"
      :scroll="{ x: 620, y: 320 }"
      class="picker-table"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'index'">
          {{ getTableRowIndex(index, pagination) }}
        </template>
        <template v-else-if="column.key === 'remark'">
          {{ displayValue(record.remark) }}
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
