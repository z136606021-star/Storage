<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { getErrorMessage } from '@/api/http'
import { createCustomer, updateCustomer } from '@/api/system/customer'
import type { SysCustomer, SysCustomerSavePayload } from '@/types/system/customer'

const props = defineProps<{
  open: boolean
  record: SysCustomer | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const STATUS_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
]

const defaultForm = (): SysCustomerSavePayload => ({
  customerCode: '',
  name: '',
  contactName: null,
  phone: null,
  email: null,
  address: null,
  remark: null,
  status: 1,
})

const formState = reactive<SysCustomerSavePayload>(defaultForm())

const isEdit = computed(() => props.record != null)
const modalTitle = computed(() => (isEdit.value ? '编辑客户' : '新增客户'))

const rules = {
  customerCode: [{ required: true, message: '请输入客户编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

watch(
  () => props.open,
  (open) => {
    if (!open) {
      return
    }
    if (props.record) {
      Object.assign(formState, {
        customerCode: props.record.customerCode,
        name: props.record.name,
        contactName: props.record.contactName,
        phone: props.record.phone,
        email: props.record.email,
        address: props.record.address,
        remark: props.record.remark,
        status: props.record.status,
      })
    } else {
      Object.assign(formState, defaultForm())
    }
  },
)

function handleCancel() {
  emit('update:open', false)
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload: SysCustomerSavePayload = {
      customerCode: formState.customerCode.trim(),
      name: formState.name.trim(),
      contactName: formState.contactName?.trim() || null,
      phone: formState.phone?.trim() || null,
      email: formState.email?.trim() || null,
      address: formState.address?.trim() || null,
      remark: formState.remark?.trim() || null,
      status: formState.status,
    }
    if (isEdit.value && props.record) {
      await updateCustomer(props.record.id, payload)
      message.success('保存成功')
    } else {
      await createCustomer(payload)
      message.success('新增成功')
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
    destroy-on-close
    width="560px"
    @cancel="handleCancel"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="formState" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="客户编号" name="customerCode">
            <a-input v-model:value="formState.customerCode" :disabled="isEdit" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="客户名称" name="name">
            <a-input v-model:value="formState.name" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="联系人" name="contactName">
            <a-input v-model:value="formState.contactName" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="电话" name="phone">
            <a-input v-model:value="formState.phone" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="邮箱" name="email">
            <a-input v-model:value="formState.email" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态" name="status">
            <a-select v-model:value="formState.status" :options="STATUS_OPTIONS" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="地址" name="address">
            <a-input v-model:value="formState.address" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注" name="remark">
            <a-textarea v-model:value="formState.remark" :rows="3" allow-clear />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>
