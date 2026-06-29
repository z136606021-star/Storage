<script setup lang="ts">
withDefaults(
  defineProps<{
    open: boolean
    title: string
    width?: number
    loading?: boolean
    showEdit?: boolean
  }>(),
  {
    width: 420,
    loading: false,
    showEdit: false,
  },
)

const emit = defineEmits<{
  'update:open': [value: boolean]
  close: []
  edit: []
}>()

function handleClose() {
  emit('update:open', false)
  emit('close')
}
</script>

<template>
  <a-drawer
    :open="open"
    :title="title"
    :width="width"
    placement="right"
    destroy-on-close
    @update:open="emit('update:open', $event)"
    @close="handleClose"
  >
    <template v-if="showEdit" #extra>
      <a-button type="primary" size="small" @click="emit('edit')">编辑</a-button>
    </template>
    <a-spin :spinning="loading">
      <slot />
    </a-spin>
  </a-drawer>
</template>

<style scoped>
:deep(.detail-block + .detail-block) {
  margin-top: 16px;
}

:deep(.detail-block .ant-descriptions-header) {
  margin-bottom: 8px;
}

:deep(.detail-block .ant-descriptions-item-label) {
  width: 96px;
  background: #fafafa;
}
</style>
