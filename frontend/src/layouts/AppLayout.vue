<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SideMenu from '@/components/layout/SideMenu.vue'
import TabBar from '@/components/layout/TabBar.vue'
import { useWorkbenchTabs } from '@/composables/useWorkbenchTabs'

const route = useRoute()
const workbenchTabs = useWorkbenchTabs()
const activePath = computed(() => route.path)
</script>

<template>
  <a-layout class="app-layout">
    <a-layout-sider :width="240" class="app-sider">
      <SideMenu />
    </a-layout-sider>
    <a-layout class="app-main">
      <TabBar />
      <a-layout-content class="app-content">
        <RouterView v-slot="{ Component }">
          <div
            v-for="tab in workbenchTabs.tabs.value"
            :key="tab.path"
            v-show="tab.path === activePath"
            class="tab-view"
          >
            <KeepAlive :max="1">
              <component
                :is="Component"
                v-if="tab.path === activePath"
                :key="`${tab.path}:${tab.refreshRevision}`"
              />
            </KeepAlive>
          </div>
        </RouterView>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.app-layout {
  min-height: 100vh;
}

.app-sider {
  background: @color-bg-base;
}

.app-main {
  background: @color-bg-layout;
}

.app-content {
  margin: @content-margin;
  min-height: calc(100vh - @header-height);
}

.tab-view {
  min-height: inherit;
}
</style>
