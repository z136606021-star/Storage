<script setup lang="ts">
defineProps<{
  title: string
  subTitle?: string
}>()
</script>

<template>
  <div class="coming-soon-page">
    <a-result
      status="info"
      :title="title"
      :sub-title="subTitle ?? '功能规划中，敬请期待。'"
    />
  </div>
</template>

<style scoped>
.coming-soon-page {
  padding: 48px 16px;
  min-height: 100%;
  background: #fff;
}
</style>
