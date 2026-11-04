import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { authService } from '../services/authService'
import { Icon } from '../components/ui/Icon'
import { Logo } from '../components/ui/Logo'
import { FieldError } from '../components/ui/FieldError'
import { errorMessage } from '../utils/consoleFormat'

export function ResetPassword() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [fieldError, setFieldError] = useState('')
  const [success, setSuccess] = useState(false)

  useEffect(() => {
    if (!token) {
      setError('Thiếu token đặt lại mật khẩu. Vui lòng yêu cầu gửi lại email.')
    }
  }, [token])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) {
      setError('Thiếu token đặt lại mật khẩu.')
      return
    }
    if (password.length < 6) {
      setFieldError('Mật khẩu mới tối thiểu 6 ký tự.')
      return
    }
    if (password !== confirm) {
      setFieldError('Mật khẩu xác nhận không khớp.')
      return
    }
    setFieldError('')
    setError('')
    setLoading(true)
    try {
      await authService.resetPassword(token, password)
      setSuccess(true)
      window.setTimeout(() => navigate('/login', { replace: true }), 2000)
    } catch (err) {
      setError(errorMessage(err, 'Không thể đặt lại mật khẩu. Token có thể đã hết hạn.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      {loading && (
        <div className="auth-loading-overlay" role="status" aria-live="polite">
          <div className="spinner" />
          <p>Đang xử lý...</p>
        </div>
      )}
      <section className="auth-hero">
        <div className="auth-hero__content">
          <Logo />
          <div className="auth-hero__quote">Đặt mật khẩu mới an toàn.</div>
        </div>
      </section>
      <section className="auth-panel">
        <div className="auth-card">
          <h1 className="auth-title">Đặt lại mật khẩu</h1>
          <p className="auth-sub">Nhập mật khẩu mới cho tài khoản của bạn.</p>

          {error && <div className="form-error" style={{ marginBottom: 14 }}>{error}</div>}

          {success ? (
            <div className="form-success" style={{ marginBottom: 14 }}>
              <p>Đặt lại mật khẩu thành công! Đang chuyển về trang đăng nhập...</p>
            </div>
          ) : (
            <form className="auth-form" onSubmit={handleSubmit} noValidate>
              <div className="field">
                <label htmlFor="password">Mật khẩu mới</label>
                <div className={`input-wrap${fieldError ? ' input-wrap--error' : ''}`}>
                  <Icon name="lock" size={17} />
                  <input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    className="input"
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value)
                      if (fieldError) setFieldError('')
                    }}
                    placeholder="Tối thiểu 6 ký tự"
                    autoComplete="new-password"
                    required
                  />
                  <button
                    type="button"
                    className="input-action"
                    onClick={() => setShowPassword((v) => !v)}
                    aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    <Icon name={showPassword ? 'eye-off' : 'eye'} size={17} />
                  </button>
                </div>
              </div>
              <div className="field">
                <label htmlFor="confirm">Xác nhận mật khẩu</label>
                <div className={`input-wrap${fieldError ? ' input-wrap--error' : ''}`}>
                  <Icon name="lock" size={17} />
                  <input
                    id="confirm"
                    type={showPassword ? 'text' : 'password'}
                    className="input"
                    value={confirm}
                    onChange={(e) => {
                      setConfirm(e.target.value)
                      if (fieldError) setFieldError('')
                    }}
                    placeholder="Nhập lại mật khẩu mới"
                    autoComplete="new-password"
                    required
                  />
                </div>
                <FieldError id="confirm-error" message={fieldError} />
              </div>
              <button type="submit" className="btn btn-gradient btn-block btn-lg" disabled={loading || !token}>
                {loading ? (
                  <>
                    <span className="spinner spinner-sm" aria-hidden="true" />
                    Đang đặt lại...
                  </>
                ) : (
                  'Đặt lại mật khẩu'
                )}
              </button>
            </form>
          )}

          <p className="auth-foot" style={{ marginTop: 16 }}>
            <Link to="/login">Quay lại đăng nhập</Link>
          </p>
        </div>
      </section>
    </div>
  )
}
