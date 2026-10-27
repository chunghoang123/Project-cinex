import { useEffect, useState } from 'react'
import { ConsolePageHeader } from '../../components/console/ConsolePageHeader'
import { Modal } from '../../components/console/Modal'
import { Icon } from '../../components/ui/Icon'
import { adminService } from '../../services/adminService'
import type { AdminUser, CreateUserPayload } from '../../types/Admin'
import { errorMessage, formatDate, initials } from '../../utils/consoleFormat'

const ROLE_LABELS: Record<string, string> = {
  ROLE_ADMIN: 'Quản trị',
  ROLE_STAFF: 'Nhân viên',
  ROLE_CUSTOMER: 'Khách hàng',
}

const ROLE_OPTIONS = ['ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_CUSTOMER']

const EMPTY_FORM: CreateUserPayload = {
  username: '',
  email: '',
  password: '',
  fullName: '',
  phone: '',
  roles: ['ROLE_STAFF'],
}

function RoleBadges({ roles }: { roles: string[] }) {
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
      {roles.map((role) => (
        <span key={role} className={`badge ${role === 'ROLE_ADMIN' ? 'badge-warning' : role === 'ROLE_STAFF' ? 'badge-accent' : 'badge-muted'}`}>
          {ROLE_LABELS[role] ?? role}
        </span>
      ))}
    </div>
  )
}

export function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')

  const [createOpen, setCreateOpen] = useState(false)
  const [form, setForm] = useState<CreateUserPayload>(EMPTY_FORM)
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  const [roleUser, setRoleUser] = useState<AdminUser | null>(null)
  const [roleSelection, setRoleSelection] = useState<string[]>([])
  const [roleError, setRoleError] = useState('')

  const load = () => {
    setLoading(true)
    setError('')
    adminService.getUsers()
      .then(setUsers)
      .catch((err) => setError(errorMessage(err, 'Không tải được danh sách người dùng')))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [])

  const openCreate = () => {
    setForm(EMPTY_FORM)
    setFormError('')
    setCreateOpen(true)
  }

  const toggleFormRole = (role: string) => {
    setForm((current) => ({
      ...current,
      roles: current.roles.includes(role)
        ? current.roles.filter((item) => item !== role)
        : [...current.roles, role],
    }))
  }

  const createUser = async () => {
    if (!form.username.trim() || !form.email.trim() || !form.password.trim()) {
      setFormError('Vui lòng nhập username, email và mật khẩu')
      return
    }
    if (form.roles.length === 0) {
      setFormError('Chọn ít nhất một vai trò')
      return
    }
    setSaving(true)
    setFormError('')
    try {
      await adminService.createUser(form)
      setCreateOpen(false)
      load()
    } catch (err) {
      setFormError(errorMessage(err, 'Không tạo được người dùng'))
    } finally {
      setSaving(false)
    }
  }

  const openRoles = (user: AdminUser) => {
    setRoleUser(user)
    setRoleSelection(user.roles)
    setRoleError('')
  }

  const saveRoles = async () => {
    if (!roleUser) return
    if (roleSelection.length === 0) {
      setRoleError('Chọn ít nhất một vai trò')
      return
    }
    try {
      await adminService.updateUserRoles(roleUser.id, roleSelection)
      setRoleUser(null)
      load()
    } catch (err) {
      setRoleError(errorMessage(err, 'Không cập nhật được vai trò'))
    }
  }

  const toggleStatus = async (user: AdminUser) => {
    const action = user.active ? 'khóa' : 'mở khóa'
    if (!window.confirm(`${action[0].toUpperCase()}${action.slice(1)} tài khoản "${user.username}"?`)) return
    try {
      await adminService.updateUserStatus(user.id, !user.active)
      load()
    } catch (err) {
      window.alert(errorMessage(err, 'Không cập nhật được trạng thái'))
    }
  }

  const filtered = users.filter((user) => {
    const keyword = search.trim().toLowerCase()
    if (!keyword) return true
    return [user.username, user.fullName, user.email, user.phone]
      .filter(Boolean)
      .some((value) => value.toLowerCase().includes(keyword))
  })

  return (
    <div>
      <ConsolePageHeader
        eyebrow="Tài khoản"
        title="Người dùng & phân quyền"
        description="Quản lý tài khoản khách hàng, nhân viên và quản trị viên. Khóa tài khoản sẽ chặn đăng nhập ngay lập tức."
        actions={
          <button type="button" className="btn btn-primary btn-sm" onClick={openCreate}>
            <Icon name="plus" size={16} /> Thêm người dùng
          </button>
        }
      />

      <div className="console-toolbar">
        <div className="input-wrap" style={{ flex: 1, maxWidth: 340 }}>
          <Icon name="search" size={16} />
          <input
            className="input"
            placeholder="Tìm theo tên, username, email, SĐT…"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
        <span className="console-toolbar__spacer" />
        <span className="badge badge-muted">{users.length} tài khoản</span>
      </div>

      {loading ? (
        <div className="console-card console-empty">Đang tải danh sách người dùng…</div>
      ) : error ? (
        <div className="console-card console-empty">
          <strong>Không tải được dữ liệu</strong>
          {error}
          <div style={{ marginTop: 14 }}>
            <button type="button" className="btn btn-ghost btn-sm" onClick={load}>Thử lại</button>
          </div>
        </div>
      ) : (
        <div className="console-table-wrap">
          <table className="console-table">
            <thead>
              <tr>
                <th>Người dùng</th>
                <th>Liên hệ</th>
                <th>Vai trò</th>
                <th>Trạng thái</th>
                <th>Ngày tạo</th>
                <th className="num">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={6}>
                    <div className="console-empty">
                      <strong>Không có người dùng phù hợp</strong>
                      Thử từ khóa khác.
                    </div>
                  </td>
                </tr>
              ) : filtered.map((user) => (
                <tr key={user.id}>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <span className="console-avatar" style={{ width: 32, height: 32, fontSize: 10 }}>
                        {initials(user.fullName || user.username)}
                      </span>
                      <div>
                        <div className="cell-main">{user.fullName || user.username}</div>
                        <div className="cell-sub">@{user.username} · #{user.id}</div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div className="cell-main">{user.email}</div>
                    <div className="cell-sub">{user.phone || 'Chưa có SĐT'}</div>
                  </td>
                  <td><RoleBadges roles={user.roles} /></td>
                  <td>
                    <span className={`badge ${user.active ? 'badge-success' : 'badge-muted'}`}>
                      {user.active ? 'Đang hoạt động' : 'Đã khóa'}
                    </span>
                  </td>
                  <td>{user.createdAt ? formatDate(user.createdAt) : '—'}</td>
                  <td>
                    <div className="cell-actions">
                      <button type="button" className="btn btn-ghost btn-sm" onClick={() => openRoles(user)}>
                        <Icon name="shield" size={14} /> Phân quyền
                      </button>
                      <button
                        type="button"
                        className={user.active ? 'btn btn-danger btn-sm' : 'btn btn-ghost btn-sm'}
                        onClick={() => toggleStatus(user)}
                      >
                        <Icon name={user.active ? 'lock' : 'check-circle'} size={14} />
                        {user.active ? 'Khóa' : 'Mở'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal
        open={createOpen}
        title="Thêm người dùng"
        subtitle="Tạo tài khoản nhân viên hoặc khách hàng mới"
        onClose={() => setCreateOpen(false)}
        footer={
          <>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setCreateOpen(false)}>Hủy</button>
            <button type="button" className="btn btn-primary btn-sm" onClick={createUser} disabled={saving}>
              {saving ? 'Đang tạo…' : 'Tạo tài khoản'}
            </button>
          </>
        }
      >
        {formError && <div className="form-error" style={{ marginBottom: 14 }}>{formError}</div>}
        <div className="console-form-grid">
          <div className="field">
            <label>Username</label>
            <input className="input" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} placeholder="nhanvien02" />
          </div>
          <div className="field">
            <label>Email</label>
            <input className="input" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="email@cinex.vn" />
          </div>
          <div className="field">
            <label>Mật khẩu</label>
            <input className="input" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="Tối thiểu 6 ký tự" />
          </div>
          <div className="field">
            <label>Họ tên</label>
            <input className="input" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} placeholder="Nguyễn Văn A" />
          </div>
          <div className="field">
            <label>Số điện thoại</label>
            <input className="input" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} placeholder="09xx xxx xxx" />
          </div>
          <div className="field field--full">
            <label>Vai trò</label>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 14, paddingTop: 4 }}>
              {ROLE_OPTIONS.map((role) => (
                <label className="checkbox" key={role}>
                  <input
                    type="checkbox"
                    checked={form.roles.includes(role)}
                    onChange={() => toggleFormRole(role)}
                  />
                  <span>{ROLE_LABELS[role]}</span>
                </label>
              ))}
            </div>
          </div>
        </div>
      </Modal>

      <Modal
        open={Boolean(roleUser)}
        title="Phân quyền tài khoản"
        subtitle={roleUser ? `@${roleUser.username}` : ''}
        onClose={() => setRoleUser(null)}
        footer={
          <>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setRoleUser(null)}>Hủy</button>
            <button type="button" className="btn btn-primary btn-sm" onClick={saveRoles}>Lưu vai trò</button>
          </>
        }
      >
        {roleError && <div className="form-error" style={{ marginBottom: 14 }}>{roleError}</div>}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {ROLE_OPTIONS.map((role) => (
            <label className="checkbox" key={role}>
              <input
                type="checkbox"
                checked={roleSelection.includes(role)}
                onChange={() => setRoleSelection((current) =>
                  current.includes(role) ? current.filter((item) => item !== role) : [...current, role]
                )}
              />
              <span>
                <strong style={{ color: 'var(--text-soft)' }}>{ROLE_LABELS[role]}</strong>
                <br />
                {role === 'ROLE_ADMIN' ? 'Toàn quyền quản trị hệ thống' : role === 'ROLE_STAFF' ? 'Bán vé, soát vé và tra cứu đơn' : 'Đặt vé online và xem lịch sử'}
              </span>
            </label>
          ))}
        </div>
      </Modal>
    </div>
  )
}
