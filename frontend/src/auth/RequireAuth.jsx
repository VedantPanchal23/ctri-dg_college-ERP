import { Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { hasAnyRole } from '../auth/roles'

export function RequireAuth({ children }) {
  const { isAuthenticated } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return children
}

export function RequireRoles({ roles, children }) {
  const { roles: userRoles } = useAuth()
  if (!hasAnyRole(userRoles, roles)) {
    return <Navigate to="/app/forbidden" replace />
  }
  return children
}
