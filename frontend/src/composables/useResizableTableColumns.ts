import type { ColumnType } from 'ant-design-vue/es/table'
import { computed, ref, watch, type MaybeRefOrGetter, toValue } from 'vue'

export const TABLE_COLUMN_WIDTH_STORAGE_PREFIX = 'storage.table-column-widths.v1'
export const DEFAULT_MIN_COLUMN_WIDTH = 64

export type ColumnWidthMap = Record<string, number>

export function buildTableColumnWidthStorageKey(tableKey: string): string {
  return `${TABLE_COLUMN_WIDTH_STORAGE_PREFIX}.${tableKey}`
}

function isPositiveFiniteNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value > 0
}

function readNumericWidth(value: unknown): number | undefined {
  if (typeof value === 'number') {
    return isPositiveFiniteNumber(value) ? value : undefined
  }
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number.parseFloat(value)
    return isPositiveFiniteNumber(parsed) ? parsed : undefined
  }
  return undefined
}

export function sanitizeColumnWidthMap(raw: unknown): ColumnWidthMap {
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) {
    return {}
  }

  const result: ColumnWidthMap = {}
  for (const [key, value] of Object.entries(raw)) {
    if (!key) {
      continue
    }
    const width = readNumericWidth(value)
    if (width) {
      result[key] = Math.round(width)
    }
  }
  return result
}

export function loadStoredColumnWidths(tableKey: string): ColumnWidthMap {
  try {
    const storageKey = buildTableColumnWidthStorageKey(tableKey)
    const raw = globalThis.localStorage?.getItem(storageKey)
    if (!raw) {
      return {}
    }
    return sanitizeColumnWidthMap(JSON.parse(raw))
  } catch {
    return {}
  }
}

export function saveStoredColumnWidths(tableKey: string, widths: ColumnWidthMap): void {
  try {
    const storageKey = buildTableColumnWidthStorageKey(tableKey)
    globalThis.localStorage?.setItem(storageKey, JSON.stringify(widths))
  } catch {
    // ignore quota / private mode errors
  }
}

export function resolveColumnMinWidth(column: ColumnType): number {
  const minWidth = readNumericWidth(column.minWidth)
  return Math.max(DEFAULT_MIN_COLUMN_WIDTH, minWidth ?? DEFAULT_MIN_COLUMN_WIDTH)
}

export function resolveColumnDefaultWidth(column: ColumnType, fallbackWidth = 120): number {
  return (
    readNumericWidth(column.width) ??
    readNumericWidth(column.minWidth) ??
    fallbackWidth
  )
}

export function clampColumnWidth(width: number, column: ColumnType): number {
  const minWidth = resolveColumnMinWidth(column)
  return Math.max(minWidth, Math.round(width))
}

export function getColumnKey(column: ColumnType): string | undefined {
  if (column.key != null && String(column.key).length > 0) {
    return String(column.key)
  }
  if (column.dataIndex != null && String(column.dataIndex).length > 0) {
    return String(column.dataIndex)
  }
  return undefined
}

export function isResizableTableColumn(column: ColumnType): boolean {
  return getColumnKey(column) != null
}

export function applyFrozenColumnWidths(
  existing: ColumnWidthMap,
  frozen: ColumnWidthMap,
  columns: ColumnType[],
): ColumnWidthMap {
  const validKeys = new Set(
    columns.map((column) => getColumnKey(column)).filter((key): key is string => Boolean(key)),
  )
  const next = { ...existing }
  for (const [key, width] of Object.entries(frozen)) {
    if (validKeys.has(key) && isPositiveFiniteNumber(width)) {
      next[key] = Math.round(width)
    }
  }
  return next
}

export function mergeResizableColumns(
  columns: ColumnType[],
  savedWidths: ColumnWidthMap,
): ColumnType[] {
  return columns.map((column) => {
    const columnKey = getColumnKey(column)
    if (!columnKey) {
      return { ...column }
    }

    const savedWidth = savedWidths[columnKey]
    const width = savedWidth ?? resolveColumnDefaultWidth(column)

    return {
      ...column,
      width,
    }
  })
}

export function sumColumnWidths(columns: ColumnType[]): number {
  return columns.reduce((total, column) => {
    const width = readNumericWidth(column.width) ?? resolveColumnDefaultWidth(column)
    return total + width
  }, 0)
}

export function resolveTableScrollX(
  columns: ColumnType[],
  fallbackScrollX?: number | string | boolean,
): number | string | boolean | undefined {
  const totalWidth = sumColumnWidths(columns)
  const fallback =
    typeof fallbackScrollX === 'number'
      ? fallbackScrollX
      : typeof fallbackScrollX === 'string'
        ? Number.parseFloat(fallbackScrollX)
        : undefined

  if (Number.isFinite(fallback) && fallback! > 0) {
    return Math.max(totalWidth, fallback!)
  }

  return totalWidth > 0 ? totalWidth : fallbackScrollX
}

export function pruneStoredColumnWidths(
  columns: ColumnType[],
  savedWidths: ColumnWidthMap,
): ColumnWidthMap {
  const validKeys = new Set(
    columns.map((column) => getColumnKey(column)).filter((key): key is string => Boolean(key)),
  )
  const next: ColumnWidthMap = {}
  for (const [key, width] of Object.entries(savedWidths)) {
    if (validKeys.has(key) && isPositiveFiniteNumber(width)) {
      next[key] = width
    }
  }
  return next
}

export function useResizableTableColumns(
  columns: MaybeRefOrGetter<ColumnType[]>,
  tableKey: MaybeRefOrGetter<string>,
) {
  const savedWidths = ref<ColumnWidthMap>({})

  watch(
    () => toValue(tableKey),
    (nextTableKey) => {
      savedWidths.value = loadStoredColumnWidths(nextTableKey)
    },
    { immediate: true },
  )

  watch(
    () => toValue(columns),
    (nextColumns) => {
      savedWidths.value = pruneStoredColumnWidths(nextColumns, savedWidths.value)
    },
    { deep: true },
  )

  const resizableColumns = computed(() =>
    mergeResizableColumns(toValue(columns), savedWidths.value),
  )

  function freezeColumnWidths(frozen: ColumnWidthMap) {
    savedWidths.value = applyFrozenColumnWidths(
      savedWidths.value,
      frozen,
      toValue(columns),
    )
    saveStoredColumnWidths(toValue(tableKey), savedWidths.value)
  }

  function updateColumnWidth(columnKey: string, width: number) {
    const sourceColumn = toValue(columns).find((column) => getColumnKey(column) === columnKey)
    if (!sourceColumn) {
      return
    }

    const nextWidth = clampColumnWidth(width, sourceColumn)
    savedWidths.value = {
      ...savedWidths.value,
      [columnKey]: nextWidth,
    }
    saveStoredColumnWidths(toValue(tableKey), savedWidths.value)
  }

  function resolveScroll(fallbackScroll?: Record<string, unknown>) {
    const scroll = fallbackScroll ? { ...fallbackScroll } : undefined
    const fallbackX = scroll?.x
    const resolvedX = resolveTableScrollX(
      resizableColumns.value,
      typeof fallbackX === 'number' || typeof fallbackX === 'string' ? fallbackX : undefined,
    )

    if (resolvedX == null) {
      return scroll
    }

    return {
      ...scroll,
      x: resolvedX,
    }
  }

  return {
    resizableColumns,
    freezeColumnWidths,
    updateColumnWidth,
    isResizableTableColumn,
    resolveScroll,
  }
}
