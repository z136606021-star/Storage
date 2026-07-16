<script setup lang="ts">
import type { Dayjs } from 'dayjs'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import WarehouseMaterialFilterPanel from '@/components/warehouse/WarehouseMaterialFilterPanel.vue'
import type { defaultIoQuery } from '@/composables/useMaterialIoList'

type IoQueryForm = ReturnType<typeof defaultIoQuery>

defineProps<{
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
  { label: '入库', value: 'IN' },
  { label: '出库', value: 'OUT' },
]
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
      <div class="filter-grid-cell">
        <a-form-item label="操作类型" class="filter-item">
          <a-select
            v-model:value="queryForm.ioType"
            :options="IO_TYPE_OPTIONS"
            allow-clear
            placeholder="全部"
            class="filter-control"
          />
        </a-form-item>
      </div>
    </template>

    <template #second-row-trailing>
      <div class="filter-grid-cell">
        <a-form-item label="项目编号" class="filter-item">
          <a-input
            v-model:value="queryForm.projectRef"
            placeholder="关键字查找"
            allow-clear
            class="filter-control"
            @press-enter="emit('search')"
          />
        </a-form-item>
      </div>
      <div class="filter-grid-cell">
        <a-form-item label="操作时间" class="filter-item">
          <a-range-picker v-model:value="operatedAtRange" class="filter-control" />
        </a-form-item>
      </div>
    </template>

    <template #actions>
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
    </template>
  </WarehouseMaterialFilterPanel>
</template>
