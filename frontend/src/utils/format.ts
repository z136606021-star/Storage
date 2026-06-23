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
