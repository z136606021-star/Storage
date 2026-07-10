<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  batchDeleteWarehouseBoms,
  deleteWarehouseBom,
  downloadWarehouseBomImportTemplate,
  exportWarehouseBoms,
  fetchBomFilterOptions,
  fetchWarehouseBomDetail,
  fetchWarehouseBomPage,
  importWarehouseBoms,
} from '@/api/warehouse/warehouseBom'
import CrudDetailDrawer from '@/components/common/CrudDetailDrawer.vue'
import CrudListPage from '@/components/common/CrudListPage.vue'
import CrudRowActions from '@/components/common/CrudRowActions.vue'
import BomFormModal from '@/components/warehouse/BomFormModal.vue'
import { useCrudDetailDrawer } from '@/composables/useCrudDetailDrawer'
import { useExcelImportExport } from '@/composables/useExcelImportExport'
import { useLinkedFilterOptions } from '@/composables/useLinkedFilterOptions'
import { usePaginatedCrudList } from '@/composables/usePaginatedCrudList'
import { useWritePermission } from '@/composables/useWritePermission'
import type { WarehouseBom } from '@/types/warehouse/warehouseBom'
import { confirmBatchDelete, confirmDelete } from '@/utils/confirmDelete'
import { displayValue, formatDateTime } from '@/utils/format'
import { getTableRowIndex } from '@/utils/tableIndex'
import { toSelectOptions } from '@/utils/selectOptions'

const { canWrite } = useWritePermission('warehouse:bom:write')

const defaultQuery = {
  category: undefined as string | undefined,
  genericName: undefined as string | undefined,
  brand: undefined as string | undefined,
  name: '',
  model: '',
}

const queryForm = reactive({ ...defaultQuery })
const formOpen = ref(false)
const editingRecord = ref<WarehouseBom | null>(null)

const { filterOptionsRaw, loadFilterOptions, createCascadeResetHandler, buildLinkageParams } =
  useLinkedFilterOptions({ queryForm })

const filterOptions = computed(() => ({
  category: filterOptionsRaw.value.categories ?? [],
  genericName: filterOptionsRaw.value.genericNames ?? [],
  brand: filterOptionsRaw.value.brands ?? [],
}))

function buildQueryParams() {
  const optionValue = (value?: string) => {
    const trimmed = value?.trim()
    return trimmed || undefined
  }

  return {
    category: optionValue(queryForm.category),
    genericName: optionValue(queryForm.genericName),
    brand: optionValue(queryForm.brand),
    name: queryForm.name.trim() || undefined,
    model: queryForm.model.trim() || undefined,
  }
}

const linkageFields = [
  { formKey: 'category' as const, paramKey: 'category' },
  { formKey: 'genericName' as const, paramKey: 'genericName' },
  { formKey: 'brand' as const, paramKey: 'brand' },
]

const ensureFields = [
  { field: 'genericName' as const, optionsKey: 'genericNames' as const },
  { field: 'brand' as const, optionsKey: 'brands' as const },
]

async function reloadFilterOptions() {
  await loadFilterOptions(
    fetchBomFilterOptions,
    buildLinkageParams(linkageFields),
    ensureFields,
    (raw, key) => raw[key] ?? [],
  )
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
} = usePaginatedCrudList<WarehouseBom, ReturnType<typeof buildQueryParams>>({
  fetchPage: fetchWarehouseBomPage,
  buildQueryParams,
  loadErrorMessage: '加载物料清单失败',
  enableRowSelection: true,
})

const {
  drawerOpen,
  detailLoading,
  detailRecord,
  openDetail,
  closeDetail,
} = useCrudDetailDrawer(fetchWarehouseBomDetail, '加载详情失败')

async function refreshAll() {
  await reloadFilterOptions()
  await loadData()
}

function handleReset() {
  Object.assign(queryForm, defaultQuery)
  handleResetQuery()
  clearSelection()
  refreshAll()
}

const handleCategoryChange = createCascadeResetHandler(['genericName', 'brand'], reloadFilterOptions)
const handleGenericNameChange = createCascadeResetHandler(['brand'], reloadFilterOptions)

const {
  exporting,
  importing,
  handleExport,
  handleImport,
  handleDownloadTemplate,
} = useExcelImportExport({
  exportFn: (params) => exportWarehouseBoms(params ?? buildQueryParams()),
  importFn: importWarehouseBoms,
  templateFn: downloadWarehouseBomImportTemplate,
  buildExportParams: buildQueryParams,
  getExportFilename: () => `物料清单-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  getTemplateFilename: () => '物料清单导入模板.xlsx',
  exportSuccessMessage: '导出成功',
  showFirstImportError: true,
  onAfterImport: refreshAll,
})

const columns = [
  { title: '序号', key: 'index', width: 64, align: 'center' as const },
  { title: '品类', dataIndex: 'category', key: 'category', width: 100, ellipsis: true },
  { title: '统称', dataIndex: 'genericName', key: 'genericName', width: 100, ellipsis: true },
  { title: '品牌', dataIndex: 'brand', key: 'brand', width: 80, ellipsis: true },
  { title: '名称', dataIndex: 'name', key: 'name', width: 140, ellipsis: true },
  { title: '规格', dataIndex: 'model', key: 'model', width: 120, ellipsis: true },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true, minWidth: 100 },
  { title: '图片', key: 'image', width: 72, align: 'center' as const },
  { title: '更新日期', dataIndex: 'updatedAt', key: 'updatedAt', width: 170 },
  { title: '操作', key: 'action', width: 160, align: 'center' as const, fixed: 'right' as const },
]

function handleCreate() {
  editingRecord.value = null
  formOpen.value = true
}

function handleEdit(record: WarehouseBom) {
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

function handleDelete(record: WarehouseBom) {
  confirmDelete({
    title: '确认删除',
    content: `确定删除物料清单项「${record.name}」吗？`,
    successMessage: '删除成功',
    onDelete: async () => {
      await deleteWarehouseBom(record.id)
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
    title: '批量删除',
    count: selectedRowKeys.value.length,
    content: `确定删除选中的 ${selectedRowKeys.value.length} 条物料清单吗？`,
    okText: '确定',
    onDelete: async () => {
      const ids = selectedRowKeys.value.map((key) => Number(key))
      await batchDeleteWarehouseBoms(ids)
    },
    onSuccess: async () => {
      clearSelection()
      await refreshAll()
    },
  })
}

onMounted(async () => {
  try {
    await reloadFilterOptions()
  } catch {
    message.warning('筛选选项加载失败，将使用默认选项')
  }
  await loadData()
})
</script>

<template>
  <CrudListPage
    table-key="warehouse.bom"
    :columns="columns"
    :loading="loading"
    :data-source="dataSource"
    :pagination="pagination"
    :row-key="(record: WarehouseBom) => record.id"
    :row-selection="canWrite ? rowSelection : undefined"
    :scroll="{ x: 1080 }"
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
        <a-row :gutter="[12, 8]" class="filter-row">
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="品类" class="filter-item">
              <a-select
                v-model:value="queryForm.category"
                :options="toSelectOptions(filterOptions.category)"
                allow-clear
                placeholder="全部"
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
                allow-clear
                placeholder="全部"
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
                allow-clear
                placeholder="全部"
                class="filter-control"
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
            <a-form-item label="规格" class="filter-item">
              <a-input
                v-model:value="queryForm.model"
                placeholder="关键字查找"
                allow-clear
                class="filter-control"
                @press-enter="handleSearch"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </template>

    <template #bodyCell="{ column, record, index }">
      <template v-if="column.key === 'index'">
        {{ getTableRowIndex(index, pagination) }}
      </template>
      <template v-else-if="column.key === 'brand'">
        {{ displayValue(record.brand) }}
      </template>
      <template v-else-if="column.key === 'remark'">
        {{ displayValue(record.remark) }}
      </template>
      <template v-else-if="column.key === 'image'">
        <div v-if="record.imageUrls?.length || record.imageUrl" class="image-cell">
          <a-image
            :src="record.imageUrls?.[0] ?? record.imageUrl"
            :width="48"
            :height="48"
            :preview="{ src: record.imageUrls?.[0] ?? record.imageUrl }"
            style="object-fit: cover; border-radius: 4px"
          />
          <span v-if="(record.imageUrls?.length ?? 0) > 1" class="image-count">
            +{{ (record.imageUrls?.length ?? 0) - 1 }}
          </span>
        </div>
        <span v-else>—</span>
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

  <BomFormModal v-model:open="formOpen" :record="editingRecord" @success="refreshAll" />

  <CrudDetailDrawer
    v-model:open="drawerOpen"
    title="物料清单详情"
    :loading="detailLoading"
    :show-edit="canWrite"
    @close="closeDetail"
    @edit="handleEditFromDrawer"
  >
    <a-descriptions v-if="detailRecord" :column="1" bordered size="small">
      <a-descriptions-item label="品类">{{ detailRecord.category }}</a-descriptions-item>
      <a-descriptions-item label="统称">{{ detailRecord.genericName }}</a-descriptions-item>
      <a-descriptions-item label="品牌">{{ displayValue(detailRecord.brand) }}</a-descriptions-item>
      <a-descriptions-item label="名称">{{ detailRecord.name }}</a-descriptions-item>
      <a-descriptions-item label="规格">{{ displayValue(detailRecord.model) }}</a-descriptions-item>
      <a-descriptions-item label="备注">{{ displayValue(detailRecord.remark) }}</a-descriptions-item>
      <a-descriptions-item label="图片">
        <a-image-preview-group v-if="detailRecord.imageUrls?.length">
          <a-space wrap>
            <a-image
              v-for="(url, index) in detailRecord.imageUrls"
              :key="`${detailRecord.id}-${index}`"
              :src="url"
              :width="96"
            />
          </a-space>
        </a-image-preview-group>
        <a-image
          v-else-if="detailRecord.imageUrl"
          :src="detailRecord.imageUrl"
          :width="120"
          :preview="{ src: detailRecord.imageUrl }"
        />
        <span v-else>—</span>
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

.filter-form {
  width: 100%;
}

.filter-row {
  width: 100%;
}

.filter-item {
  margin-bottom: 0;
}

.filter-control {
  width: 100%;
  min-width: 120px;
}

.filter-actions-col {
  display: flex;
  align-items: flex-start;
}

.image-cell {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.image-count {
  margin-left: 4px;
  font-size: 12px;
  color: @color-text-tertiary;
}
</style>
