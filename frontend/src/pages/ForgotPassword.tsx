import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { authService } from '../services/authService'
import { Icon } from '../components/ui/Icon'
import { Logo } from '../components/ui/Logo'
import { FieldError } from '../components/ui/FieldError'
import { errorMessage } from '../utils/consoleFormat'

export function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [fieldError, setFieldError] = useState('')
  const [result, setResult] = useState<{ message: string; resetToken?: string | null } | null>(null)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!email.trim()) {
      setFieldError('Vui lòng nhập email đã đăng ký.')
      return
    }
    setFieldError('')
    setError('')
    setLoading(true)
    try {
      const res = await authService.forgotPassword(email.trim())
      setResult(res)
    } catch (err) {
      setError(errorMessage(err, 'Không thể gửi yêu cầu. Vui lòng thử lại.'))
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
          <div className="auth-hero__quote">Khôi phục quyền truy cập tài khoản.</div>
        </div>
      </section>
      <section className="auth-panel">
        <div className="auth-card">
          <h1 className="auth-title">Quên mật khẩu</h1>
          <p className="auth-sub">Nhập email đã đăng ký, chúng tôi sẽ gửi hướng dẫn đặt lại mật khẩu.</p>

          {error && <div className="form-error" style={{ marginBottom: 14 }}>{error}</div>}

          {result ? (
            <div className="form-success" style={{ marginBottom: 14 }}>
              <p>{result.message}</p>
              {result.resetToken && (
                <div style={{ marginTop: 12 }}>
                  <p className="muted" style={{ fontSize: 13 }}>
                    (Chế độ dev: không có SMTP, mã OTP đặt lại được trả trực tiếp để bạn test.)
                  </p>
                  <p style={{ marginTop: 8, wordBreak: 'break-all' }}>
                    <strong>Mã OTP:</strong> <code>{result.resetToken}</code>
                  </p>
                  <p style={{ marginTop: 8 }}>
                    <Link to={`/reset-password?token=${encodeURIComponent(result.resetToken)}`}>
                      Đặt lại mật khẩu ngay →
                    </Link>
                  </p>
                </div>
              )}
            </div>
          ) : (
            <form className="auth-form" onSubmit={handleSubmit} noValidate>
              <div className="field">
                <label htmlFor="email">Email</label>
                <div className={`input-wrap${fieldError ? ' input-wrap--error' : ''}`}>
                  <Icon name="mail" size={17} />
                  <input
                    id="email"
                    type="email"
                    className="input"
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value)
                      if (fieldError) setFieldError('')
                    }}
                    placeholder="Nhập email đã đăng ký"
                    autoComplete="email"
                    required
                    aria-invalid={Boolean(fieldError)}
                  />
                </div>
                <FieldError id="email-error" message={fieldError} />
              </div>
              <button type="submit" className="btn btn-gradient btn-block btn-lg" disabled={loading}>
                {loading ? (
                  <>
                    <span className="spinner spinner-sm" aria-hidden="true" />
                    Đang gửi...
                  </>
                ) : (
                  'Gửi hướng dẫn'
                )}
              </button>
            </form>
          )}

          <p className="auth-foot" style={{ marginTop: 16 }}>
            Nhớ mật khẩu? <Link to="/login">Quay lại đăng nhập</Link>
          </p>
        </div>
      </section>
    </div>
  )
}
