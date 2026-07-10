<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter, type RouteLocationNormalizedLoaded } from 'vue-router'
import type { MenuInfo } from 'ant-design-vue/es/menu/src/interface'
import {
  ArrowLeftOutlined,
  ArrowRightOutlined,
  CloseOutlined,
  ReloadOutlined,
  SwapOutlined,
} from '@ant-design/icons-vue'
import { useAuth } from '@/composables/useAuth'
import {
  canClearAllTabs,
  canCloseOtherTabs,
  canCloseTabsLeft,
  canCloseTabsRight,
  PERSONAL_CENTER_PERMISSION,
  resolveClearAllTargetPath,
  useWorkbenchTabs,
  type WorkbenchTab,
} from '@/composables/useWorkbenchTabs'
import { useMenuStore } from '@/stores/menu'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const menu = useMenuStore()
const workbenchTabs = useWorkbenchTabs()

const activeKey = computed(() => route.path)
const displayName = computed(() => auth.session.value?.user.displayName ?? '用户')
const personalCenterPath = computed(
  () => menu.findRouteByPermission(PERSONAL_CENTER_PERMISSION)?.path ?? null,
)

const draggingPath = ref<string | null>(null)
const dropTargetPath = ref<string | null>(null)

function handleTabChange(key: string | number) {
  const path = String(key)
  const target = workbenchTabs.tabNavigationTarget(path)
  if (target && target !== route.fullPath) {
    router.push(target)
  }
}

function handleCloseTab(path: string, event: MouseEvent) {
  event.stopPropagation()
  const nextTarget = workbenchTabs.removeTab(path)
  if (nextTarget === null) {
    return
  }
  if (route.path === path) {
    router.push(nextTarget)
  }
}

async function navigateIfNeeded(nextTarget: string | null) {
  if (nextTarget && route.fullPath !== nextTarget) {
    await router.push(nextTarget)
  }
}

async function handleRefreshTab(path: string) {
  const target = workbenchTabs.tabNavigationTarget(path)
  if (target && route.fullPath !== target) {
    await router.push(target)
  }
  workbenchTabs.requestRefresh(path)
}

async function handleCloseTabsLeft(path: string) {
  const nextPath = workbenchTabs.closeTabsLeft(path, route.path)
  await navigateIfNeeded(nextPath)
}

async function handleCloseTabsRight(path: string) {
  const nextPath = workbenchTabs.closeTabsRight(path, route.path)
  await navigateIfNeeded(nextPath)
}

async function handleCloseOtherTabs(path: string) {
  const nextPath = workbenchTabs.closeOtherTabs(path, route.path)
  await navigateIfNeeded(nextPath)
}

async function handleClearAllTabs() {
  const targetPath = resolveClearAllTargetPath(
    (permission) => menu.findRouteByPermission(permission),
    () => menu.getDefaultRoute(),
  )
  if (!targetPath) {
    return
  }
  const resolved = router.resolve(targetPath) as RouteLocationNormalizedLoaded
  workbenchTabs.replaceTabsWithRoute(resolved)
  if (route.fullPath !== resolved.fullPath) {
    await router.push(resolved.fullPath)
  }
}

async function handleContextMenuClick(path: string, info: MenuInfo) {
  const key = String(info.key)
  switch (key) {
    case 'refresh':
      await handleRefreshTab(path)
      break
    case 'close-left':
      await handleCloseTabsLeft(path)
      break
    case 'close-right':
      await handleCloseTabsRight(path)
      break
    case 'close-others':
      await handleCloseOtherTabs(path)
      break
    case 'clear-all':
      await handleClearAllTabs()
      break
    default:
      break
  }
}

function isContextMenuItemDisabled(path: string, key: string): boolean {
  const tabList = workbenchTabs.tabs.value
  switch (key) {
    case 'close-left':
      return !canCloseTabsLeft(tabList, path)
    case 'close-right':
      return !canCloseTabsRight(tabList, path)
    case 'close-others':
      return !canCloseOtherTabs(tabList, path)
    case 'clear-all':
      return !canClearAllTabs(tabList, personalCenterPath.value)
    default:
      return false
  }
}

function handleDragStart(path: string, event: DragEvent) {
  draggingPath.value = path
  dropTargetPath.value = null
  event.dataTransfer?.setData('text/plain', path)
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
  }
}

function handleDragOver(path: string, event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  dropTargetPath.value = path
}

function handleDragLeave(path: string) {
  if (dropTargetPath.value === path) {
    dropTargetPath.value = null
  }
}

function handleDrop(path: string, event: DragEvent) {
  event.preventDefault()
  event.stopPropagation()
  const sourcePath = draggingPath.value ?? event.dataTransfer?.getData('text/plain')
  if (sourcePath) {
    workbenchTabs.moveTab(sourcePath, path)
  }
  draggingPath.value = null
  dropTargetPath.value = null
}

function handleDragEnd() {
  draggingPath.value = null
  dropTargetPath.value = null
}

function tabLabelClass(tab: WorkbenchTab) {
  return {
    'tab-label': true,
    'tab-label--dragging': draggingPath.value === tab.path,
    'tab-label--drop-target': dropTargetPath.value === tab.path && draggingPath.value !== tab.path,
  }
}

async function handleLogout() {
  workbenchTabs.clearTabs()
  await auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="tab-bar">
    <a-tabs
      :active-key="activeKey"
      type="card"
      size="small"
      :tab-bar-gutter="4"
      class="tabs"
      @change="handleTabChange"
    >
      <a-tab-pane v-for="tab in workbenchTabs.tabs.value" :key="tab.path">
        <template #tab>
          <a-dropdown :trigger="['contextmenu']">
            <span
              :class="tabLabelClass(tab)"
              draggable="true"
              @dragstart="handleDragStart(tab.path, $event)"
              @dragover="handleDragOver(tab.path, $event)"
              @dragleave="handleDragLeave(tab.path)"
              @drop="handleDrop(tab.path, $event)"
              @dragend="handleDragEnd"
            >
              {{ tab.title }}
              <CloseOutlined
                v-if="tab.closable"
                class="tab-close"
                @click="handleCloseTab(tab.path, $event)"
              />
            </span>
            <template #overlay>
              <a-menu @click="(info: MenuInfo) => handleContextMenuClick(tab.path, info)">
                <a-menu-item key="refresh">
                  <ReloadOutlined />
                  刷新
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="close-left" :disabled="isContextMenuItemDisabled(tab.path, 'close-left')">
                  <ArrowLeftOutlined />
                  关闭左侧
                </a-menu-item>
                <a-menu-item key="close-right" :disabled="isContextMenuItemDisabled(tab.path, 'close-right')">
                  <ArrowRightOutlined />
                  关闭右侧
                </a-menu-item>
                <a-menu-item key="close-others" :disabled="isContextMenuItemDisabled(tab.path, 'close-others')">
                  <SwapOutlined />
                  关闭其它
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="clear-all" :disabled="isContextMenuItemDisabled(tab.path, 'clear-all')">
                  清空全部
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
      </a-tab-pane>
    </a-tabs>
    <div class="tab-bar__actions">
      <span class="tab-bar__user">{{ displayName }}</span>
      <a-button type="link" size="small" @click="handleLogout">退出登录</a-button>
    </div>
  </div>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.tab-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: @spacing-md;
  background: @color-bg-base;
  padding: @spacing-sm @spacing-md 0;
  border-bottom: 1px solid @color-border;
}

.tabs {
  flex: 1;
  min-width: 0;

  :deep(.ant-tabs-nav) {
    margin-bottom: 0;
  }
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: grab;
  user-select: none;

  &--dragging {
    opacity: 0.55;
    cursor: grabbing;
  }

  &--drop-target {
    box-shadow: inset 0 -2px 0 @color-primary;
  }
}

.tab-close {
  font-size: 10px;
  color: @color-text-tertiary;
  cursor: pointer;

  &:hover {
    color: @color-text;
  }
}

.tab-bar__actions {
  display: flex;
  align-items: center;
  gap: @spacing-xs;
  padding-top: @spacing-xs;
  white-space: nowrap;
}

.tab-bar__user {
  color: @color-text-secondary;
  font-size: @font-size-sm;
}
</style>
