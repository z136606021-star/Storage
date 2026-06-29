export function parseMaterialLedgerIdFromQuery(raw: unknown): number | null {
  const id = typeof raw === 'string' ? Number(raw) : NaN
  return Number.isFinite(id) && id > 0 ? id : null
}
