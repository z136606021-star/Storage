export function parseIoRecordIdFromQuery(raw: unknown): number | null {
  if (typeof raw !== 'string') {
    return null
  }
  const trimmed = raw.trim()
  if (!/^\d+$/.test(trimmed)) {
    return null
  }
  const id = Number(trimmed)
  return Number.isFinite(id) && id > 0 ? id : null
}
