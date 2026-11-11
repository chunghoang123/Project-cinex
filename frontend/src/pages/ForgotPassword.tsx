import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { authService } from '../services/authService'
import { Icon } from '../components/ui/Icon'
import { Logo } from '../components/ui/Logo'
import { FieldError } from '../components/ui/FieldError'
import { errorMessage } from '../utils/consoleFormat'

type Step = 'email' | 'otp' | 'success'

export function ForgotPassword() {
  const [step, setStep] = useState<Step>('email')
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [fieldError, setFieldError] = useState('')

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

  // Bước 2: xác minh OTP và đặt mật khẩu mới
  const handleVerifyOtp = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!otp.trim()) {
      setFieldError('Vui lòng nhập mã OTP từ email.')
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
      await authService.resetPassword(otp.trim(), password)
      setStep('success')
    } catch (err) {
      setError(errorMessage(err, 'Mã OTP không đúng hoặc đã hết hạn. Vui lòng gửi lại mã.'))
    } finally {
      setLoading(false)
    }
  }

  const handleResendOtp = async () => {
    setFieldError('')
    setError('')
    setLoading(true)
    try {
      const res = await authService.forgotPassword(email.trim())
      if (res.resetToken) {
        setOtp(res.resetToken)
      }
    } catch (err) {
      setError(errorMessage(err, 'Không gửi được lại mã OTP. Vui lòng thử lại.'))
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
          {step === 'email' && (
            <>
              <h1 className="auth-title">Quên mật khẩu</h1>
              <p className="auth-sub">Nhập email đã đăng ký, chúng tôi sẽ gửi mã OTP khôi phục.</p>

              {error && <div className="form-error" style={{ marginBottom: 14 }}>{error}</div>}

              <form className="auth-form" onSubmit={handleSendOtp} noValidate>
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
                <div className="field">
                  <label htmlFor="confirm-password">Xác nhận mật khẩu</label>
                  <div className={`input-wrap${fieldError ? ' input-wrap--error' : ''}`}>
                    <Icon name="lock" size={17} />
                    <input
                      id="confirm-password"
                      type={showPassword ? 'text' : 'password'}
                      className="input"
                      value={confirm}
                      onChange={(e) => setConfirm(e.target.value)}
                      placeholder="Nhập lại mật khẩu mới"
                      autoComplete="new-password"
                      required
                    />
                  </div>
                  <FieldError id="confirm-password-error" message={fieldError} />
                </div>
                <button type="submit" className="btn btn-gradient btn-block btn-lg" disabled={loading}>
                  {loading ? (
                    <>
                      <span className="spinner spinner-sm" aria-hidden="true" />
                      Đang đặt lại...
                    </>
                  ) : (
                    'Đặt lại mật khẩu'
                  )}
                </button>
                <p className="muted" style={{ marginTop: 10, fontSize: 13, textAlign: 'center' }}>
                  Không nhận được mã?{' '}
                  <button type="button" className="btn-link" onClick={handleResendOtp} disabled={loading}>
                    Gửi lại mã OTP
                  </button>
                </p>
              </form>
            </>
          )}

          {step === 'success' && (
            <>
              <h1 className="auth-title">Thành công</h1>
              <div className="form-success" style={{ marginBottom: 14 }}>
                <p>Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.</p>
              </div>
              <div style={{ marginTop: 16 }}>
                <Link to="/login" className="btn btn-gradient btn-block btn-lg">
                  Về trang đăng nhập
                </Link>
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
