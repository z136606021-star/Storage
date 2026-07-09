export function buildBinCodePreview(
  rowNo?: number | null,
  colNo?: number | null,
  levelNo?: number | null,
): string {
  if (rowNo == null || rowNo < 1) {
    return ''
  }
  if (levelNo != null && levelNo >= 1) {
    if (colNo == null || colNo < 1) {
      return ''
    }
    return `${rowNo}-${colNo}-${levelNo}`
  }
  if (colNo != null && colNo >= 1) {
    return `${rowNo}-${colNo}`
  }
  return String(rowNo)
}

export function normalizeBinCoordinate(value?: number | null): number | null | undefined {
  if (value == null) {
    return null
  }
  return value
}
