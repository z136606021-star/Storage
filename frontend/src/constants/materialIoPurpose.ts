export const MATERIAL_IO_PURPOSE_OPTIONS = [
  { value: 'EMPLOYEE_PICKUP', label: '员工领用' },
  { value: 'MACHINING', label: '机加工用' },
  { value: 'PROJECT_USE', label: '项目领用' },
  { value: 'RETURN_IN', label: '退库入库' },
  { value: 'PROCUREMENT', label: '采购入库' },
  { value: 'ADJUSTMENT', label: '盘点调整' },
  { value: 'OTHER', label: '其他' },
] as const

export type MaterialIoPurposeCode = (typeof MATERIAL_IO_PURPOSE_OPTIONS)[number]['value']

const PURPOSE_LABEL_MAP = Object.fromEntries(
  MATERIAL_IO_PURPOSE_OPTIONS.map((item) => [item.value, item.label]),
) as Record<MaterialIoPurposeCode, string>

export function formatPurposeLabel(
  purpose: string | null | undefined,
  purposeLabel?: string | null,
): string {
  if (purposeLabel) {
    return purposeLabel
  }
  if (!purpose) {
    return ''
  }
  return PURPOSE_LABEL_MAP[purpose as MaterialIoPurposeCode] ?? purpose
}

export function outboundPurposeOptions() {
  return MATERIAL_IO_PURPOSE_OPTIONS.filter((item) => item.value !== 'RETURN_IN' && item.value !== 'PROCUREMENT')
}

export function inboundPurposeOptions() {
  return MATERIAL_IO_PURPOSE_OPTIONS.filter((item) =>
    ['RETURN_IN', 'PROCUREMENT', 'ADJUSTMENT', 'OTHER'].includes(item.value),
  )
}
