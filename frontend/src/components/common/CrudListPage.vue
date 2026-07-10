<script setup lang="ts">
import { computed, toRef } from 'vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import type { ColumnType } from 'ant-design-vue/es/table'
import CrudToolbar from '@/components/common/CrudToolbar.vue'
import ResizableTableHeaderCell from '@/components/common/ResizableTableHeaderCell.vue'
import { useResizableTableColumns } from '@/composables/useResizableTableColumns'

const props = withDefaults(
  defineProps<{
    tableKey: string
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

const {
  resizableColumns,
  freezeColumnWidths,
  updateColumnWidth,
  isResizableTableColumn,
  resolveScroll,
} = useResizableTableColumns(toRef(props, 'columns'), toRef(props, 'tableKey'))

const tableScroll = computed(() => resolveScroll(props.scroll))

function handleColumnResizeStart(_columnKey: string, renderedWidths: Record<string, number>) {
  freezeColumnWidths(renderedWidths)
}

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
        class="crud-resizable-table"
        :columns="resizableColumns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="rowKey"
        :row-selection="rowSelection"
        :custom-row="customRow"
        :scroll="tableScroll"
        :size="size"
        :bordered="bordered"
        @change="handleTableChange"
      >
        <template #headerCell="{ column }">
          <ResizableTableHeaderCell
            :title="column.title"
            :column-key="String(column.key ?? column.dataIndex ?? '')"
            :resizable="isResizableTableColumn(column)"
            @resize-start="handleColumnResizeStart"
            @resize="updateColumnWidth"
          />
        </template>
        <template #bodyCell="scope">
          <slot name="bodyCell" v-bind="scope" />
        </template>
      </a-table>
    </component>

    <slot name="extra" />
  </div>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';
@import '@/styles/mixins.less';

.crud-list-page {
  display: flex;
  flex-direction: column;
  gap: @spacing-sm;
}

.filter-card,
.table-card {
  border-radius: @radius-sm;
}

.filter-card :deep(.ant-card-body),
.table-card :deep(.ant-card-body) {
  padding: @spacing-md @spacing-lg;
}

.filters-inline {
  margin-bottom: @spacing-md;
}

.filter-card :deep(.filter-form),
.filters-inline :deep(.filter-form) {
  width: 100%;
}

.filter-card :deep(.filter-row),
.filters-inline :deep(.filter-row) {
  width: 100%;
}

.filter-card :deep(.filter-item),
.filters-inline :deep(.filter-item) {
  width: 100%;
  margin-inline-end: 0;
  margin-bottom: 0;

  .ant-form-item-row {
    flex-wrap: nowrap;
    align-items: center;
  }

  .ant-form-item-label {
    .filter-form-label();
  }

  .ant-form-item-control {
    flex: 1;
    min-width: 0;
  }
}

.filter-card :deep(.filter-control),
.filters-inline :deep(.filter-control) {
  width: 100%;
}

.filter-card :deep(.filter-actions-col),
.filters-inline :deep(.filter-actions-col) {
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;
}

.filter-card :deep(.filter-actions),
.filters-inline :deep(.filter-actions) {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 992px) {
  .filter-card :deep(.filter-actions-col),
  .filters-inline :deep(.filter-actions-col) {
    justify-content: flex-start;
    align-items: flex-start;
  }

  .filter-card :deep(.filter-actions),
  .filters-inline :deep(.filter-actions) {
    justify-content: flex-start;
    width: 100%;
  }
}

@media (max-width: 576px) {
  .filter-card :deep(.ant-card-body),
  .table-card :deep(.ant-card-body) {
    padding: @spacing-md;
  }
}

.table-card {
  :deep(.crud-resizable-table table) {
    table-layout: fixed;
  }

  :deep(.ant-table-thead > tr > th) {
    background: @color-bg-elevated;
    font-weight: 500;
    padding: @spacing-sm;
    font-size: @font-size-sm;
    position: relative;
  }

  :deep(.ant-table-tbody > tr > td) {
    padding: 6px @spacing-sm;
    font-size: @font-size-sm;
  }

  :deep(.ant-pagination) {
    margin: @spacing-md 0 @spacing-xs;
  }

  :deep(.ant-table-cell) {
    vertical-align: middle;
  }
}

.action-link {
  padding: 0 @spacing-xs;
  height: auto;
}
</style>
