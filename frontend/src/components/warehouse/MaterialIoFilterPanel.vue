<script setup lang="ts">
import { computed, watch } from 'vue'
import type { Dayjs } from 'dayjs'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import WarehouseMaterialFilterPanel from '@/components/warehouse/WarehouseMaterialFilterPanel.vue'
import type { defaultIoQuery } from '@/composables/useMaterialIoList'
import { ALL_OPTION } from '@/constants/filter'
import {
  inboundPurposeOptions,
  MATERIAL_IO_PURPOSE_OPTIONS,
  outboundPurposeOptions,
} from '@/constants/materialIoPurpose'

type IoQueryForm = ReturnType<typeof defaultIoQuery>

const props = defineProps<{
  queryForm: IoQueryForm
  filterOptions: {
    category: string[]
    genericName: string[]
    brand: string[]
    model: string[]
    binLocation: string[]
  }
}>()

const operatedAtRange = defineModel<[Dayjs, Dayjs] | null>('operatedAtRange', { required: true })

const emit = defineEmits<{
  categoryChange: []
  genericNameChange: []
  brandChange: []
  search: []
  reset: []
}>()

const IO_TYPE_OPTIONS = [
  { label: '全部', value: ALL_OPTION },
  { label: '入库', value: 'IN' },
  { label: '出库', value: 'OUT' },
]

const purposeOptions = computed(() => {
  const allOption = { label: '全部', value: ALL_OPTION }
  if (props.queryForm.ioType === 'OUT') {
    return [allOption, ...outboundPurposeOptions().map((item) => ({ label: item.label, value: item.value }))]
  }
  if (props.queryForm.ioType === 'IN') {
    return [allOption, ...inboundPurposeOptions().map((item) => ({ label: item.label, value: item.value }))]
  }
  return [
    allOption,
    ...MATERIAL_IO_PURPOSE_OPTIONS.map((item) => ({ label: item.label, value: item.value })),
  ]
})

watch(
  () => props.queryForm.ioType,
  () => {
    const valid = purposeOptions.value.some((item) => item.value === props.queryForm.purpose)
    if (!valid) {
      props.queryForm.purpose = ALL_OPTION
    }
  },
)
</script>

<template>
  <WarehouseMaterialFilterPanel
    :query-form="queryForm"
    :filter-options="filterOptions"
    variant="split"
    @category-change="emit('categoryChange')"
    @generic-name-change="emit('genericNameChange')"
    @brand-change="emit('brandChange')"
    @search="emit('search')"
  >
    <template #first-row-trailing>
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-form-item label="操作类型" class="filter-item">
          <a-select
            v-model:value="queryForm.ioType"
            :options="IO_TYPE_OPTIONS"
            class="filter-control"
          />
        </a-form-item>
      </a-col>
    </template>

    <template #second-row-trailing>
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-form-item label="用途" class="filter-item">
          <a-select
            v-model:value="queryForm.purpose"
            :options="purposeOptions"
            class="filter-control"
          />
        </a-form-item>
      </a-col>
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-form-item label="项目编号" class="filter-item">
          <a-input
            v-model:value="queryForm.projectRef"
            placeholder="关键字查找"
            allow-clear
            class="filter-control"
            @press-enter="emit('search')"
          />
        </a-form-item>
      </a-col>
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-form-item label="操作时间" class="filter-item">
          <a-range-picker v-model:value="operatedAtRange" class="filter-control" />
        </a-form-item>
      </a-col>
    </template>

    <template #actions>
      <a-form-item class="filter-item filter-actions">
        <a-space>
          <a-button type="primary" @click="emit('search')">
            <template #icon><SearchOutlined /></template>
            查询
          </a-button>
          <a-button @click="emit('reset')">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
        </a-space>
      </a-form-item>
    </template>
  </WarehouseMaterialFilterPanel>
</template>
