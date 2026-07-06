import { computed, h, ref, watch, type Ref } from 'vue'
import { Checkbox, Modal } from 'ant-design-vue'
import { fetchMaterialIoSafetyHints } from '@/api/warehouse/materialIo'
import { availableStockForRow, type MaterialIoStockContext } from '@/composables/useMaterialIoStock'
import type { MaterialIoFormRow, MaterialIoSafetyHint } from '@/types/warehouse/materialIo'

const SAFETY_CONFIRM_SKIP_KEY = 'material-io-safety-confirm-skip'

export function shouldSkipSafetyConfirm(): boolean {
  return sessionStorage.getItem(SAFETY_CONFIRM_SKIP_KEY) === '1'
}

export function setSkipSafetyConfirm(skip: boolean) {
  if (skip) {
    sessionStorage.setItem(SAFETY_CONFIRM_SKIP_KEY, '1')
  } else {
    sessionStorage.removeItem(SAFETY_CONFIRM_SKIP_KEY)
  }
}

export function confirmSafetyStockSubmit(onOk: () => void | Promise<void>) {
  if (shouldSkipSafetyConfirm()) {
    return Promise.resolve(onOk())
  }

  const skip = ref(false)
  return new Promise<void>((resolve, reject) => {
    Modal.confirm({
      title: '安全库存预警',
      content: () =>
        h('div', { class: 'safety-confirm-body' }, [
          h('p', { style: 'margin-bottom: 12px' }, '部分物料出库后将低于安全库存，是否继续提交？'),
          h(
            Checkbox,
            {
              checked: skip.value,
              'onUpdate:checked': (value: boolean) => {
                skip.value = value
              },
            },
            () => '本次会话不再提示',
          ),
        ]),
      okText: '继续提交',
      cancelText: '返回修改',
      onOk: async () => {
        if (skip.value) {
          setSkipSafetyConfirm(true)
        }
        await onOk()
        resolve()
      },
      onCancel: () => {
        reject(new Error('cancelled'))
      },
    })
  })
}

export function projectedStockAfterOutbound(
  row: MaterialIoFormRow,
  rowIndex: number,
  context: MaterialIoStockContext,
): number | null {
  const available = availableStockForRow(row, rowIndex, context)
  if (available == null || row.quantity == null) {
    return null
  }
  return available - row.quantity
}

export function wouldTriggerSafetyWarning(
  hint: MaterialIoSafetyHint | undefined,
  projectedStock: number | null,
): boolean {
  if (!hint?.warningEnabled || hint.safetyQuantity == null || projectedStock == null) {
    return false
  }
  return projectedStock < hint.safetyQuantity
}

export function useMaterialIoSafetyHint(
  rows: Ref<MaterialIoFormRow[]>,
  stockContext: MaterialIoStockContext,
  enabled: Ref<boolean>,
) {
  const hints = ref<Map<number, MaterialIoSafetyHint>>(new Map())
  const loading = ref(false)

  async function refreshHints() {
    if (!enabled.value) {
      hints.value = new Map()
      return
    }
    const ledgerIds = rows.value
      .map((row) => row.materialLedgerId)
      .filter((id): id is number => id != null)
    const uniqueIds = [...new Set(ledgerIds)]
    if (uniqueIds.length === 0) {
      hints.value = new Map()
      return
    }
    loading.value = true
    try {
      const result = await fetchMaterialIoSafetyHints(uniqueIds)
      hints.value = new Map(result.map((item) => [item.materialLedgerId, item]))
    } catch {
      hints.value = new Map()
    } finally {
      loading.value = false
    }
  }

  let debounceTimer: ReturnType<typeof setTimeout> | undefined

  function scheduleRefresh() {
    if (debounceTimer) {
      clearTimeout(debounceTimer)
    }
    debounceTimer = setTimeout(() => {
      void refreshHints()
    }, 200)
  }

  watch([rows, enabled], scheduleRefresh, { deep: true })

  const warningRowIndexes = computed(() => {
    if (!enabled.value) {
      return new Set<number>()
    }
    const indexes = new Set<number>()
    rows.value.forEach((row, index) => {
      if (!row.materialLedgerId) {
        return
      }
      const projected = projectedStockAfterOutbound(row, index, stockContext)
      const hint = hints.value.get(row.materialLedgerId)
      if (wouldTriggerSafetyWarning(hint, projected)) {
        indexes.add(index)
      }
    })
    return indexes
  })

  const hasWarnings = computed(() => warningRowIndexes.value.size > 0)

  function warningMessageForRow(rowIndex: number): string | null {
    const row = rows.value[rowIndex]
    if (!row?.materialLedgerId) {
      return null
    }
    const hint = hints.value.get(row.materialLedgerId)
    const projected = projectedStockAfterOutbound(row, rowIndex, stockContext)
    if (!wouldTriggerSafetyWarning(hint, projected) || hint?.safetyQuantity == null) {
      return null
    }
    return `出库后将低于安全库存（${hint.safetyQuantity}）`
  }

  return {
    hints,
    loading,
    warningRowIndexes,
    hasWarnings,
    warningMessageForRow,
    refreshHints,
  }
}
