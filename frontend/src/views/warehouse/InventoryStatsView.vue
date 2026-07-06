<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { fetchWarehouseStatsOverview } from '@/api/warehouse/warehouseStats'
import { getErrorMessage } from '@/api/http'
import type { WarehouseStatsOverview } from '@/types/warehouse/warehouseStats'
import type { SafetyStockRecord } from '@/types/warehouse/safetyStock'
import { displayValue } from '@/utils/format'
import { materialIdentityColumns } from '@/utils/warehouseMaterialTable'

const RECENT_DAYS_OPTIONS = [7, 14, 30, 90] as const

const router = useRouter()
const loading = ref(false)
const recentDays = ref<number>(7)
const stats = ref<WarehouseStatsOverview | null>(null)

const warningColumns = [
  { title: '序号', key: 'index', width: 64, align: 'center' as const },
  ...materialIdentityColumns('statsWarning'),
  { title: '库存数量', dataIndex: 'stockQuantity', key: 'stockQuantity', width: 90, align: 'center' as const },
  { title: '安全库存数', dataIndex: 'safetyQuantity', key: 'safetyQuantity', width: 100, align: 'center' as const },
]

async function loadStats() {
  loading.value = true
  try {
    const { data } = await fetchWarehouseStatsOverview(recentDays.value)
    stats.value = data
  } catch (error) {
    message.error(getErrorMessage(error, '加载库存统计失败'))
  } finally {
    loading.value = false
  }
}

function goSafetyStock() {
  router.push('/warehouse/safety-stock')
}

function goMaterialIo() {
  router.push('/warehouse/material-io')
}

function goLedger(materialLedgerId: number) {
  router.push({ path: '/warehouse/material-ledger', query: { materialLedgerId: String(materialLedgerId) } })
}

onMounted(loadStats)

watch(recentDays, () => {
  loadStats()
})
</script>

<template>
  <div class="inventory-stats-page">
    <div class="inventory-stats-toolbar">
      <span class="inventory-stats-toolbar__label">统计天数</span>
      <a-select
        v-model:value="recentDays"
        :options="RECENT_DAYS_OPTIONS.map((days) => ({ value: days, label: `近 ${days} 日` }))"
        style="width: 120px"
      />
    </div>
    <a-spin :spinning="loading">
      <a-row v-if="stats" :gutter="[16, 16]" class="stat-cards">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card :bordered="false" class="stat-card">
            <a-statistic title="物料台账数" :value="stats.totalLedgerCount" />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card :bordered="false" class="stat-card">
            <a-statistic title="库存总量" :value="stats.totalStockQuantity" />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card :bordered="false" class="stat-card stat-card-warning">
            <a-statistic title="预警物料数" :value="stats.warningMaterialCount" />
            <a-button type="link" class="stat-link" @click="goSafetyStock">查看安全库存</a-button>
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card :bordered="false" class="stat-card">
            <a-statistic :title="`近 ${stats.recentDays} 日出入库笔数`" :value="stats.inboundRecordCount + stats.outboundRecordCount" />
            <a-button type="link" class="stat-link" @click="goMaterialIo">查看出入库</a-button>
          </a-card>
        </a-col>
      </a-row>

      <a-row v-if="stats" :gutter="16" class="io-summary-row">
        <a-col :xs="24" :md="12">
          <a-card :bordered="false" title="近 N 日入库">
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="统计天数">{{ stats.recentDays }} 天</a-descriptions-item>
              <a-descriptions-item label="入库笔数">{{ displayValue(stats.inboundRecordCount) }}</a-descriptions-item>
              <a-descriptions-item label="入库数量">{{ displayValue(stats.inboundQuantitySum) }}</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-card :bordered="false" title="近 N 日出库">
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="统计天数">{{ stats.recentDays }} 天</a-descriptions-item>
              <a-descriptions-item label="出库笔数">{{ displayValue(stats.outboundRecordCount) }}</a-descriptions-item>
              <a-descriptions-item label="出库数量">{{ displayValue(stats.outboundQuantitySum) }}</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>
      </a-row>

      <a-card v-if="stats" :bordered="false" title="预警物料预览" class="warning-table-card">
        <a-table
          :columns="warningColumns"
          :data-source="stats.warningMaterials"
          :pagination="false"
          :row-key="(record: SafetyStockRecord) => record.materialLedgerId"
          size="small"
          bordered
          :custom-row="(record: SafetyStockRecord) => ({
            class: 'warning-row',
            onClick: () => goLedger(record.materialLedgerId),
          })"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'index'">
              {{ index + 1 }}
            </template>
            <template v-else-if="column.key === 'brand'">
              {{ record.brand ?? '' }}
            </template>
          </template>
        </a-table>
      </a-card>
    </a-spin>
  </div>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.inventory-stats-page {
  display: flex;
  flex-direction: column;
  gap: @spacing-lg;
}

.inventory-stats-toolbar {
  display: flex;
  align-items: center;
  gap: @spacing-sm;
}

.inventory-stats-toolbar__label {
  color: @color-text-secondary;
}

.stat-card {
  height: 100%;
}

.stat-card-warning :deep(.ant-statistic-content-value) {
  color: @color-warning;
}

.stat-link {
  padding: 0;
  height: auto;
  margin-top: @spacing-xs;
}

.io-summary-row {
  margin-top: 0;
}

.warning-table-card {
  margin-top: 0;
}

:deep(.warning-row) {
  cursor: pointer;
}

:deep(.warning-row td) {
  background: @color-bg-warning;
}
</style>
