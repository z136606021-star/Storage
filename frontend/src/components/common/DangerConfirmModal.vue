<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    open: boolean
    title: string
    description: string
    confirmText: string
    loading?: boolean
  }>(),
  { loading: false },
)

const emit = defineEmits<{
  'update:open': [value: boolean]
  confirm: []
}>()

const inputValue = ref('')
const canConfirm = computed(() => !props.loading && inputValue.value === props.confirmText)

watch(
  () => props.open,
  (open) => {
    if (open) inputValue.value = ''
  },
)

function confirm() {
  if (canConfirm.value) emit('confirm')
}

function close() {
  if (!props.loading) emit('update:open', false)
}
</script>

<template>
  <a-modal
    :open="open"
    :title="title"
    :confirm-loading="loading"
    :ok-button-props="{ danger: true, disabled: !canConfirm || loading }"
    ok-text="确认全部删除"
    cancel-text="取消"
    :mask-closable="false"
    @cancel="close"
    @ok="confirm"
  >
    <a-alert type="error" show-icon :message="description" />
    <p class="danger-confirm-hint">请输入“{{ confirmText }}”以确认此不可撤销操作：</p>
    <a-input
      v-model:value="inputValue"
      :placeholder="confirmText"
      :disabled="loading"
      @press-enter="confirm"
    />
  </a-modal>
</template>

<style scoped lang="less">
.danger-confirm-hint {
  margin: 16px 0 8px;
}
</style>
