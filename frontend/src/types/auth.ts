export interface AuthUser {
  id: number
  username: string
  displayName: string
  maskedEmail?: string | null
}

export interface AuthSession {
  user: AuthUser
  roles: string[]
  permissions: string[]
  accessToken?: string | null
}

export interface LoginRequest {
  username: string
  password: string
  rememberMe?: boolean
}

export interface ForgotPasswordRequest {
  username: string
  email: string
}

export interface ResetPasswordRequest {
  token: string
  newPassword: string
}

export interface ChangePasswordByCurrentPasswordRequest {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export interface ChangePasswordByVerificationCodeRequest {
  verificationCode: string
  newPassword: string
  confirmPassword: string
}
