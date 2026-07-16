import { describe, expect, it } from 'vitest'
import { buildBinCodePreview } from '@/utils/warehouseBin'

describe('buildBinCodePreview', () => {
  it('returns row only when col and level are empty', () => {
    expect(buildBinCodePreview('铁柜', null, null)).toBe('铁柜')
    expect(buildBinCodePreview('  铁柜  ', undefined, undefined)).toBe('铁柜')
  })

  it('returns row-col when only col is provided', () => {
    expect(buildBinCodePreview('A', 2, null)).toBe('A-2')
  })

  it('returns row-col-level when all coordinates are provided', () => {
    expect(buildBinCodePreview('A', 2, 3)).toBe('A-2-3')
  })

  it('returns empty preview when row is missing or level exists without col', () => {
    expect(buildBinCodePreview(null, 2, 3)).toBe('')
    expect(buildBinCodePreview('A', null, 3)).toBe('')
  })
})
