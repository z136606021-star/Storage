import { describe, expect, it } from 'vitest'
import { buildBinCodePreview } from '@/utils/warehouseBin'

describe('buildBinCodePreview', () => {
  it('returns row only when col and level are empty', () => {
    expect(buildBinCodePreview(1, null, null)).toBe('1')
    expect(buildBinCodePreview(1, undefined, undefined)).toBe('1')
  })

  it('returns row-col when only col is provided', () => {
    expect(buildBinCodePreview(1, 2, null)).toBe('1-2')
  })

  it('returns row-col-level when all coordinates are provided', () => {
    expect(buildBinCodePreview(1, 2, 3)).toBe('1-2-3')
  })

  it('returns empty preview when row is missing or level exists without col', () => {
    expect(buildBinCodePreview(null, 2, 3)).toBe('')
    expect(buildBinCodePreview(1, null, 3)).toBe('')
  })
})
