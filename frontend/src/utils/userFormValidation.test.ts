import { describe, expect, it } from 'vitest'
import {
  buildUserSavePayload,
  shouldBlockUserSubmit,
  validateDisplayName,
  validateEmail,
  validateNewPassword,
  validateRoleIds,
  validateUsername,
} from './userFormValidation'

describe('userFormValidation', () => {
  it('allows display names with internal spaces', () => {
    expect(validateDisplayName('Mandy Liu')).toBeUndefined()
    expect(
      buildUserSavePayload({
        username: '2317362',
        displayName: 'Mandy Liu',
        email: 'mandy_liu7362@jabil.com',
        phone: '18820777053',
        status: 1,
        roleIds: [1],
        newPassword: '',
      }),
    ).toEqual({
      username: '2317362',
      displayName: 'Mandy Liu',
      email: 'mandy_liu7362@jabil.com',
      phone: '18820777053',
      status: 1,
      roleIds: [1],
    })
  })

  it('rejects whitespace in NTID', () => {
    expect(validateUsername('123 asd')).toBe('NTID 不能包含空格或空白字符')
  })

  it('rejects empty roles', () => {
    expect(validateRoleIds([])).toBe('请至少选择一个角色')
  })

  it('rejects invalid email format', () => {
    expect(validateEmail('not-an-email')).toBe('邮箱格式不正确')
  })

  it('validates optional edit password length', () => {
    expect(validateNewPassword('', true)).toBeUndefined()
    expect(validateNewPassword('12345', true)).toBe('密码长度为 6-64 个字符')
    expect(validateNewPassword('secret12', true)).toBeUndefined()
    expect(validateNewPassword('secret12', false)).toBeUndefined()
  })

  it('blocks duplicate submit while submitting', () => {
    expect(shouldBlockUserSubmit(true)).toBe(true)
    expect(shouldBlockUserSubmit(false)).toBe(false)
  })
})
