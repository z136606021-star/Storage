<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { getErrorMessage } from '@/api/http'
import { updateSafetyStock } from '@/api/warehouse/safetyStock'
import MaterialIdentityDescriptions from '@/components/warehouse/MaterialIdentityDescriptions.vue'
import type { SafetyStockRecord, SafetyStockUpdatePayload } from '@/types/warehouse/safetyStock'
import { displayValue } from '@/utils/format'

const props = defineProps<{
  open: boolean
  record: SafetyStockRecord | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const defaultForm = (): SafetyStockUpdatePayload => ({
  safetyQuantity: 0,
  warningEnabled: false,
})

const formState = reactive<SafetyStockUpdatePayload>(defaultForm())

watch(
  () => props.open,
  (open) => {
    if (!open || !props.record) {
      return
    }
    formState.safetyQuantity = props.record.safetyQuantity ?? 0
    formState.warningEnabled = props.record.warningEnabled ?? false
  },
)

function handleCancel() {
  emit('update:open', false)
}

async function handleSubmit() {
  if (!props.record) {
    return
  }

  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await updateSafetyStock(props.record.materialLedgerId, {
      safetyQuantity: formState.safetyQuantity,
      warningEnabled: formState.warningEnabled,
    })
    message.success('保存成功')
    emit('update:open', false)
    emit('success')
  } catch (error) {
    message.error(getErrorMessage(error, '保存失败'))
  } finally {
    submitting.value = false
  }
}

const identityRecord = computed(() => props.record)

const rules = {
  safetyQuantity: [{ required: true, message: '请输入安全库存数', trigger: 'blur' }],
}
</script>

<template>
  <a-modal
    :open="open"
    title="编辑安全库存"
    :confirm-loading="submitting"
    destroy-on-close
    width="640px"
    @cancel="handleCancel"
    @ok="handleSubmit"
  >
    <MaterialIdentityDescriptions v-if="identityRecord" :record="identityRecord" />

    <a-descriptions :column="1" bordered size="small" class="detail-block stock-block">
      <a-descriptions-item label="库存数量">
        {{ displayValue(record?.stockQuantity) }}
      </a-descriptions-item>
    </a-descriptions>

    <a-form
      ref="formRef"
      :model="formState"
      :rules="rules"
      layout="vertical"
      class="safety-stock-form"
    >
      <a-form-item label="安全库存数" name="safetyQuantity">
        <a-input-number
          v-model:value="formState.safetyQuantity"
          :min="0"
          :precision="0"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="预警开关" name="warningEnabled">
        <a-switch
          v-model:checked="formState.warningEnabled"
          checked-children="开"
          un-checked-children="关"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.stock-block {
  margin-top: @spacing-md;
}

.safety-stock-form {
  margin-top: @spacing-lg;
}
</style>
