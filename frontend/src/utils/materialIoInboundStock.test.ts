import { describe, expect, it } from 'vitest'
import { applyInboundStockOption, findInboundStockOption } from './materialIoInboundStock'

const options = [
  { materialLedgerId: 1, binLocation: 'A-01', model: 'M1', stockQuantity: 8 },
  { materialLedgerId: 2, binLocation: 'B-02', model: '', stockQuantity: 0 },
]

describe('materialIoInboundStock', () => {
  it('matches normalized Bin and model combinations', () => {
    expect(findInboundStockOption(options, ' A-01 ', ' M1 ')?.materialLedgerId).toBe(1)
    expect(findInboundStockOption(options, 'B-02', null)?.stockQuantity).toBe(0)
  })

  it('falls back to zero stock when no candidate matches', () => {
    expect(applyInboundStockOption(undefined)).toEqual({
      materialLedgerId: undefined,
      stockQuantity: 0,
    })
  })
})
