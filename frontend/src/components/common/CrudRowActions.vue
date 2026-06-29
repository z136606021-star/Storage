<script setup lang="ts">
withDefaults(
  defineProps<{
    canWrite?: boolean
    showDelete?: boolean
  }>(),
  {
    canWrite: false,
    showDelete: true,
  },
)

const emit = defineEmits<{
  view: []
  edit: []
  delete: []
}>()
</script>

<template>
  <a-space :size="0">
    <a-button type="link" size="small" class="action-link" @click="emit('view')">查看</a-button>
    <a-button v-if="canWrite" type="link" size="small" class="action-link" @click="emit('edit')">
      编辑
    </a-button>
    <a-button
      v-if="canWrite && showDelete"
      type="link"
      size="small"
      danger
      class="action-link"
      @click="emit('delete')"
    >
      删除
    </a-button>
  </a-space>
</template>
