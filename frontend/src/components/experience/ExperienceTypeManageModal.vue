<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import {
  createExperienceType,
  deleteExperienceType,
  fetchExperienceTypes,
  updateExperienceType,
} from '@/api/experience'
import { getErrorMessage } from '@/api/http'
import type { ExperienceType, ExperienceTypeSavePayload } from '@/types/experience'
import { confirmDelete } from '@/utils/confirmDelete'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  changed: []
}>()

const loading = ref(false)
const saving = ref(false)
const records = ref<ExperienceType[]>([])
const editingId = ref<number | null>(null)

const formState = reactive<ExperienceTypeSavePayload>({
  name: '',
  status: 1,
  sortOrder: 0,
})

const columns = [
  { title: '类型名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90, align: 'center' as const },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80, align: 'center' as const },
  { title: '操作', key: 'action', width: 130, align: 'center' as const },
]

watch(
  () => props.open,
  (open) => {
    if (open) {
      loadData()
    }
  },
)

async function loadData() {
  loading.value = true
  try {
    records.value = await fetchExperienceTypes()
  } catch (error) {
    message.error(getErrorMessage(error, '加载类型失败'))
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  formState.name = ''
  formState.status = 1
  formState.sortOrder = 0
}

function editRecord(record: ExperienceType) {
  editingId.value = record.id
  formState.name = record.name
  formState.status = record.status
  formState.sortOrder = record.sortOrder
}

async function handleSave() {
  if (!formState.name.trim()) {
    message.warning('请输入类型名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: formState.name.trim(),
      status: formState.status,
      sortOrder: Number(formState.sortOrder) || 0,
    }
    if (editingId.value) {
      await updateExperienceType(editingId.value, payload)
      message.success('类型已更新')
    } else {
      await createExperienceType(payload)
      message.success('类型已新增')
    }
    resetForm()
    await loadData()
    emit('changed')
  } catch (error) {
    message.error(getErrorMessage(error, '保存类型失败'))
  } finally {
    saving.value = false
  }
}

function handleDelete(record: ExperienceType) {
  confirmDelete({
    title: '确认删除',
    content: `确定删除类型「${record.name}」吗？`,
    successMessage: '删除成功',
    onDelete: async () => {
      await deleteExperienceType(record.id)
    },
    onSuccess: async () => {
      if (editingId.value === record.id) {
        resetForm()
      }
      await loadData()
      emit('changed')
    },
  })
}

function handleClose() {
  emit('update:open', false)
}
</script>

<template>
  <a-modal
    :open="open"
    title="类型管理"
    width="720px"
    :footer="null"
    destroy-on-close
    @cancel="handleClose"
  >
    <a-form layout="inline" class="type-form">
      <a-form-item label="类型名称">
        <a-input v-model:value="formState.name" allow-clear placeholder="请输入类型名称" />
      </a-form-item>
      <a-form-item label="状态">
        <a-select
          v-model:value="formState.status"
          :options="[
            { label: '启用', value: 1 },
            { label: '停用', value: 0 },
          ]"
          style="width: 96px"
        />
      </a-form-item>
      <a-form-item label="排序">
        <a-input-number v-model:value="formState.sortOrder" :min="0" :precision="0" />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" :loading="saving" @click="handleSave">
            <template #icon><PlusOutlined v-if="!editingId" /><EditOutlined v-else /></template>
            {{ editingId ? '保存' : '新增' }}
          </a-button>
          <a-button @click="resetForm">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      :columns="columns"
      :data-source="records"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="small"
      bordered
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'success' : 'default'">
            {{ record.status === 1 ? '启用' : '停用' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space :size="0">
            <a-button type="link" size="small" @click="editRecord(record)">编辑</a-button>
            <a-button type="link" size="small" danger @click="handleDelete(record)">
              <template #icon><DeleteOutlined /></template>
              删除
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.type-form {
  margin-bottom: @spacing-md;
  row-gap: @spacing-sm;
}
</style>
