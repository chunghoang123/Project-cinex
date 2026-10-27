import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { authService } from '../../services/authService'

interface ProtectedRouteProps {
  roles: string[]
  children: ReactNode
}

export function ProtectedRoute({ roles, children }: ProtectedRouteProps) {
  const location = useLocation()

  if (!authService.isAuthenticated()) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }

  const allowed = roles.length === 0 || roles.some((role) => authService.hasRole(role))
  if (!allowed) {
    return <Navigate to={authService.homePath()} replace />
  }

  return <>{children}</>
}
