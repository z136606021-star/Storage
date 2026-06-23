import axios from 'axios'

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

export function getErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && error.response?.data?.message) {
    return String(error.response.data.message)
  }
  return fallback
}
