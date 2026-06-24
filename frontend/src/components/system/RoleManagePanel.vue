<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
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
  createRole,
  deleteRole,
  downloadRoleImportTemplate,
  exportRoles,
  fetchRoleDetail,
  fetchRoles,
  importRoles,
  updateRole,
} from '@/api/system/role'
import { fetchMenuTree } from '@/api/system/menu'
import { useAuth } from '@/composables/useAuth'
import type { SysMenu, SysRole, SysRoleSave } from '@/types/system'
import { downloadBlob } from '@/utils/download'
import { displayValue } from '@/utils/format'

const auth = useAuth()
const canWrite = () => auth.hasPermission('system:role:write')

const loading = ref(false)
const exporting = ref(false)
const importing = ref(false)
const dataSource = ref<SysRole[]>([])
const menuTree = ref<SysMenu[]>([])
const modalOpen = ref(false)
const drawerOpen = ref(false)
const editingId = ref<number | null>(null)
const viewRecord = ref<SysRole | null>(null)

const queryForm = reactive({
  roleId: undefined as number | undefined,
})

const formState = reactive<SysRoleSave>({
  code: '',
  name: '',
  status: 1,
  menuIds: [],
})

const filteredData = computed(() => {
  if (!queryForm.roleId) {
    return dataSource.value
  }
  return dataSource.value.filter((role) => role.id === queryForm.roleId)
})

const menuTreeData = computed(() => mapMenuTree(menuTree.value))
const viewMenuTreeData = computed(() => mapMenuTree(menuTree.value))

const columns = [
  { title: '角色编码', dataIndex: 'code', key: 'code' },
  { title: '角色', dataIndex: 'name', key: 'name' },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'actions', width: 200 },
]

function mapMenuTree(menus: SysMenu[]): Array<{ title: string; key: number; children?: ReturnType<typeof mapMenuTree> }> {
  return menus.map((menu) => ({
    title: `${menu.name}${menu.permission ? ` (${menu.permission})` : ''}`,
    key: menu.id,
    children: menu.children?.length ? mapMenuTree(menu.children) : undefined,
  }))
}

async function loadMenus() {
  const { data } = await fetchMenuTree()
  menuTree.value = data
}

async function loadData() {
  loading.value = true
  try {
    const { data } = await fetchRoles()
    dataSource.value = data
  } catch (error) {
    message.error(getErrorMessage(error, '加载角色列表失败'))
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryForm.roleId = undefined
}

function resetForm() {
  formState.code = ''
  formState.name = ''
  formState.status = 1
  formState.menuIds = []
  editingId.value = null
}

function openCreate() {
  resetForm()
  modalOpen.value = true
}

function openEdit(record: SysRole) {
  editingId.value = record.id
  formState.code = record.code
  formState.name = record.name
  formState.status = record.status
  formState.menuIds = [...record.menuIds]
  modalOpen.value = true
}

async function openView(record: SysRole) {
  try {
    const { data } = await fetchRoleDetail(record.id)
    viewRecord.value = data
    drawerOpen.value = true
  } catch (error) {
    message.error(getErrorMessage(error, '加载角色详情失败'))
  }
}

async function handleSubmit() {
  try {
    if (editingId.value) {
      await updateRole(editingId.value, formState)
      message.success('角色已更新')
    } else {
      await createRole(formState)
      message.success('角色已创建')
    }
    modalOpen.value = false
    await loadData()
  } catch (error) {
    message.error(getErrorMessage(error, '保存失败'))
  }
}

function handleDelete(record: SysRole) {
  Modal.confirm({
    title: '删除角色',
    content: `确定删除角色「${record.name}」吗？`,
    okType: 'danger',
    onOk: async () => {
      await deleteRole(record.id)
      message.success('角色已删除')
      await loadData()
    },
  })
}

async function handleExport() {
  exporting.value = true
  try {
    const blob = await exportRoles()
    downloadBlob(blob, '角色列表.xlsx')
  } catch (error) {
    message.error(getErrorMessage(error, '导出失败'))
  } finally {
    exporting.value = false
  }
}

async function handleDownloadTemplate() {
  try {
    const blob = await downloadRoleImportTemplate()
    downloadBlob(blob, '角色导入模板.xlsx')
  } catch (error) {
    message.error(getErrorMessage(error, '下载模板失败'))
  }
}

async function handleImport(file: File) {
  importing.value = true
  try {
    const result = await importRoles(file)
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

onMounted(async () => {
  await loadMenus()
  await loadData()
})
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <a-form layout="inline" :model="queryForm">
        <a-form-item label="角色">
          <a-select
            v-model:value="queryForm.roleId"
            allow-clear
            placeholder="全部"
            style="width: 160px"
          >
            <a-select-option v-for="role in dataSource" :key="role.id" :value="role.id">
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

    <a-table row-key="id" :columns="columns" :data-source="filteredData" :loading="loading" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="(record as SysRole).status === 1 ? 'processing' : 'default'">
            {{ (record as SysRole).status === 1 ? '启用' : '停用' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-space>
            <a-button type="link" size="small" @click="openView(record as SysRole)">查看</a-button>
            <template v-if="canWrite()">
              <a-button type="link" size="small" @click="openEdit(record as SysRole)">编辑</a-button>
              <a-button
                v-if="!['ADMIN', 'USER'].includes((record as SysRole).code)"
                type="link"
                size="small"
                danger
                @click="handleDelete(record as SysRole)"
              >
                删除
              </a-button>
            </template>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑角色' : '新增角色'"
      width="720px"
      ok-text="提交"
      cancel-text="关闭"
      @ok="handleSubmit"
    >
      <a-form layout="vertical">
        <a-form-item label="角色编码" required>
          <a-input
            v-model:value="formState.code"
            :disabled="editingId !== null && ['ADMIN', 'USER'].includes(formState.code)"
          />
        </a-form-item>
        <a-form-item label="角色名称" required>
          <a-input v-model:value="formState.name" />
        </a-form-item>
        <a-form-item label="是否启用" required>
          <a-switch v-model:checked="formState.status" :checked-value="1" :un-checked-value="0" />
        </a-form-item>
        <a-form-item label="菜单权限" required>
          <a-tree
            v-model:checked-keys="formState.menuIds"
            checkable
            default-expand-all
            :tree-data="menuTreeData"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="drawerOpen" title="角色详情" width="480">
      <a-descriptions v-if="viewRecord" :column="1" bordered size="small">
        <a-descriptions-item label="角色编码">{{ displayValue(viewRecord.code) }}</a-descriptions-item>
        <a-descriptions-item label="角色名称">{{ displayValue(viewRecord.name) }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          {{ viewRecord.status === 1 ? '启用' : '停用' }}
        </a-descriptions-item>
        <a-descriptions-item label="权限标识">
          {{ viewRecord.permissions?.join('、') || '-' }}
        </a-descriptions-item>
      </a-descriptions>
      <div v-if="viewRecord" class="drawer-tree">
        <div class="drawer-tree-title">菜单权限</div>
        <a-tree
          :tree-data="viewMenuTreeData"
          :checked-keys="viewRecord.menuIds"
          checkable
          disabled
          default-expand-all
        />
      </div>
    </a-drawer>
  </div>
</template>

<style scoped>
.page {
  padding: 0 16px 16px;
  min-height: 100%;
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

.drawer-tree {
  margin-top: 16px;
}

.drawer-tree-title {
  font-weight: 600;
  margin-bottom: 8px;
}
</style>
