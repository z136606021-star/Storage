<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const route = useRoute()
const router = useRouter()
const auth = useAuth()

const activeTab = computed(() => {
  if (route.name === 'RoleManage') {
    return 'roles'
  }
  if (route.name === 'MenuManage') {
    return 'menus'
  }
  return 'users'
})

const tabs = computed(() => {
  const items: Array<{ key: string; label: string; name: string }> = [
    { key: 'users', label: '用户管理', name: 'UserManage' },
  ]
  if (auth.hasPermission('system:role:read')) {
    items.push({ key: 'roles', label: '角色管理', name: 'RoleManage' })
  }
  if (auth.hasPermission('system:menu:read')) {
    items.push({ key: 'menus', label: '菜单管理', name: 'MenuManage' })
  }
  return items
})

function onTabChange(key: string) {
  const tab = tabs.value.find((item) => item.key === key)
  if (tab) {
    router.push({ name: tab.name })
  }
}
</script>

<template>
  <div class="system-layout">
    <a-tabs :active-key="activeTab" @change="onTabChange">
      <a-tab-pane v-for="tab in tabs" :key="tab.key" :tab="tab.label" />
    </a-tabs>
    <router-view />
  </div>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.system-layout {
  min-height: 100%;
  background: @color-bg-base;
}

.system-layout :deep(.ant-tabs-nav) {
  margin: 0;
  padding: 0 @spacing-lg;
}
</style>
