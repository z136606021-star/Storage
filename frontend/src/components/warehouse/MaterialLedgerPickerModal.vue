<script setup lang="ts">

import { computed, ref, watch } from 'vue'

import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'

import { message } from 'ant-design-vue'

import WarehouseMaterialFilterPanel from '@/components/warehouse/WarehouseMaterialFilterPanel.vue'

import { useMaterialLedgerList } from '@/composables/useMaterialLedgerList'

import type { MaterialLedger } from '@/types/warehouse/materialLedger'

import { displayValue } from '@/utils/format'

import { getTableRowIndex } from '@/utils/tableIndex'

import { materialIdentityColumns } from '@/utils/warehouseMaterialTable'



const props = defineProps<{

  open: boolean

  outboundMode?: boolean

}>()



const emit = defineEmits<{

  'update:open': [value: boolean]

  select: [record: MaterialLedger]

}>()



const selectedRowKeys = ref<number[]>([])



const {

  queryForm,

  filterOptions,

  reloadFilterOptions,

  handleCategoryChange,

  handleGenericNameChange,

  handleBrandChange,

  resetQueryForm,

  handleReset: resetList,

  loading,

  dataSource,

  pagination,

  loadData,

  handleSearch,

  handleTableChange,

} = useMaterialLedgerList({

  loadErrorMessage: '加载物料台账失败',

})



const rowSelection = computed(() => ({

  type: 'radio' as const,

  selectedRowKeys: selectedRowKeys.value,

  onChange: (keys: (string | number)[]) => {

    selectedRowKeys.value = keys.map((key) => Number(key))

  },

}))



const columns = [

  { title: '序号', key: 'index', width: 64, align: 'center' as const },

  ...materialIdentityColumns('picker'),

  { title: '库存', dataIndex: 'stockQuantity', key: 'stockQuantity', width: 72, align: 'center' as const },

]



function handleReset() {

  resetQueryForm()

  pagination.current = 1

  selectedRowKeys.value = []

  resetList()

}



function handleClose() {

  emit('update:open', false)

}



function confirmSelect(record: MaterialLedger) {

  if (props.outboundMode && (record.stockQuantity ?? 0) <= 0) {

    message.warning('该物料当前无库存，无法出库')

    return

  }

  emit('select', record)

  emit('update:open', false)

}



function handleConfirm() {

  if (selectedRowKeys.value.length === 0) {

    message.warning('请选择一条物料台账记录')

    return

  }

  const selected = dataSource.value.find((item) => item.id === selectedRowKeys.value[0])

  if (!selected) {

    message.warning('所选记录无效，请重新选择')

    return

  }

  confirmSelect(selected)

}



function customRow(record: MaterialLedger) {

  const zeroStock = props.outboundMode && (record.stockQuantity ?? 0) <= 0

  return {

    class: zeroStock ? 'picker-row-zero-stock' : '',

    onDblclick: () => {

      selectedRowKeys.value = [record.id]

      confirmSelect(record)

    },

  }

}



watch(

  () => props.open,

  async (open) => {

    if (open) {

      selectedRowKeys.value = []

      resetQueryForm()

      pagination.current = 1

      try {

        await reloadFilterOptions()

      } catch {

        message.warning('筛选选项加载失败')

      }

      await loadData()

    }

  },

)

</script>



<template>

  <a-modal

    :open="open"

    title="从台账选择"

    width="1100px"

    destroy-on-close

    @cancel="handleClose"

  >

    <WarehouseMaterialFilterPanel

      class="picker-filter"

      variant="compact"

      :query-form="queryForm"

      :filter-options="filterOptions"

      @category-change="handleCategoryChange"

      @generic-name-change="handleGenericNameChange"

      @brand-change="handleBrandChange"

      @search="handleSearch"

    >

      <template #actions>
        <a-space>
          <a-button type="primary" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            查询
          </a-button>
          <a-button @click="handleReset">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
        </a-space>
      </template>

    </WarehouseMaterialFilterPanel>



    <a-table

      :columns="columns"

      :data-source="dataSource"

      :loading="loading"

      :pagination="pagination"

      :row-key="(record: MaterialLedger) => record.id"

      :row-selection="rowSelection"

      :custom-row="customRow"

      size="small"

      :scroll="{ x: 900, y: 320 }"

      class="picker-table"

      @change="handleTableChange"

    >

      <template #bodyCell="{ column, record, index }">

        <template v-if="column.key === 'index'">

          {{ getTableRowIndex(index, pagination) }}

        </template>

        <template v-else-if="column.key === 'brand'">

          {{ displayValue(record.brand) }}

        </template>

        <template v-else-if="column.key === 'stockQuantity'">

          <span :class="{ 'zero-stock': outboundMode && (record.stockQuantity ?? 0) <= 0 }">

            {{ displayValue(record.stockQuantity) }}

            <template v-if="outboundMode && (record.stockQuantity ?? 0) <= 0">（无库存）</template>

          </span>

        </template>

      </template>

    </a-table>



    <p class="picker-hint">提示：双击行可快速选中</p>



    <template #footer>

      <a-button @click="handleClose">取消</a-button>

      <a-button type="primary" @click="handleConfirm">确定</a-button>

    </template>

  </a-modal>

</template>



<style scoped lang="less">
@import '@/styles/variables.less';

.picker-filter {
  margin-bottom: @spacing-md;
  width: 100%;
}

.picker-table {
  margin-top: @spacing-xs;
}

.picker-hint {
  margin: @spacing-sm 0 0;
  font-size: 12px;
  color: @color-text-tertiary;
}

:deep(.picker-row-zero-stock) td {
  color: rgba(0, 0, 0, 0.35);
}

.zero-stock {
  color: rgba(0, 0, 0, 0.35);
}
</style>
