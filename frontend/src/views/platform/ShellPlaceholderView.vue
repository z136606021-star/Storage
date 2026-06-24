<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ComingSoonPage from '@/components/common/ComingSoonPage.vue'

const route = useRoute()

const title = computed(() => {
  const matched = route.matched
    .map((record) => record.meta.title)
    .filter((value): value is string => Boolean(value))
  return matched.at(-1) ?? '功能规划'
})
</script>

<template>
  <ComingSoonPage :title="title" />
</template>
