<script setup lang="ts">
import {
  DeleteOutlined,
  DownloadOutlined,
  ExportOutlined,
  PlusOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'

withDefaults(
  defineProps<{
    canWrite?: boolean
    showCreate?: boolean
    showImport?: boolean
    showExport?: boolean
    showTemplate?: boolean
    showBatchExport?: boolean
    showBatchDelete?: boolean
    createGreen?: boolean
    importing?: boolean
    exporting?: boolean
    batchExporting?: boolean
    hasSelection?: boolean
    exportIcon?: 'export' | 'download'
    templateRequiresWrite?: boolean
    batchExportRequiresWrite?: boolean
  }>(),
  {
    canWrite: true,
    showCreate: true,
    showImport: true,
    showExport: true,
    showTemplate: true,
    showBatchExport: false,
    showBatchDelete: false,
    createGreen: false,
    importing: false,
    exporting: false,
    batchExporting: false,
    hasSelection: false,
    exportIcon: 'export',
    templateRequiresWrite: true,
    batchExportRequiresWrite: false,
  },
)

const emit = defineEmits<{
  create: []
  export: []
  'batch-export': []
  'batch-delete': []
  import: [file: File]
  'download-template': []
}>()

function onImport(file: File) {
  emit('import', file)
  return false
}
</script>

<template>
  <div class="crud-toolbar">
    <a-space wrap>
      <slot name="prepend" />

      <slot name="create">
        <a-button
          v-if="showCreate && canWrite"
          type="primary"
          :class="{ 'btn-add': createGreen }"
          @click="emit('create')"
        >
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
      </slot>

      <template v-if="showImport && canWrite">
        <a-upload :show-upload-list="false" accept=".xlsx,.xls" :before-upload="onImport">
          <a-button :loading="importing" :type="createGreen ? 'primary' : undefined">
            <template #icon><UploadOutlined /></template>
            导入
          </a-button>
        </a-upload>
      </template>

      <a-button
        v-if="showTemplate && (templateRequiresWrite ? canWrite : true)"
        :type="createGreen ? 'link' : undefined"
        @click="emit('download-template')"
      >
        下载模板
      </a-button>

      <a-button v-if="showExport" type="primary" :loading="exporting" @click="emit('export')">
        <template #icon>
          <ExportOutlined v-if="exportIcon === 'export'" />
          <DownloadOutlined v-else />
        </template>
        导出
      </a-button>

      <a-button
        v-if="showBatchExport && (!batchExportRequiresWrite || canWrite)"
        :disabled="!hasSelection"
        :loading="batchExporting"
        @click="emit('batch-export')"
      >
        <template #icon><ExportOutlined /></template>
        批量导出
      </a-button>

      <a-button
        v-if="showBatchDelete && canWrite"
        danger
        :disabled="!hasSelection"
        @click="emit('batch-delete')"
      >
        <template #icon><DeleteOutlined /></template>
        批量删除
      </a-button>

      <slot name="append" />
    </a-space>
  </div>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';
@import '@/styles/mixins.less';

.crud-toolbar {
  margin-bottom: @spacing-sm;
}

.btn-add {
  .btn-success-primary();
}
</style>
