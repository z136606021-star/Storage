<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CloseOutlined } from '@ant-design/icons-vue'
import { useAuth } from '@/composables/useAuth'
import { useWorkbenchTabs } from '@/composables/useWorkbenchTabs'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const workbenchTabs = useWorkbenchTabs()

const activeKey = computed(() => route.path)

const displayName = computed(() => auth.session.value?.user.displayName ?? '用户')

function handleTabChange(key: string | number) {
  const path = String(key)
  if (path !== route.path) {
    router.push(path)
  }
}

function handleCloseTab(path: string, event: MouseEvent) {
  event.stopPropagation()
  const nextPath = workbenchTabs.removeTab(path)
  if (nextPath === null) {
    return
  }
  if (route.path === path) {
    router.push(nextPath)
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
          <span class="tab-label">
            {{ tab.title }}
            <CloseOutlined
              v-if="tab.closable"
              class="tab-close"
              @click="handleCloseTab(tab.path, $event)"
            />
          </span>
        </template>
      </a-tab-pane>
    </a-tabs>
    <div class="tab-bar__actions">
      <span class="tab-bar__user">{{ displayName }}</span>
      <a-button type="link" size="small" @click="handleLogout">退出登录</a-button>
    </div>
  </div>
</template>

<style scoped>
.tab-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  background: #fff;
  padding: 8px 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.tabs {
  flex: 1;
  min-width: 0;
}

.tabs :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-close {
  font-size: 10px;
  color: rgba(0, 0, 0, 0.45);
}

.tab-close:hover {
  color: rgba(0, 0, 0, 0.88);
}

.tab-bar__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-top: 4px;
  white-space: nowrap;
}

.tab-bar__user {
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
}
</style>
