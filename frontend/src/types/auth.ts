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
