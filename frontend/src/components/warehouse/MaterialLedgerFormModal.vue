<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { getErrorMessage } from '@/api/http'
import {
  createMaterialLedger,
  fetchFilterOptions,
  fetchMaterialBomCatalog,
  updateMaterialLedger,
} from '@/api/warehouse/materialLedger'
import type {
  BomCatalogItem,
  MaterialLedger,
  MaterialSavePayload,
} from '@/types/warehouse/materialLedger'
import { toSelectOptions } from '@/utils/selectOptions'

const props = defineProps<{
  open: boolean
  record: MaterialLedger | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const optionsLoading = ref(false)
const bomCatalogLoading = ref(false)
const bomCatalog = ref<BomCatalogItem[]>([])
const selectedBomId = ref<number | undefined>(undefined)

const formFilterOptions = reactive({
  categories: [] as string[],
  genericNames: [] as string[],
  brands: [] as string[],
  models: [] as string[],
  binLocations: [] as string[],
})

const bomCatalogOptions = computed(() =>
  bomCatalog.value.map((item) => ({
    label: item.displayLabel,
    value: item.id,
  })),
)

const defaultForm = (): MaterialSavePayload => ({
  category: '',
  genericName: '',
  brand: null,
  name: '',
  model: '',
  binLocation: '',
  unitPrice: null,
  remark: null,
})

const formState = reactive<MaterialSavePayload>(defaultForm())

const isEdit = computed(() => props.record != null)
const modalTitle = computed(() => (isEdit.value ? '编辑物料' : '新增物料'))
const stockDisplay = computed(() => (isEdit.value ? String(props.record?.stockQuantity ?? 0) : '0'))

function mergeOption(options: string[], current?: string | null): string[] {
  const value = current?.trim()
  if (!value || options.includes(value)) {
    return options
  }
  return [value, ...options]
}

const categorySelectOptions = computed(() =>
  toSelectOptions(
    mergeOption(formFilterOptions.categories, isEdit.value ? formState.category : undefined),
    { excludeAll: true },
  ),
)
const genericNameSelectOptions = computed(() =>
  toSelectOptions(
    mergeOption(formFilterOptions.genericNames, isEdit.value ? formState.genericName : undefined),
    { excludeAll: true },
  ),
)
const brandSelectOptions = computed(() =>
  toSelectOptions(
    mergeOption(formFilterOptions.brands, isEdit.value ? formState.brand : undefined),
    { excludeAll: true },
  ),
)
const modelSelectOptions = computed(() =>
  toSelectOptions(
    mergeOption(formFilterOptions.models, isEdit.value ? formState.model : undefined),
    { excludeAll: true },
  ),
)
const binLocationSelectOptions = computed(() =>
  toSelectOptions(
    mergeOption(formFilterOptions.binLocations, isEdit.value ? formState.binLocation : undefined),
    { excludeAll: true },
  ),
)

const rules = {
  category: [{ required: true, message: '请选择品类', trigger: 'change' }],
  genericName: [{ required: true, message: '请选择统称', trigger: 'change' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  model: [{ required: true, message: '请选择型号', trigger: 'change' }],
  binLocation: [{ required: true, message: '请选择Bin位', trigger: 'change' }],
}

watch(
  () => props.open,
  async (open) => {
    if (!open) {
      selectedBomId.value = undefined
      return
    }
    selectedBomId.value = undefined
    await Promise.all([loadFormOptions(), loadBomCatalog()])
    if (props.record) {
      Object.assign(formState, {
        category: props.record.category,
        genericName: props.record.genericName,
        brand: props.record.brand,
        name: props.record.name,
        model: props.record.model,
        binLocation: props.record.binLocation,
        unitPrice: props.record.unitPrice,
        remark: props.record.remark,
      })
      syncSelectedBomFromForm()
    } else {
      Object.assign(formState, defaultForm())
    }
  },
)

async function loadFormOptions() {
  optionsLoading.value = true
  try {
    const options = await fetchFilterOptions({})
    formFilterOptions.categories = options.categories ?? []
    formFilterOptions.genericNames = options.genericNames ?? []
    formFilterOptions.brands = options.brands ?? []
    formFilterOptions.models = options.models ?? []
    formFilterOptions.binLocations = options.binLocations ?? []
  } catch {
    formFilterOptions.categories = []
    formFilterOptions.genericNames = []
    formFilterOptions.brands = []
    formFilterOptions.models = []
    formFilterOptions.binLocations = []
    message.warning('表单选项加载失败，部分下拉可能不可用')
  } finally {
    optionsLoading.value = false
  }
}

async function loadBomCatalog() {
  bomCatalogLoading.value = true
  try {
    bomCatalog.value = await fetchMaterialBomCatalog()
  } catch {
    bomCatalog.value = []
    message.warning('物料清单加载失败，请手动填写主数据字段')
  } finally {
    bomCatalogLoading.value = false
  }
}

function syncSelectedBomFromForm() {
  const matched = bomCatalog.value.find(
    (item) =>
      item.category === formState.category &&
      item.genericName === formState.genericName &&
      (item.brand ?? null) === (formState.brand ?? null) &&
      item.name === formState.name,
  )
  selectedBomId.value = matched?.id
}

function handleBomPick(bomId: number | undefined) {
  if (bomId == null) {
    return
  }
  const item = bomCatalog.value.find((entry) => entry.id === bomId)
  if (!item) {
    return
  }
  formState.category = item.category
  formState.genericName = item.genericName
  formState.brand = item.brand
  formState.name = item.name
}

function handleCancel() {
  emit('update:open', false)
  formRef.value?.resetFields()
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  const payload: MaterialSavePayload = {
    category: formState.category,
    genericName: formState.genericName,
    brand: formState.brand || null,
    name: formState.name.trim(),
    model: formState.model,
    binLocation: formState.binLocation,
    unitPrice: formState.unitPrice ?? null,
    remark: formState.remark?.trim() || null,
  }

  try {
    if (isEdit.value && props.record) {
      await updateMaterialLedger(props.record.id, payload)
      message.success('更新成功')
    } else {
      await createMaterialLedger(payload)
      message.success('新增成功')
    }
    emit('success')
    handleCancel()
  } catch (error) {
    message.error(getErrorMessage(error, '保存失败，请稍后重试'))
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
    ok-text="保存"
    cancel-text="取消"
    width="560px"
    destroy-on-close
    @cancel="handleCancel"
    @ok="handleSubmit"
  >
    <a-spin :spinning="optionsLoading">
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        layout="vertical"
        class="material-form"
      >
        <a-form-item label="从清单选择">
          <a-select
            v-model:value="selectedBomId"
            :options="bomCatalogOptions"
            :loading="bomCatalogLoading"
            placeholder="搜索并选择物料清单项"
            show-search
            allow-clear
            option-filter-prop="label"
            @change="handleBomPick"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="品类" name="category">
              <a-select
                v-model:value="formState.category"
                :options="categorySelectOptions"
                placeholder="请选择品类"
                show-search
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="统称" name="genericName">
              <a-select
                v-model:value="formState.genericName"
                :options="genericNameSelectOptions"
                placeholder="请选择统称"
                show-search
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="品牌" name="brand">
              <a-select
                v-model:value="formState.brand"
                :options="brandSelectOptions"
                placeholder="请选择品牌"
                show-search
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="名称" name="name">
              <a-input v-model:value="formState.name" placeholder="请输入名称" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="型号" name="model">
              <a-select
                v-model:value="formState.model"
                :options="modelSelectOptions"
                placeholder="请选择型号"
                show-search
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="Bin位" name="binLocation">
              <a-select
                v-model:value="formState.binLocation"
                :options="binLocationSelectOptions"
                placeholder="请选择Bin位"
                show-search
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="库存数量">
              <a-input :value="stockDisplay" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="单价" name="unitPrice">
              <a-input-number
                v-model:value="formState.unitPrice"
                :min="0"
                :precision="2"
                placeholder="请输入单价"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark">
              <a-textarea
                v-model:value="formState.remark"
                placeholder="请输入备注"
                :rows="3"
                allow-clear
              />
            </a-form-item>
          </a-col>
        </a-row>
        <p v-if="!isEdit" class="form-tip">库存数量默认为 0，后续通过物料出入库模块调整。</p>
        <p v-else class="form-tip">库存数量不可在此修改，请通过物料出入库模块调整。</p>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.material-form {
  margin-top: @spacing-sm;
}

.form-tip {
  margin: 0;
  font-size: 12px;
  color: @color-text-tertiary;
}
</style>
