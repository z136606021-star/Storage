<script setup lang="ts">
import type { MaterialLedger } from '@/types/warehouse/materialLedger'
import type { IoType } from '@/types/warehouse/materialIo'
import { displayValue } from '@/utils/format'

defineProps<{
  material: MaterialLedger
  canWrite?: boolean
}>()

const emit = defineEmits<{
  clear: []
  create: [type: IoType]
}>()
</script>

<template>
  <div class="material-io-context-bar">
    <a-space wrap :size="[8, 8]" class="context-tags">
      <span class="context-label">当前物料</span>
      <a-tag>{{ displayValue(material.category) }}</a-tag>
      <a-tag>{{ displayValue(material.genericName) }}</a-tag>
      <a-tag v-if="material.brand">{{ displayValue(material.brand) }}</a-tag>
      <a-tag color="blue">{{ displayValue(material.name) }}</a-tag>
      <a-tag>{{ displayValue(material.model) }}</a-tag>
      <a-tag>Bin {{ displayValue(material.binLocation) }}</a-tag>
      <a-tag color="processing">库存 {{ displayValue(material.stockQuantity) }}</a-tag>
    </a-space>
    <a-space :size="8" class="context-actions">
      <a-button type="link" size="small" class="clear-btn" @click="emit('clear')">
        清除物料筛选
      </a-button>
      <template v-if="canWrite">
        <a-button size="small" type="primary" @click="emit('create', 'IN')">
          新增入库
        </a-button>
        <a-button size="small" @click="emit('create', 'OUT')">
          新增出库
        </a-button>
      </template>
    </a-space>
  </div>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.material-io-context-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: @spacing-sm;
  width: 100%;
  margin-bottom: @spacing-sm;
  padding: @spacing-sm @spacing-md;
  background: @color-bg-spotlight;
  border: 1px solid @color-border-info;
  border-radius: @spacing-xs;
}

.context-label {
  color: @color-text-secondary;
  font-size: @font-size-sm;
}

.clear-btn {
  padding: 0;
  height: auto;
}

.context-actions {
  flex-shrink: 0;
}
</style>
