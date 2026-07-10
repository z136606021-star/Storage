<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import type { ColumnWidthMap } from '@/composables/useResizableTableColumns'

const props = withDefaults(
  defineProps<{
    title?: unknown
    columnKey: string
    resizable?: boolean
  }>(),
  {
    resizable: true,
  },
)

const emit = defineEmits<{
  resizeStart: [columnKey: string, renderedWidths: ColumnWidthMap]
  resize: [columnKey: string, width: number]
}>()

const headerRef = ref<HTMLElement | null>(null)
const dragging = ref(false)

let startX = 0
let startWidth = 0
let activeColumnKey = ''

function resolveHeaderCellElement(): HTMLElement | null {
  const current = headerRef.value
  if (!current) {
    return null
  }
  return current.closest('th') ?? current.parentElement
}

function collectRenderedColumnWidths(headerCell: HTMLElement): ColumnWidthMap {
  const row = headerCell.closest('tr')
  if (!row) {
    return {}
  }

  const renderedWidths: ColumnWidthMap = {}
  row.querySelectorAll<HTMLElement>('[data-column-key]').forEach((element) => {
    const key = element.dataset.columnKey
    const cell = element.closest('th')
    if (!key || !cell) {
      return
    }
    renderedWidths[key] = Math.round(cell.getBoundingClientRect().width)
  })
  return renderedWidths
}

function handleResizeStart(event: MouseEvent) {
  if (!props.resizable) {
    return
  }

  event.preventDefault()
  event.stopPropagation()

  const headerCell = resolveHeaderCellElement()
  if (!headerCell) {
    return
  }

  dragging.value = true
  startX = event.clientX
  startWidth = headerCell.getBoundingClientRect().width
  activeColumnKey = props.columnKey

  emit('resizeStart', activeColumnKey, collectRenderedColumnWidths(headerCell))

  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', handleResizeMove)
  document.addEventListener('mouseup', handleResizeEnd)
}

function handleResizeMove(event: MouseEvent) {
  if (!dragging.value) {
    return
  }

  const nextWidth = startWidth + (event.clientX - startX)
  emit('resize', activeColumnKey, nextWidth)
}

function handleResizeEnd() {
  if (!dragging.value) {
    return
  }

  dragging.value = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  document.removeEventListener('mousemove', handleResizeMove)
  document.removeEventListener('mouseup', handleResizeEnd)
}

onBeforeUnmount(() => {
  handleResizeEnd()
})
</script>

<template>
  <div
    ref="headerRef"
    class="resizable-table-header-cell"
    :data-column-key="columnKey"
  >
    <span class="resizable-table-header-cell__title">
      <slot>{{ title }}</slot>
    </span>
    <span
      v-if="resizable"
      class="resizable-table-header-cell__handle"
      aria-hidden="true"
      @mousedown="handleResizeStart"
    />
  </div>
</template>

<style scoped lang="less">
@import '@/styles/variables.less';

.resizable-table-header-cell {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  padding-right: 8px;
}

.resizable-table-header-cell__title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resizable-table-header-cell__handle {
  position: absolute;
  top: 0;
  right: -4px;
  z-index: 1;
  width: 8px;
  height: 100%;
  cursor: col-resize;
  touch-action: none;
}

.resizable-table-header-cell__handle::after {
  content: '';
  position: absolute;
  top: 20%;
  right: 3px;
  width: 1px;
  height: 60%;
  background: @color-border;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.resizable-table-header-cell:hover .resizable-table-header-cell__handle::after,
.resizable-table-header-cell__handle:hover::after {
  opacity: 1;
}
</style>
