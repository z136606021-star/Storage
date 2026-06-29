<script setup lang="ts">
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
</script>

<template>
  <a-form layout="inline" class="filter-form">
    <a-row :gutter="variant === 'compact' ? [8, 8] : [12, 8]" class="filter-row">
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-form-item label="品类" class="filter-item">
          <a-select
            v-model:value="queryForm.category"
            :options="toSelectOptions(filterOptions.category)"
            class="filter-control"
            @change="emit('categoryChange')"
          />
        </a-form-item>
      </a-col>
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-form-item label="统称" class="filter-item">
          <a-select
            v-model:value="queryForm.genericName"
            :options="toSelectOptions(filterOptions.genericName)"
            class="filter-control"
            @change="emit('genericNameChange')"
          />
        </a-form-item>
      </a-col>
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-form-item label="品牌" class="filter-item">
          <a-select
            v-model:value="queryForm.brand"
            :options="toSelectOptions(filterOptions.brand)"
            class="filter-control"
            @change="emit('brandChange')"
          />
        </a-form-item>
      </a-col>

      <slot name="first-row-trailing" />

      <template v-if="variant === 'compact'">
        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <a-form-item label="名称" class="filter-item">
            <a-input
              v-model:value="queryForm.name"
              placeholder="关键字"
              allow-clear
              class="filter-control"
              @press-enter="emit('search')"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <a-form-item label="型号" class="filter-item">
            <a-select
              v-model:value="queryForm.model"
              :options="toSelectOptions(filterOptions.model)"
              class="filter-control"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <a-form-item label="Bin位" class="filter-item">
            <a-select
              v-model:value="queryForm.binLocation"
              :options="toSelectOptions(filterOptions.binLocation)"
              class="filter-control"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="24" :md="24" :lg="12" class="filter-actions-col">
          <slot name="actions" />
        </a-col>
      </template>

      <a-col
        v-else
        :xs="24"
        :sm="24"
        :md="24"
        :lg="6"
        class="filter-actions-col"
      >
        <slot name="actions" />
      </a-col>
    </a-row>

    <a-row v-if="variant !== 'compact'" :gutter="[12, 8]" class="filter-row">
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-form-item label="名称" class="filter-item">
          <a-input
            v-model:value="queryForm.name"
            placeholder="关键字查找"
            allow-clear
            class="filter-control"
            @press-enter="emit('search')"
          />
        </a-form-item>
      </a-col>
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-form-item label="型号" class="filter-item">
          <a-select
            v-model:value="queryForm.model"
            :options="toSelectOptions(filterOptions.model)"
            class="filter-control"
          />
        </a-form-item>
      </a-col>
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-form-item label="Bin位" class="filter-item">
          <a-select
            v-model:value="queryForm.binLocation"
            :options="toSelectOptions(filterOptions.binLocation)"
            class="filter-control"
          />
        </a-form-item>
      </a-col>

      <slot name="second-row-trailing" />
    </a-row>
  </a-form>
</template>
