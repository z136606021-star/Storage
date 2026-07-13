import { describe, expect, it } from 'vitest'
import { containsWhitespace, normalizeEmail } from './format'

describe('normalizeEmail', () => {
  it('returns null for blank input', () => {
    expect(normalizeEmail(null)).toBeNull()
    expect(normalizeEmail(undefined)).toBeNull()
    expect(normalizeEmail('')).toBeNull()
    expect(normalizeEmail('   ')).toBeNull()
  })

  it('trims and lowercases email', () => {
    expect(normalizeEmail('  Bo_Lv@Jabil.COM  ')).toBe('bo_lv@jabil.com')
    expect(normalizeEmail('User@Example.com')).toBe('user@example.com')
  })

  it('detects whitespace', () => {
    expect(containsWhitespace('123 asd')).toBe(true)
    expect(containsWhitespace('abc')).toBe(false)
  })
})
