<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { Modal, message } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import { cleanupExceptionLogs, fetchExceptionLogDetail, fetchExceptionLogPage } from '@/api/system/exceptionLog'
import CrudDetailDrawer from '@/components/common/CrudDetailDrawer.vue'
import CrudListPage from '@/components/common/CrudListPage.vue'
import { useCrudDetailDrawer } from '@/composables/useCrudDetailDrawer'
import { usePaginatedCrudList } from '@/composables/usePaginatedCrudList'
import { useWritePermission } from '@/composables/useWritePermission'
import type { SysExceptionLog } from '@/types/system/exceptionLog'
import { displayValue, formatDateTime } from '@/utils/format'
import { getTableRowIndex } from '@/utils/tableIndex'

const { canWrite } = useWritePermission('system:exception-log:write')

const queryForm = reactive({
  source: undefined as 'BACKEND' | 'FRONTEND' | undefined,
  httpStatus: undefined as number | undefined,
  exceptionClass: '',
  requestId: '',
  requestPath: '',
  keyword: '',
  occurredAtRange: undefined as [Dayjs, Dayjs] | undefined,
})

function buildQueryParams() {
  return {
    source: queryForm.source,
    httpStatus: queryForm.httpStatus,
    exceptionClass: queryForm.exceptionClass.trim() || undefined,
    requestId: queryForm.requestId.trim() || undefined,
    requestPath: queryForm.requestPath.trim() || undefined,
    keyword: queryForm.keyword.trim() || undefined,
    occurredAtStart: queryForm.occurredAtRange?.[0]?.startOf('day').format('YYYY-MM-DDTHH:mm:ss'),
    occurredAtEnd: queryForm.occurredAtRange?.[1]?.endOf('day').format('YYYY-MM-DDTHH:mm:ss'),
  }
}

const {
  loading,
  dataSource,
  pagination,
  loadData,
  handleSearch,
  handleResetQuery,
  handleTableChange,
} = usePaginatedCrudList<SysExceptionLog, ReturnType<typeof buildQueryParams>>({
  fetchPage: fetchExceptionLogPage,
  buildQueryParams,
  loadErrorMessage: '加载异常日志失败',
})

const {
  drawerOpen,
  detailLoading,
  detailRecord,
  openDetail,
  closeDetail,
} = useCrudDetailDrawer(fetchExceptionLogDetail, '加载异常日志详情失败')

const columns = [
  { title: '序号', key: 'index', width: 64, align: 'center' as const },
  { title: '发生时间', dataIndex: 'occurredAt', key: 'occurredAt', width: 168 },
  { title: '来源', dataIndex: 'source', key: 'source', width: 88, align: 'center' as const },
  { title: '错误码', dataIndex: 'errorCode', key: 'errorCode', width: 140, ellipsis: true },
  { title: '请求ID', dataIndex: 'requestId', key: 'requestId', width: 180, ellipsis: true },
  { title: 'HTTP', dataIndex: 'httpStatus', key: 'httpStatus', width: 72, align: 'center' as const },
  { title: '异常类型', dataIndex: 'exceptionClass', key: 'exceptionClass', width: 180, ellipsis: true },
  { title: '摘要', dataIndex: 'summary', key: 'summary', width: 220, ellipsis: true },
  { title: '路径', dataIndex: 'requestPath', key: 'requestPath', width: 180, ellipsis: true },
  { title: '操作人', dataIndex: 'operatorUsername', key: 'operatorUsername', width: 100 },
  { title: '操作', key: 'action', width: 88, align: 'center' as const, fixed: 'right' as const },
]

function formatSource(source: string) {
  return source === 'BACKEND' ? '后端' : source === 'FRONTEND' ? '前端' : source
}

function handleReset() {
  queryForm.source = undefined
  queryForm.httpStatus = undefined
  queryForm.exceptionClass = ''
  queryForm.requestId = ''
  queryForm.requestPath = ''
  queryForm.keyword = ''
  queryForm.occurredAtRange = undefined
  handleResetQuery()
  loadData()
}

function handleCleanup() {
  Modal.confirm({
    title: '清理异常日志',
    content: '将删除所选截止时间之前的异常日志，此操作不可恢复。默认建议清理 30 天前的记录。',
    okText: '确认清理',
    cancelText: '取消',
    onOk: async () => {
      const before = dayjs().subtract(30, 'day').endOf('day').format('YYYY-MM-DDTHH:mm:ss')
      const result = await cleanupExceptionLogs({ before })
      message.success(`已清理 ${result.deleted} 条异常日志`)
      await loadData()
    },
  })
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <CrudListPage
    table-key="system-exception-log"
    :columns="columns"
    :loading="loading"
    :data-source="dataSource"
    :pagination="pagination"
    :show-toolbar="false"
    @change="handleTableChange"
  >
    <template #filters>
      <a-form layout="inline" class="filter-form">
        <a-form-item label="时间范围">
          <a-range-picker v-model:value="queryForm.occurredAtRange" allow-clear />
        </a-form-item>
        <a-form-item label="来源">
          <a-select
            v-model:value="queryForm.source"
            allow-clear
            placeholder="全部"
            style="width: 120px"
          >
            <a-select-option value="BACKEND">后端</a-select-option>
            <a-select-option value="FRONTEND">前端</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="HTTP 状态">
          <a-input-number
            v-model:value="queryForm.httpStatus"
            :min="100"
            :max="599"
            placeholder="如 500"
            style="width: 120px"
          />
        </a-form-item>
        <a-form-item label="请求ID">
          <a-input v-model:value="queryForm.requestId" allow-clear placeholder="requestId" />
        </a-form-item>
        <a-form-item label="异常类型">
          <a-input v-model:value="queryForm.exceptionClass" allow-clear placeholder="异常类名" />
        </a-form-item>
        <a-form-item label="路径">
          <a-input v-model:value="queryForm.requestPath" allow-clear placeholder="请求路径" />
        </a-form-item>
        <a-form-item label="关键字">
          <a-input v-model:value="queryForm.keyword" allow-clear placeholder="摘要/堆栈" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">
              <template #icon><SearchOutlined /></template>
              查询
            </a-button>
            <a-button @click="handleReset">
              <template #icon><ReloadOutlined /></template>
              重置
            </a-button>
            <a-button v-if="canWrite" danger @click="handleCleanup">清理 30 天前日志</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </template>

    <template #bodyCell="{ column, record, index }">
      <template v-if="column.key === 'index'">
        {{ getTableRowIndex(index, pagination) }}
      </template>
      <template v-else-if="column.key === 'occurredAt'">
        {{ formatDateTime(record.occurredAt) }}
      </template>
      <template v-else-if="column.key === 'source'">
        {{ formatSource(record.source) }}
      </template>
      <template v-else-if="column.key === 'action'">
        <a-button type="link" size="small" @click="openDetail(record.id)">详情</a-button>
      </template>
      <template v-else>
        {{ displayValue(record[column.dataIndex as keyof SysExceptionLog]) }}
      </template>
    </template>
  </CrudListPage>

  <CrudDetailDrawer
    :open="drawerOpen"
    title="异常日志详情"
    :width="720"
    :loading="detailLoading"
    @update:open="drawerOpen = $event"
    @close="closeDetail"
  >
    <template v-if="detailRecord">
      <a-descriptions bordered :column="1" size="small" class="detail-block">
        <a-descriptions-item label="发生时间">{{ formatDateTime(detailRecord.occurredAt) }}</a-descriptions-item>
        <a-descriptions-item label="来源">{{ formatSource(detailRecord.source) }}</a-descriptions-item>
        <a-descriptions-item label="错误码">{{ displayValue(detailRecord.errorCode) }}</a-descriptions-item>
        <a-descriptions-item label="请求ID">{{ displayValue(detailRecord.requestId) }}</a-descriptions-item>
        <a-descriptions-item label="HTTP 状态">{{ displayValue(detailRecord.httpStatus) }}</a-descriptions-item>
        <a-descriptions-item label="HTTP 方法">{{ displayValue(detailRecord.httpMethod) }}</a-descriptions-item>
        <a-descriptions-item label="请求路径">{{ displayValue(detailRecord.requestPath) }}</a-descriptions-item>
        <a-descriptions-item label="前端路由">{{ displayValue(detailRecord.frontendRoute) }}</a-descriptions-item>
        <a-descriptions-item label="异常类型">{{ displayValue(detailRecord.exceptionClass) }}</a-descriptions-item>
        <a-descriptions-item label="操作人">{{ displayValue(detailRecord.operatorUsername) }}</a-descriptions-item>
        <a-descriptions-item label="浏览器信息">{{ displayValue(detailRecord.browserInfo) }}</a-descriptions-item>
        <a-descriptions-item label="摘要">{{ displayValue(detailRecord.summary) }}</a-descriptions-item>
      </a-descriptions>

      <div v-if="detailRecord.stackTrace" class="detail-block stack-block">
        <div class="stack-title">堆栈信息</div>
        <pre class="stack-trace">{{ detailRecord.stackTrace }}</pre>
      </div>
    </template>
  </CrudDetailDrawer>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.filter-form {
  row-gap: @spacing-sm;
}

.stack-block {
  margin-top: @spacing-lg;
}

.stack-title {
  margin-bottom: @spacing-sm;
  font-weight: 600;
}

.stack-trace {
  max-height: 420px;
  overflow: auto;
  margin: 0;
  padding: @spacing-sm;
  background: #f7f7f7;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.5;
}
</style>
