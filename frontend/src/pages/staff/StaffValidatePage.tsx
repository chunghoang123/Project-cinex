import { useState } from 'react'
import { ConsolePageHeader } from '../../components/console/ConsolePageHeader'
import { StatusBadge } from '../../components/console/StatusBadge'
import { Icon } from '../../components/ui/Icon'
import { staffService } from '../../services/staffService'
import type { TicketValidation } from '../../types/Admin'
import { formatVnd } from '../../utils/format'
import { errorMessage, formatDateTime } from '../../utils/consoleFormat'

export function StaffValidatePage() {
  const [code, setCode] = useState('')
  const [result, setResult] = useState<TicketValidation | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const validate = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!code.trim()) {
      setError('Vui lòng nhập mã vé')
      return
    }
    setLoading(true)
    setError('')
    setResult(null)
    try {
      setResult(await staffService.validateTicket(code.trim()))
    } catch (err) {
      setError(errorMessage(err, 'Không thể soát vé'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <ConsolePageHeader
        eyebrow="Kiểm soát vé"
        title="Soát vé tại cổng"
        description="Nhập mã vé in trên vé điện tử để xác thực và đánh dấu đã sử dụng. Mỗi vé chỉ có hiệu lực một lần."
      />

      <div className="scan-panel">
        <div className="console-card">
          <div className="console-card__head">
            <div>
              <h2>Nhập mã vé</h2>
              <p>Ví dụ: TKDEMO0001 · mã gồm chữ và số</p>
            </div>
            <Icon name="scan" size={22} />
          </div>

          <form className="scan-input-row" onSubmit={validate}>
            <input
              className="input"
              value={code}
              onChange={(event) => setCode(event.target.value.toUpperCase())}
              placeholder="TKDEMO0001"
              autoFocus
            />
            <button type="submit" className="btn btn-primary" disabled={loading}>
              <Icon name="scan" size={17} />
              {loading ? 'Đang kiểm tra…' : 'Soát vé'}
            </button>
          </form>

          {error && <div className="form-error" style={{ marginTop: 14 }}>{error}</div>}

          {result && (
            <div className={`scan-result ${result.valid ? 'is-valid' : 'is-invalid'}`}>
              <div className="scan-result__head">
                <Icon name={result.valid ? 'check-circle' : 'alert'} size={24} />
                <div>
                  <strong>{result.valid ? 'Vé hợp lệ' : 'Không thể soát vé'}</strong>
                  <span>{result.message}</span>
                </div>
              </div>

              <div className="scan-result__details">
                <div><span>Mã vé</span><strong className="mono">{result.ticketCode}</strong></div>
                {result.ticketStatus && <div><span>Trạng thái vé</span><strong><StatusBadge status={result.ticketStatus} /></strong></div>}
                {result.scannedAt && <div><span>Thời điểm soát</span><strong>{formatDateTime(result.scannedAt)}</strong></div>}
                {result.booking && (
                  <>
                    <div><span>Phim</span><strong>{result.booking.showtime.movieTitle}</strong></div>
                    <div><span>Suất chiếu</span><strong>{formatDateTime(result.booking.showtime.startTime)}</strong></div>
                    <div><span>Rạp / phòng</span><strong>{result.booking.showtime.cinemaName} · {result.booking.showtime.auditoriumName}</strong></div>
                    <div><span>Ghế</span><strong>{result.booking.seats.map((seat) => seat.seatCode).join(', ')}</strong></div>
                    <div><span>Khách</span><strong>{result.booking.customerName || 'Khách lẻ'}</strong></div>
                    <div><span>Tổng tiền</span><strong>{formatVnd(result.booking.totalAmount)}</strong></div>
                  </>
                )}
              </div>
            </div>
          )}

          <div className="console-empty" style={{ paddingBottom: 4 }}>
            <strong>Mẹo soát vé</strong>
            Vé đã soát sẽ chuyển sang trạng thái “Đã soát” và không thể sử dụng lại.
          </div>
        </div>
      </div>
    </div>
  )
}
