import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { demoUser } from '../data/mockData'
import { authService } from '../services/authService'
import { Avatar } from '../components/ui/Avatar'
import { Icon, type IconName } from '../components/ui/Icon'
import { PageBar } from '../components/ui/PageBar'

const menuItems: Array<{ icon: IconName; label: string; to?: string; action?: 'payments' | 'favorites' | 'settings' }> = [
  { icon: 'ticket', label: 'Vé của tôi', to: '/history' },
  { icon: 'card', label: 'Phương thức thanh toán', action: 'payments' },
  { icon: 'heart', label: 'Phim yêu thích', action: 'favorites' },
  { icon: 'settings', label: 'Cài đặt', action: 'settings' },
  { icon: 'info', label: 'Trợ giúp & hỗ trợ', to: '/ai-chat' },
]

export function Profile() {
  const navigate = useNavigate()
  const token = authService.getToken()
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(demoUser.name)
  const [email, setEmail] = useState(demoUser.email)
  const [phone, setPhone] = useState(demoUser.phone)

  const handleLogout = () => {
    authService.logout()
    navigate('/login')
  }

  const handleMenuAction = (action?: 'payments' | 'favorites' | 'settings') => {
    if (action === 'payments') {
      document.getElementById('profile-payments')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      return
    }
    window.alert('Tính năng đang được phát triển trong bản demo.')
  }

  if (!token) {
    return (
      <div className="container">
        <PageBar title="Tài khoản" backTo="/" />
        <div className="empty-state">
          <Icon name="user" size={42} />
          <strong>Bạn chưa đăng nhập</strong>
          <p>Đăng nhập để xem vé, điểm thưởng và thông tin tài khoản.</p>
          <Link to="/login" className="btn btn-primary btn-sm">
            Đăng nhập
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="container">
      <PageBar title="Tài khoản" backTo="/" />

      <div className="profile-layout">
        <aside>
          <div className="surface card-pad">
            <div className="profile-head">
              <Avatar src={demoUser.avatar} alt={name} size="lg" />
              <div className="profile-name">{name}</div>
              <div className="profile-email">{email}</div>
              <span className="badge badge-accent">Thành viên từ {demoUser.memberSince}</span>
            </div>

            <div className="stat-grid">
              <div className="stat-card">
                <span>Vé đã xem</span>
                <strong>12</strong>
              </div>
              <div className="stat-card">
                <span>Điểm thưởng</span>
                <strong>{demoUser.points.toLocaleString('vi-VN')}</strong>
              </div>
            </div>

            <div className="menu-list">
              {menuItems.map((item) =>
                item.to ? (
                  <Link key={item.label} to={item.to} className="menu-item">
                    <Icon name={item.icon} size={18} />
                    {item.label}
                    <Icon name="chevron-right" size={16} className="menu-item__chevron" />
                  </Link>
                ) : (
                  <button
                    key={item.label}
                    type="button"
                    className="menu-item"
                    onClick={() => handleMenuAction(item.action)}
                  >
                    <Icon name={item.icon} size={18} />
                    {item.label}
                    <Icon name="chevron-right" size={16} className="menu-item__chevron" />
                  </button>
                ),
              )}
              <button type="button" className="menu-item" onClick={handleLogout} style={{ color: 'var(--danger)' }}>
                <Icon name="logout" size={18} />
                Đăng xuất
              </button>
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

          <section className="surface card-pad">
            <div className="row between" style={{ marginBottom: 12 }}>
              <h2 className="h3">Thông tin cá nhân</h2>
              <button type="button" className="btn-link" onClick={() => setEditing((value) => !value)}>
                {editing ? 'Đóng' : 'Chỉnh sửa'}
              </button>
            </div>

            {editing ? (
              <div className="stack gap-12">
                <div className="field">
                  <label htmlFor="profile-name">Họ và tên</label>
                  <input id="profile-name" className="input" value={name} onChange={(event) => setName(event.target.value)} />
                </div>
                <div className="field">
                  <label htmlFor="profile-email">Email</label>
                  <input id="profile-email" className="input" value={email} onChange={(event) => setEmail(event.target.value)} />
                </div>
                <div className="field">
                  <label htmlFor="profile-phone">Số điện thoại</label>
                  <input id="profile-phone" className="input" value={phone} onChange={(event) => setPhone(event.target.value)} />
                </div>
                <button type="button" className="btn btn-primary" onClick={() => setEditing(false)}>
                  Lưu thay đổi
                </button>
              </div>
            ) : (
              <div className="stack gap-10">
                <div className="summary-line">
                  <span>Họ và tên</span>
                  <strong>{name}</strong>
                </div>
                <div className="summary-line">
                  <span>Email</span>
                  <strong>{email}</strong>
                </div>
                <div className="summary-line">
                  <span>Số điện thoại</span>
                  <strong>{phone}</strong>
                </div>
                <div className="summary-line">
                  <span>Hạng thành viên</span>
                  <strong style={{ color: 'var(--accent)' }}>Silver</strong>
                </div>
              </div>
            )}
          </section>

          <section className="surface card-pad" id="profile-payments">
            <div className="row between" style={{ marginBottom: 12 }}>
              <h2 className="h3">Phương thức thanh toán</h2>
              <button
                type="button"
                className="btn-link"
                onClick={() => window.alert('Tính năng thêm thẻ đang được phát triển trong bản demo.')}
              >
                Thêm thẻ
              </button>
            </div>
            <div className="payment-card">
              <span className="payment-card__brand" style={{ color: '#93c5fd' }}>
                VISA
              </span>
              <div>
                <div style={{ fontSize: 13, fontWeight: 700 }}>Visa •••• 4242</div>
                <div className="muted" style={{ fontSize: 11 }}>
                  Hết hạn 08/2027
                </div>
              </div>
              <span className="badge badge-success" style={{ marginLeft: 'auto' }}>
                Mặc định
              </span>
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
              </div>
              <button type="button" className="header-icon" style={{ marginLeft: 'auto', width: 34, height: 34 }} aria-label="Xóa thẻ">
                <Icon name="trash" size={15} />
              </button>
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}
