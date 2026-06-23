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

<style scoped>
.app-layout {
  min-height: 100vh;
}

.app-sider {
  background: #fff;
}

.app-main {
  background: #f0f2f5;
}

.app-content {
  margin: 12px;
  min-height: calc(100vh - 56px);
}
</style>
