export function buildBinCodePreview(
  rowNo?: string | null,
  colNo?: number | null,
  levelNo?: number | null,
): string {
  const normalizedRow = rowNo?.trim()
  if (!normalizedRow) {
    return ''
  }
  if (levelNo != null && levelNo >= 1) {
    if (colNo == null || colNo < 1) {
      return ''
    }
    return `${normalizedRow}-${colNo}-${levelNo}`
  }
  if (colNo != null && colNo >= 1) {
    return `${normalizedRow}-${colNo}`
  }
  return normalizedRow
}

export function normalizeBinCoordinate(value?: number | null): number | null | undefined {
  if (value == null) {
    return null
  }
  return value
}
