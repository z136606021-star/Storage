import type { IoType, MaterialIoRecord } from '@/types/materialIo'

import { formatPurposeLabel } from '@/constants/materialIoPurpose'

export function formatOperator(record: MaterialIoRecord): string {
  return record.operatorDisplayName || record.operatorUsername || '-'
}

export function formatIoTypeLabel(ioType: string): string {
  if (ioType === 'IN') return '入库'
  if (ioType === 'OUT') return '出库'
  return ioType
}

export function getIoTypeTagColor(ioType: IoType | string): string {
  if (ioType === 'IN') return 'success'
  if (ioType === 'OUT') return 'error'
  return 'default'
}

export { formatPurposeLabel }
