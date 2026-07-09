<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  createDesignStage,
  deleteDesignStage,
  exportDesignStages,
  fetchDesignStagePage,
  updateDesignStage,
} from '@/api/design/designGuide'
import { getErrorMessage } from '@/api/http'
import CrudListPage from '@/components/common/CrudListPage.vue'
import CrudRowActions from '@/components/common/CrudRowActions.vue'
import { useExcelImportExport } from '@/composables/useExcelImportExport'
import { usePaginatedCrudList } from '@/composables/usePaginatedCrudList'
import type { DesignStage, DesignStageSavePayload } from '@/types/design/designGuide'
import { confirmDelete } from '@/utils/confirmDelete'
import { displayValue, formatDateTime } from '@/utils/format'

const props = defineProps<{
  open: boolean
  canWrite: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  changed: []
}>()

const queryForm = reactive({
  stageName: '',
})

const formOpen = ref(false)
const formRef = ref<FormInstance>()
const submitting = ref(false)
const editingRecord = ref<DesignStage | null>(null)
const formState = reactive<DesignStageSavePayload>({
  sortOrder: 1,
  stageName: '',
  enabled: true,
})

const rules = {
  sortOrder: [{ required: true, message: '请输入顺序', trigger: 'blur' }],
  stageName: [{ required: true, message: '请输入阶段', trigger: 'blur' }],
}

function buildQueryParams() {
  return {
    stageName: queryForm.stageName.trim() || undefined,
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
} = usePaginatedCrudList<DesignStage, ReturnType<typeof buildQueryParams>>({
  fetchPage: fetchDesignStagePage,
  buildQueryParams,
  loadErrorMessage: '加载阶段配置失败',
})

const { exporting, handleExport } = useExcelImportExport({
  exportFn: (params) => exportDesignStages(params ?? buildQueryParams()),
  buildExportParams: buildQueryParams,
  getExportFilename: () => `阶段配置-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  exportSuccessMessage: '导出成功',
})

const columns = [
  { title: '顺序', key: 'sortOrder', dataIndex: 'sortOrder', width: 90, align: 'center' as const },
  { title: '阶段', dataIndex: 'stageName', key: 'stageName', minWidth: 180 },
  { title: '是否启用', key: 'enabled', width: 120, align: 'center' as const },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 140 },
  { title: '操作日期', dataIndex: 'operatedAt', key: 'operatedAt', width: 170 },
  { title: '操作', key: 'action', width: 150, align: 'center' as const, fixed: 'right' as const },
]

watch(
  () => props.open,
  (open) => {
    if (open) {
      loadData()
    }
  },
)

function handleReset() {
  queryForm.stageName = ''
  handleResetQuery()
  loadData()
}

function handleCreate() {
  editingRecord.value = null
  Object.assign(formState, { sortOrder: 1, stageName: '', enabled: true })
  formOpen.value = true
}

function handleEdit(record: DesignStage) {
  editingRecord.value = record
  Object.assign(formState, {
    sortOrder: record.sortOrder,
    stageName: record.stageName,
    enabled: record.enabled === 1,
  })
  formOpen.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    if (editingRecord.value) {
      await updateDesignStage(editingRecord.value.id, formState)
      message.success('阶段已更新')
    } else {
      await createDesignStage(formState)
      message.success('阶段已创建')
    }
    formOpen.value = false
    emit('changed')
    await loadData()
  } catch (error) {
    message.error(getErrorMessage(error, '保存失败'))
  } finally {
    submitting.value = false
  }
}

function handleDelete(record: DesignStage) {
  confirmDelete({
    title: '确认删除',
    content: `确定删除阶段「${record.stageName}」吗？`,
    successMessage: '删除成功',
    onDelete: async () => {
      await deleteDesignStage(record.id)
    },
    onSuccess: async () => {
      emit('changed')
      await loadData()
    },
  })
}

async function handleToggle(record: DesignStage, checked: boolean) {
  try {
    await updateDesignStage(record.id, {
      sortOrder: record.sortOrder,
      stageName: record.stageName,
      enabled: checked,
    })
    message.success(checked ? '阶段已启用' : '阶段已停用')
    emit('changed')
    await loadData()
  } catch (error) {
    message.error(getErrorMessage(error, '状态更新失败'))
  }
}

onMounted(() => {
  if (props.open) {
    loadData()
  }
})
</script>

<template>
  <a-modal
    :open="open"
    title="阶段配置"
    width="1040px"
    :footer="null"
    destroy-on-close
    @cancel="emit('update:open', false)"
  >
    <CrudListPage
      :columns="columns"
      :loading="loading"
      :data-source="dataSource"
      :pagination="pagination"
      :row-key="(record: DesignStage) => record.id"
      :scroll="{ x: 820 }"
      toolbar-create-green
      toolbar-export-icon="download"
      :toolbar-can-write="canWrite"
      :toolbar-show-import="false"
      :toolbar-show-template="false"
      :toolbar-exporting="exporting"
      @change="handleTableChange"
      @toolbar-create="handleCreate"
      @toolbar-export="handleExport"
    >
      <template #filters>
        <a-form layout="inline" class="filter-form">
          <a-form-item label="阶段">
            <a-input
              v-model:value="queryForm.stageName"
              allow-clear
              placeholder="关键字查找"
              style="width: 220px"
              @press-enter="handleSearch"
            />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch">
                <template #icon><SearchOutlined /></template>
                搜索
              </a-button>
              <a-button @click="handleReset">
                <template #icon><ReloadOutlined /></template>
                重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'enabled'">
          <a-switch
            :checked="record.enabled === 1"
            :disabled="!canWrite"
            @change="(checked: boolean) => handleToggle(record, checked)"
          />
        </template>
        <template v-else-if="column.key === 'operatorName'">
          {{ displayValue(record.operatorName) }}
        </template>
        <template v-else-if="column.key === 'operatedAt'">
          {{ formatDateTime(record.operatedAt) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <CrudRowActions
            :can-write="canWrite"
            :show-delete="canWrite"
            @view="handleEdit(record)"
            @edit="handleEdit(record)"
            @delete="handleDelete(record)"
          />
        </template>
      </template>
    </CrudListPage>

    <a-modal
      v-model:open="formOpen"
      :title="editingRecord ? '编辑阶段' : '新增阶段'"
      :confirm-loading="submitting"
      ok-text="提交"
      cancel-text="关闭"
      destroy-on-close
      @ok="handleSubmit"
    >
      <a-form ref="formRef" :model="formState" :rules="rules" layout="vertical">
        <a-form-item label="顺序" name="sortOrder">
          <a-input-number
            v-model:value="formState.sortOrder"
            :min="1"
            :precision="0"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="阶段" name="stageName">
          <a-input v-model:value="formState.stageName" allow-clear />
        </a-form-item>
        <a-form-item label="是否启用" name="enabled">
          <a-switch v-model:checked="formState.enabled" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-modal>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.filter-form {
  row-gap: @spacing-sm;
}
</style>
