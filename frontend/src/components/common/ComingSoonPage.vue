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

<style scoped lang="less">
@import '@/styles/variables.less';

.coming-soon-page {
  padding: (@spacing-xl * 2) @spacing-lg;
  min-height: 100%;
  background: @color-bg-base;
}
</style>
