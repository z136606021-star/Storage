import { describe, expect, it } from 'vitest'
import { parseIoRecordIdFromQuery } from '@/utils/materialIoRouteQuery'

describe('parseIoRecordIdFromQuery', () => {
  it('returns null for undefined', () => {
    expect(parseIoRecordIdFromQuery(undefined)).toBeNull()
  })

  it('returns null for empty string', () => {
    expect(parseIoRecordIdFromQuery('')).toBeNull()
  })

  it('returns null for non-numeric string', () => {
    expect(parseIoRecordIdFromQuery('abc')).toBeNull()
  })

  it('returns null for zero or negative numbers', () => {
    expect(parseIoRecordIdFromQuery('0')).toBeNull()
    expect(parseIoRecordIdFromQuery('-1')).toBeNull()
  })

  it('returns positive integer id', () => {
    expect(parseIoRecordIdFromQuery('42')).toBe(42)
  })

  it('returns null for decimal numbers', () => {
    expect(parseIoRecordIdFromQuery('1.5')).toBeNull()
  })
})
