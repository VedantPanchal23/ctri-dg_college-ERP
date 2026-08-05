import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import { loadStoredAuth, loginWithPassword, logoutLocal, refreshAccessToken } from './keycloak'
import { hasAnyRole } from './roles'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => loadStoredAuth())
  const [profile, setProfile] = useState(null)
  const [tenant, setTenant] = useState(null)

  const login = useCallback(async (username, password) => {
    const next = await loginWithPassword(username, password)
    setAuth(next)
    return next
  }, [])

  const logout = useCallback(() => {
    logoutLocal()
    setAuth(null)
    setProfile(null)
    setTenant(null)
  }, [])

  const refresh = useCallback(async () => {
    if (!auth?.refreshToken) throw new Error('No refresh token')
    const next = await refreshAccessToken(auth.refreshToken)
    setAuth(next)
    return next
  }, [auth])

  const value = useMemo(
    () => ({
      auth,
      token: auth?.accessToken || null,
      roles: auth?.roles || [],
      claims: auth?.claims || null,
      profile,
      tenant,
      setProfile,
      setTenant,
      login,
      logout,
      refresh,
      can: (allowed) => hasAnyRole(auth?.roles || [], allowed),
      isAuthenticated: Boolean(auth?.accessToken),
    }),
    [auth, profile, tenant, login, logout, refresh],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
