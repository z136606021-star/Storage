import type { MaterialIoInboundStockOption } from '@/types/warehouse/materialIo'

export function inboundStockOptionValue(option: MaterialIoInboundStockOption) {
  return String(option.materialLedgerId)
}

export function findInboundStockOption(
  options: MaterialIoInboundStockOption[] | undefined,
  binLocation?: string,
  model?: string | null,
) {
  const normalizedBin = binLocation?.trim() ?? ''
  const normalizedModel = model?.trim() ?? ''
  return options?.find(
    (option) => option.binLocation.trim() === normalizedBin
      && (option.model?.trim() ?? '') === normalizedModel,
  )
}

export function applyInboundStockOption(
  option: MaterialIoInboundStockOption | undefined,
) {
  return {
    materialLedgerId: option?.materialLedgerId,
    stockQuantity: option?.stockQuantity ?? 0,
  }
}
