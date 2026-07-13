const materialLedgerDirty = { value: false }

export function markMaterialLedgerDirty() {
  materialLedgerDirty.value = true
}

export function consumeMaterialLedgerDirty(): boolean {
  if (!materialLedgerDirty.value) {
    return false
  }
  materialLedgerDirty.value = false
  return true
}

export function resetMaterialLedgerDirty() {
  materialLedgerDirty.value = false
}
