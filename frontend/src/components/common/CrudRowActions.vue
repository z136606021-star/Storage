<script setup lang="ts">
withDefaults(
  defineProps<{
    canWrite?: boolean
    canEdit?: boolean
    canDelete?: boolean
    showDelete?: boolean
    showView?: boolean
  }>(),
  {
    canWrite: false,
    canEdit: undefined,
    canDelete: undefined,
    showDelete: true,
    showView: true,
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
    <a-button
      v-if="showView"
      type="link"
      size="small"
      class="action-link"
      @click="emit('view')"
    >
      查看
    </a-button>
    <a-button v-if="canEdit ?? canWrite" type="link" size="small" class="action-link" @click="emit('edit')">
      编辑
    </a-button>
    <a-button
      v-if="(canDelete ?? canWrite) && showDelete"
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
