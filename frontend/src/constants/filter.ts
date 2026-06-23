export const ALL_OPTION = '全部'

export function isAllOption(value: string | undefined): boolean {
  return !value || value === ALL_OPTION
}
