<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import { Modal, message } from 'ant-design-vue'
import {
  DownloadOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
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
import { useAuth } from '@/composables/useAuth'
import type { SysMenu, SysRole, SysUser, SysUserSave } from '@/types/system'
import { downloadBlob } from '@/utils/download'
import { displayValue } from '@/utils/format'

const auth = useAuth()
const canWrite = () => auth.hasPermission('system:user:write')

const loading = ref(false)
const exporting = ref(false)
const importing = ref(false)
const dataSource = ref<SysUser[]>([])
const roleOptions = ref<SysRole[]>([])
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

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const formState = reactive({
  username: '',
  displayName: '',
  email: '',
  phone: '',
  status: 1,
  roleId: undefined as number | undefined,
  newPassword: '',
})

const columns = [
  { title: 'NTID', dataIndex: 'username', key: 'username', width: 120 },
  { title: '用户姓名', dataIndex: 'displayName', key: 'displayName', width: 120 },
  { title: '邮箱', dataIndex: 'email', key: 'email', ellipsis: true },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '角色', key: 'roles', width: 140 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'actions', width: 180, fixed: 'right' as const },
]

const permissionTreeData = computed(() => mapMenuTree(permissionMenuTree.value))

function mapMenuTree(menus: SysMenu[]): Array<{ title: string; key: number; children?: ReturnType<typeof mapMenuTree> }> {
  return menus.map((menu) => ({
    title: menu.name,
    key: menu.id,
    children: menu.children?.length ? mapMenuTree(menu.children) : undefined,
  }))
}

function buildSavePayload(): SysUserSave {
  if (!formState.roleId) {
    throw new Error('请选择角色')
  }
  return {
    username: formState.username,
    displayName: formState.displayName,
    email: formState.email || undefined,
    phone: formState.phone || undefined,
    status: formState.status,
    roleIds: [formState.roleId],
    password: formState.newPassword || undefined,
  }
}

async function loadRoles() {
  const { data } = await fetchRoles()
  roleOptions.value = data.filter((role) => role.status === 1)
}

async function loadData() {
  loading.value = true
  try {
    const { data } = await fetchUserPage({
      page: pagination.current,
      pageSize: pagination.pageSize,
      username: queryForm.username || undefined,
      displayName: queryForm.displayName || undefined,
      email: queryForm.email || undefined,
      roleId: queryForm.roleId,
    })
    dataSource.value = data.records
    pagination.total = data.total
    pagination.current = data.current
    pagination.pageSize = data.size
    if (selectedUserId.value && !data.records.some((item) => item.id === selectedUserId.value)) {
      selectedUserId.value = data.records[0]?.id ?? null
    }
  } catch (error) {
    message.error(getErrorMessage(error, '加载用户列表失败'))
  } finally {
    loading.value = false
  }
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
  pagination.current = 1
  loadData()
}

function resetForm() {
  formState.username = ''
  formState.displayName = ''
  formState.email = ''
  formState.phone = ''
  formState.status = 1
  formState.roleId = undefined
  formState.newPassword = ''
  editingId.value = null
}

function openCreate() {
  resetForm()
  modalOpen.value = true
}

function openEdit(record: SysUser) {
  editingId.value = record.id
  formState.username = record.username
  formState.displayName = record.displayName
  formState.email = record.email || ''
  formState.phone = record.phone || ''
  formState.status = record.status
  formState.roleId = record.roleIds[0]
  formState.newPassword = ''
  modalOpen.value = true
}

function openView(record: SysUser) {
  viewRecord.value = record
  drawerOpen.value = true
}

function selectUser(record: SysUser) {
  selectedUserId.value = record.id
}

async function handleSubmit() {
  try {
    const payload = buildSavePayload()
    if (editingId.value) {
      await updateUser(editingId.value, payload)
      if (formState.newPassword) {
        await resetUserPassword(editingId.value, formState.newPassword)
      }
      message.success('用户已更新')
    } else {
      await createUser(payload)
      message.success('用户已创建')
      message.info(`初始密码为 ${formState.username}@123`)
    }
    modalOpen.value = false
    await loadData()
  } catch (error) {
    message.error(getErrorMessage(error, '保存失败'))
  }
}

function handleDelete(record: SysUser) {
  Modal.confirm({
    title: '删除用户',
    content: `确定删除用户「${record.displayName}」吗？`,
    okType: 'danger',
    onOk: async () => {
      await deleteUser(record.id)
      message.success('用户已删除')
      if (selectedUserId.value === record.id) {
        selectedUserId.value = null
      }
      await loadData()
    },
  })
}

async function handleExport() {
  exporting.value = true
  try {
    const blob = await exportUsers({
      username: queryForm.username || undefined,
      displayName: queryForm.displayName || undefined,
      email: queryForm.email || undefined,
      roleId: queryForm.roleId,
    })
    downloadBlob(blob, '用户列表.xlsx')
  } catch (error) {
    message.error(getErrorMessage(error, '导出失败'))
  } finally {
    exporting.value = false
  }
}

async function handleDownloadTemplate() {
  try {
    const blob = await downloadUserImportTemplate()
    downloadBlob(blob, '用户导入模板.xlsx')
  } catch (error) {
    message.error(getErrorMessage(error, '下载模板失败'))
  }
}

async function handleImport(file: File) {
  importing.value = true
  try {
    const result = await importUsers(file)
    if (result.failCount > 0) {
      message.warning(`导入完成：成功 ${result.successCount} 条，失败 ${result.failCount} 条`)
    } else {
      message.success(`导入成功 ${result.successCount} 条`)
    }
    await loadData()
  } catch (error) {
    message.error(getErrorMessage(error, '导入失败'))
  } finally {
    importing.value = false
  }
  return false
}

function handleTableChange(page: TablePaginationConfig) {
  pagination.current = page.current
  pagination.pageSize = page.pageSize
  loadData()
}

function customRow(record: SysUser) {
  return {
    onClick: () => selectUser(record),
    class: selectedUserId.value === record.id ? 'row-selected' : '',
  }
}

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
        <div class="toolbar">
          <a-form layout="inline" :model="queryForm">
            <a-form-item label="NTID">
              <a-input v-model:value="queryForm.username" allow-clear placeholder="NTID" />
            </a-form-item>
            <a-form-item label="用户名称">
              <a-input v-model:value="queryForm.displayName" allow-clear placeholder="用户名称" />
            </a-form-item>
            <a-form-item label="邮箱">
              <a-input v-model:value="queryForm.email" allow-clear placeholder="邮箱" />
            </a-form-item>
            <a-form-item label="角色">
              <a-select
                v-model:value="queryForm.roleId"
                allow-clear
                placeholder="全部"
                style="width: 140px"
              >
                <a-select-option v-for="role in roleOptions" :key="role.id" :value="role.id">
                  {{ role.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="loadData"><SearchOutlined />查询</a-button>
                <a-button @click="resetQuery"><ReloadOutlined />重置</a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </div>

        <div class="action-bar">
          <a-space>
            <a-button v-if="canWrite()" type="primary" class="btn-add" @click="openCreate">
              <PlusOutlined />新增
            </a-button>
            <a-button type="primary" :loading="exporting" @click="handleExport">
              <DownloadOutlined />导出
            </a-button>
            <template v-if="canWrite()">
              <a-upload :show-upload-list="false" :before-upload="handleImport" accept=".xlsx,.xls">
                <a-button type="primary" :loading="importing">
                  <UploadOutlined />导入
                </a-button>
              </a-upload>
              <a-button type="link" @click="handleDownloadTemplate">下载模板</a-button>
            </template>
          </a-space>
        </div>

        <a-table
          row-key="id"
          :columns="columns"
          :data-source="dataSource"
          :loading="loading"
          :pagination="pagination"
          :custom-row="customRow"
          :scroll="{ x: 1000 }"
          @change="handleTableChange"
        >
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
                  v-if="canWrite()"
                  type="link"
                  size="small"
                  @click.stop="openEdit(record as SysUser)"
                >
                  编辑
                </a-button>
                <a-button
                  v-if="canWrite()"
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
        </a-table>
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
      ok-text="提交"
      cancel-text="关闭"
      @ok="handleSubmit"
    >
      <a-form layout="vertical">
        <a-form-item label="NTID" required>
          <a-input v-model:value="formState.username" :disabled="!!editingId" />
        </a-form-item>
        <a-form-item label="用户姓名" required>
          <a-input v-model:value="formState.displayName" />
        </a-form-item>
        <a-form-item label="邮箱">
          <a-input v-model:value="formState.email" />
        </a-form-item>
        <a-form-item label="手机号">
          <a-input v-model:value="formState.phone" />
        </a-form-item>
        <a-form-item label="角色" required>
          <a-select v-model:value="formState.roleId" placeholder="请选择角色" style="width: 100%">
            <a-select-option v-for="role in roleOptions" :key="role.id" :value="role.id">
              {{ role.name }}（{{ role.code }}）
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="是否启用" required>
          <a-switch v-model:checked="formState.status" :checked-value="1" :un-checked-value="0" />
        </a-form-item>
        <a-form-item v-if="editingId" label="重置密码（留空不修改）">
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

<style scoped>
.page {
  padding: 0 16px 16px;
  min-height: 100%;
}

.page-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.main-panel {
  flex: 1;
  min-width: 0;
}

.auth-panel {
  width: 280px;
  flex-shrink: 0;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
  max-height: calc(100vh - 200px);
  overflow: auto;
}

.auth-panel-title {
  font-weight: 600;
  margin-bottom: 12px;
}

.auth-empty {
  color: #999;
  font-size: 13px;
}

.toolbar {
  margin-bottom: 12px;
}

.action-bar {
  margin-bottom: 12px;
}

.btn-add {
  background: #52c41a;
  border-color: #52c41a;
}

.btn-add:hover {
  background: #73d13d;
  border-color: #73d13d;
}

:deep(.row-selected) {
  background: #e6f4ff !important;
}
</style>
