export interface AuthUser {
  id: number
  username: string
  displayName: string
}

export interface AuthSession {
  user: AuthUser
  roles: string[]
  permissions: string[]
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
