import api from '../api/axios'
import { AuthResponse, User } from '../types/User'

const SESSION_KEY = 'cinex_session'

function saveSession(data: AuthResponse) {
  localStorage.setItem('token', data.token)
  localStorage.setItem(SESSION_KEY, JSON.stringify(data))
}

export const authService = {
  login: async (username: string, password: string): Promise<AuthResponse> => {
    const res = await api.post<{ success: boolean; message: string; data: AuthResponse }>(
      '/auth/login', { username, password }
    )
    saveSession(res.data.data)
    return res.data.data
  },
  register: async (data: {
    username: string; email: string; password: string; fullName?: string; phone?: string
  }): Promise<AuthResponse> => {
    const res = await api.post<{ success: boolean; message: string; data: AuthResponse }>(
      '/auth/register', data
    )
    saveSession(res.data.data)
    return res.data.data
  },
  me: async (): Promise<User> => {
    const res = await api.get<{ success: boolean; message: string; data: User }>('/auth/me')
    const current = authService.getUser()
    if (current) {
      localStorage.setItem(SESSION_KEY, JSON.stringify({
        ...current,
        username: res.data.data.username,
        email: res.data.data.email,
        fullName: res.data.data.fullName,
        roles: res.data.data.roles,
      }))
    }
    return res.data.data
  },
  logout: () => {
    localStorage.removeItem('token')
    localStorage.removeItem(SESSION_KEY)
  },
  getToken: () => localStorage.getItem('token'),
  getUser: (): AuthResponse | null => {
    const raw = localStorage.getItem(SESSION_KEY)
    if (!raw) return null
    try {
      return JSON.parse(raw) as AuthResponse
    } catch {
      return null
    }
  },
  getRoles: (): string[] => authService.getUser()?.roles ?? [],
  hasRole: (role: string): boolean => authService.getRoles().includes(role),
  isAuthenticated: (): boolean => Boolean(authService.getToken()),
  isStaffOrAdmin: (): boolean =>
    authService.hasRole('ROLE_ADMIN') || authService.hasRole('ROLE_STAFF'),
  homePath: (): string => {
    if (authService.hasRole('ROLE_ADMIN')) return '/admin'
    if (authService.hasRole('ROLE_STAFF')) return '/staff/pos'
    return '/'
  },
}
