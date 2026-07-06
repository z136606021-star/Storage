<script setup lang="ts">
import SideMenu from '@/components/layout/SideMenu.vue'
import TabBar from '@/components/layout/TabBar.vue'
</script>

<template>
  <a-layout class="app-layout">
    <a-layout-sider :width="240" class="app-sider">
      <SideMenu />
    </a-layout-sider>
    <a-layout class="app-main">
      <TabBar />
      <a-layout-content class="app-content">
        <RouterView />
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
</style>
