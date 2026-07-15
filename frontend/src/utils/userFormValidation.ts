import type { Rule } from 'ant-design-vue/es/form'
import type { SysUserSave } from '@/types/system'
import { containsWhitespace, normalizeEmail } from '@/utils/format'

export interface UserFormState {
  username: string
  displayName: string
  email: string
  phone: string
  status: number
  roleIds: number[]
  newPassword: string
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function validateUsername(value: string | undefined | null): string | undefined {
  const raw = value ?? ''
  if (!raw.trim()) {
    return '请输入 NTID'
  }
  if (raw.length < 3 || raw.length > 64) {
    return 'NTID 长度为 3-64 个字符'
  }
  if (containsWhitespace(raw)) {
    return 'NTID 不能包含空格或空白字符'
  }
  return undefined
}

export function validateDisplayName(value: string | undefined | null): string | undefined {
  const raw = value ?? ''
  if (!raw.trim()) {
    return '请输入用户姓名'
  }
  if (raw.length > 64) {
    return '用户姓名不能超过 64 个字符'
  }
  return undefined
}

export function validateEmail(value: string | undefined | null): string | undefined {
  const raw = value ?? ''
  if (!raw.trim()) {
    return undefined
  }
  if (raw.length > 128) {
    return '邮箱不能超过 128 个字符'
  }
  const normalized = normalizeEmail(raw)
  if (normalized && !EMAIL_PATTERN.test(normalized)) {
    return '邮箱格式不正确'
  }
  return undefined
}

export function validatePhone(value: string | undefined | null): string | undefined {
  const raw = value ?? ''
  if (!raw.trim()) {
    return undefined
  }
  if (raw.length > 32) {
    return '手机号不能超过 32 个字符'
  }
  return undefined
}

export function validateRoleIds(value: number[] | undefined | null): string | undefined {
  if (!value?.length) {
    return '请至少选择一个角色'
  }
  return undefined
}

export function validateStatus(value: number | undefined | null): string | undefined {
  if (value !== 0 && value !== 1) {
    return '请选择状态'
  }
  return undefined
}

export function validateNewPassword(
  value: string | undefined | null,
  editing: boolean,
): string | undefined {
  if (!editing) {
    return undefined
  }
  const raw = value ?? ''
  if (!raw) {
    return undefined
  }
  if (raw.length < 6 || raw.length > 64) {
    return '密码长度为 6-64 个字符'
  }
  return undefined
}

export function createUserFormRules(editing: () => boolean): Record<string, Rule[]> {
  const toRule = (validate: (value: unknown) => string | undefined): Rule => ({
    validator: async (_rule, value) => {
      const message = validate(value)
      if (message) {
        throw new Error(message)
      }
    },
    trigger: 'blur',
  })

  return {
    username: [
      { required: true, message: '请输入 NTID', trigger: 'blur' },
      toRule((value) => validateUsername(String(value ?? ''))),
    ],
    displayName: [
      { required: true, message: '请输入用户姓名', trigger: 'blur' },
      toRule((value) => validateDisplayName(String(value ?? ''))),
    ],
    email: [toRule((value) => validateEmail(String(value ?? '')))],
    phone: [toRule((value) => validatePhone(String(value ?? '')))],
    roleIds: [
      {
        validator: async (_rule, value) => {
          const message = validateRoleIds(value as number[] | undefined)
          if (message) {
            throw new Error(message)
          }
        },
        trigger: 'change',
      },
    ],
    status: [
      {
        validator: async (_rule, value) => {
          const message = validateStatus(value as number | undefined)
          if (message) {
            throw new Error(message)
          }
        },
        trigger: 'change',
      },
    ],
    newPassword: [
      {
        validator: async (_rule, value) => {
          const message = validateNewPassword(String(value ?? ''), editing())
          if (message) {
            throw new Error(message)
          }
        },
        trigger: 'blur',
      },
    ],
  }
}

export function buildUserSavePayload(formState: UserFormState): SysUserSave {
  return {
    username: formState.username.trim(),
    displayName: formState.displayName.trim(),
    email: normalizeEmail(formState.email) || undefined,
    phone: formState.phone.trim() || undefined,
    status: formState.status,
    roleIds: [...formState.roleIds],
  }
}

export function shouldBlockUserSubmit(submitting: boolean): boolean {
  return submitting
}
