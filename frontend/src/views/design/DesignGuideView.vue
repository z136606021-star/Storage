<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  batchDeleteDesignGuides,
  deleteDesignGuide,
  downloadDesignGuideImportTemplate,
  exportDesignGuides,
  fetchDesignGuideDetail,
  fetchDesignGuideFilterOptions,
  fetchDesignGuidePage,
  importDesignGuides,
} from '@/api/design/designGuide'
import CrudDetailDrawer from '@/components/common/CrudDetailDrawer.vue'
import CrudListPage from '@/components/common/CrudListPage.vue'
import CrudRowActions from '@/components/common/CrudRowActions.vue'
import DesignGuideFormModal from '@/components/design/DesignGuideFormModal.vue'
import DesignProductTypeConfigModal from '@/components/design/DesignProductTypeConfigModal.vue'
import DesignStageConfigModal from '@/components/design/DesignStageConfigModal.vue'
import { useCrudDetailDrawer } from '@/composables/useCrudDetailDrawer'
import { useExcelImportExport } from '@/composables/useExcelImportExport'
import { usePaginatedCrudList } from '@/composables/usePaginatedCrudList'
import { useWritePermission } from '@/composables/useWritePermission'
import type { DesignGuide, DesignGuideFilterOptions } from '@/types/design/designGuide'
import { confirmBatchDelete, confirmDelete } from '@/utils/confirmDelete'
import { displayValue, formatDateTime } from '@/utils/format'
import { getTableRowIndex } from '@/utils/tableIndex'

const { canWrite } = useWritePermission('platform:design:write')

const defaultFilterOptions = (): DesignGuideFilterOptions => ({
  productTypes: [],
  stages: [],
  scopes: [],
})

const queryForm = reactive({
  productTypeId: undefined as number | undefined,
  stageId: undefined as number | undefined,
  scope: undefined as string | undefined,
  checkItem: '',
})

const filterOptions = ref<DesignGuideFilterOptions>(defaultFilterOptions())
const formOpen = ref(false)
const productTypeConfigOpen = ref(false)
const stageConfigOpen = ref(false)
const editingRecord = ref<DesignGuide | null>(null)

function buildQueryParams() {
  return {
    productTypeId: queryForm.productTypeId,
    stageId: queryForm.stageId,
    scope: queryForm.scope,
    checkItem: queryForm.checkItem.trim() || undefined,
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
} = usePaginatedCrudList<DesignGuide, ReturnType<typeof buildQueryParams>>({
  fetchPage: fetchDesignGuidePage,
  buildQueryParams,
  loadErrorMessage: '加载设计指引失败',
  enableRowSelection: true,
})

const {
  drawerOpen,
  detailLoading,
  detailRecord,
  openDetail,
  closeDetail,
} = useCrudDetailDrawer(fetchDesignGuideDetail, '加载设计指引详情失败')

const {
  exporting,
  importing,
  handleExport,
  handleImport,
  handleDownloadTemplate,
} = useExcelImportExport({
  exportFn: (params) => exportDesignGuides(params ?? buildQueryParams()),
  importFn: importDesignGuides,
  templateFn: downloadDesignGuideImportTemplate,
  buildExportParams: buildQueryParams,
  getExportFilename: () => `设计指引-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  getTemplateFilename: () => '设计指引导入模板.xlsx',
  exportSuccessMessage: '导出成功',
  showFirstImportError: true,
  onAfterImport: async () => {
    await reloadFilterOptions()
    await loadData()
  },
})

const productTypeOptions = () => [
  { value: undefined, label: '全部' },
  ...filterOptions.value.productTypes.map((item) => ({
    value: item.id,
    label: item.label,
  })),
]

const stageOptions = () => [
  { value: undefined, label: '全部' },
  ...filterOptions.value.stages.map((item) => ({
    value: item.id,
    label: item.label,
  })),
]

const scopeOptions = () => [
  { value: undefined, label: '全部' },
  ...filterOptions.value.scopes.map((scope) => ({
    value: scope,
    label: scope,
  })),
]

const columns = [
  { title: '序号', key: 'index', width: 70, align: 'center' as const },
  { title: '产品类型编号', dataIndex: 'productTypeCode', key: 'productTypeCode', width: 120 },
  { title: '产品类型', dataIndex: 'productTypeName', key: 'productTypeName', width: 140 },
  { title: '阶段', dataIndex: 'stageName', key: 'stageName', width: 120 },
  { title: '适用范围', dataIndex: 'scope', key: 'scope', width: 120 },
  { title: '检查项', dataIndex: 'checkItem', key: 'checkItem', ellipsis: true, minWidth: 260 },
  { title: '记录人', dataIndex: 'recorderName', key: 'recorderName', width: 120 },
  { title: '记录时间', dataIndex: 'recordedAt', key: 'recordedAt', width: 170 },
  { title: '操作', key: 'action', width: 160, align: 'center' as const, fixed: 'right' as const },
]

async function reloadFilterOptions() {
  filterOptions.value = await fetchDesignGuideFilterOptions()
}

function handleReset() {
  queryForm.productTypeId = undefined
  queryForm.stageId = undefined
  queryForm.scope = undefined
  queryForm.checkItem = ''
  handleResetQuery()
  clearSelection()
  loadData()
}

function handleCreate() {
  editingRecord.value = null
  formOpen.value = true
}

function handleEdit(record: DesignGuide) {
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

function handleDelete(record: DesignGuide) {
  confirmDelete({
    title: '确认删除',
    content: `确定删除「${record.productTypeCode} / ${record.stageName}」这条设计指引吗？`,
    successMessage: '删除成功',
    onDelete: async () => {
      await deleteDesignGuide(record.id)
    },
    onSuccess: async () => {
      removeFromSelection(record.id)
      await reloadFilterOptions()
      await loadData()
    },
  })
}

function handleBatchDelete() {
  if (!hasSelection.value) {
    return
  }
  confirmBatchDelete({
    count: selectedRowKeys.value.length,
    entityLabel: '设计指引',
    onDelete: async () => {
      await batchDeleteDesignGuides(selectedRowKeys.value.map((key) => Number(key)))
    },
    onSuccess: async () => {
      clearSelection()
      await reloadFilterOptions()
      await loadData()
    },
  })
}

async function handleFormSuccess() {
  await reloadFilterOptions()
  await loadData()
}

async function handleConfigChanged() {
  await reloadFilterOptions()
}

onMounted(async () => {
  try {
    await reloadFilterOptions()
  } catch {
    message.warning('筛选选项加载失败，请稍后刷新')
  }
  await loadData()
})
</script>

<template>
  <CrudListPage
    :columns="columns"
    :loading="loading"
    :data-source="dataSource"
    :pagination="pagination"
    :row-key="(record: DesignGuide) => record.id"
    :row-selection="canWrite ? rowSelection : undefined"
    :scroll="{ x: 1260 }"
    toolbar-create-green
    :toolbar-can-write="canWrite"
    :toolbar-importing="importing"
    :toolbar-exporting="exporting"
    :toolbar-show-batch-delete="canWrite"
    :toolbar-has-selection="hasSelection"
    @change="handleTableChange"
    @toolbar-create="handleCreate"
    @toolbar-export="handleExport"
    @toolbar-import="handleImport"
    @toolbar-download-template="handleDownloadTemplate"
    @toolbar-batch-delete="handleBatchDelete"
  >
    <template #filters>
      <a-form layout="inline" class="filter-form">
        <a-row :gutter="[24, 12]" class="filter-row">
          <a-col :xs="24" :md="12" :xl="6">
            <a-form-item label="产品类型" class="filter-item">
              <a-select
                v-model:value="queryForm.productTypeId"
                :options="productTypeOptions()"
                class="filter-control"
                show-search
                :filter-option="(input: string, option: any) => String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12" :xl="6">
            <a-form-item label="项目阶段" class="filter-item">
              <a-select
                v-model:value="queryForm.stageId"
                :options="stageOptions()"
                class="filter-control"
                show-search
                :filter-option="(input: string, option: any) => String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12" :xl="6">
            <a-form-item label="检查项" class="filter-item">
              <a-input
                v-model:value="queryForm.checkItem"
                allow-clear
                placeholder="关键字查找"
                class="filter-control"
                @press-enter="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12" :xl="6">
            <a-form-item label="适用范围" class="filter-item">
              <a-select
                v-model:value="queryForm.scope"
                :options="scopeOptions()"
                class="filter-control"
                show-search
                :filter-option="(input: string, option: any) => String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24" class="filter-actions-col">
            <a-space class="filter-actions">
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
    </template>

    <template #bodyCell="{ column, record, index }">
      <template v-if="column.key === 'index'">
        {{ getTableRowIndex(index, pagination) }}
      </template>
      <template v-else-if="column.key === 'checkItem'">
        {{ displayValue(record.checkItem) }}
      </template>
      <template v-else-if="column.key === 'recorderName'">
        {{ displayValue(record.recorderName) }}
      </template>
      <template v-else-if="column.key === 'recordedAt'">
        {{ formatDateTime(record.recordedAt) }}
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

  <DesignGuideFormModal
    v-model:open="formOpen"
    :record="editingRecord"
    :filter-options="filterOptions"
    :can-write="canWrite"
    @success="handleFormSuccess"
    @configure-product-types="productTypeConfigOpen = true"
    @configure-stages="stageConfigOpen = true"
  />

  <DesignProductTypeConfigModal
    v-model:open="productTypeConfigOpen"
    :can-write="canWrite"
    @changed="handleConfigChanged"
  />

  <DesignStageConfigModal
    v-model:open="stageConfigOpen"
    :can-write="canWrite"
    @changed="handleConfigChanged"
  />

  <CrudDetailDrawer
    v-model:open="drawerOpen"
    title="设计指引详情"
    :loading="detailLoading"
    :show-edit="canWrite"
    :width="520"
    @close="closeDetail"
    @edit="handleEditFromDrawer"
  >
    <a-descriptions v-if="detailRecord" :column="1" bordered size="small">
      <a-descriptions-item label="产品类型编号">
        {{ detailRecord.productTypeCode }}
      </a-descriptions-item>
      <a-descriptions-item label="产品类型">
        {{ detailRecord.productTypeName }}
      </a-descriptions-item>
      <a-descriptions-item label="项目阶段">
        {{ detailRecord.stageName }}
      </a-descriptions-item>
      <a-descriptions-item label="适用范围">
        {{ detailRecord.scope }}
      </a-descriptions-item>
      <a-descriptions-item label="检查项">
        {{ detailRecord.checkItem }}
      </a-descriptions-item>
      <a-descriptions-item label="备注">
        {{ displayValue(detailRecord.remark) }}
      </a-descriptions-item>
      <a-descriptions-item label="记录人">
        {{ displayValue(detailRecord.recorderName) }}
      </a-descriptions-item>
      <a-descriptions-item label="记录时间">
        {{ formatDateTime(detailRecord.recordedAt) }}
      </a-descriptions-item>
    </a-descriptions>
  </CrudDetailDrawer>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.filter-form {
  width: 100%;
}
</style>
