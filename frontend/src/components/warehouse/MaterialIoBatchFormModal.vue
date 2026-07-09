<script setup lang="ts">
import { computed, ref, toRef, watch } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import { batchCreateMaterialIo, updateMaterialIo } from '@/api/warehouse/materialIo'
import { fetchMaterialLedgerDetail } from '@/api/warehouse/materialLedger'
import { getErrorMessage } from '@/api/http'
import MaterialLedgerPickerModal from '@/components/warehouse/MaterialLedgerPickerModal.vue'
import WarehouseBinPickerModal from '@/components/warehouse/WarehouseBinPickerModal.vue'
import WarehouseBomPickerModal from '@/components/warehouse/WarehouseBomPickerModal.vue'
import {
  confirmSafetyStockSubmit,
  shouldSkipSafetyConfirm,
  useMaterialIoSafetyHint,
} from '@/composables/useMaterialIoSafetyHint'
import { useMaterialIoStock } from '@/composables/useMaterialIoStock'
import type { MaterialLedger } from '@/types/warehouse/materialLedger'
import type { IoType, MaterialIoFormRow, MaterialIoRecord } from '@/types/warehouse/materialIo'
import type { WarehouseBin } from '@/types/warehouse/warehouseBin'
import type { WarehouseBom } from '@/types/warehouse/warehouseBom'
import { displayValue } from '@/utils/format'
import { materialIdentityColumns } from '@/utils/warehouseMaterialTable'

const IDENTITY_KEYS = ['category', 'genericName', 'brand', 'name', 'model', 'binLocation'] as const
type TableColumn = NonNullable<TableProps['columns']>[number]

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
const rows = ref<MaterialIoFormRow[]>([])
const selectedRowKeys = ref<string[]>([])
const ledgerPickerOpen = ref(false)
const bomPickerOpen = ref(false)
const binPickerOpen = ref(false)
const pickingRowKey = ref<string | null>(null)

const isEdit = computed(() => !!props.record)
const editingRecord = toRef(props, 'record')
const showStockColumn = computed(() => ioType.value === 'OUT')
const showProjectRefColumn = computed(() => ioType.value === 'OUT')
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

const tableColumns = computed(() => {
  const cols: TableColumn[] = [
    { title: '序号', key: 'index', width: 64, align: 'center' as const },
    ...materialIdentityColumns('batchForm'),
  ]
  if (showStockColumn.value) {
    cols.push({ title: '可用库存', key: 'stockQuantity', width: 80, align: 'center' as const })
  }
  cols.push({ title: '数量', key: 'quantity', width: 100, align: 'center' as const })
  cols.push({ title: '单价', key: 'unitPrice', width: 110, align: 'center' as const })
  if (showProjectRefColumn.value) {
    cols.push({ title: '项目编号', key: 'projectRef', width: 120 })
  }
  if (showWarningColumn.value) {
    cols.push({ title: '预警', key: 'warning', width: 120, align: 'center' as const })
  }
  cols.push(
    { title: '备注', key: 'remark', width: 120 },
    { title: '操作', key: 'action', width: 170, align: 'center' as const },
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

function createEmptyRow(): MaterialIoFormRow {
  return {
    key: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    quantity: 1,
    unitPrice: null,
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
    unitPrice: material.unitPrice ?? null,
    quantity: 1,
    projectRef: '',
    remark: '',
  }
}

function rowHasMaterial(record: MaterialIoFormRow) {
  return ioType.value === 'IN' ? record.bomId != null : record.materialLedgerId != null
}

async function resetForm() {
  selectedRowKeys.value = []
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
        unitPrice: props.record.unitPrice ?? null,
        quantity: props.record.quantity,
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
  if (ioType.value === 'OUT' && props.initialLedger) {
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

function openLedgerPicker(rowKey: string) {
  pickingRowKey.value = rowKey
  ledgerPickerOpen.value = true
}

function openBomPicker(rowKey: string) {
  pickingRowKey.value = rowKey
  bomPickerOpen.value = true
}

function openBinPicker(rowKey: string) {
  pickingRowKey.value = rowKey
  binPickerOpen.value = true
}

function handleIdentityCellClick(record: MaterialIoFormRow, columnKey: string) {
  if (isEdit.value) {
    return
  }
  if (ioType.value === 'IN') {
    if (columnKey === 'binLocation' && !record.binLocation) {
      openBinPicker(record.key)
    } else if (!record.bomId) {
      openBomPicker(record.key)
    }
    return
  }
  if (!record.materialLedgerId) {
    openLedgerPicker(record.key)
  }
}

function stopCellInputPropagation(event: Event) {
  event.stopPropagation()
}

function handleLedgerSelect(material: MaterialLedger) {
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

function hasInboundDuplicate(row: MaterialIoFormRow, bomId: number, binLocation?: string) {
  if (!binLocation) {
    return false
  }
  return rows.value.some(
    (item) => item.key !== row.key && item.bomId === bomId && item.binLocation === binLocation,
  )
}

function handleBomSelect(material: WarehouseBom) {
  if (!pickingRowKey.value) {
    return
  }
  const row = rows.value.find((item) => item.key === pickingRowKey.value)
  if (!row) {
    return
  }
  if (hasInboundDuplicate(row, material.id, row.binLocation)) {
    message.warning('该物料和 Bin 位已在其他行中选择，请合并数量或删除重复行')
    return
  }
  row.bomId = material.id
  row.materialLedgerId = undefined
  row.category = material.category
  row.genericName = material.genericName
  row.brand = material.brand
  row.name = material.name
  row.model = material.model
  row.stockQuantity = undefined
  pickingRowKey.value = null
}

function handleBinSelect(bin: WarehouseBin) {
  if (!pickingRowKey.value) {
    return
  }
  const row = rows.value.find((item) => item.key === pickingRowKey.value)
  if (!row) {
    return
  }
  if (row.bomId != null && hasInboundDuplicate(row, row.bomId, bin.binCode)) {
    message.warning('该物料和 Bin 位已在其他行中选择，请合并数量或删除重复行')
    return
  }
  row.binLocation = bin.binCode
  pickingRowKey.value = null
}

function validateRows(): boolean {
  if (rows.value.length === 0) {
    message.warning('请至少添加一行物料')
    return false
  }
  for (let i = 0; i < rows.value.length; i += 1) {
    const row = rows.value[i]
    const rowNo = i + 1
    if (ioType.value === 'IN') {
      if (!row.bomId) {
        message.warning(`第 ${rowNo} 行请选择物料清单`)
        return false
      }
      if (!row.binLocation) {
        message.warning(`第 ${rowNo} 行请选择Bin位`)
        return false
      }
    } else {
      if (!row.materialLedgerId) {
        message.warning(`第 ${rowNo} 行请选择物料台账`)
        return false
      }
    }
    if (!row.quantity || row.quantity < 1) {
      message.warning(`第 ${rowNo} 行数量必须大于 0`)
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
        unitPrice: row.unitPrice ?? null,
        remark: row.remark?.trim() || null,
        projectRef: row.projectRef?.trim() || null,
      })
      message.success('保存成功')
    } else {
      await batchCreateMaterialIo({
        ioType: ioType.value,
        items: rows.value.map((row) => {
          const base = {
            quantity: row.quantity!,
            unitPrice: row.unitPrice ?? null,
            remark: row.remark?.trim() || null,
          }
          if (ioType.value === 'IN') {
            return {
              ...base,
              bomId: row.bomId!,
              binLocation: row.binLocation!,
            }
          }
          return {
            ...base,
            materialLedgerId: row.materialLedgerId!,
            projectRef: row.projectRef?.trim() || null,
          }
        }),
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
  if (isEdit.value || newType === oldType) {
    return
  }
  rows.value = rows.value.map((row) => ({
    ...createEmptyRow(),
    key: row.key,
    quantity: row.quantity ?? 1,
    remark: row.remark ?? '',
  }))
  selectedRowKeys.value = []
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
      <a-radio-group v-model:value="ioType" button-style="solid">
        <a-radio-button value="IN">入库</a-radio-button>
        <a-radio-button value="OUT">出库</a-radio-button>
      </a-radio-group>
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
      :key="`${ioType}-${showProjectRefColumn}`"
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
            :class="{ 'material-picker-cell': !isEdit && ((ioType === 'IN' && column.key === 'binLocation' && !record.binLocation) || !rowHasMaterial(record)) }"
            @click="handleIdentityCellClick(record, String(column.key))"
          >
            <span v-if="record[column.key as keyof MaterialIoFormRow]">
              {{ displayValue(record[column.key as keyof MaterialIoFormRow]) }}
            </span>
            <span v-else-if="!isEdit && column.key === 'category'" class="picker-hint">
              点击选择物料
            </span>
            <span v-else-if="!isEdit && ioType === 'IN' && column.key === 'binLocation'" class="picker-hint">
              点击选择Bin
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
        <template v-else-if="column.key === 'unitPrice'">
          <a-input-number
            v-model:value="record.unitPrice"
            :min="0"
            :precision="2"
            placeholder="选填"
            style="width: 100%"
            @click="stopCellInputPropagation"
          />
        </template>
        <template v-else-if="column.key === 'projectRef'">
          <a-input
            v-model:value="record.projectRef"
            allow-clear
            placeholder="选填"
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
              v-if="ioType === 'OUT'"
              type="link"
              size="small"
              @click="openLedgerPicker(record.key)"
            >
              {{ record.materialLedgerId ? '重选' : '选择' }}
            </a-button>
            <a-button
              v-if="ioType === 'IN'"
              type="link"
              size="small"
              @click="openBomPicker(record.key)"
            >
              {{ record.bomId ? '重选物料' : '选物料' }}
            </a-button>
            <a-button
              v-if="ioType === 'IN'"
              type="link"
              size="small"
              @click="openBinPicker(record.key)"
            >
              {{ record.binLocation ? '重选Bin' : '选Bin' }}
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
    v-model:open="ledgerPickerOpen"
    :outbound-mode="!isEdit && ioType === 'OUT'"
    @select="handleLedgerSelect"
  />
  <WarehouseBomPickerModal
    v-model:open="bomPickerOpen"
    @select="handleBomSelect"
  />
  <WarehouseBinPickerModal
    v-model:open="binPickerOpen"
    @select="handleBinSelect"
  />
</template>

<style scoped lang="less">
@import '@/styles/variables.less';
@import '@/styles/mixins.less';

.form-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: @spacing-md;
  margin-bottom: @spacing-md;
}

.form-header-edit {
  justify-content: flex-start;
}

.create-btn {
  .btn-success-primary();
}

.warning-summary {
  margin-bottom: @spacing-sm;
}

.identity-cell {
  min-height: 22px;
}

.material-picker-cell {
  cursor: pointer;
  color: @color-link;
}

.picker-hint {
  color: @color-link;
}

.picker-placeholder {
  color: @color-text-quaternary;
}
</style>
