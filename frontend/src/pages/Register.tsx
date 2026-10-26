import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authService } from '../services/authService'
import { Icon } from '../components/ui/Icon'
import { Logo } from '../components/ui/Logo'

const heroImage =
  'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=1600&q=80'

export function Register() {
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    if (password !== confirm) {
      setError('Mật khẩu xác nhận không khớp.')
      return
    }
    setLoading(true)
    try {
      await authService.register({
        username: email,
        email,
        password,
        fullName,
        phone,
      })
      navigate('/')
    } catch {
      setError('Không thể kết nối máy chủ. Bạn có thể dùng chế độ demo để trải nghiệm giao diện.')
    } finally {
      setLoading(false)
    }
  }

  const handleDemo = () => {
    localStorage.setItem('token', 'demo-token')
    navigate('/')
  }

  return (
    <div className="auth-page">
      <section className="auth-hero">
        <img className="auth-hero__bg" src={heroImage} alt="" />
        <div className="auth-hero__shade" />
        <div className="auth-hero__content">
          <Logo />
          <div className="auth-hero__quote">Bắt đầu hành trình điện ảnh của bạn.</div>
          <p className="muted" style={{ marginTop: 10, maxWidth: 380 }}>
            Tạo tài khoản miễn phí, đặt vé nhanh hơn và nhận ưu đãi dành riêng cho thành viên.
          </p>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <h1 className="auth-title">Tạo tài khoản</h1>
          <p className="auth-sub">Chỉ mất chưa đến một phút.</p>

          {error && <div className="form-error" style={{ marginBottom: 14 }}>{error}</div>}

          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="fullName">Họ và tên</label>
              <div className="input-wrap">
                <Icon name="user" size={17} />
                <input
                  id="fullName"
                  className="input"
                  value={fullName}
                  onChange={(event) => setFullName(event.target.value)}
                  placeholder="Nguyễn Văn A"
                  autoComplete="name"
                  required
                />
              </div>
            </div>

            <div className="field">
              <label htmlFor="email">Email</label>
              <div className="input-wrap">
                <Icon name="mail" size={17} />
                <input
                  id="email"
                  className="input"
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="you@example.com"
                  autoComplete="email"
                  required
                />
              </div>
            </div>

            <div className="field">
              <label htmlFor="phone">Số điện thoại</label>
              <div className="input-wrap">
                <Icon name="phone" size={17} />
                <input
                  id="phone"
                  className="input"
                  value={phone}
                  onChange={(event) => setPhone(event.target.value)}
                  placeholder="090 123 4567"
                  autoComplete="tel"
                />
              </div>
            </div>

            <div className="field">
              <label htmlFor="password">Mật khẩu</label>
              <div className="input-wrap">
                <Icon name="lock" size={17} />
                <input
                  id="password"
                  className="input"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="Tối thiểu 6 ký tự"
                  autoComplete="new-password"
                  minLength={6}
                  required
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
            </div>

            <div className="field">
              <label htmlFor="confirm">Xác nhận mật khẩu</label>
              <div className="input-wrap">
                <Icon name="lock" size={17} />
                <input
                  id="confirm"
                  className="input"
                  type={showPassword ? 'text' : 'password'}
                  value={confirm}
                  onChange={(event) => setConfirm(event.target.value)}
                  placeholder="Nhập lại mật khẩu"
                  autoComplete="new-password"
                  required
                />
              </div>
            </div>

            <label className="checkbox">
              <input type="checkbox" required />
              <span>
                Tôi đồng ý với <a href="#terms" className="btn-link">điều khoản</a> và{' '}
                <a href="#privacy" className="btn-link">chính sách bảo mật</a>.
              </span>
            </label>

            <button type="submit" className="btn btn-gradient btn-block btn-lg" disabled={loading}>
              {loading ? 'Đang tạo tài khoản...' : 'Đăng ký'}
              {!loading && <Icon name="arrow-right" size={17} />}
            </button>
          </form>

          <div className="auth-sep">hoặc</div>

          <button type="button" className="btn btn-outline btn-block" onClick={handleDemo}>
            <Icon name="sparkles" size={17} />
            Dùng tài khoản demo
          </button>

          <p className="auth-foot">
            Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
          </p>
        </div>
      </section>
    </div>
  )
}
