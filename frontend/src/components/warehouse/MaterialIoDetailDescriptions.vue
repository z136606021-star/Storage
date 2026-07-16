<script setup lang="ts">
import type { MaterialIoRecord } from '@/types/warehouse/materialIo'
import { formatPurposeLabel } from '@/constants/materialIoPurpose'
import { displayValue, formatDateTime, formatUnitPrice } from '@/utils/format'
import { formatIoTypeLabel, formatOperator, getIoTypeTagColor } from '@/utils/materialIo'
import { buildMaterialIoShareUrl } from '@/utils/materialIoShareUrl'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'

defineProps<{
  record: MaterialIoRecord
  showLedgerLink?: boolean
  showCopyLink?: boolean
}>()

defineEmits<{
  viewLedger: []
}>()

const route = useRoute()
const router = useRouter()

async function handleCopyLink(record: MaterialIoRecord) {
  const url = buildMaterialIoShareUrl(router, route.path, record.id, {
    materialLedgerId: route.query.materialLedgerId as string | undefined,
  })
  try {
    await navigator.clipboard.writeText(url)
    message.success('链接已复制')
  } catch {
    message.error('复制失败，请手动复制地址栏链接')
  }
}
</script>

<template>
  <a-descriptions title="出入库信息" :column="1" bordered size="small" class="detail-block">
    <a-descriptions-item label="操作类型">
      <a-tag :color="getIoTypeTagColor(record.ioType)">
        {{ formatIoTypeLabel(record.ioType) }}
      </a-tag>
    </a-descriptions-item>
    <a-descriptions-item v-if="record.purpose" label="用途">
      {{ formatPurposeLabel(record.purpose, record.purposeLabel) }}
    </a-descriptions-item>
    <a-descriptions-item label="数量">
      {{ displayValue(record.quantity) }}
    </a-descriptions-item>
    <a-descriptions-item label="单价">
      {{ formatUnitPrice(record.unitPrice) || '—' }}
    </a-descriptions-item>
    <a-descriptions-item v-if="record.projectRef" label="项目编号">
      {{ displayValue(record.projectRef) }}
    </a-descriptions-item>
    <a-descriptions-item label="当前库存">
      {{ displayValue(record.stockQuantity) }}
    </a-descriptions-item>
    <a-descriptions-item label="备注">
      {{ displayValue(record.remark) }}
    </a-descriptions-item>
    <a-descriptions-item label="操作人">
      {{ formatOperator(record) }}
    </a-descriptions-item>
    <a-descriptions-item label="时间">
      {{ formatDateTime(record.operatedAt) }}
    </a-descriptions-item>
    <a-descriptions-item v-if="showCopyLink" label="分享">
      <a-button type="link" class="ledger-link" @click="handleCopyLink(record)">
        复制链接
      </a-button>
    </a-descriptions-item>
    <a-descriptions-item v-if="showLedgerLink && record.materialLedgerId" label="台账">
      <a-button type="link" class="ledger-link" @click="$emit('viewLedger')">
        查看台账
      </a-button>
    </a-descriptions-item>
  </a-descriptions>

  <a-descriptions title="系统信息" :column="1" bordered size="small" class="detail-block">
    <a-descriptions-item label="创建时间">
      {{ formatDateTime(record.createdAt) }}
    </a-descriptions-item>
    <a-descriptions-item label="更新时间">
      {{ formatDateTime(record.updatedAt) }}
    </a-descriptions-item>
  </a-descriptions>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.ledger-link {
  padding: 0;
  height: auto;
}

.detail-block + .detail-block {
  margin-top: @spacing-lg;
}
</style>
