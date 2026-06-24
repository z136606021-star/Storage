<script setup lang="ts">
import { h, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AppstoreOutlined } from '@ant-design/icons-vue'
import { fetchNavTree } from '@/api/menu'
import { getErrorMessage } from '@/api/http'
import { useAuth } from '@/composables/useAuth'
import type { NavMenuNode } from '@/types/system'
import { resolveIcon } from '@/utils/icons'
import { message } from 'ant-design-vue'

const DEFAULT_OPEN_KEYS = ['100', '110']

const route = useRoute()
const router = useRouter()
const auth = useAuth()

const openKeys = ref<string[]>([])
const selectedKeys = ref<string[]>([])
const menuItems = ref<Array<Record<string, unknown>>>([])
const pathByKey = ref<Record<string, string>>({})

function mapNavNodes(nodes: NavMenuNode[]): Array<Record<string, unknown>> {
  return nodes.map((node) => {
    const iconComponent = resolveIcon(node.icon)
    if (node.path) {
      pathByKey.value[node.key] = node.path
    }
    const item: Record<string, unknown> = {
      key: node.key,
      label: node.label,
      icon: iconComponent ? () => h(iconComponent) : undefined,
    }
    if (node.children?.length) {
      item.children = mapNavNodes(node.children)
    }
    return item
  })
}

function collectOpenKeys(nodes: Array<Record<string, unknown>>, path: string, parents: string[] = []): string[] {
  for (const node of nodes) {
    const key = String(node.key)
    const children = node.children as Array<Record<string, unknown>> | undefined
    if (node.path === path) {
      return [...parents, key]
    }
    if (children?.length) {
      const found = collectOpenKeys(children, path, [...parents, key])
      if (found.length) {
        return found
      }
    }
  }
  return []
}

function findSelectedKey(nodes: Array<Record<string, unknown>>, path: string): string | null {
  for (const node of nodes) {
    if (node.path === path) {
      return String(node.key)
    }
    const children = node.children as Array<Record<string, unknown>> | undefined
    if (children?.length) {
      const childKey = findSelectedKey(children, path)
      if (childKey) {
        return childKey
      }
    }
  }
  return null
}

async function loadMenu() {
  if (!auth.isAuthenticated()) {
    menuItems.value = []
    pathByKey.value = {}
    return
  }
  try {
    const { data } = await fetchNavTree()
    pathByKey.value = {}
    menuItems.value = mapNavNodes(data)
    if (openKeys.value.length === 0) {
      openKeys.value = [...DEFAULT_OPEN_KEYS]
    }
    syncSelectedKeys()
  } catch (error) {
    message.error(getErrorMessage(error, '加载菜单失败'))
  }
}

function syncSelectedKeys() {
  const selected = findSelectedKey(menuItems.value, route.path)
  selectedKeys.value = selected ? [selected] : []
  const keys = collectOpenKeys(menuItems.value, route.path)
  openKeys.value = keys.length ? keys : openKeys.value
}

watch(() => route.path, syncSelectedKeys)
watch(() => auth.session.value, loadMenu, { deep: true })

function handleMenuClick({ key }: { key: string }) {
  const path = pathByKey.value[key]
  if (path) {
    router.push(path)
  }
}

onMounted(async () => {
  await auth.initialize()
  await loadMenu()
})
</script>

<template>
  <div class="side-menu">
    <div class="logo">
      <AppstoreOutlined class="logo-icon" />
      <span class="logo-text">项目管理平台</span>
    </div>
    <a-menu
      v-model:open-keys="openKeys"
      v-model:selected-keys="selectedKeys"
      mode="inline"
      :items="menuItems"
      class="menu"
      @click="handleMenuClick"
    />
  </div>
</template>

<style scoped>
.side-menu {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #f0f0f0;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.logo-icon {
  font-size: 22px;
  color: #1677ff;
}

.logo-text {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  white-space: nowrap;
}

.menu {
  flex: 1;
  overflow: auto;
  border-inline-end: none !important;
}

.menu :deep(.ant-menu-item-selected) {
  background-color: #1677ff !important;
  color: #fff !important;
}

.menu :deep(.ant-menu-item-selected .ant-menu-title-content) {
  color: #fff !important;
}
</style>
