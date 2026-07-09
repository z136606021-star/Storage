<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { SettingOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import {
  createDesignGuide,
  updateDesignGuide,
} from '@/api/design/designGuide'
import { getErrorMessage } from '@/api/http'
import type {
  DesignGuide,
  DesignGuideFilterOptions,
  DesignGuideSavePayload,
} from '@/types/design/designGuide'

const props = defineProps<{
  open: boolean
  record: DesignGuide | null
  filterOptions: DesignGuideFilterOptions
  canWrite: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
  'configure-product-types': []
  'configure-stages': []
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const formState = reactive<{
  productTypeId?: number
  stageId?: number
  scope: string
  checkItem: string
  remark: string | null
}>({
  productTypeId: undefined,
  stageId: undefined,
  scope: '',
  checkItem: '',
  remark: null,
})

const rules = {
  productTypeId: [{ required: true, message: '请选择产品类型', trigger: 'change' }],
  stageId: [{ required: true, message: '请选择项目阶段', trigger: 'change' }],
  scope: [{ required: true, message: '请输入适用范围', trigger: 'blur' }],
  checkItem: [{ required: true, message: '请输入检查项', trigger: 'blur' }],
}

const modalTitle = computed(() => (props.record ? '编辑设计指引' : '新增设计指引'))

const selectedProductType = computed(() =>
  props.filterOptions.productTypes.find((item) => item.id === formState.productTypeId),
)

const productTypeOptions = computed(() =>
  props.filterOptions.productTypes.map((item) => ({
    value: item.id,
    label: item.label,
  })),
)

const stageOptions = computed(() =>
  props.filterOptions.stages.map((item) => ({
    value: item.id,
    label: item.label,
  })),
)

const scopeOptions = computed(() =>
  props.filterOptions.scopes.map((scope) => ({
    value: scope,
  })),
)

watch(
  () => props.open,
  (open) => {
    if (!open) {
      return
    }
    if (props.record) {
      Object.assign(formState, {
        productTypeId: props.record.productTypeId,
        stageId: props.record.stageId,
        scope: props.record.scope,
        checkItem: props.record.checkItem,
        remark: props.record.remark,
      })
    } else {
      Object.assign(formState, {
        productTypeId: undefined,
        stageId: undefined,
        scope: '',
        checkItem: '',
        remark: null,
      })
    }
  },
)

function handleCancel() {
  emit('update:open', false)
}

function buildPayload(): DesignGuideSavePayload {
  return {
    productTypeId: Number(formState.productTypeId),
    stageId: Number(formState.stageId),
    scope: formState.scope.trim(),
    checkItem: formState.checkItem.trim(),
    remark: formState.remark?.trim() || null,
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    if (props.record) {
      await updateDesignGuide(props.record.id, buildPayload())
      message.success('设计指引已更新')
    } else {
      await createDesignGuide(buildPayload())
      message.success('设计指引已创建')
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
    width="980px"
    :confirm-loading="submitting"
    ok-text="提交"
    cancel-text="关闭"
    destroy-on-close
    @cancel="handleCancel"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="formState" :rules="rules" :label-col="{ span: 4 }">
      <a-form-item label="产品类型编号">
        <a-input :value="selectedProductType?.typeCode || '根据产品类型自动生成'" disabled />
      </a-form-item>

      <a-form-item label="产品类型" name="productTypeId">
        <a-input-group compact>
          <a-select
            v-model:value="formState.productTypeId"
            :options="productTypeOptions"
            show-search
            allow-clear
            placeholder="请选择产品类型"
            style="width: calc(100% - 40px)"
            :filter-option="(input: string, option: any) => String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())"
          />
          <a-tooltip title="产品类型配置">
            <a-button :disabled="!canWrite" @click="emit('configure-product-types')">
              <template #icon><SettingOutlined /></template>
            </a-button>
          </a-tooltip>
        </a-input-group>
      </a-form-item>

      <a-form-item label="项目阶段" name="stageId">
        <a-input-group compact>
          <a-select
            v-model:value="formState.stageId"
            :options="stageOptions"
            show-search
            allow-clear
            placeholder="请选择项目阶段"
            style="width: calc(100% - 40px)"
            :filter-option="(input: string, option: any) => String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())"
          />
          <a-tooltip title="阶段配置">
            <a-button :disabled="!canWrite" @click="emit('configure-stages')">
              <template #icon><SettingOutlined /></template>
            </a-button>
          </a-tooltip>
        </a-input-group>
      </a-form-item>

      <a-form-item label="适用范围" name="scope">
        <a-auto-complete
          v-model:value="formState.scope"
          :options="scopeOptions"
          allow-clear
          placeholder="请输入或选择适用范围"
        />
      </a-form-item>

      <a-form-item label="检查项" name="checkItem">
        <a-textarea v-model:value="formState.checkItem" :rows="4" allow-clear />
      </a-form-item>

      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formState.remark" :rows="3" allow-clear />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
