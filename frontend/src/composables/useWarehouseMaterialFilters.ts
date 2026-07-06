import { computed, type UnwrapNestedRefs } from 'vue'
import { useLinkedFilterOptions } from '@/composables/useLinkedFilterOptions'
import { ALL_OPTION } from '@/constants/filter'
import type { FilterOptions } from '@/types/warehouse/materialLedger'
import { withAllOption } from '@/utils/selectOptions'

export interface WarehouseMaterialQuery {
  category: string
  genericName: string
  brand: string
  name: string
  model: string
  binLocation: string
}

export function defaultMaterialQuery(): WarehouseMaterialQuery {
  return {
    category: ALL_OPTION,
    genericName: ALL_OPTION,
    brand: ALL_OPTION,
    name: '',
    model: ALL_OPTION,
    binLocation: ALL_OPTION,
  }
}

export function assignDefaultMaterialFields<T extends WarehouseMaterialQuery>(queryForm: T) {
  Object.assign(queryForm, defaultMaterialQuery())
}

const linkageFields = [
  { formKey: 'category' as const, paramKey: 'category' },
  { formKey: 'genericName' as const, paramKey: 'genericName' },
  { formKey: 'brand' as const, paramKey: 'brand' },
]

const ensureFields = [
  { field: 'genericName' as const, optionsKey: 'genericNames' as const },
  { field: 'brand' as const, optionsKey: 'brands' as const },
  { field: 'model' as const, optionsKey: 'models' as const },
  { field: 'binLocation' as const, optionsKey: 'binLocations' as const },
]

export function useWarehouseMaterialFilters<T extends WarehouseMaterialQuery>(
  queryForm: UnwrapNestedRefs<T> | T,
  fetchOptionsFn: (linkageParams: Record<string, string | undefined>) => Promise<FilterOptions>,
) {
  const { filterOptionsRaw, loadFilterOptions, createCascadeResetHandler, buildLinkageParams } =
    useLinkedFilterOptions({ queryForm: queryForm as T & Record<string, string> })

  const filterOptions = computed(() => ({
    category: withAllOption(filterOptionsRaw.value.categories ?? []),
    genericName: withAllOption(filterOptionsRaw.value.genericNames ?? []),
    brand: withAllOption(filterOptionsRaw.value.brands ?? []),
    model: withAllOption(filterOptionsRaw.value.models ?? []),
    binLocation: withAllOption(filterOptionsRaw.value.binLocations ?? []),
  }))

  async function reloadFilterOptions() {
    await loadFilterOptions(
      fetchOptionsFn,
      buildLinkageParams(linkageFields),
      ensureFields,
      withAllOption,
      (raw, key) => raw[key] ?? [],
    )
  }

  const handleCategoryChange = createCascadeResetHandler(
    ['genericName', 'brand', 'model'],
    reloadFilterOptions,
  )
  const handleGenericNameChange = createCascadeResetHandler(['brand', 'model'], reloadFilterOptions)
  const handleBrandChange = createCascadeResetHandler(['model'], reloadFilterOptions)

  return {
    filterOptions,
    reloadFilterOptions,
    handleCategoryChange,
    handleGenericNameChange,
    handleBrandChange,
  }
}
