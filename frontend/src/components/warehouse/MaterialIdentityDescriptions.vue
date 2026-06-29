<script setup lang="ts">
import { displayValue } from '@/utils/format'

defineProps<{
  record: {
    category?: string | null
    genericName?: string | null
    brand?: string | null
    name?: string | null
    model?: string | null
    binLocation?: string | null
  }
}>()
</script>

<template>
  <a-descriptions :column="1" bordered size="small" class="detail-block">
    <a-descriptions-item label="品类">
      {{ displayValue(record.category) }}
    </a-descriptions-item>
    <a-descriptions-item label="统称">
      {{ displayValue(record.genericName) }}
    </a-descriptions-item>
    <a-descriptions-item label="品牌">
      {{ displayValue(record.brand) }}
    </a-descriptions-item>
    <a-descriptions-item label="名称">
      {{ displayValue(record.name) }}
    </a-descriptions-item>
    <a-descriptions-item label="型号">
      {{ displayValue(record.model) }}
    </a-descriptions-item>
    <a-descriptions-item label="Bin位">
      {{ displayValue(record.binLocation) }}
    </a-descriptions-item>
  </a-descriptions>
</template>
