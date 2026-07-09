<script setup lang="ts">
import { message } from 'ant-design-vue'
import type { SafetyStockRecord } from '@/types/warehouse/safetyStock'
import { displayValue } from '@/utils/format'
import { useMenuStore } from '@/stores/menu'
import { useRouter } from 'vue-router'

defineProps<{
  record: SafetyStockRecord
  showLedgerLink?: boolean
  showMaterialIoLink?: boolean
}>()

const router = useRouter()
const menu = useMenuStore()

function viewMaterialLedger(materialLedgerId: number) {
  const target = menu.findRouteByPermission('warehouse:material-ledger:read')
  if (!target) {
    message.info('台账菜单未配置')
    return
  }
  router.push({
    path: target.path,
    query: { materialLedgerId: String(materialLedgerId) },
  })
}

function viewMaterialIo(materialLedgerId: number) {
  const target = menu.findRouteByPermission('warehouse:material-io:read')
  if (!target) {
    message.info('出入库菜单未配置')
    return
  }
  router.push({
    path: target.path,
    query: { materialLedgerId: String(materialLedgerId) },
  })
}

function formatWarningPeriod(inWarningPeriod: boolean) {
  return inWarningPeriod ? '是' : '否'
}
</script>

<template>
  <a-descriptions title="库存预警" :column="1" bordered size="small" class="detail-block">
    <a-descriptions-item label="库存总数">
      {{ displayValue(record.stockQuantity) }}
    </a-descriptions-item>
    <a-descriptions-item label="安全库存数">
      {{ displayValue(record.safetyQuantity) }}
    </a-descriptions-item>
    <a-descriptions-item label="库存预警">
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
