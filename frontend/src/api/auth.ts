import { http } from '@/api/http'
import type { AuthSession, ForgotPasswordRequest, LoginRequest } from '@/types/auth'
import type { RegisterRequest } from '@/types/system'

export function login(data: LoginRequest) {
  return http.post<AuthSession>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return http.post<AuthSession>('/auth/register', data)
}

export function logout() {
  return http.post<void>('/auth/logout')
}

export function fetchMe() {
  return http.get<AuthSession>('/auth/me')
}

export function forgotPassword(data: ForgotPasswordRequest) {
  return http.post<void>('/auth/forgot-password', data)
}
