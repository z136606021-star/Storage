const KEY = 'storage.login.username'

export function loadRememberedUsername(): string {
  try {
    return localStorage.getItem(KEY) ?? ''
  } catch {
    return ''
  }
}

export function saveRememberedUsername(username: string): void {
  try {
    localStorage.setItem(KEY, username.trim())
  } catch {
    // ignore quota / private mode errors
  }
}

export function clearRememberedUsername(): void {
  try {
    localStorage.removeItem(KEY)
  } catch {
    // ignore
  }
}
