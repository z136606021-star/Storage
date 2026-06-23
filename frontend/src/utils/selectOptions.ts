export interface SelectOption {
  label: string
  value: string
}

export function toSelectOptions(
  values: string[],
  options?: { excludeAll?: boolean; allLabel?: string },
): SelectOption[] {
  const allLabel = options?.allLabel ?? '全部'
  const filtered = options?.excludeAll
    ? values.filter((value) => value !== allLabel)
    : values
  return filtered.map((value) => ({ label: value, value }))
}

export function withAllOption(values: string[], allLabel = '全部'): string[] {
  return [allLabel, ...values]
}
