<script setup lang="ts">
import { computed, ref, toRef, watch } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import { batchCreateMaterialIo, updateMaterialIo } from '@/api/materialIo'
import { fetchMaterialLedgerDetail } from '@/api/materialLedger'
import { getErrorMessage } from '@/api/http'
import MaterialLedgerPickerModal from '@/components/warehouse/MaterialLedgerPickerModal.vue'
import { outboundPurposeOptions } from '@/constants/materialIoPurpose'
import {
  confirmSafetyStockSubmit,
  shouldSkipSafetyConfirm,
  useMaterialIoSafetyHint,
} from '@/composables/useMaterialIoSafetyHint'
import { useMaterialIoStock } from '@/composables/useMaterialIoStock'
import type { MaterialLedger } from '@/types/materialLedger'
import type { IoType, MaterialIoFormRow, MaterialIoRecord } from '@/types/materialIo'
import { displayValue } from '@/utils/format'
import { materialIdentityColumns } from '@/utils/warehouseMaterialTable'

const IDENTITY_KEYS = ['category', 'genericName', 'brand', 'name', 'model', 'binLocation'] as const

const props = defineProps<{
  open: boolean
  record?: MaterialIoRecord | null
  initialLedger?: MaterialLedger | null
  initialIoType?: IoType
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const submitting = ref(false)
const ioType = ref<IoType>('IN')
const operatedAt = ref<Dayjs>(dayjs())
const rows = ref<MaterialIoFormRow[]>([])
const selectedRowKeys = ref<string[]>([])
const pickerOpen = ref(false)
const pickingRowKey = ref<string | null>(null)

const isEdit = computed(() => !!props.record)
const editingRecord = toRef(props, 'record')
const showStockColumn = computed(() => ioType.value === 'OUT')
const showPurposeColumn = computed(() => ioType.value === 'OUT')
const showWarningColumn = computed(() => !isEdit.value && ioType.value === 'OUT')

const stockContext = {
  ioType,
  rows,
  isEdit,
  editingRecord,
}

const { availableStockForRow, getDisplayStock } = useMaterialIoStock(stockContext)

const safetyEnabled = computed(() => showWarningColumn.value)
const {
  hasWarnings,
  warningMessageForRow,
  warningRowIndexes,
} = useMaterialIoSafetyHint(rows, stockContext, safetyEnabled)

const warningRowCount = computed(() => warningRowIndexes.value.size)

const modalTitle = computed(() => (isEdit.value ? '编辑出入库' : '新增'))

const purposeOptions = computed(() => outboundPurposeOptions())

const tableColumns = computed(() => {
  const cols = [
    { title: '序号', key: 'index', width: 64, align: 'center' as const },
    ...materialIdentityColumns('batchForm'),
  ]
  if (showStockColumn.value) {
    cols.push({ title: '可用库存', key: 'stockQuantity', width: 80, align: 'center' as const })
  }
  cols.push({ title: '数量', key: 'quantity', width: 100, align: 'center' as const })
  if (showPurposeColumn.value) {
    cols.push({ title: '用途', key: 'purpose', width: 130 })
    cols.push({ title: '项目编号', key: 'projectRef', width: 120 })
  }
  if (showWarningColumn.value) {
    cols.push({ title: '预警', key: 'warning', width: 120, align: 'center' as const })
  }
  cols.push(
    { title: '备注', key: 'remark', width: 120 },
    { title: '操作', key: 'action', width: 100, align: 'center' as const },
  )
  return cols
})

const rowSelection = computed(() => {
  if (isEdit.value) {
    return undefined
  }
  return {
    selectedRowKeys: selectedRowKeys.value,
    onChange: (keys: (string | number)[]) => {
      selectedRowKeys.value = keys.map(String)
    },
  }
})

const pickerOutboundMode = computed(() => !isEdit.value && ioType.value === 'OUT')

function createEmptyRow(): MaterialIoFormRow {
  return {
    key: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    quantity: 1,
    purpose: undefined,
    projectRef: '',
    remark: '',
  }
}

function rowFromLedger(material: MaterialLedger, key: string): MaterialIoFormRow {
  return {
    key,
    materialLedgerId: material.id,
    category: material.category,
    genericName: material.genericName,
    brand: material.brand,
    name: material.name,
    model: material.model,
    binLocation: material.binLocation,
    stockQuantity: material.stockQuantity ?? undefined,
    quantity: 1,
    purpose: undefined,
    projectRef: '',
    remark: '',
  }
}

async function resetForm() {
  selectedRowKeys.value = []
  operatedAt.value = dayjs()
  if (props.record) {
    ioType.value = props.record.ioType
    rows.value = [
      {
        key: 'edit-row',
        materialLedgerId: props.record.materialLedgerId,
        category: props.record.category,
        genericName: props.record.genericName,
        brand: props.record.brand,
        name: props.record.name,
        model: props.record.model,
        binLocation: props.record.binLocation,
        stockQuantity: props.record.stockQuantity ?? undefined,
        quantity: props.record.quantity,
        purpose: props.record.purpose ?? undefined,
        projectRef: props.record.projectRef ?? '',
        remark: props.record.remark ?? '',
      },
    ]
    try {
      const ledger = await fetchMaterialLedgerDetail(props.record.materialLedgerId)
      if (rows.value[0]) {
        rows.value[0].stockQuantity = ledger.stockQuantity ?? undefined
      }
    } catch {
      message.warning('实时库存加载失败，将使用快照库存校验')
    }
    return
  }
  ioType.value = props.initialIoType ?? 'IN'
  if (props.initialLedger) {
    rows.value = [rowFromLedger(props.initialLedger, 'prefill-row')]
    return
  }
  rows.value = [createEmptyRow()]
}

function handleAddRow() {
  rows.value.push(createEmptyRow())
}

function handleRemoveRow(key: string) {
  if (rows.value.length <= 1) {
    message.warning('至少保留一行')
    return
  }
  rows.value = rows.value.filter((row) => row.key !== key)
  selectedRowKeys.value = selectedRowKeys.value.filter((item) => item !== key)
}

function handleBatchRemoveRows() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先勾选要删除的行')
    return
  }
  if (rows.value.length - selectedRowKeys.value.length < 1) {
    message.warning('至少保留一行')
    return
  }
  const removeSet = new Set(selectedRowKeys.value)
  rows.value = rows.value.filter((row) => !removeSet.has(row.key))
  selectedRowKeys.value = []
}

function openPicker(rowKey: string) {
  pickingRowKey.value = rowKey
  pickerOpen.value = true
}

function handleIdentityCellClick(record: MaterialIoFormRow) {
  if (isEdit.value || record.materialLedgerId) {
    return
  }
  openPicker(record.key)
}

function stopCellInputPropagation(event: Event) {
  event.stopPropagation()
}

function handleMaterialSelect(material: MaterialLedger) {
  if (!pickingRowKey.value) {
    return
  }
  const row = rows.value.find((item) => item.key === pickingRowKey.value)
  if (!row) {
    return
  }
  const duplicate = rows.value.some(
    (item) => item.key !== row.key && item.materialLedgerId === material.id,
  )
  if (duplicate) {
    message.warning('该物料已在其他行中选择，请合并数量或删除重复行')
    return
  }
  row.materialLedgerId = material.id
  row.category = material.category
  row.genericName = material.genericName
  row.brand = material.brand
  row.name = material.name
  row.model = material.model
  row.binLocation = material.binLocation
  row.stockQuantity = material.stockQuantity ?? undefined
  pickingRowKey.value = null
}

function validateOutboundRows(): boolean {
  for (let i = 0; i < rows.value.length; i += 1) {
    const row = rows.value[i]
    if (!row.materialLedgerId) {
      continue
    }
    const available = availableStockForRow(row, i)
    if (available != null && (row.quantity ?? 0) > available) {
      message.warning(`第 ${i + 1} 行出库数量不能超过可用库存 ${available}`)
      if (available > 0) {
        row.quantity = available
      }
      return false
    }
  }
  return true
}

function validateRows(): boolean {
  if (rows.value.length === 0) {
    message.warning('请至少添加一行物料')
    return false
  }
  for (let i = 0; i < rows.value.length; i += 1) {
    const row = rows.value[i]
    const rowNo = i + 1
    if (!row.materialLedgerId) {
      message.warning(`第 ${rowNo} 行请选择物料台账`)
      return false
    }
    if (!row.quantity || row.quantity < 1) {
      message.warning(`第 ${rowNo} 行数量必须大于 0`)
      return false
    }
    if (ioType.value === 'OUT' && !row.purpose) {
      message.warning(`第 ${rowNo} 行请选择用途`)
      return false
    }
    if (ioType.value === 'OUT' && row.purpose === 'PROJECT_USE' && !row.projectRef?.trim()) {
      message.warning(`第 ${rowNo} 行项目领用须填写项目编号`)
      return false
    }
    if (ioType.value === 'OUT') {
      const available = availableStockForRow(row, i)
      if (available != null && row.quantity > available) {
        message.warning(`第 ${rowNo} 行出库数量不能超过可用库存 ${available}`)
        return false
      }
    }
  }
  return true
}

async function submitPayload() {
  submitting.value = true
  try {
    if (isEdit.value && props.record) {
      const row = rows.value[0]
      await updateMaterialIo(props.record.id, {
        quantity: row.quantity!,
        remark: row.remark?.trim() || null,
        purpose: row.purpose || null,
        projectRef: row.purpose === 'PROJECT_USE' ? row.projectRef?.trim() || null : null,
      })
      message.success('保存成功')
    } else {
      await batchCreateMaterialIo({
        ioType: ioType.value,
        operatedAt: operatedAt.value.format('YYYY-MM-DDTHH:mm:ss'),
        items: rows.value.map((row) => ({
          materialLedgerId: row.materialLedgerId!,
          quantity: row.quantity!,
          remark: row.remark?.trim() || null,
          purpose: row.purpose || null,
          projectRef: row.purpose === 'PROJECT_USE' ? row.projectRef?.trim() || null : null,
        })),
      })
      message.success('提交成功')
    }
    emit('update:open', false)
    emit('success')
  } catch (error: unknown) {
    message.error(getErrorMessage(error, '提交失败'))
  } finally {
    submitting.value = false
  }
}

async function handleSubmit() {
  if (!validateRows()) {
    return
  }
  if (!isEdit.value && ioType.value === 'OUT' && hasWarnings.value && !shouldSkipSafetyConfirm()) {
    try {
      await confirmSafetyStockSubmit(() => submitPayload())
    } catch {
      // user cancelled
    }
    return
  }
  await submitPayload()
}

function handleClose() {
  emit('update:open', false)
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      resetForm()
    }
  },
)

watch(ioType, (newType, oldType) => {
  if (isEdit.value || newType !== 'OUT' || oldType === 'OUT') {
    return
  }
  validateOutboundRows()
})
</script>

<template>
  <a-modal
    :open="open"
    :title="modalTitle"
    width="1180px"
    destroy-on-close
    :confirm-loading="submitting"
    @cancel="handleClose"
  >
    <div v-if="!isEdit" class="form-header">
      <div class="form-header-left">
        <a-radio-group v-model:value="ioType" button-style="solid">
          <a-radio-button value="IN">入库</a-radio-button>
          <a-radio-button value="OUT">出库</a-radio-button>
        </a-radio-group>
        <div class="operated-at-field">
          <span class="operated-at-label">操作时间</span>
          <a-date-picker
            v-model:value="operatedAt"
            show-time
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 220px"
          />
        </div>
      </div>
      <a-space>
        <a-button type="primary" class="create-btn" @click="handleAddRow">
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
        <a-button :disabled="selectedRowKeys.length === 0" @click="handleBatchRemoveRows">
          删除勾选行
        </a-button>
      </a-space>
    </div>

    <div v-else class="form-header form-header-edit">
      <a-radio-group v-model:value="ioType" button-style="solid" disabled>
        <a-radio-button value="IN">入库</a-radio-button>
        <a-radio-button value="OUT">出库</a-radio-button>
      </a-radio-group>
    </div>

    <a-alert
      v-if="showWarningColumn && hasWarnings"
      type="warning"
      show-icon
      class="warning-summary"
      :message="`${warningRowCount} 行出库后将低于安全库存`"
    />

    <a-table
      :key="`${ioType}-${showPurposeColumn}`"
      :columns="tableColumns"
      :data-source="rows"
      :pagination="false"
      :row-key="(record: MaterialIoFormRow) => record.key"
      :row-selection="rowSelection"
      size="small"
      bordered
      :scroll="{ x: 1060 }"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'index'">
          {{ index + 1 }}
        </template>
        <template v-else-if="IDENTITY_KEYS.includes(column.key as typeof IDENTITY_KEYS[number])">
          <div
            class="identity-cell"
            :class="{ 'material-picker-cell': !isEdit && !record.materialLedgerId }"
            @click="handleIdentityCellClick(record)"
          >
            <span v-if="record.materialLedgerId">
              {{ displayValue(record[column.key as keyof MaterialIoFormRow]) }}
            </span>
            <span v-else-if="!isEdit && column.key === 'category'" class="picker-hint">
              点击选择物料
            </span>
            <span v-else-if="!isEdit" class="picker-placeholder">—</span>
            <span v-else>-</span>
          </div>
        </template>
        <template v-else-if="column.key === 'stockQuantity'">
          <span v-if="record.materialLedgerId != null">
            {{ displayValue(getDisplayStock(record, index)) }}
          </span>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'quantity'">
          <a-input-number
            v-model:value="record.quantity"
            :min="1"
            :max="ioType === 'OUT' && record.materialLedgerId != null ? availableStockForRow(record, index) ?? undefined : undefined"
            :precision="0"
            style="width: 100%"
            @click="stopCellInputPropagation"
          />
        </template>
        <template v-else-if="column.key === 'purpose'">
          <a-select
            v-model:value="record.purpose"
            :options="purposeOptions"
            allow-clear
            placeholder="请选择"
            style="width: 100%"
            @click="stopCellInputPropagation"
            @change="() => { if (record.purpose !== 'PROJECT_USE') record.projectRef = '' }"
          />
        </template>
        <template v-else-if="column.key === 'projectRef'">
          <a-input
            v-model:value="record.projectRef"
            :disabled="record.purpose !== 'PROJECT_USE'"
            allow-clear
            placeholder="项目领用必填"
            @click="stopCellInputPropagation"
          />
        </template>
        <template v-else-if="column.key === 'warning'">
          <a-tooltip v-if="warningMessageForRow(index)" :title="warningMessageForRow(index)!">
            <a-tag color="warning">库存预警</a-tag>
          </a-tooltip>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'remark'">
          <a-input
            v-model:value="record.remark"
            allow-clear
            @click="stopCellInputPropagation"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space v-if="!isEdit" :size="0">
            <a-button
              v-if="record.materialLedgerId"
              type="link"
              size="small"
              @click="openPicker(record.key)"
            >
              重选
            </a-button>
            <a-button
              type="link"
              danger
              size="small"
              @click="handleRemoveRow(record.key)"
            >
              删除
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <template #footer>
      <a-button @click="handleClose">关闭</a-button>
      <a-button type="primary" :loading="submitting" @click="handleSubmit">提交</a-button>
    </template>
  </a-modal>

  <MaterialLedgerPickerModal
    v-model:open="pickerOpen"
    :outbound-mode="pickerOutboundMode"
    @select="handleMaterialSelect"
  />
</template>

<style scoped>
.form-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.form-header-edit {
  justify-content: flex-start;
}

.form-header-left {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
}

.operated-at-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.operated-at-label {
  color: rgba(0, 0, 0, 0.88);
  white-space: nowrap;
}

.create-btn {
  background: #52c41a;
  border-color: #52c41a;
}

.create-btn:hover,
.create-btn:focus {
  background: #73d13d;
  border-color: #73d13d;
}

.warning-summary {
  margin-bottom: 8px;
}

.identity-cell {
  min-height: 22px;
}

.material-picker-cell {
  cursor: pointer;
  color: #1677ff;
}

.picker-hint {
  color: #1677ff;
}

.picker-placeholder {
  color: rgba(0, 0, 0, 0.25);
}
</style>
