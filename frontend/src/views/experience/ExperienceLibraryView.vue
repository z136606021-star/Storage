<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  batchDeleteExperienceRecords,
  deleteExperienceRecord,
  downloadExperienceImportTemplate,
  exportExperienceRecords,
  fetchExperienceDetail,
  fetchExperienceFilterOptions,
  fetchExperiencePage,
  importExperienceRecords,
} from '@/api/experience'
import { downloadFile } from '@/api/file'
import { getErrorMessage } from '@/api/http'
import CrudDetailDrawer from '@/components/common/CrudDetailDrawer.vue'
import CrudListPage from '@/components/common/CrudListPage.vue'
import CrudRowActions from '@/components/common/CrudRowActions.vue'
import ExperienceFormModal from '@/components/experience/ExperienceFormModal.vue'
import ExperienceTypeManageModal from '@/components/experience/ExperienceTypeManageModal.vue'
import { useCrudDetailDrawer } from '@/composables/useCrudDetailDrawer'
import { useExcelImportExport } from '@/composables/useExcelImportExport'
import { usePaginatedCrudList } from '@/composables/usePaginatedCrudList'
import { useWritePermission } from '@/composables/useWritePermission'
import type {
  ExperienceFilterOptions,
  ExperienceRecord,
  ExperienceRecordDetail,
  ExperienceRecordQuery,
} from '@/types/experience'
import { confirmBatchDelete, confirmDelete } from '@/utils/confirmDelete'
import { downloadBlob } from '@/utils/download'
import { displayValue, formatDateTime } from '@/utils/format'
import { getTableRowIndex } from '@/utils/tableIndex'

const { canWrite } = useWritePermission('platform:experience:write')

const queryForm = reactive<{
  typeId?: number
  recorderName?: string
  keyword: string
  recordedRange: [Dayjs, Dayjs] | []
}>({
  typeId: undefined,
  recorderName: undefined,
  keyword: '',
  recordedRange: [],
})

const filterOptions = ref<ExperienceFilterOptions>({
  types: [],
  recorderNames: [],
})
const formOpen = ref(false)
const typeManageOpen = ref(false)
const editingRecord = ref<ExperienceRecordDetail | null>(null)
const downloadingAttachmentKey = ref<string | null>(null)

const typeOptions = computed(() =>
  filterOptions.value.types.map((item) => ({ label: item.name, value: item.id })),
)
const recorderOptions = computed(() =>
  filterOptions.value.recorderNames.map((name) => ({ label: name, value: name })),
)

function buildQueryParams(): ExperienceRecordQuery {
  return {
    typeId: queryForm.typeId,
    recorderName: queryForm.recorderName,
    keyword: queryForm.keyword.trim() || undefined,
    recordedStart: queryForm.recordedRange[0]?.format('YYYY-MM-DD'),
    recordedEnd: queryForm.recordedRange[1]?.format('YYYY-MM-DD'),
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
} = usePaginatedCrudList<ExperienceRecord, ExperienceRecordQuery>({
  fetchPage: fetchExperiencePage,
  buildQueryParams,
  loadErrorMessage: '加载经验库列表失败',
  enableRowSelection: true,
})

const {
  drawerOpen,
  detailLoading,
  detailRecord,
  openDetail,
  closeDetail,
} = useCrudDetailDrawer(fetchExperienceDetail, '加载经验详情失败')

const {
  exporting,
  importing,
  batchExporting,
  handleExport,
  handleBatchExport,
  handleImport,
  handleDownloadTemplate,
} = useExcelImportExport<ExperienceRecordQuery, ExperienceRecordQuery>({
  exportFn: (params) => exportExperienceRecords(params ?? buildQueryParams()),
  batchExportFn: (params) => exportExperienceRecords(params),
  importFn: importExperienceRecords,
  templateFn: downloadExperienceImportTemplate,
  buildExportParams: buildQueryParams,
  buildBatchExportParams: (ids) => ({ ...buildQueryParams(), ids }),
  getExportFilename: () => `经验库-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  getBatchExportFilename: () => `经验库-勾选-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  getTemplateFilename: () => '经验库导入模板.xlsx',
  exportSuccessMessage: '导出成功',
  showFirstImportError: true,
  onAfterImport: async () => {
    await reloadFilterOptions()
    await loadData()
  },
})

const columns = [
  { title: '序号', key: 'index', width: 70, align: 'center' as const },
  { title: '类型', dataIndex: 'typeName', key: 'typeName', width: 160, ellipsis: true },
  { title: '描述', dataIndex: 'description', key: 'description', width: 250, ellipsis: true },
  { title: '影响', dataIndex: 'impact', key: 'impact', width: 210, ellipsis: true },
  { title: '建议', dataIndex: 'suggestion', key: 'suggestion', width: 190, ellipsis: true },
  { title: '行动方案', dataIndex: 'actionPlan', key: 'actionPlan', width: 190, ellipsis: true },
  { title: '记录人', dataIndex: 'recorderName', key: 'recorderName', width: 110 },
  { title: '记录时间', dataIndex: 'recordedAt', key: 'recordedAt', width: 150 },
  { title: '操作', key: 'action', width: 150, align: 'center' as const, fixed: 'right' as const },
]

async function reloadFilterOptions() {
  filterOptions.value = await fetchExperienceFilterOptions()
}

function handleReset() {
  queryForm.typeId = undefined
  queryForm.recorderName = undefined
  queryForm.keyword = ''
  queryForm.recordedRange = []
  handleResetQuery()
  clearSelection()
  loadData()
}

function handleCreate() {
  editingRecord.value = null
  formOpen.value = true
}

async function handleEdit(record: ExperienceRecord) {
  try {
    editingRecord.value = await fetchExperienceDetail(record.id)
    formOpen.value = true
  } catch (error) {
    message.error(getErrorMessage(error, '加载编辑数据失败'))
  }
}

async function handleEditFromDrawer() {
  if (!detailRecord.value) {
    return
  }
  editingRecord.value = detailRecord.value
  drawerOpen.value = false
  formOpen.value = true
}

function handleDelete(record: ExperienceRecord) {
  confirmDelete({
    title: '确认删除',
    content: `确定删除该条经验记录吗？`,
    successMessage: '删除成功',
    onDelete: async () => {
      await deleteExperienceRecord(record.id)
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
    content: `确定删除选中的 ${selectedRowKeys.value.length} 条经验记录吗？`,
    onDelete: async () => {
      await batchDeleteExperienceRecords(selectedRowKeys.value.map((key) => Number(key)))
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

async function handleFormSuccess() {
  await reloadFilterOptions()
  await loadData()
  if (detailRecord.value) {
    await openDetail({ id: detailRecord.value.id })
  }
}

function attachmentLabel(record: ExperienceRecord) {
  return record.attachmentCount > 0 ? `${record.attachmentCount} 个附件` : '无附件'
}

async function handleDownloadAttachment(file: ExperienceRecordDetail['attachments'][number]) {
  downloadingAttachmentKey.value = file.objectKey
  try {
    const blob = await downloadFile(file.objectKey)
    downloadBlob(blob, file.originalName)
  } catch (error) {
    message.error(getErrorMessage(error, '下载失败'))
  } finally {
    downloadingAttachmentKey.value = null
  }
}

onMounted(async () => {
  try {
    await reloadFilterOptions()
  } catch {
    message.warning('筛选选项加载失败')
  }
  await loadData()
})
</script>

<template>
  <CrudListPage
    table-key="experience.library"
    :columns="columns"
    :loading="loading"
    :data-source="dataSource"
    :pagination="pagination"
    :row-key="(record: ExperienceRecord) => record.id"
    :row-selection="canWrite ? rowSelection : undefined"
    :scroll="{ x: 1500 }"
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
            <a-form-item label="类型" class="filter-item">
              <a-select
                v-model:value="queryForm.typeId"
                :options="typeOptions"
                allow-clear
                placeholder="全部"
                class="filter-control"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="模糊查找" class="filter-item">
              <a-input
                v-model:value="queryForm.keyword"
                allow-clear
                placeholder="关键字查找"
                class="filter-control"
                @press-enter="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="记录人" class="filter-item">
              <a-select
                v-model:value="queryForm.recorderName"
                :options="recorderOptions"
                allow-clear
                show-search
                option-filter-prop="label"
                placeholder="全部"
                class="filter-control"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="记录时间" class="filter-item">
              <a-range-picker v-model:value="queryForm.recordedRange" class="filter-control" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="24" :md="24" :lg="24" class="filter-actions-col">
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
      <template v-else-if="column.key === 'typeName'">
        {{ displayValue(record.typeName) }}
      </template>
      <template v-else-if="column.key === 'description'">
        {{ displayValue(record.description) }}
      </template>
      <template v-else-if="column.key === 'impact'">
        {{ displayValue(record.impact) }}
      </template>
      <template v-else-if="column.key === 'suggestion'">
        {{ displayValue(record.suggestion) }}
      </template>
      <template v-else-if="column.key === 'actionPlan'">
        {{ displayValue(record.actionPlan) }}
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

  <ExperienceFormModal
    v-model:open="formOpen"
    :record="editingRecord"
    :types="filterOptions.types"
    @success="handleFormSuccess"
    @manage-types="typeManageOpen = true"
  />

  <ExperienceTypeManageModal
    v-model:open="typeManageOpen"
    @changed="reloadFilterOptions"
  />

  <CrudDetailDrawer
    v-model:open="drawerOpen"
    title="经验详情"
    :loading="detailLoading"
    :show-edit="canWrite"
    @close="closeDetail"
    @edit="handleEditFromDrawer"
  >
    <a-descriptions v-if="detailRecord" :column="1" bordered size="small">
      <a-descriptions-item label="类型">
        {{ displayValue(detailRecord.typeName) }}
      </a-descriptions-item>
      <a-descriptions-item label="描述">
        <div class="detail-text">{{ detailRecord.description }}</div>
      </a-descriptions-item>
      <a-descriptions-item label="影响">
        <div class="detail-text">{{ displayValue(detailRecord.impact) }}</div>
      </a-descriptions-item>
      <a-descriptions-item label="建议">
        <div class="detail-text">{{ displayValue(detailRecord.suggestion) }}</div>
      </a-descriptions-item>
      <a-descriptions-item label="行动方案">
        <div class="detail-text">{{ displayValue(detailRecord.actionPlan) }}</div>
      </a-descriptions-item>
      <a-descriptions-item label="关联项目">
        <a-space v-if="detailRecord.projectNames.length" wrap>
          <a-tag v-for="project in detailRecord.projectNames" :key="project">
            {{ project }}
          </a-tag>
        </a-space>
        <span v-else>-</span>
      </a-descriptions-item>
      <a-descriptions-item label="文件/图片">
        <div v-if="detailRecord.attachments.length" class="detail-attachments">
          <template v-for="file in detailRecord.attachments" :key="file.objectKey">
            <a
              v-if="file.previewable && file.url"
              :href="file.url"
              target="_blank"
              rel="noreferrer"
            >
              {{ file.originalName }}
            </a>
            <a-button
              v-else
              type="link"
              class="detail-attachment-link"
              :loading="downloadingAttachmentKey === file.objectKey"
              @click="handleDownloadAttachment(file)"
            >
              {{ file.originalName }}
            </a-button>
          </template>
        </div>
        <span v-else>{{ attachmentLabel(detailRecord) }}</span>
      </a-descriptions-item>
      <a-descriptions-item label="记录人">
        {{ detailRecord.recorderName }}
      </a-descriptions-item>
      <a-descriptions-item label="记录时间">
        {{ formatDateTime(detailRecord.recordedAt) }}
      </a-descriptions-item>
    </a-descriptions>
  </CrudDetailDrawer>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.detail-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-attachments {
  display: flex;
  flex-direction: column;
  gap: @spacing-xs;
  align-items: flex-start;
}

.detail-attachment-link {
  height: auto;
  padding: 0;
  line-height: 1.4;
  text-align: left;
}
</style>
