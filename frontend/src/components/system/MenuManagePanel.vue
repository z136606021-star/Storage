<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { getErrorMessage } from '@/api/http'
import { createMenu, deleteMenu, fetchMenuTree, updateMenu } from '@/api/system/menu'
import { useAuth } from '@/composables/useAuth'
import type { SysMenu, SysMenuSave } from '@/types/system'
import {
  allowedParentTypesFor,
  buildParentOptions,
  collectDescendantIds,
  isButtonMenu,
  isTopMenu,
  menuTypeLabel,
} from '@/utils/menuAuthTree'
import { displayValue } from '@/utils/format'

const auth = useAuth()
const canWrite = () => auth.hasPermission('system:menu:write')

const loading = ref(false)
const menuTree = ref<SysMenu[]>([])
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const selectedMenuId = ref<number | null>(null)
const createMode = ref<'top' | 'sub' | 'button'>('top')

const formState = reactive<SysMenuSave>({
  parentId: null,
  menuType: 'TOP',
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
  { title: '类型', key: 'menuType', width: 110 },
  { title: '权限标识', dataIndex: 'permission', key: 'permission', ellipsis: true },
  { title: '路由', dataIndex: 'path', key: 'path', ellipsis: true, width: 180 },
  { title: '组件路径', dataIndex: 'componentKey', key: 'componentKey', ellipsis: true, width: 220 },
  { title: '显示', key: 'visible', width: 70 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 70 },
  { title: '操作', key: 'actions', width: 240 },
]

const showParentSelector = computed(() => formState.menuType !== 'TOP' && formState.menuType !== 'CATALOG')

const isGroupSubMode = computed(() => {
  return formState.menuType === 'SUB' && !formState.permission?.trim()
})

const isPageSubMode = computed(() => {
  return formState.menuType === 'SUB' && Boolean(formState.permission?.trim())
})

const parentTreeData = computed(() => {
  const excludeIds = editingId.value ? collectDescendantIds(menuTree.value, editingId.value) : new Set<number>()
  if (editingId.value) {
    excludeIds.add(editingId.value)
  }
  return buildParentOptions(menuTree.value, excludeIds, allowedParentTypesFor(formState.menuType))
})

watch(
  () => formState.menuType,
  (menuType) => {
    if (menuType === 'TOP' || menuType === 'CATALOG') {
      formState.parentId = null
    }
    if (menuType === 'BUTTON') {
      formState.path = ''
      formState.componentKey = ''
      formState.visible = 0
    }
  },
)

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

function resetForm(parentId: number | null = null, menuType: SysMenuSave['menuType'] = 'TOP') {
  formState.parentId = parentId
  formState.menuType = menuType
  formState.name = ''
  formState.permission = ''
  formState.path = ''
  formState.componentKey = ''
  formState.icon = ''
  formState.visible = menuType === 'BUTTON' ? 0 : 1
  formState.sortOrder = 0
  editingId.value = null
}

function openCreateTop() {
  createMode.value = 'top'
  resetForm(null, 'TOP')
  modalOpen.value = true
}

function openCreate(parentId: number | null, menuType: SysMenuSave['menuType']) {
  createMode.value = menuType === 'BUTTON' ? 'button' : menuType === 'TOP' ? 'top' : 'sub'
  resetForm(parentId, menuType)
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

function openCreateChild(record: SysMenu, menuType: SysMenuSave['menuType']) {
  openCreate(record.id, menuType)
}

async function handleSubmit() {
  if (!formState.name.trim()) {
    message.warning('请输入菜单名称')
    return
  }
  if (isButtonMenu(formState.menuType) && !formState.permission?.trim()) {
    message.warning('按钮权限必须填写权限标识')
    return
  }
  if (isPageSubMode.value && !formState.permission?.trim()) {
    message.warning('页面子菜单必须填写权限标识')
    return
  }
  if (isGroupSubMode.value) {
    if (formState.path?.trim() || formState.componentKey?.trim()) {
      message.warning('分组子菜单不能填写路由或组件路径')
      return
    }
  }
  if (
    !isButtonMenu(formState.menuType)
    && !isGroupSubMode.value
    && formState.visible === 1
    && formState.path?.trim()
    && !formState.componentKey?.trim()
  ) {
    message.warning('可见路由菜单必须填写组件路径')
    return
  }
  if (!isTopMenu(formState.menuType) && formState.parentId == null) {
    message.warning('请选择父级菜单')
    return
  }
  const payload: SysMenuSave = {
    parentId: isTopMenu(formState.menuType) ? null : formState.parentId,
    menuType: formState.menuType,
    name: formState.name.trim(),
    permission: isGroupSubMode.value ? undefined : formState.permission?.trim() || undefined,
    path: isButtonMenu(formState.menuType) || isGroupSubMode.value ? undefined : formState.path?.trim() || undefined,
    componentKey: isButtonMenu(formState.menuType) || isGroupSubMode.value ? undefined : formState.componentKey?.trim() || undefined,
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
        <a-button v-if="canWrite()" type="primary" class="btn-add" @click="openCreateTop()">
          <PlusOutlined />新增一级菜单
        </a-button>
        <a-button
          v-if="canWrite() && selectedMenuId"
          type="primary"
          @click="openCreate(selectedMenuId, 'SUB')"
        >
          <PlusOutlined />新增子菜单
        </a-button>
        <a-button
          v-if="canWrite() && selectedMenuId"
          @click="openCreate(selectedMenuId, 'BUTTON')"
        >
          <PlusOutlined />新增按钮权限
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
          {{ menuTypeLabel((record as SysMenu).menuType) }}
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
            <a-button
              v-if="isTopMenu((record as SysMenu).menuType)"
              type="link"
              size="small"
              @click.stop="openCreateChild(record as SysMenu, 'SUB')"
            >
              子菜单
            </a-button>
            <a-button
              v-else-if="!isButtonMenu((record as SysMenu).menuType)"
              type="link"
              size="small"
              @click.stop="openCreateChild(record as SysMenu, 'BUTTON')"
            >
              按钮
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
        <a-form-item label="菜单类型" required>
          <a-radio-group v-model:value="formState.menuType" :disabled="Boolean(editingId)">
            <a-radio value="TOP">一级菜单</a-radio>
            <a-radio value="SUB">子菜单</a-radio>
            <a-radio value="BUTTON">按钮权限</a-radio>
          </a-radio-group>
          <div v-if="formState.menuType === 'SUB'" class="form-hint">
            无权限标识的子菜单仅作分组，需在其下挂载具体页面子菜单
          </div>
        </a-form-item>
        <a-form-item v-if="showParentSelector" label="父菜单" required>
          <a-tree-select
            v-model:value="formState.parentId"
            :tree-data="parentTreeData"
            placeholder="请选择父级菜单"
            tree-default-expand-all
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="菜单名称" required>
          <a-input v-model:value="formState.name" placeholder="显示名称" />
        </a-form-item>
        <a-form-item
          v-if="!isTopMenu(formState.menuType) && !isButtonMenu(formState.menuType)"
          label="权限标识"
          :required="isPageSubMode"
        >
          <a-input
            v-model:value="formState.permission"
            :placeholder="isGroupSubMode ? '留空表示仅作分组' : '填写权限标识'"
          />
        </a-form-item>
        <a-form-item v-if="!isButtonMenu(formState.menuType) && !isGroupSubMode" label="路由路径">
          <a-input v-model:value="formState.path" placeholder="从菜单管理维护前端路由" />
        </a-form-item>
        <a-form-item
          v-if="!isButtonMenu(formState.menuType) && !isGroupSubMode && formState.path?.trim()"
          label="组件路径"
          :required="formState.visible === 1"
        >
          <a-input
            v-model:value="formState.componentKey"
            placeholder="如 views/warehouse/MaterialLedgerView.vue"
          />
        </a-form-item>
        <a-form-item v-if="!isButtonMenu(formState.menuType)" label="图标">
          <a-input v-model:value="formState.icon" placeholder="Ant Design 图标名，如 SettingOutlined" />
        </a-form-item>
        <a-form-item v-if="!isButtonMenu(formState.menuType)" label="侧栏显示" required>
          <a-switch v-model:checked="formState.visible" :checked-value="1" :un-checked-value="0" />
        </a-form-item>
        <a-form-item label="排序" required>
          <a-input-number v-model:value="formState.sortOrder" :min="0" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';
@import '@/styles/mixins.less';

.page {
  padding: 0 @spacing-lg @spacing-lg;
  min-height: 100%;
}

.action-bar {
  margin-bottom: @spacing-md;
}

.btn-add {
  .btn-success-primary();
}

.form-hint {
  margin-top: @spacing-xs;
  color: @color-text-secondary;
  font-size: @font-size-sm;
}

:deep(.row-selected) {
  .row-highlight();
}
</style>
