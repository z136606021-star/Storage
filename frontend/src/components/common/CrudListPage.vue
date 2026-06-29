<script setup lang="ts">
import type { TablePaginationConfig } from 'ant-design-vue'
import type { ColumnType } from 'ant-design-vue/es/table'
import CrudToolbar from '@/components/common/CrudToolbar.vue'

withDefaults(
  defineProps<{
    columns: ColumnType[]
    loading?: boolean
    dataSource: unknown[]
    pagination?: false | TablePaginationConfig
    rowKey?: string | ((record: any) => string | number)
    rowSelection?: Record<string, unknown>
    customRow?: (record: any, index: number) => Record<string, unknown>
    scroll?: Record<string, unknown>
    size?: 'small' | 'middle' | 'large'
    bordered?: boolean
    filterLayout?: 'card' | 'none'
    tableInCard?: boolean
    showToolbar?: boolean
    toolbarCanWrite?: boolean
    toolbarShowCreate?: boolean
    toolbarShowImport?: boolean
    toolbarShowExport?: boolean
    toolbarShowTemplate?: boolean
    toolbarShowBatchExport?: boolean
    toolbarShowBatchDelete?: boolean
    toolbarCreateGreen?: boolean
    toolbarImporting?: boolean
    toolbarExporting?: boolean
    toolbarBatchExporting?: boolean
    toolbarHasSelection?: boolean
    toolbarExportIcon?: 'export' | 'download'
    toolbarTemplateRequiresWrite?: boolean
    toolbarBatchExportRequiresWrite?: boolean
  }>(),
  {
    loading: false,
    pagination: undefined,
    rowKey: 'id',
    size: 'small',
    bordered: true,
    filterLayout: 'card',
    tableInCard: true,
    showToolbar: true,
    toolbarCanWrite: true,
    toolbarShowCreate: true,
    toolbarShowImport: true,
    toolbarShowExport: true,
    toolbarShowTemplate: true,
    toolbarShowBatchExport: false,
    toolbarShowBatchDelete: false,
    toolbarCreateGreen: false,
    toolbarImporting: false,
    toolbarExporting: false,
    toolbarBatchExporting: false,
    toolbarHasSelection: false,
    toolbarExportIcon: 'export',
    toolbarTemplateRequiresWrite: true,
    toolbarBatchExportRequiresWrite: false,
  },
)

const emit = defineEmits<{
  change: [pagination: TablePaginationConfig]
  'toolbar-create': []
  'toolbar-export': []
  'toolbar-batch-export': []
  'toolbar-batch-delete': []
  'toolbar-import': [file: File]
  'toolbar-download-template': []
}>()

function handleTableChange(page: TablePaginationConfig) {
  emit('change', page)
}
</script>

<template>
  <div class="crud-list-page">
    <template v-if="$slots.filters">
      <a-card v-if="filterLayout === 'card'" :bordered="false" class="filter-card">
        <slot name="filters" />
      </a-card>
      <div v-else class="filters-inline">
        <slot name="filters" />
      </div>
    </template>

    <component :is="tableInCard ? 'a-card' : 'div'" :bordered="false" class="table-card">
      <CrudToolbar
        v-if="showToolbar"
        :can-write="toolbarCanWrite"
        :show-create="toolbarShowCreate"
        :show-import="toolbarShowImport"
        :show-export="toolbarShowExport"
        :show-template="toolbarShowTemplate"
        :show-batch-export="toolbarShowBatchExport"
        :show-batch-delete="toolbarShowBatchDelete"
        :create-green="toolbarCreateGreen"
        :importing="toolbarImporting"
        :exporting="toolbarExporting"
        :batch-exporting="toolbarBatchExporting"
        :has-selection="toolbarHasSelection"
        :export-icon="toolbarExportIcon"
        :template-requires-write="toolbarTemplateRequiresWrite"
        :batch-export-requires-write="toolbarBatchExportRequiresWrite"
        @create="emit('toolbar-create')"
        @export="emit('toolbar-export')"
        @batch-export="emit('toolbar-batch-export')"
        @batch-delete="emit('toolbar-batch-delete')"
        @import="emit('toolbar-import', $event)"
        @download-template="emit('toolbar-download-template')"
      >
        <template v-if="$slots.toolbarPrepend" #prepend>
          <slot name="toolbarPrepend" />
        </template>
        <template v-if="$slots.toolbarAppend" #append>
          <slot name="toolbarAppend" />
        </template>
      </CrudToolbar>

      <slot v-else name="toolbar" />

      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="rowKey"
        :row-selection="rowSelection"
        :custom-row="customRow"
        :scroll="scroll"
        :size="size"
        :bordered="bordered"
        @change="handleTableChange"
      >
        <template #bodyCell="scope">
          <slot name="bodyCell" v-bind="scope" />
        </template>
      </a-table>
    </component>

    <slot name="extra" />
  </div>
</template>

<style scoped>
.crud-list-page {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-card,
.table-card {
  border-radius: 2px;
}

.filter-card :deep(.ant-card-body),
.table-card :deep(.ant-card-body) {
  padding: 12px 16px;
}

.filters-inline {
  margin-bottom: 12px;
}

.filter-form {
  width: 100%;
}

.filter-row {
  width: 100%;
}

.filter-item {
  width: 100%;
  margin-inline-end: 0;
  margin-bottom: 0;
}

.filter-item :deep(.ant-form-item-row) {
  flex-wrap: nowrap;
}

.filter-item :deep(.ant-form-item-label) {
  width: 52px;
  flex: none;
  text-align: right;
}

.filter-item :deep(.ant-form-item-label > label) {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.filter-item :deep(.ant-form-item-control) {
  flex: 1;
  min-width: 0;
}

.filter-control {
  width: 100%;
}

.filter-actions-col {
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
}

.table-card :deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 500;
  padding: 8px;
  font-size: 13px;
}

.table-card :deep(.ant-table-tbody > tr > td) {
  padding: 6px 8px;
  font-size: 13px;
}

.table-card :deep(.ant-pagination) {
  margin: 12px 0 4px;
}

.table-card :deep(.ant-table-cell) {
  vertical-align: middle;
}

.action-link {
  padding: 0 4px;
  height: auto;
}
</style>
