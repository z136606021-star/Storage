<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { getErrorMessage } from '@/api/http'
import {
  createUser,
  deleteUser,
  downloadUserImportTemplate,
  exportUsers,
  fetchUserPage,
  fetchUserPermissions,
  importUsers,
  resetUserPassword,
  updateUser,
} from '@/api/system/user'
import { fetchRoles } from '@/api/system/role'
import CrudListPage from '@/components/common/CrudListPage.vue'
import { useWritePermission } from '@/composables/useWritePermission'
import { useExcelImportExport } from '@/composables/useExcelImportExport'
import { usePaginatedCrudList } from '@/composables/usePaginatedCrudList'
import type { SysMenu, SysRole, SysUser } from '@/types/system'
import { buildMenuAuthTreeNodes } from '@/utils/menuAuthTree'
import { confirmDelete } from '@/utils/confirmDelete'
import { displayValue, normalizeEmail } from '@/utils/format'
import {
  buildUserSavePayload,
  createUserFormRules,
  shouldBlockUserSubmit,
} from '@/utils/userFormValidation'

const { canWrite } = useWritePermission('system:user:write')

const roleOptions = ref<SysRole[]>([])
const formRef = ref<FormInstance>()
const submitting = ref(false)
const modalOpen = ref(false)
const drawerOpen = ref(false)
const editingId = ref<number | null>(null)
const viewRecord = ref<SysUser | null>(null)
const selectedUserId = ref<number | null>(null)
const permissionLoading = ref(false)
const checkedMenuIds = ref<number[]>([])
const permissionMenuTree = ref<SysMenu[]>([])

const queryForm = reactive({
  username: '',
  displayName: '',
  email: '',
  roleId: undefined as number | undefined,
})

function buildQueryParams() {
  return {
    username: queryForm.username || undefined,
    displayName: queryForm.displayName || undefined,
    email: normalizeEmail(queryForm.email) || undefined,
    roleId: queryForm.roleId,
  }
}

const {
  loading,
  dataSource,
  pagination,
  loadData,
  handleResetQuery,
  handleTableChange,
} = usePaginatedCrudList<SysUser, ReturnType<typeof buildQueryParams>>({
  fetchPage: async (params) => {
    const { data } = await fetchUserPage(params)
    return data
  },
  buildQueryParams,
  loadErrorMessage: '加载用户列表失败',
  onAfterLoad: (records) => {
    if (selectedUserId.value && !records.some((item) => item.id === selectedUserId.value)) {
      selectedUserId.value = records[0]?.id ?? null
    }
  },
})

const { exporting, importing, handleExport, handleImport, handleDownloadTemplate } =
  useExcelImportExport({
    exportFn: exportUsers,
    importFn: importUsers,
    templateFn: downloadUserImportTemplate,
    buildExportParams: buildQueryParams,
    getExportFilename: () => '用户列表.xlsx',
    getTemplateFilename: () => '用户导入模板.xlsx',
    exportErrorMessage: '导出失败',
    templateErrorMessage: '下载模板失败',
    importErrorMessage: '导入失败',
    onAfterImport: loadData,
  })

const formState = reactive({
  username: '',
  displayName: '',
  email: '',
  phone: '',
  status: 1,
  roleIds: [] as number[],
  newPassword: '',
})

const formRules = computed(() => createUserFormRules(() => editingId.value != null))

const columns = [
  { title: 'NTID', dataIndex: 'username', key: 'username', width: 120 },
  { title: '用户姓名', dataIndex: 'displayName', key: 'displayName', width: 120 },
  { title: '邮箱', dataIndex: 'email', key: 'email', ellipsis: true },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '角色', key: 'roles', width: 140 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'actions', width: 180, fixed: 'right' as const },
]

const permissionTreeData = computed(() => buildMenuAuthTreeNodes(permissionMenuTree.value))

async function loadRoles() {
  const { data } = await fetchRoles()
  roleOptions.value = data.filter((role) => role.status === 1)
}

async function loadPermissions(userId: number) {
  permissionLoading.value = true
  try {
    const { data } = await fetchUserPermissions(userId)
    permissionMenuTree.value = data.menuTree
    checkedMenuIds.value = data.checkedMenuIds
  } catch (error) {
    message.error(getErrorMessage(error, '加载授权信息失败'))
  } finally {
    permissionLoading.value = false
  }
}

function resetQuery() {
  queryForm.username = ''
  queryForm.displayName = ''
  queryForm.email = ''
  queryForm.roleId = undefined
  handleResetQuery()
  loadData()
}

function resetForm() {
  formState.username = ''
  formState.displayName = ''
  formState.email = ''
  formState.phone = ''
  formState.status = 1
  formState.roleIds = []
  formState.newPassword = ''
  editingId.value = null
}

function openCreate() {
  resetForm()
  modalOpen.value = true
}

function openEdit(record: SysUser) {
  resetForm()
  editingId.value = record.id
  formState.username = record.username
  formState.displayName = record.displayName
  formState.email = record.email || ''
  formState.phone = record.phone || ''
  formState.status = record.status
  formState.roleIds = [...record.roleIds]
  formState.newPassword = ''
  modalOpen.value = true
}

function openView(record: SysUser) {
  viewRecord.value = record
  drawerOpen.value = true
}

function normalizeEmailField(value: string) {
  const normalized = normalizeEmail(value)
  return normalized ?? ''
}

function selectUser(record: SysUser) {
  selectedUserId.value = record.id
}

async function handleSubmit() {
  if (shouldBlockUserSubmit(submitting.value)) {
    return
  }

  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  let saved = false
  const editingUserId = editingId.value
  const newPassword = formState.newPassword.trim()
  const payload = buildUserSavePayload(formState)

  try {
    if (editingUserId) {
      await updateUser(editingUserId, payload)
      if (newPassword) {
        await resetUserPassword(editingUserId, newPassword)
      }
      message.success(newPassword ? '用户资料已更新，密码已重置' : '用户已更新')
    } else {
      await createUser(payload)
      message.success('用户已创建')
      message.info(`初始密码为 ${payload.username}@123`)
    }
    saved = true
    modalOpen.value = false
  } catch (error) {
    message.error(getErrorMessage(error, '保存失败'))
  } finally {
    submitting.value = false
  }

  if (!saved) {
    return
  }

  try {
    await loadData()
  } catch (error) {
    message.warning(getErrorMessage(error, '用户已保存，但列表刷新失败'))
  }
}

function handleDelete(record: SysUser) {
  confirmDelete({
    title: '删除用户',
    content: `确定删除用户「${record.displayName}」吗？`,
    successMessage: '用户已删除',
    errorMessage: '删除失败',
    onDelete: async () => {
      await deleteUser(record.id)
    },
    onSuccess: async () => {
      if (selectedUserId.value === record.id) {
        selectedUserId.value = null
      }
      await loadData()
    },
  })
}

function customRow(record: SysUser) {
  return {
    onClick: () => selectUser(record),
    class: selectedUserId.value === record.id ? 'row-selected' : '',
  }
}

watch(modalOpen, (open) => {
  if (open) {
    formRef.value?.clearValidate()
  }
})

watch(selectedUserId, (userId) => {
  if (userId) {
    loadPermissions(userId)
  } else {
    checkedMenuIds.value = []
    permissionMenuTree.value = []
  }
})

onMounted(async () => {
  await loadRoles()
  await loadData()
  if (dataSource.value.length > 0) {
    selectedUserId.value = dataSource.value[0].id
  }
})
</script>

<template>
  <div class="page">
    <div class="page-body">
      <div class="main-panel">
        <CrudListPage
          table-key="system.user"
          :columns="columns"
          :loading="loading"
          :data-source="dataSource"
          :pagination="pagination"
          :custom-row="customRow"
          :scroll="{ x: 1000 }"
          :size="'middle'"
          :bordered="false"
          toolbar-create-green
          toolbar-export-icon="download"
          :toolbar-can-write="canWrite"
          :toolbar-importing="importing"
          :toolbar-exporting="exporting"
          @change="handleTableChange"
          @toolbar-create="openCreate"
          @toolbar-export="handleExport"
          @toolbar-import="handleImport"
          @toolbar-download-template="handleDownloadTemplate"
        >
          <template #filters>
            <a-form
              layout="horizontal"
              class="filter-form"
              :label-col="{ flex: '72px' }"
              :wrapper-col="{ flex: '1' }"
            >
              <a-row :gutter="[12, 8]" class="filter-row">
                <a-col :xs="24" :sm="12" :md="8" :lg="5">
                  <a-form-item label="NTID" class="filter-item">
                    <a-input
                      v-model:value="queryForm.username"
                      allow-clear
                      placeholder="关键字查找"
                      class="filter-control"
                      @press-enter="loadData"
                    />
                  </a-form-item>
                </a-col>
                <a-col :xs="24" :sm="12" :md="8" :lg="5">
                  <a-form-item label="用户名称" class="filter-item">
                    <a-input
                      v-model:value="queryForm.displayName"
                      allow-clear
                      placeholder="关键字查找"
                      class="filter-control"
                      @press-enter="loadData"
                    />
                  </a-form-item>
                </a-col>
                <a-col :xs="24" :sm="12" :md="8" :lg="5">
                  <a-form-item label="邮箱" class="filter-item">
                    <a-input
                      v-model:value="queryForm.email"
                      allow-clear
                      placeholder="关键字查找"
                      class="filter-control"
                      @press-enter="loadData"
                      @blur="queryForm.email = normalizeEmailField(queryForm.email)"
                    />
                  </a-form-item>
                </a-col>
                <a-col :xs="24" :sm="12" :md="8" :lg="5">
                  <a-form-item label="角色" class="filter-item">
                    <a-select
                      v-model:value="queryForm.roleId"
                      allow-clear
                      placeholder="全部"
                      class="filter-control"
                    >
                      <a-select-option v-for="role in roleOptions" :key="role.id" :value="role.id">
                        {{ role.name }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :xs="24" :sm="24" :md="24" :lg="4" class="filter-actions-col">
                  <a-form-item class="filter-item filter-actions">
                    <a-space>
                      <a-button type="primary" @click="loadData">
                        <template #icon><SearchOutlined /></template>
                        查询
                      </a-button>
                      <a-button @click="resetQuery">
                        <template #icon><ReloadOutlined /></template>
                        重置
                      </a-button>
                    </a-space>
                  </a-form-item>
                </a-col>
              </a-row>
            </a-form>
          </template>

          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'roles'">
              {{ (record as SysUser).roleNames?.join('、') || (record as SysUser).roleCodes?.join('、') || '-' }}
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="(record as SysUser).status === 1 ? 'processing' : 'default'">
                {{ (record as SysUser).status === 1 ? '启用' : '停用' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-space>
                <a-button type="link" size="small" @click.stop="openView(record as SysUser)">查看</a-button>
                <a-button
                  v-if="canWrite"
                  type="link"
                  size="small"
                  @click.stop="openEdit(record as SysUser)"
                >
                  编辑
                </a-button>
                <a-button
                  v-if="canWrite"
                  type="link"
                  size="small"
                  danger
                  @click.stop="handleDelete(record as SysUser)"
                >
                  删除
                </a-button>
              </a-space>
            </template>
          </template>
        </CrudListPage>
      </div>

      <div class="auth-panel">
        <div class="auth-panel-title">授权管理</div>
        <a-spin :spinning="permissionLoading">
          <div v-if="!selectedUserId" class="auth-empty">请在左侧选择用户查看权限</div>
          <a-tree
            v-else
            :tree-data="permissionTreeData"
            :checked-keys="checkedMenuIds"
            checkable
            disabled
            default-expand-all
          />
        </a-spin>
      </div>
    </div>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑用户' : '新增用户'"
      :confirm-loading="submitting"
      destroy-on-close
      ok-text="提交"
      cancel-text="关闭"
      @ok="handleSubmit"
    >
      <a-form ref="formRef" :model="formState" :rules="formRules" layout="vertical">
        <a-form-item label="NTID" name="username">
          <a-input v-model:value="formState.username" :disabled="!!editingId" allow-clear />
        </a-form-item>
        <a-form-item label="用户姓名" name="displayName">
          <a-input v-model:value="formState.displayName" allow-clear />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input
            v-model:value="formState.email"
            allow-clear
            @blur="formState.email = normalizeEmailField(formState.email)"
          />
        </a-form-item>
        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="formState.phone" allow-clear />
        </a-form-item>
        <a-form-item label="角色" name="roleIds">
          <a-select
            v-model:value="formState.roleIds"
            mode="multiple"
            placeholder="请选择角色"
            style="width: 100%"
          >
            <a-select-option v-for="role in roleOptions" :key="role.id" :value="role.id">
              {{ role.name }}（{{ role.code }}）
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="是否启用" name="status">
          <a-switch v-model:checked="formState.status" :checked-value="1" :un-checked-value="0" />
        </a-form-item>
        <a-form-item v-if="editingId" label="重置密码（留空不修改）" name="newPassword">
          <a-input-password v-model:value="formState.newPassword" placeholder="请输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="drawerOpen" title="用户详情" width="420">
      <a-descriptions v-if="viewRecord" :column="1" bordered size="small">
        <a-descriptions-item label="NTID">{{ displayValue(viewRecord.username) }}</a-descriptions-item>
        <a-descriptions-item label="用户姓名">{{ displayValue(viewRecord.displayName) }}</a-descriptions-item>
        <a-descriptions-item label="邮箱">{{ displayValue(viewRecord.email) }}</a-descriptions-item>
        <a-descriptions-item label="手机号">{{ displayValue(viewRecord.phone) }}</a-descriptions-item>
        <a-descriptions-item label="角色">
          {{ viewRecord.roleNames?.join('、') || viewRecord.roleCodes?.join('、') || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          {{ viewRecord.status === 1 ? '启用' : '停用' }}
        </a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </div>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';
@import '@/styles/mixins.less';

.page {
  padding: 0 @spacing-lg @spacing-lg;
  min-height: 100%;
}

.page-body {
  display: flex;
  flex-wrap: wrap;
  gap: @spacing-lg;
  align-items: flex-start;
}

.main-panel {
  flex: 1 1 640px;
  min-width: 0;
}

.auth-panel {
  flex: 1 1 280px;
  width: 100%;
  max-width: 100%;
  border: 1px solid @color-border;
  border-radius: @radius-md;
  padding: @spacing-md;
  background: @color-bg-elevated;
  max-height: calc(100vh - 200px);
  overflow: auto;
}

@media (min-width: 1200px) {
  .auth-panel {
    flex: 0 0 280px;
    width: 280px;
  }
}

.auth-panel-title {
  font-weight: 600;
  margin-bottom: @spacing-md;
}

.auth-empty {
  color: @color-text-disabled;
  font-size: @font-size-sm;
}

:deep(.row-selected) {
  .row-highlight();
}
</style>
