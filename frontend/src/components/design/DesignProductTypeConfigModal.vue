<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  createDesignProductType,
  deleteDesignProductType,
  exportDesignProductTypes,
  fetchDesignProductTypePage,
  updateDesignProductType,
} from '@/api/design/designGuide'
import { getErrorMessage } from '@/api/http'
import CrudListPage from '@/components/common/CrudListPage.vue'
import CrudRowActions from '@/components/common/CrudRowActions.vue'
import { useExcelImportExport } from '@/composables/useExcelImportExport'
import { usePaginatedCrudList } from '@/composables/usePaginatedCrudList'
import type {
  DesignProductType,
  DesignProductTypeSavePayload,
} from '@/types/design/designGuide'
import { confirmDelete } from '@/utils/confirmDelete'
import { displayValue, formatDateTime } from '@/utils/format'
import { getTableRowIndex } from '@/utils/tableIndex'

const props = defineProps<{
  open: boolean
  canWrite: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  changed: []
}>()

const queryForm = reactive({
  typeCode: '',
  typeName: '',
})

const formOpen = ref(false)
const formRef = ref<FormInstance>()
const submitting = ref(false)
const editingRecord = ref<DesignProductType | null>(null)
const formState = reactive<DesignProductTypeSavePayload>({
  typeCode: '',
  typeName: '',
  enabled: true,
})

const rules = {
  typeCode: [{ required: true, message: '请输入产品类型编号', trigger: 'blur' }],
  typeName: [{ required: true, message: '请输入产品类型', trigger: 'blur' }],
}

function buildQueryParams() {
  return {
    typeCode: queryForm.typeCode.trim() || undefined,
    typeName: queryForm.typeName.trim() || undefined,
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
} = usePaginatedCrudList<DesignProductType, ReturnType<typeof buildQueryParams>>({
  fetchPage: fetchDesignProductTypePage,
  buildQueryParams,
  loadErrorMessage: '加载产品类型配置失败',
})

const { exporting, handleExport } = useExcelImportExport({
  exportFn: (params) => exportDesignProductTypes(params ?? buildQueryParams()),
  buildExportParams: buildQueryParams,
  getExportFilename: () => `产品类型配置-${dayjs().format('YYYY-MM-DD')}.xlsx`,
  exportSuccessMessage: '导出成功',
})

const columns = [
  { title: 'NO.', key: 'index', width: 70, align: 'center' as const },
  { title: '产品类型编号', dataIndex: 'typeCode', key: 'typeCode', width: 130 },
  { title: '产品类型', dataIndex: 'typeName', key: 'typeName', minWidth: 160 },
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
  queryForm.typeCode = ''
  queryForm.typeName = ''
  handleResetQuery()
  loadData()
}

function handleCreate() {
  editingRecord.value = null
  Object.assign(formState, { typeCode: '', typeName: '', enabled: true })
  formOpen.value = true
}

function handleEdit(record: DesignProductType) {
  editingRecord.value = record
  Object.assign(formState, {
    typeCode: record.typeCode,
    typeName: record.typeName,
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
      await updateDesignProductType(editingRecord.value.id, formState)
      message.success('产品类型已更新')
    } else {
      await createDesignProductType(formState)
      message.success('产品类型已创建')
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

function handleDelete(record: DesignProductType) {
  confirmDelete({
    title: '确认删除',
    content: `确定删除产品类型「${record.typeCode} / ${record.typeName}」吗？`,
    successMessage: '删除成功',
    onDelete: async () => {
      await deleteDesignProductType(record.id)
    },
    onSuccess: async () => {
      emit('changed')
      await loadData()
    },
  })
}

async function handleToggle(record: DesignProductType, checked: boolean) {
  try {
    await updateDesignProductType(record.id, {
      typeCode: record.typeCode,
      typeName: record.typeName,
      enabled: checked,
    })
    message.success(checked ? '产品类型已启用' : '产品类型已停用')
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
    title="产品类型配置"
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
      :row-key="(record: DesignProductType) => record.id"
      :scroll="{ x: 920 }"
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
          <a-form-item label="产品类型编号">
            <a-input
              v-model:value="queryForm.typeCode"
              allow-clear
              placeholder="关键字查找"
              style="width: 180px"
              @press-enter="handleSearch"
            />
          </a-form-item>
          <a-form-item label="产品类型">
            <a-input
              v-model:value="queryForm.typeName"
              allow-clear
              placeholder="关键字查找"
              style="width: 180px"
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

      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'index'">
          {{ getTableRowIndex(index, pagination) }}
        </template>
        <template v-else-if="column.key === 'enabled'">
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
      :title="editingRecord ? '编辑产品类型' : '新增产品类型'"
      :confirm-loading="submitting"
      ok-text="提交"
      cancel-text="关闭"
      destroy-on-close
      @ok="handleSubmit"
    >
      <a-form ref="formRef" :model="formState" :rules="rules" layout="vertical">
        <a-form-item label="产品类型编号" name="typeCode">
          <a-input v-model:value="formState.typeCode" allow-clear />
        </a-form-item>
        <a-form-item label="产品类型" name="typeName">
          <a-input v-model:value="formState.typeName" allow-clear />
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
