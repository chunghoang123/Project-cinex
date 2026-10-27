import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { authService } from '../services/authService'
import { Avatar } from '../components/ui/Avatar'
import { FieldError } from '../components/ui/FieldError'
import { Icon } from '../components/ui/Icon'
import { PageBar } from '../components/ui/PageBar'
import { errorMessage, formatDate } from '../utils/consoleFormat'

const menuItems: Array<{ icon: IconName; label: string; to?: string; action?: 'payments' | 'favorites' | 'settings' }> = [
  { icon: 'ticket', label: 'Vé của tôi', to: '/history' },
  { icon: 'card', label: 'Phương thức thanh toán', action: 'payments' },
  { icon: 'heart', label: 'Phim yêu thích', action: 'favorites' },
  { icon: 'settings', label: 'Cài đặt', action: 'settings' },
  { icon: 'info', label: 'Trợ giúp & hỗ trợ', to: '/ai-chat' },
]

export function Profile() {
  const token = authService.getToken()

  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(Boolean(token))
  const [loadError, setLoadError] = useState('')

  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(demoUser.name)
  const [email, setEmail] = useState(demoUser.email)
  const [phone, setPhone] = useState(demoUser.phone)

  const cancelEditing = () => {
    if (user) setProfileForm(toProfileForm(user))
    setProfileErrors({})
    setProfileError('')
    setEditing(false)
  }

  const handleProfileSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const form: ProfileForm = {
      fullName: profileForm.fullName.trim(),
      email: profileForm.email.trim(),
      phone: profileForm.phone.trim(),
    }
    const errors: Partial<Record<ProfileField, string>> = {}
    if (form.fullName.length > 100) {
      errors.fullName = 'Họ tên tối đa 100 ký tự.'
    }
    if (!form.email) {
      errors.email = 'Vui lòng nhập email.'
    } else if (!EMAIL_PATTERN.test(form.email)) {
      errors.email = 'Email không hợp lệ.'
    }
    if (form.phone && !PHONE_PATTERN.test(form.phone)) {
      errors.phone = 'Số điện thoại không hợp lệ.'
    }

    setProfileErrors(errors)
    if (Object.keys(errors).length > 0) {
      const first = (Object.keys(errors) as ProfileField[])[0]
      focusField(`profile-${first}`)
      return
    }
    window.alert('Tính năng đang được phát triển trong bản demo.')
  }

  if (!token) {
    return (
      <div className="container">
        <PageBar title="Tài khoản" backTo="/" />
        <div className="empty-state">
          <Icon name="user" size={40} />
          <strong>Bạn chưa đăng nhập</strong>
          <p>Đăng nhập để xem và cập nhật thông tin tài khoản Cinex.</p>
          <Link to="/login" className="btn btn-primary btn-sm">
            Đăng nhập
          </Link>
        </div>
      </div>
    )
  }

  if (loading && !user) {
    return (
      <div className="container">
        <PageBar title="Tài khoản" backTo="/" />
        <div className="empty-state">
          <p>Đang tải thông tin tài khoản...</p>
        </div>
      </div>
    )
  }

  if (!user) {
    return (
      <div className="container">
        <PageBar title="Tài khoản" backTo="/" />
        <div className="empty-state">
          <Icon name="alert" size={40} />
          <strong>Không tải được hồ sơ</strong>
          <p>{loadError || 'Phiên đăng nhập đã hết hạn.'}</p>
          <Link to="/login" className="btn btn-primary btn-sm">
            Đăng nhập lại
          </Link>
        </div>
      </div>
    )
  }

  const displayName = user.fullName?.trim() || user.username
  const consolePath = authService.hasRole('ROLE_ADMIN') ? '/admin' : '/staff/pos'

  return (
    <div className="container">
      <PageBar title="Tài khoản" backTo="/" />

      <div className="profile-layout">
        <aside>
          <div className="surface card-pad">
            <div className="profile-head">
              <Avatar src={user.avatarUrl} alt={displayName} size="lg" />
              <div className="profile-name">{displayName}</div>
              <div className="profile-email">@{user.username}</div>
              {user.roles.length > 0 && (
                <div className="row center wrap gap-6" style={{ marginTop: 4 }}>
                  {user.roles.map((role) => (
                    <span key={role} className="badge badge-accent">
                      {roleLabels[role] ?? role}
                    </span>
                  ))}
                </div>
              )}
            </div>

            <hr className="divider" style={{ margin: '16px 0' }} />

            <div className="stat-grid" style={{ marginTop: 0 }}>
              <div className="stat-card">
                <span>Vé đã xem</span>
                <strong>12</strong>
              </div>
              <div className="stat-card">
                <span>Trạng thái</span>
                <strong>{user.active ? 'Hoạt động' : 'Tạm khóa'}</strong>
              </div>
            </div>
          </div>
        </aside>

        <div className="stack gap-16">
          <div className="promo-banner">
            <span className="eyebrow">Cinex Rewards</span>
            <h3>{demoUser.points.toLocaleString('vi-VN')} điểm thưởng</h3>
            <p>Bạn còn 720 điểm nữa để lên hạng Gold và nhận vé miễn phí mỗi tháng.</p>
            <div className="row gap-10" style={{ marginTop: 14 }}>
              <Link to="/movies" className="btn btn-accent btn-sm">
                Đặt vé tích điểm
              </Link>
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={() => window.alert('Tính năng đổi điểm đang được phát triển trong bản demo.')}
              >
                Đổi điểm
              </button>
            </div>
          </div>

        <div className="stack gap-16">
          <section className="surface card-pad">
            <div className="profile-section-head">
              <div>
                <h2 className="h3">Thông tin cá nhân</h2>
                <p className="muted">Họ tên, email và số điện thoại dùng khi đặt vé.</p>
              </div>
              {!editing && (
                <button type="button" className="btn btn-outline btn-sm" onClick={startEditing}>
                  <Icon name="edit" size={14} />
                  Chỉnh sửa
                </button>
              )}
            </div>

            {editing ? (
              <form className="profile-form" onSubmit={handleProfileSubmit} noValidate>
                <div className="field">
                  <label htmlFor="profile-name">Họ và tên</label>
                  <input id="profile-name" className="input" value={name} onChange={(event) => setName(event.target.value)} />
                </div>

                <div className="field">
                  <label htmlFor="profile-email">Email</label>
                  <div className={`input-wrap${profileErrors.email ? ' input-wrap--error' : ''}`}>
                    <Icon name="mail" size={17} />
                    <input
                      id="profile-email"
                      className="input"
                      type="email"
                      value={profileForm.email}
                      onChange={(event) => {
                        setProfileForm((form) => ({ ...form, email: event.target.value }))
                        setProfileErrors((errors) => ({ ...errors, email: undefined }))
                      }}
                      placeholder="ban@cinex.vn"
                      autoComplete="email"
                      maxLength={100}
                      aria-invalid={Boolean(profileErrors.email)}
                      aria-describedby={profileErrors.email ? 'profile-email-error' : undefined}
                    />
                  </div>
                  <FieldError id="profile-email-error" message={profileErrors.email} />
                </div>
                <div className="field">
                  <label htmlFor="profile-phone">Số điện thoại</label>
                  <div className={`input-wrap${profileErrors.phone ? ' input-wrap--error' : ''}`}>
                    <Icon name="phone" size={17} />
                    <input
                      id="profile-phone"
                      className="input"
                      type="tel"
                      value={profileForm.phone}
                      onChange={(event) => {
                        setProfileForm((form) => ({ ...form, phone: event.target.value }))
                        setProfileErrors((errors) => ({ ...errors, phone: undefined }))
                      }}
                      placeholder="09xx xxx xxx"
                      autoComplete="tel"
                      maxLength={15}
                      aria-invalid={Boolean(profileErrors.phone)}
                      aria-describedby={profileErrors.phone ? 'profile-phone-error' : undefined}
                    />
                  </div>
                  <FieldError id="profile-phone-error" message={profileErrors.phone} />
                </div>

                {profileError && <div className="form-error">{profileError}</div>}

                <div className="profile-actions">
                  <button type="submit" className="btn btn-primary" disabled={savingProfile}>
                    {savingProfile ? 'Đang lưu...' : 'Lưu thay đổi'}
                  </button>
                  <button
                    type="button"
                    className="btn btn-ghost"
                    onClick={cancelEditing}
                    disabled={savingProfile}
                  >
                    Hủy
                  </button>
                </div>
              </form>
            ) : (
              <div className="stack gap-10">
                <div className="summary-line">
                  <span>Họ và tên</span>
                  <strong>{name}</strong>
                </div>
                <div className="profile-info__row">
                  <Icon name="mail" size={16} />
                  <span className="profile-info__label">Email</span>
                  <span className="profile-info__value">{user.email}</span>
                </div>
                <div className="profile-info__row">
                  <Icon name="phone" size={16} />
                  <span className="profile-info__label">Số điện thoại</span>
                  <span className="profile-info__value">{user.phone || 'Chưa cập nhật'}</span>
                </div>
                <div className="summary-line">
                  <span>Hạng thành viên</span>
                  <strong style={{ color: 'var(--accent)' }}>Silver</strong>
                </div>
              </div>
            )}
          </section>

          <section className="surface card-pad">
            <div className="profile-section-head">
              <div>
                <h2 className="h3">Đổi mật khẩu</h2>
                <p className="muted">Dùng mật khẩu riêng cho Cinex và thay đổi định kỳ.</p>
              </div>
              <Icon name="shield" size={18} />
            </div>
            <div className="payment-card">
              <span className="payment-card__brand" style={{ color: '#fca5a5' }}>
                MC
              </span>
              <div>
                <div style={{ fontSize: 13, fontWeight: 700 }}>Mastercard •••• 8888</div>
                <div className="muted" style={{ fontSize: 11 }}>
                  Hết hạn 12/2026
                </div>
                <FieldError id="pwd-currentPassword-error" message={passwordErrors.currentPassword} />
              </div>

              <div className="field">
                <label htmlFor="pwd-newPassword">Mật khẩu mới</label>
                <div className={`input-wrap${passwordErrors.newPassword ? ' input-wrap--error' : ''}`}>
                  <Icon name="lock" size={17} />
                  <input
                    id="pwd-newPassword"
                    className="input"
                    type={showPassword ? 'text' : 'password'}
                    value={passwordForm.newPassword}
                    onChange={(event) => {
                      setPasswordForm((form) => ({ ...form, newPassword: event.target.value }))
                      setPasswordErrors((errors) => ({ ...errors, newPassword: undefined }))
                    }}
                    placeholder="Tối thiểu 6 ký tự"
                    autoComplete="new-password"
                    aria-invalid={Boolean(passwordErrors.newPassword)}
                    aria-describedby={passwordErrors.newPassword ? 'pwd-newPassword-error' : undefined}
                  />
                  <button
                    type="button"
                    className="input-action"
                    onClick={() => setShowPassword((value) => !value)}
                    aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    <Icon name={showPassword ? 'eye-off' : 'eye'} size={17} />
                  </button>
                </div>
                <FieldError id="pwd-newPassword-error" message={passwordErrors.newPassword} />
              </div>

              <div className="field">
                <label htmlFor="pwd-confirmPassword">Nhập lại mật khẩu mới</label>
                <div className={`input-wrap${passwordErrors.confirmPassword ? ' input-wrap--error' : ''}`}>
                  <Icon name="lock" size={17} />
                  <input
                    id="pwd-confirmPassword"
                    className="input"
                    type={showPassword ? 'text' : 'password'}
                    value={passwordForm.confirmPassword}
                    onChange={(event) => {
                      setPasswordForm((form) => ({ ...form, confirmPassword: event.target.value }))
                      setPasswordErrors((errors) => ({ ...errors, confirmPassword: undefined }))
                    }}
                    placeholder="Nhập lại mật khẩu mới"
                    autoComplete="new-password"
                    aria-invalid={Boolean(passwordErrors.confirmPassword)}
                    aria-describedby={passwordErrors.confirmPassword ? 'pwd-confirmPassword-error' : undefined}
                  />
                </div>
                <FieldError id="pwd-confirmPassword-error" message={passwordErrors.confirmPassword} />
              </div>

              {passwordError && <div className="form-error">{passwordError}</div>}

              <div className="profile-actions">
                <button type="submit" className="btn btn-primary" disabled={savingPassword}>
                  {savingPassword ? 'Đang cập nhật...' : 'Đổi mật khẩu'}
                </button>
              </div>
            </form>
          </section>
        </div>
      </div>
    </div>
  )
}
