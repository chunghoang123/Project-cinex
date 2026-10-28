export interface User {
  id: number
  username: string
  email: string
  fullName: string
  phone: string
  roles: string[]
  active: boolean
  createdAt?: string
}

export interface AuthResponse {
  token: string
  username: string
  email: string
  fullName: string
  roles: string[]
}

export interface DemoAccount {
  label: string
  username: string
  password: string
  description: string
  roles: string[]
}

export const DEMO_ACCOUNTS: DemoAccount[] = [
  {
    label: 'Quản trị viên',
    username: 'admin',
    password: 'Admin@123',
    description: 'Dashboard, phim, rạp, suất chiếu, đơn hàng',
    roles: ['ROLE_ADMIN'],
  },
  {
    label: 'Nhân viên bán vé',
    username: 'staff',
    password: 'Staff@123',
    description: 'POS bán vé tại quầy, soát vé, doanh thu ca',
    roles: ['ROLE_STAFF'],
  },
  {
    label: 'Khách hàng',
    username: 'customer',
    password: 'Customer@123',
    description: 'Trải nghiệm đặt vé online như khách hàng',
    roles: ['ROLE_CUSTOMER'],
  },
]
