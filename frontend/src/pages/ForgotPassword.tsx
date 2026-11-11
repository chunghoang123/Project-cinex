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
  const [otp, setOtp] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [fieldError, setFieldError] = useState('')
  const [result, setResult] = useState<{ message: string; resetToken?: string | null } | null>(null)

  // Bước 1: gửi mã OTP tới email
  const handleSendOtp = async (event: FormEvent<HTMLFormElement>) => {
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
      setStep('otp')
      if (res.resetToken) {
        // Chế độ dev: chưa có SMTP, mã OTP được trả trực tiếp
        setOtp(res.resetToken)
      }
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
                <button type="submit" className="btn btn-gradient btn-block btn-lg" disabled={loading}>
                  {loading ? (
                    <>
                      <span className="spinner spinner-sm" aria-hidden="true" />
                      Đang gửi...
                    </>
                  ) : (
                    'Gửi mã OTP'
                  )}
                </button>
              </form>
            </>
          )}

          {step === 'otp' && (
            <>
              <h1 className="auth-title">Nhập mã OTP</h1>
              <p className="auth-sub">
                Mã OTP 6 số đã được gửi tới <strong>{email}</strong> (hiệu lực 5 phút). Kiểm tra cả mục Spam nhé.
              </p>

              {error && <div className="form-error" style={{ marginBottom: 14 }}>{error}</div>}

              <form className="auth-form" onSubmit={handleVerifyOtp} noValidate>
                <div className="field">
                  <label htmlFor="otp">Mã OTP</label>
                  <div className={`input-wrap${fieldError ? ' input-wrap--error' : ''}`}>
                    <Icon name="lock" size={17} />
                    <input
                      id="otp"
                      type="text"
                      className="input"
                      value={otp}
                      onChange={(e) => {
                        setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))
                        if (fieldError) setFieldError('')
                      }}
                      placeholder="Nhập 6 số từ email"
                      inputMode="numeric"
                      autoComplete="one-time-code"
                      maxLength={6}
                      style={{ letterSpacing: 4, fontWeight: 600 }}
                      required
                      aria-invalid={Boolean(fieldError)}
                    />
                  </div>
                  <FieldError id="otp-error" message={fieldError} />
                </div>
                <div className="field">
                  <label htmlFor="new-password">Mật khẩu mới</label>
                  <div className={`input-wrap${fieldError ? ' input-wrap--error' : ''}`}>
                    <Icon name="lock" size={17} />
                    <input
                      id="new-password"
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
                <FieldError id="email-error" message={fieldError} />
              </div>
            </>
          )}

          {step !== 'success' && (
            <p className="auth-foot" style={{ marginTop: 16 }}>
              {step === 'email' && (
                <>
                  Nhớ mật khẩu? <Link to="/login">Quay lại đăng nhập</Link>
                </>
              )}
              {step === 'otp' && (
                <>
                  Dùng email khác?{' '}
                  <button type="button" className="btn-link" onClick={() => setStep('email')}>
                    Quay lại bước 1
                  </button>
                </>
              )}
            </p>
          )}
        </div>
      </section>
    </div>
  )
}
