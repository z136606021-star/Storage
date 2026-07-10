import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ColumnType } from 'ant-design-vue/es/table'
import {
  applyFrozenColumnWidths,
  buildTableColumnWidthStorageKey,
  clampColumnWidth,
  loadStoredColumnWidths,
  mergeResizableColumns,
  pruneStoredColumnWidths,
  resolveColumnDefaultWidth,
  resolveTableScrollX,
  sanitizeColumnWidthMap,
  saveStoredColumnWidths,
  sumColumnWidths,
} from '@/composables/useResizableTableColumns'

const storage = new Map<string, string>()

function stubLocalStorage() {
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => storage.get(key) ?? null,
    setItem: (key: string, value: string) => {
      storage.set(key, value)
    },
    removeItem: (key: string) => {
      storage.delete(key)
    },
  })
}

const sampleColumns: ColumnType[] = [
  { title: '序号', key: 'index', width: 64, align: 'center' },
  { title: '品类', dataIndex: 'category', key: 'category', width: 100, ellipsis: true },
  { title: '备注', key: 'remark', minWidth: 120, ellipsis: true },
  { title: '操作', key: 'action', width: 160, fixed: 'right' },
]

describe('useResizableTableColumns helpers', () => {
  beforeEach(() => {
    storage.clear()
    stubLocalStorage()
  })

  it('builds versioned storage key', () => {
    expect(buildTableColumnWidthStorageKey('warehouse.bom')).toBe(
      'storage.table-column-widths.v1.warehouse.bom',
    )
  })

  it('sanitizes stored width map', () => {
    expect(
      sanitizeColumnWidthMap({
        category: 180,
        remark: '120',
        action: -10,
        invalid: 'abc',
      }),
    ).toEqual({
      category: 180,
      remark: 120,
    })
  })

  it('returns empty map for invalid stored json', () => {
    storage.set(buildTableColumnWidthStorageKey('warehouse.bom'), '{bad-json')
    expect(loadStoredColumnWidths('warehouse.bom')).toEqual({})
  })

  it('freezes rendered widths for all known business columns', () => {
    const frozen = applyFrozenColumnWidths(
      { category: 100 },
      {
        index: 64,
        category: 180,
        remark: 240,
        action: 160,
        unknown: 99,
      },
      sampleColumns,
    )

    expect(frozen).toEqual({
      category: 180,
      index: 64,
      remark: 240,
      action: 160,
    })
  })

  it('keeps only target column changed after freeze and single update flow', () => {
    const frozen = applyFrozenColumnWidths({}, {
      index: 64,
      category: 150,
      remark: 200,
      action: 160,
    }, sampleColumns)

    const afterTargetResize: Record<string, number> = {
      ...frozen,
      category: clampColumnWidth(220, sampleColumns[1]!),
    }

    expect(afterTargetResize.index).toBe(64)
    expect(afterTargetResize.remark).toBe(200)
    expect(afterTargetResize.action).toBe(160)
    expect(afterTargetResize.category).toBe(220)
  })

  it('merges saved widths by column key without mutating source columns', () => {
    const saved = { category: 180, remark: 220 }
    const merged = mergeResizableColumns(sampleColumns, saved)

    expect(merged[1].width).toBe(180)
    expect(merged[2].width).toBe(220)
    expect(merged[3].fixed).toBe('right')
    expect(sampleColumns[1].width).toBe(100)
  })

  it('uses default width for columns without width', () => {
    const merged = mergeResizableColumns(sampleColumns, {})
    expect(merged[2].width).toBe(resolveColumnDefaultWidth(sampleColumns[2]!))
  })

  it('clamps width by column minWidth', () => {
    expect(clampColumnWidth(40, sampleColumns[2]!)).toBe(120)
    expect(clampColumnWidth(300, sampleColumns[2]!)).toBe(300)
  })

  it('prunes stale keys when columns change', () => {
    const pruned = pruneStoredColumnWidths(sampleColumns, {
      category: 180,
      removed: 100,
    })
    expect(pruned).toEqual({ category: 180 })
  })

  it('persists widths per table key independently', () => {
    saveStoredColumnWidths('warehouse.bom', { category: 180 })
    saveStoredColumnWidths('warehouse.bin', { category: 140 })

    expect(loadStoredColumnWidths('warehouse.bom')).toEqual({ category: 180 })
    expect(loadStoredColumnWidths('warehouse.bin')).toEqual({ category: 140 })
  })

  it('ignores localStorage setItem failures', () => {
    vi.stubGlobal('localStorage', {
      getItem: () => null,
      setItem: () => {
        throw new Error('quota exceeded')
      },
      removeItem: () => undefined,
    })

    expect(() => saveStoredColumnWidths('warehouse.bom', { category: 180 })).not.toThrow()
  })

  it('resolves scroll x from merged column widths and fallback', () => {
    const merged = mergeResizableColumns(sampleColumns, { category: 220 })
    const total = sumColumnWidths(merged)
    expect(resolveTableScrollX(merged, 1080)).toBe(Math.max(total, 1080))
    expect(resolveTableScrollX(merged)).toBe(total)
  })
})
