<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { getErrorMessage } from '@/api/http'
import { createWarehouseBin, updateWarehouseBin } from '@/api/warehouse/warehouseBin'
import type { WarehouseBin, WarehouseBinSavePayload } from '@/types/warehouse/warehouseBin'
import { buildBinCodePreview, normalizeBinCoordinate } from '@/utils/warehouseBin'

const props = defineProps<{
  open: boolean
  record: WarehouseBin | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const defaultForm = (): WarehouseBinSavePayload => ({
  rowNo: 1,
  colNo: null,
  levelNo: null,
  remark: null,
})

const formState = reactive<WarehouseBinSavePayload>(defaultForm())

const isEdit = computed(() => props.record != null)
const modalTitle = computed(() => (isEdit.value ? '编辑 Bin 位' : '新增 Bin 位'))

const previewBinCode = computed(() =>
  buildBinCodePreview(formState.rowNo, formState.colNo, formState.levelNo),
)

const rules = {
  rowNo: [{ required: true, message: '请输入排', trigger: 'blur' }],
}

watch(
  () => props.open,
  (open) => {
    if (!open) {
      return
    }
    if (props.record) {
      Object.assign(formState, {
        rowNo: props.record.rowNo,
        colNo: props.record.colNo,
        levelNo: props.record.levelNo,
        remark: props.record.remark,
      })
    } else {
      Object.assign(formState, defaultForm())
    }
  },
)

function handleCancel() {
  emit('update:open', false)
}

function buildPayload(): WarehouseBinSavePayload {
  return {
    rowNo: formState.rowNo,
    colNo: normalizeBinCoordinate(formState.colNo),
    levelNo: normalizeBinCoordinate(formState.levelNo),
    remark: formState.remark,
  }
}

async function handleSubmit() {
  if (!previewBinCode.value) {
    message.error('请填写有效的排/列/层组合')
    return
  }

  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload = buildPayload()
    if (isEdit.value && props.record) {
      await updateWarehouseBin(props.record.id, payload)
      message.success('Bin 位已更新')
    } else {
      await createWarehouseBin(payload)
      message.success('Bin 位已创建')
    }
    emit('update:open', false)
    emit('success')
  } catch (error) {
    message.error(getErrorMessage(error, '保存失败'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <a-modal
    :open="open"
    :title="modalTitle"
    :confirm-loading="submitting"
    ok-text="提交"
    cancel-text="关闭"
    destroy-on-close
    @cancel="handleCancel"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="formState" :rules="rules" layout="vertical">
      <a-form-item label="Bin位编号">
        <a-input :value="previewBinCode || '自动带出'" disabled />
      </a-form-item>
      <a-form-item label="排" name="rowNo" required>
        <a-input-number
          v-model:value="formState.rowNo"
          :min="1"
          :precision="0"
          placeholder="例：1"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="列" name="colNo">
        <a-input-number
          v-model:value="formState.colNo"
          :min="1"
          :precision="0"
          placeholder="选填"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="层" name="levelNo">
        <a-input-number
          v-model:value="formState.levelNo"
          :min="1"
          :precision="0"
          placeholder="选填"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea
          v-model:value="formState.remark"
          :rows="3"
          placeholder="请输入……"
          allow-clear
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
