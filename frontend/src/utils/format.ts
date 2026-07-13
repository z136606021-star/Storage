import dayjs from 'dayjs'

export function formatUnitPrice(value: number | null | undefined): string {
  return value == null ? '' : value.toFixed(2)
}

export function formatDateTime(value?: string | null): string {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'
}

export function displayValue(value: string | number | null | undefined): string {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return String(value)
}

/** Trim and lowercase email for API submission; blank input becomes null. */
export function normalizeEmail(value: string | null | undefined): string | null {
  if (value == null) {
    return null
  }
  const trimmed = value.trim()
  return trimmed ? trimmed.toLowerCase() : null
}

export function containsWhitespace(value: string | null | undefined): boolean {
  if (value == null || value === '') {
    return false
  }
  return /\s/u.test(value)
}
