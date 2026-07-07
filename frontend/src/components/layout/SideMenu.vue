<script setup lang="ts">
import { h, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AppstoreOutlined } from '@ant-design/icons-vue'
import { getErrorMessage } from '@/api/http'
import { useAuth } from '@/composables/useAuth'
import { useMenuStore } from '@/stores/menu'
import type { NavMenuNode } from '@/types/system'
import { resolveIcon } from '@/utils/icons'
import { message } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const menu = useMenuStore()

const openKeys = ref<string[]>([])
const selectedKeys = ref<string[]>([])
const menuItems = ref<Array<Record<string, unknown>>>([])
const pathByKey = ref<Record<string, string>>({})

function isSidebarVisible(node: NavMenuNode) {
  return node.visible === undefined || node.visible === 1
}

function mapNavNodes(nodes: NavMenuNode[], parentPath?: string): Array<Record<string, unknown>> {
  return nodes
    .filter(isSidebarVisible)
    .map((node) => {
      const iconComponent = resolveIcon(node.icon)
      const fullPath = node.path ? resolveSidebarPath(node.path, parentPath) : undefined
      if (node.path) {
        pathByKey.value[node.key] = fullPath!
      }
      const item: Record<string, unknown> = {
        key: node.key,
        label: node.label,
        path: fullPath,
        componentKey: node.componentKey,
        icon: iconComponent ? () => h(iconComponent) : undefined,
      }
      if (node.children?.length) {
        const children = mapNavNodes(node.children, fullPath ?? parentPath)
        if (children.length) {
          item.children = children
        }
      }
      return item
    })
}

function resolveSidebarPath(path: string, parentPath?: string) {
  if (path.startsWith('/')) {
    return path
  }
  if (!parentPath) {
    return `/${path}`
  }
  return `${parentPath.replace(/\/$/, '')}/${path.replace(/^\//, '')}`
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
    await menu.ensureDynamicRoutes(router)
    pathByKey.value = {}
    menuItems.value = mapNavNodes(menu.navTree)
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
  } else {
    message.info('页面未配置')
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

<style scoped lang="less">
@import '@/styles/mixins.less';

.side-menu {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: @color-bg-base;
  border-right: 1px solid @color-border;
}

.logo {
  height: @header-height;
  display: flex;
  align-items: center;
  padding: 0 @spacing-lg;
  gap: 10px;
  border-bottom: 1px solid @color-border;
}

.logo-icon {
  font-size: 22px;
  color: @color-primary;
}

.logo-text {
  font-size: @font-size-lg;
  font-weight: 600;
  color: @color-text;
  white-space: nowrap;
}

.menu {
  flex: 1;
  overflow: auto;
  border-inline-end: none !important;

  :deep(.ant-menu-item-selected) {
    background-color: @color-primary !important;
    color: @color-bg-base !important;
  }

  :deep(.ant-menu-item-selected .ant-menu-title-content) {
    color: @color-bg-base !important;
  }
}
</style>
