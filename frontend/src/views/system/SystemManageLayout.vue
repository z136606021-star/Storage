<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMenuStore } from '@/stores/menu'
import UserManageView from '@/views/system/UserManageView.vue'

const route = useRoute()
const router = useRouter()
const menu = useMenuStore()

const tabs = computed(() => {
  const parent = menu.findRouteByComponentKey('SystemManageLayout')
  if (!parent) {
    return []
  }
  return [
    { key: parent.componentKey ?? parent.key, label: parent.label, path: parent.path },
    ...menu.collectChildRoutes('SystemManageLayout').map((child) => ({
      key: child.componentKey ?? child.key,
      label: child.label,
      path: child.path,
    })),
  ]
})

const activeTab = computed(() => {
  return tabs.value.find((tab) => tab.path === route.path)?.key ?? tabs.value[0]?.key
})

const hasChildRoute = computed(() => tabs.value.some((tab) => tab.path === route.path && tab.path !== tabs.value[0]?.path))

function onTabChange(key: string) {
  const tab = tabs.value.find((item) => item.key === key)
  if (tab) {
    router.push(tab.path)
  }
}
</script>

<template>
  <div class="system-layout">
    <a-tabs :active-key="activeTab" @change="onTabChange">
      <a-tab-pane v-for="tab in tabs" :key="tab.key" :tab="tab.label" />
    </a-tabs>
    <router-view v-if="hasChildRoute" />
    <UserManageView v-else />
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
