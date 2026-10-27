import { useEffect, useState } from 'react'
import { ConsolePageHeader } from '../../components/console/ConsolePageHeader'
import { StatCard } from '../../components/console/StatCard'
import { StatusBadge } from '../../components/console/StatusBadge'
import { Icon } from '../../components/ui/Icon'
import { staffService } from '../../services/staffService'
import type { ShiftSummary } from '../../types/Admin'
import { formatVnd } from '../../utils/format'
import { errorMessage, formatTime, todayIso } from '../../utils/consoleFormat'

export function StaffShiftPage() {
  const [date, setDate] = useState(todayIso())
  const [summary, setSummary] = useState<ShiftSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = () => {
    setLoading(true)
    setError('')
    staffService.getShiftSummary(date)
      .then(setSummary)
      .catch((err) => setError(errorMessage(err, 'Không tải được doanh thu ca')))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [date])

  return (
    <div>
      <ConsolePageHeader
        eyebrow="Kết ca"
        title="Doanh thu ca bán vé"
        description="Tổng hợp giao dịch bán tại quầy trong ngày. Chỉ tính các đơn đã xác nhận và thanh toán thành công."
        actions={
          <div className="console-page-head__actions">
            <input className="input" type="date" value={date} onChange={(event) => setDate(event.target.value)} />
            <button type="button" className="btn btn-ghost btn-sm" onClick={load}>
              <Icon name="refresh" size={15} /> Làm mới
            </button>
            <button type="button" className="btn btn-primary btn-sm" onClick={() => window.print()}>
              <Icon name="printer" size={15} /> In báo cáo
            </button>
          </div>
        }
      />

      {loading ? (
        <div className="console-card console-empty">Đang tổng hợp doanh thu…</div>
      ) : error ? (
        <div className="console-card console-empty">
          <strong>Không tải được dữ liệu</strong>
          {error}
          <div style={{ marginTop: 14 }}>
            <button type="button" className="btn btn-ghost btn-sm" onClick={load}>Thử lại</button>
          </div>
        </div>
      ) : summary ? (
        <>
          <div className="console-stat-grid">
            <StatCard icon="cash" label="Tổng doanh thu" value={formatVnd(summary.totalRevenue)} hint={`Ngày ${summary.date}`} tone="accent" />
            <StatCard icon="ticket" label="Vé đã bán" value={summary.ticketCount.toLocaleString('vi-VN')} hint="Ghế tại quầy" tone="violet" />
            <StatCard icon="receipt" label="Giao dịch" value={summary.transactionCount.toLocaleString('vi-VN')} hint="Đơn đã xác nhận" tone="green" />
            <StatCard icon="cash" label="Tiền mặt" value={formatVnd(summary.cashRevenue)} hint="Thanh toán tiền mặt" tone="amber" />
            <StatCard icon="credit-card" label="Thẻ / chuyển khoản" value={formatVnd(summary.cardRevenue)} hint="Thanh toán không tiền mặt" tone="accent" />
          </div>

          <div className="console-card">
            <div className="console-card__head">
              <div>
                <h2>Giao dịch gần đây</h2>
                <p>10 giao dịch bán tại quầy mới nhất trong ngày</p>
              </div>
              <span className="badge badge-accent">{summary.date}</span>
            </div>
            <div className="console-table-wrap">
              <table className="console-table">
                <thead>
                  <tr>
                    <th>Mã đơn</th>
                    <th>Khách hàng</th>
                    <th>Phim</th>
                    <th>Suất</th>
                    <th>Ghế</th>
                    <th>Thanh toán</th>
                    <th className="num">Tổng tiền</th>
                    <th>Trạng thái</th>
                  </tr>
                </thead>
                <tbody>
                  {summary.recentSales.length === 0 ? (
                    <tr>
                      <td colSpan={8}>
                        <div className="console-empty">
                          <strong>Chưa có giao dịch</strong>
                          Doanh thu ca sẽ xuất hiện sau khi bán vé tại quầy.
                        </div>
                      </td>
                    </tr>
                  ) : summary.recentSales.map((booking) => (
                    <tr key={booking.id}>
                      <td className="mono">{booking.bookingCode}</td>
                      <td>
                        <div className="cell-main">{booking.customerName || 'Khách lẻ'}</div>
                        <div className="cell-sub">{booking.customerPhone || '—'}</div>
                      </td>
                      <td>{booking.showtime.movieTitle}</td>
                      <td>{formatTime(booking.showtime.startTime)}</td>
                      <td>{booking.seats.map((seat) => seat.seatCode).join(', ')}</td>
                      <td><StatusBadge status={booking.paymentMethod} /></td>
                      <td className="num">{formatVnd(booking.totalAmount)}</td>
                      <td><StatusBadge status={booking.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="pos-print-area">
            <h2>CINEX</h2>
            <p>Báo cáo doanh thu ca · {summary.date}</p>
            <div>Tổng doanh thu: {formatVnd(summary.totalRevenue)}</div>
            <div>Số vé đã bán: {summary.ticketCount}</div>
            <div>Số giao dịch: {summary.transactionCount}</div>
            <div>Tiền mặt: {formatVnd(summary.cashRevenue)}</div>
            <div>Thẻ / chuyển khoản: {formatVnd(summary.cardRevenue)}</div>
            <div style={{ marginTop: 20, fontWeight: 700 }}>Giao dịch gần đây</div>
            {summary.recentSales.map((booking) => (
              <div key={booking.id}>
                {booking.bookingCode} · {booking.showtime.movieTitle} · {booking.seats.map((seat) => seat.seatCode).join(', ')} · {formatVnd(booking.totalAmount)}
              </div>
            ))}
          </div>
        </>
      ) : null}
    </div>
  )
}
