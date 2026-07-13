import { http } from '@/api/http'
import type {
  AuthSession,
  AuthUser,
  ChangePasswordByCurrentPasswordRequest,
  ChangePasswordByVerificationCodeRequest,
  ForgotPasswordRequest,
  LoginRequest,
  ResetPasswordRequest,
  SendRegistrationVerificationCodeRequest,
  UpdateCurrentUserPhoneRequest,
} from '@/types/auth'
import type { RegisterRequest } from '@/types/system'

export function login(data: LoginRequest) {
  return http.post<AuthSession>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return http.post<AuthSession>('/auth/register', data)
}

export function sendRegistrationVerificationCode(data: SendRegistrationVerificationCodeRequest) {
  return http.post<void>('/auth/register/verification-code', data)
}

export function logout() {
  return http.post<void>('/auth/logout')
}

export function fetchMe() {
  return http.get<AuthSession>('/auth/me')
}

export function updateMyPhone(data: UpdateCurrentUserPhoneRequest) {
  return http.put<AuthUser>('/auth/me/phone', data)
}

export function forgotPassword(data: ForgotPasswordRequest) {
  return http.post<void>('/auth/forgot-password', data)
}

export function resetPassword(data: ResetPasswordRequest) {
  return http.post<void>('/auth/reset-password', data)
}

export function changePasswordByCurrentPassword(data: ChangePasswordByCurrentPasswordRequest) {
  return http.put<void>('/auth/password', data)
}

export function sendPasswordVerificationCode() {
  return http.post<void>('/auth/password/verification-code')
}

export function changePasswordByVerificationCode(data: ChangePasswordByVerificationCodeRequest) {
  return http.put<void>('/auth/password/by-verification-code', data)
}
