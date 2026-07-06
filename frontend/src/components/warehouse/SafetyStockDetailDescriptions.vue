<script setup lang="ts">
import type { SafetyStockRecord } from '@/types/warehouse/safetyStock'
import { displayValue } from '@/utils/format'
import { useRouter } from 'vue-router'

defineProps<{
  record: SafetyStockRecord
  showLedgerLink?: boolean
  showMaterialIoLink?: boolean
}>()

const router = useRouter()

function viewMaterialLedger(materialLedgerId: number) {
  router.push({
    path: '/warehouse/material-ledger',
    query: { materialLedgerId: String(materialLedgerId) },
  })
}

function viewMaterialIo(materialLedgerId: number) {
  router.push({
    path: '/warehouse/material-io',
    query: { materialLedgerId: String(materialLedgerId) },
  })
}

function formatWarningPeriod(inWarningPeriod: boolean) {
  return inWarningPeriod ? '是' : '否'
}
</script>

<template>
  <a-descriptions title="库存预警" :column="1" bordered size="small" class="detail-block">
    <a-descriptions-item label="库存数量">
      {{ displayValue(record.stockQuantity) }}
    </a-descriptions-item>
    <a-descriptions-item label="安全库存数">
      {{ displayValue(record.safetyQuantity) }}
    </a-descriptions-item>
    <a-descriptions-item label="预警开关">
      {{ record.warningEnabled ? '开' : '关' }}
    </a-descriptions-item>
    <a-descriptions-item label="预警期">
      <a-tag :color="record.inWarningPeriod ? 'warning' : 'default'">
        {{ formatWarningPeriod(record.inWarningPeriod) }}
      </a-tag>
    </a-descriptions-item>
    <a-descriptions-item v-if="showLedgerLink" label="台账">
      <a-button
        type="link"
        class="nav-link"
        @click="viewMaterialLedger(record.materialLedgerId)"
      >
        查看台账
      </a-button>
    </a-descriptions-item>
    <a-descriptions-item v-if="showMaterialIoLink" label="出入库">
      <a-button
        type="link"
        class="nav-link"
        @click="viewMaterialIo(record.materialLedgerId)"
      >
        查看出入库流水
      </a-button>
    </a-descriptions-item>
  </a-descriptions>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.nav-link {
  padding: 0;
  height: auto;
}

.detail-block {
  margin-top: @spacing-md;
}
</style>
