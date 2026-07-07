<script setup lang="ts">
import { computed, useSlots } from 'vue'
import type { WarehouseMaterialQuery } from '@/composables/useWarehouseMaterialFilters'
import { toSelectOptions } from '@/utils/selectOptions'

withDefaults(
  defineProps<{
    queryForm: WarehouseMaterialQuery
    filterOptions: {
      category: string[]
      genericName: string[]
      brand: string[]
      model: string[]
      binLocation: string[]
    }
    variant?: 'split' | 'compact'
  }>(),
  { variant: 'split' },
)

const emit = defineEmits<{
  categoryChange: []
  genericNameChange: []
  brandChange: []
  search: []
}>()

const slots = useSlots()
const hasSecondRowTrailing = computed(() => !!slots['second-row-trailing'])
</script>

<template>
  <a-form layout="horizontal" class="filter-form">
    <div
      v-if="variant === 'compact'"
      class="filter-grid-compact"
    >
      <div class="filter-grid-cell filter-grid-cell--category">
        <a-form-item label="品类" class="filter-item">
          <a-select
            v-model:value="queryForm.category"
            :options="toSelectOptions(filterOptions.category)"
            class="filter-control"
            @change="emit('categoryChange')"
          />
        </a-form-item>
      </div>
      <div class="filter-grid-cell filter-grid-cell--generic">
        <a-form-item label="统称" class="filter-item">
          <a-select
            v-model:value="queryForm.genericName"
            :options="toSelectOptions(filterOptions.genericName)"
            class="filter-control"
            @change="emit('genericNameChange')"
          />
        </a-form-item>
      </div>
      <div class="filter-grid-cell filter-grid-cell--brand">
        <a-form-item label="品牌" class="filter-item">
          <a-select
            v-model:value="queryForm.brand"
            :options="toSelectOptions(filterOptions.brand)"
            class="filter-control"
            @change="emit('brandChange')"
          />
        </a-form-item>
      </div>
      <div class="filter-grid-cell filter-grid-cell--name">
        <a-form-item label="名称" class="filter-item">
          <a-input
            v-model:value="queryForm.name"
            placeholder="关键字"
            allow-clear
            class="filter-control"
            @press-enter="emit('search')"
          />
        </a-form-item>
      </div>
      <div class="filter-grid-cell filter-grid-cell--model">
        <a-form-item label="型号" class="filter-item">
          <a-select
            v-model:value="queryForm.model"
            :options="toSelectOptions(filterOptions.model)"
            class="filter-control"
          />
        </a-form-item>
      </div>
      <div class="filter-grid-cell filter-grid-cell--bin">
        <a-form-item label="Bin位" class="filter-item">
          <a-select
            v-model:value="queryForm.binLocation"
            :options="toSelectOptions(filterOptions.binLocation)"
            class="filter-control"
          />
        </a-form-item>
      </div>
      <div class="filter-grid-actions filter-grid-actions--compact">
        <slot name="actions" />
      </div>
    </div>

    <div
      v-else
      class="filter-grid-split"
      :class="{ 'filter-grid-split--extended': hasSecondRowTrailing }"
    >
      <div class="filter-grid-cell">
        <a-form-item label="品类" class="filter-item">
          <a-select
            v-model:value="queryForm.category"
            :options="toSelectOptions(filterOptions.category)"
            class="filter-control"
            @change="emit('categoryChange')"
          />
        </a-form-item>
      </div>
      <div class="filter-grid-cell">
        <a-form-item label="统称" class="filter-item">
          <a-select
            v-model:value="queryForm.genericName"
            :options="toSelectOptions(filterOptions.genericName)"
            class="filter-control"
            @change="emit('genericNameChange')"
          />
        </a-form-item>
      </div>
      <div class="filter-grid-cell">
        <a-form-item label="品牌" class="filter-item">
          <a-select
            v-model:value="queryForm.brand"
            :options="toSelectOptions(filterOptions.brand)"
            class="filter-control"
            @change="emit('brandChange')"
          />
        </a-form-item>
      </div>
      <div class="filter-grid-cell">
        <a-form-item label="名称" class="filter-item">
          <a-input
            v-model:value="queryForm.name"
            placeholder="关键字查找"
            allow-clear
            class="filter-control"
            @press-enter="emit('search')"
          />
        </a-form-item>
      </div>

      <div class="filter-grid-slot filter-grid-slot--first">
        <slot name="first-row-trailing" />
      </div>

      <div class="filter-grid-cell">
        <a-form-item label="型号" class="filter-item">
          <a-select
            v-model:value="queryForm.model"
            :options="toSelectOptions(filterOptions.model)"
            class="filter-control"
          />
        </a-form-item>
      </div>
      <div class="filter-grid-cell">
        <a-form-item label="Bin位" class="filter-item">
          <a-select
            v-model:value="queryForm.binLocation"
            :options="toSelectOptions(filterOptions.binLocation)"
            class="filter-control"
          />
        </a-form-item>
      </div>

      <div class="filter-grid-slot filter-grid-slot--second">
        <slot name="second-row-trailing" />
      </div>

      <div class="filter-grid-actions">
        <slot name="actions" />
      </div>
    </div>
  </a-form>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';
@import '@/styles/mixins.less';

.filter-form {
  width: 100%;
}

.filter-grid-split,
.filter-grid-compact {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  column-gap: 12px;
  row-gap: 8px;
  width: 100%;
  align-items: end;
}

.filter-grid-slot {
  display: contents;
}

.filter-grid-split :deep(.filter-grid-cell),
.filter-grid-compact :deep(.filter-grid-cell) {
  min-width: 0;
}

.filter-grid-cell {
  min-width: 0;
}

.filter-grid-split:not(.filter-grid-split--extended) .filter-grid-cell--category {
  grid-column: 1;
  grid-row: 1;
}

.filter-grid-split:not(.filter-grid-split--extended) .filter-grid-cell--generic {
  grid-column: 2;
  grid-row: 1;
}

.filter-grid-split:not(.filter-grid-split--extended) .filter-grid-cell--brand {
  grid-column: 3;
  grid-row: 1;
}

.filter-grid-split:not(.filter-grid-split--extended) .filter-grid-cell--name {
  grid-column: 1;
  grid-row: 2;
}

.filter-grid-split:not(.filter-grid-split--extended) .filter-grid-cell--model {
  grid-column: 2;
  grid-row: 2;
}

.filter-grid-split:not(.filter-grid-split--extended) .filter-grid-cell--bin {
  grid-column: 3;
  grid-row: 2;
}

.filter-grid-split :deep(.filter-item),
.filter-grid-compact :deep(.filter-item) {
  width: 100%;
  margin-inline-end: 0;
  margin-bottom: 0;
}

.filter-item {
  width: 100%;
  margin-inline-end: 0;
  margin-bottom: 0;
}

.filter-item :deep(.ant-form-item-row),
:deep(.filter-item .ant-form-item-row) {
  flex-wrap: nowrap;
  align-items: center;
}

.filter-item :deep(.ant-form-item-label),
:deep(.filter-item .ant-form-item-label) {
  .filter-form-label();
}

.filter-item :deep(.ant-form-item-control),
:deep(.filter-item .ant-form-item-control) {
  flex: 1;
  min-width: 0;
}

.filter-grid-split :deep(.filter-control),
.filter-grid-compact :deep(.filter-control),
.filter-control {
  width: 100%;
}

.filter-grid-actions {
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;
  min-height: 32px;
}

.filter-grid-split:not(.filter-grid-split--extended) .filter-grid-actions {
  grid-column: 4;
  grid-row: 1;
}

.filter-grid-split--extended .filter-grid-actions {
  grid-column: 1 / -1;
}

.filter-grid-compact .filter-grid-actions--compact {
  grid-column: 3 / -1;
  grid-row: 2;
}

@media (max-width: 1200px) {
  .filter-grid-split,
  .filter-grid-compact {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-grid-split:not(.filter-grid-split--extended) .filter-grid-actions,
  .filter-grid-compact .filter-grid-actions--compact {
    grid-column: 1 / -1;
    grid-row: auto;
  }

  .filter-grid-split:not(.filter-grid-split--extended) .filter-grid-cell--category,
  .filter-grid-split:not(.filter-grid-split--extended) .filter-grid-cell--generic,
  .filter-grid-split:not(.filter-grid-split--extended) .filter-grid-cell--brand,
  .filter-grid-split:not(.filter-grid-split--extended) .filter-grid-cell--name,
  .filter-grid-split:not(.filter-grid-split--extended) .filter-grid-cell--model,
  .filter-grid-split:not(.filter-grid-split--extended) .filter-grid-cell--bin {
    grid-column: auto;
    grid-row: auto;
  }
}

@media (max-width: 576px) {
  .filter-grid-split,
  .filter-grid-compact {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
