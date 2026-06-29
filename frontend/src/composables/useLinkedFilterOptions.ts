import { ref } from 'vue'
import { ALL_OPTION } from '@/constants/filter'

export interface LinkedFilterOptionsConfig<TQuery extends Record<string, unknown>> {
  queryForm: TQuery
  allOptionValue?: string
}

export function useLinkedFilterOptions<TQuery extends Record<string, string>>(
  config: LinkedFilterOptionsConfig<TQuery>,
) {
  const allOption = config.allOptionValue ?? ALL_OPTION
  const filterOptionsRaw = ref<Record<string, string[]>>({})

  function ensureOptionValue(field: keyof TQuery, options: string[]) {
    const value = config.queryForm[field]
    if (typeof value === 'string' && value !== allOption && !options.includes(value)) {
      config.queryForm[field] = allOption as TQuery[keyof TQuery]
    }
  }

  async function loadFilterOptions<TOptions>(
    fetchFn: (linkageParams: Record<string, string | undefined>) => Promise<TOptions>,
    linkageParams: Record<string, string | undefined>,
    ensureFields: Array<{ field: keyof TQuery; optionsKey: keyof TOptions }>,
    withAllOptionFn: (items: string[]) => string[],
    pickList: (raw: TOptions, key: keyof TOptions) => string[],
  ) {
    const raw = await fetchFn(linkageParams)
    filterOptionsRaw.value = raw as Record<string, string[]>
    for (const { field, optionsKey } of ensureFields) {
      const options = withAllOptionFn(pickList(raw, optionsKey))
      ensureOptionValue(field, options)
    }
  }

  function createCascadeResetHandler(
    fieldsToReset: Array<keyof TQuery>,
    reload: () => Promise<void>,
  ) {
    return async () => {
      for (const field of fieldsToReset) {
        config.queryForm[field] = allOption as TQuery[keyof TQuery]
      }
      await reload()
    }
  }

  function buildLinkageParams(
    fields: Array<{ formKey: keyof TQuery; paramKey: string }>,
  ): Record<string, string | undefined> {
    const params: Record<string, string | undefined> = {}
    for (const { formKey, paramKey } of fields) {
      const value = config.queryForm[formKey]
      if (typeof value === 'string' && value !== allOption) {
        params[paramKey] = value
      }
    }
    return params
  }

  return {
    filterOptionsRaw,
    ensureOptionValue,
    loadFilterOptions,
    createCascadeResetHandler,
    buildLinkageParams,
  }
}
