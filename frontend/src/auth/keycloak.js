const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8081'
const REALM = import.meta.env.VITE_KEYCLOAK_REALM || 'college-admin'
const CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'college-admin-web'

const STORAGE_KEY = 'ca_auth'

function decodeJwt(token) {
  try {
    const payload = token.split('.')[1]
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const json = atob(normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), '='))
    return JSON.parse(json)
  } catch {
    return null
  }
}

export function loadStoredAuth() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const data = JSON.parse(raw)
    if (!data?.accessToken) return null
    const claims = decodeJwt(data.accessToken)
    if (!claims || (claims.exp && claims.exp * 1000 < Date.now())) {
      sessionStorage.removeItem(STORAGE_KEY)
      return null
    }
    return { ...data, claims, roles: extractRoles(claims) }
  } catch {
    return null
  }
}

export function extractRoles(claims) {
  const realmRoles = claims?.realm_access?.roles || []
  return realmRoles.filter((r) =>
    [
      'PLATFORM_SUPER_ADMIN',
      'TENANT_ADMIN',
      'ACADEMIC_ADMIN',
      'EXAM_CONTROLLER',
      'FACULTY',
      'HOD',
      'STUDENT',
      'PLACEMENT_OFFICER',
      'RECRUITER',
    ].includes(r),
  )
}

export async function loginWithPassword(username, password) {
  const body = new URLSearchParams({
    grant_type: 'password',
    client_id: CLIENT_ID,
    username,
    password,
  })
  const res = await fetch(`${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || 'Login failed')
  }
  const tokens = await res.json()
  const claims = decodeJwt(tokens.access_token)
  const auth = {
    accessToken: tokens.access_token,
    refreshToken: tokens.refresh_token,
    claims,
    roles: extractRoles(claims),
  }
  sessionStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
    }),
  )
  return auth
}

export function logoutLocal() {
  sessionStorage.removeItem(STORAGE_KEY)
}

export async function refreshAccessToken(refreshToken) {
  const body = new URLSearchParams({
    grant_type: 'refresh_token',
    client_id: CLIENT_ID,
    refresh_token: refreshToken,
  })
  const res = await fetch(`${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })
  if (!res.ok) throw new Error('Session expired')
  const tokens = await res.json()
  const claims = decodeJwt(tokens.access_token)
  const auth = {
    accessToken: tokens.access_token,
    refreshToken: tokens.refresh_token || refreshToken,
    claims,
    roles: extractRoles(claims),
  }
  sessionStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
    }),
  )
  return auth
}
