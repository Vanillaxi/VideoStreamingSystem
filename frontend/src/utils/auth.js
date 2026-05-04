const TOKEN_KEY = 'token'
const LEGACY_TOKEN_KEY = 'video_streaming_token'
const USER_KEY = 'video_streaming_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || localStorage.getItem(LEGACY_TOKEN_KEY)
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.removeItem(LEGACY_TOKEN_KEY)
  }
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(LEGACY_TOKEN_KEY)
}

export function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function setStoredUser(user) {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  }
}

export function removeStoredUser() {
  localStorage.removeItem(USER_KEY)
}

export function clearAuth() {
  removeToken()
  removeStoredUser()
}

export function parseJwtPayload(token = getToken()) {
  if (!token || token.split('.').length < 2) return null
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((char) => `%${`00${char.charCodeAt(0).toString(16)}`.slice(-2)}`)
        .join('')
    )
    return JSON.parse(json)
  } catch {
    return null
  }
}

export function getTokenUserId(token = getToken()) {
  const payload = parseJwtPayload(token)
  return payload?.userId || payload?.id || payload?.sub || null
}

export function getTokenRole(token = getToken()) {
  const payload = parseJwtPayload(token)
  return payload?.role ?? null
}
