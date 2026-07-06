<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { getErrorMessage } from '@/api/http'
import { createMenu, deleteMenu, fetchMenuTree, updateMenu } from '@/api/system/menu'
import { useAuth } from '@/composables/useAuth'
import type { SysMenu, SysMenuSave } from '@/types/system'
import { displayValue } from '@/utils/format'

const auth = useAuth()
const canWrite = () => auth.hasPermission('system:menu:write')

const loading = ref(false)
const menuTree = ref<SysMenu[]>([])
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const selectedMenuId = ref<number | null>(null)

const formState = reactive<SysMenuSave>({
  parentId: null,
  menuType: 'MENU',
  name: '',
  permission: '',
  path: '',
  componentKey: '',
  icon: '',
  visible: 1,
  sortOrder: 0,
})

const columns = [
  { title: '菜单名称', dataIndex: 'name', key: 'name', width: 200 },
  { title: '类型', key: 'menuType', width: 90 },
  { title: '权限标识', dataIndex: 'permission', key: 'permission', ellipsis: true },
  { title: '路由', dataIndex: 'path', key: 'path', ellipsis: true, width: 180 },
  { title: '组件 Key', dataIndex: 'componentKey', key: 'componentKey', ellipsis: true, width: 160 },
  { title: '显示', key: 'visible', width: 70 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 70 },
  { title: '操作', key: 'actions', width: 200 },
]

const parentTreeData = computed(() => {
  const excludeIds = editingId.value ? collectDescendantIds(menuTree.value, editingId.value) : new Set<number>()
  if (editingId.value) {
    excludeIds.add(editingId.value)
  }
  return [{ title: '（根节点）', value: null, children: buildParentOptions(menuTree.value, excludeIds) }]
})

function collectDescendantIds(menus: SysMenu[], rootId: number): Set<number> {
  const ids = new Set<number>()
  const collectChildren = (node: SysMenu) => {
    ids.add(node.id)
    for (const child of node.children ?? []) {
      collectChildren(child)
    }
  }
  const findAndCollect = (nodes: SysMenu[]): boolean => {
    for (const node of nodes) {
      if (node.id === rootId) {
        collectChildren(node)
        return true
      }
      if (node.children?.length && findAndCollect(node.children)) {
        return true
      }
    }
    return false
  }
  findAndCollect(menus)
  return ids
}

function buildParentOptions(
  menus: SysMenu[],
  excludeIds: Set<number>,
): Array<{ title: string; value: number; children?: ReturnType<typeof buildParentOptions> }> {
  return menus
    .filter((menu) => !excludeIds.has(menu.id))
    .map((menu) => ({
      title: menu.name,
      value: menu.id,
      children: menu.children?.length ? buildParentOptions(menu.children, excludeIds) : undefined,
    }))
}

async function loadData() {
  loading.value = true
  try {
    const { data } = await fetchMenuTree()
    menuTree.value = data
  } catch (error) {
    message.error(getErrorMessage(error, '加载菜单树失败'))
  } finally {
    loading.value = false
  }
}

function resetForm(parentId: number | null = null) {
  formState.parentId = parentId
  formState.menuType = 'MENU'
  formState.name = ''
  formState.permission = ''
  formState.path = ''
  formState.componentKey = ''
  formState.icon = ''
  formState.visible = 1
  formState.sortOrder = 0
  editingId.value = null
}

function openCreate(parentId: number | null = null) {
  resetForm(parentId)
  modalOpen.value = true
}

function openEdit(record: SysMenu) {
  editingId.value = record.id
  formState.parentId = record.parentId
  formState.menuType = record.menuType
  formState.name = record.name
  formState.permission = record.permission ?? ''
  formState.path = record.path ?? ''
  formState.componentKey = record.componentKey ?? ''
  formState.icon = record.icon ?? ''
  formState.visible = record.visible
  formState.sortOrder = record.sortOrder
  modalOpen.value = true
}

function openCreateChild(record: SysMenu) {
  openCreate(record.id)
}

async function handleSubmit() {
  if (!formState.name.trim()) {
    message.warning('请输入菜单名称')
    return
  }
  if (formState.menuType === 'MENU' && !formState.permission?.trim()) {
    message.warning('菜单类型为 MENU 时必须填写权限标识')
    return
  }
  if (
    formState.menuType === 'MENU'
    && formState.visible === 1
    && formState.path?.trim()
    && !formState.componentKey?.trim()
  ) {
    message.warning('可见路由菜单必须填写组件 Key')
    return
  }
  const payload: SysMenuSave = {
    parentId: formState.parentId,
    menuType: formState.menuType,
    name: formState.name.trim(),
    permission: formState.permission?.trim() || undefined,
    path: formState.path?.trim() || undefined,
    componentKey: formState.componentKey?.trim() || undefined,
    icon: formState.icon?.trim() || undefined,
    visible: formState.visible,
    sortOrder: formState.sortOrder,
  }
  try {
    if (editingId.value) {
      await updateMenu(editingId.value, payload)
      message.success('菜单已更新')
    } else {
      await createMenu(payload)
      message.success('菜单已创建')
    }
    modalOpen.value = false
    await loadData()
  } catch (error) {
    message.error(getErrorMessage(error, '保存失败'))
  }
}

function handleDelete(record: SysMenu) {
  Modal.confirm({
    title: '删除菜单',
    content: `确定删除菜单「${record.name}」吗？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await deleteMenu(record.id)
        message.success('菜单已删除')
        if (selectedMenuId.value === record.id) {
          selectedMenuId.value = null
        }
        await loadData()
      } catch (error) {
        message.error(getErrorMessage(error, '删除失败'))
      }
    },
  })
}

function customRow(record: SysMenu) {
  return {
    onClick: () => {
      selectedMenuId.value = record.id
    },
    class: selectedMenuId.value === record.id ? 'row-selected' : '',
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <div class="action-bar">
      <a-space>
        <a-button v-if="canWrite()" type="primary" class="btn-add" @click="openCreate()">
          <PlusOutlined />新增根菜单
        </a-button>
        <a-button
          v-if="canWrite() && selectedMenuId"
          type="primary"
          @click="openCreate(selectedMenuId)"
        >
          <PlusOutlined />新增子菜单
        </a-button>
        <a-button @click="loadData"><ReloadOutlined />刷新</a-button>
      </a-space>
    </div>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="menuTree"
      :loading="loading"
      :pagination="false"
      :custom-row="customRow"
      default-expand-all-rows
      :scroll="{ x: 1100 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'menuType'">
          {{ (record as SysMenu).menuType === 'CATALOG' ? '目录' : '菜单' }}
        </template>
        <template v-else-if="column.key === 'permission'">
          {{ displayValue((record as SysMenu).permission) }}
        </template>
        <template v-else-if="column.key === 'path'">
          {{ displayValue((record as SysMenu).path) }}
        </template>
        <template v-else-if="column.key === 'componentKey'">
          {{ displayValue((record as SysMenu).componentKey) }}
        </template>
        <template v-else-if="column.key === 'visible'">
          {{ (record as SysMenu).visible === 1 ? '是' : '否' }}
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-space>
            <a-button type="link" size="small" @click.stop="openCreateChild(record as SysMenu)">
              子菜单
            </a-button>
            <template v-if="canWrite()">
              <a-button type="link" size="small" @click.stop="openEdit(record as SysMenu)">编辑</a-button>
              <a-button type="link" size="small" danger @click.stop="handleDelete(record as SysMenu)">
                删除
              </a-button>
            </template>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑菜单' : '新增菜单'"
      width="640px"
      ok-text="提交"
      cancel-text="关闭"
      @ok="handleSubmit"
    >
      <a-form layout="vertical">
        <a-form-item label="父菜单">
          <a-tree-select
            v-model:value="formState.parentId"
            :tree-data="parentTreeData"
            allow-clear
            placeholder="留空为根节点"
            tree-default-expand-all
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="菜单类型" required>
          <a-radio-group v-model:value="formState.menuType">
            <a-radio value="CATALOG">目录（CATALOG）</a-radio>
            <a-radio value="MENU">菜单（MENU）</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="菜单名称" required>
          <a-input v-model:value="formState.name" placeholder="显示名称" />
        </a-form-item>
        <a-form-item v-if="formState.menuType === 'MENU'" label="权限标识" required>
          <a-input v-model:value="formState.permission" placeholder="如 warehouse:material-ledger:read" />
        </a-form-item>
        <a-form-item label="路由路径">
          <a-input v-model:value="formState.path" placeholder="如 /warehouse/material-ledger，动作权限可留空" />
        </a-form-item>
        <a-form-item
          v-if="formState.menuType === 'MENU' && formState.path?.trim()"
          label="组件 Key"
          :required="formState.visible === 1"
        >
          <a-input v-model:value="formState.componentKey" placeholder="如 MaterialLedger 或 ShellPlaceholder" />
        </a-form-item>
        <a-form-item label="图标">
          <a-input v-model:value="formState.icon" placeholder="Ant Design 图标名，如 SettingOutlined" />
        </a-form-item>
        <a-form-item label="侧栏显示" required>
          <a-switch v-model:checked="formState.visible" :checked-value="1" :un-checked-value="0" />
        </a-form-item>
        <a-form-item label="排序" required>
          <a-input-number v-model:value="formState.sortOrder" :min="0" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.page {
  padding: 0 16px 16px;
  min-height: 100%;
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
