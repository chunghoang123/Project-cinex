import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ConsolePageHeader } from '../../components/console/ConsolePageHeader'
import { Modal } from '../../components/console/Modal'
import { StatusBadge } from '../../components/console/StatusBadge'
import { Icon } from '../../components/ui/Icon'
import { adminService } from '../../services/adminService'
import type { Booking } from '../../types/Admin'
import { formatVnd } from '../../utils/format'
import { errorMessage, formatDateTime, formatTime, todayIso } from '../../utils/consoleFormat'

export function AdminBookingsPage() {
  const [searchParams] = useSearchParams()
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [status, setStatus] = useState('')
  const [date, setDate] = useState(todayIso())
  const [q, setQ] = useState(searchParams.get('q') ?? '')
  const [selected, setSelected] = useState<Booking | null>(null)
  const [cancelling, setCancelling] = useState(false)

  const load = () => {
    setLoading(true)
    setError('')
    adminService.getBookings({
      status: status || undefined,
      date: date || undefined,
      q: q.trim() || undefined,
    })
      .then(setBookings)
      .catch((err) => setError(errorMessage(err, 'Không tải được danh sách đơn')))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, date])

  const submitSearch = (event: React.FormEvent) => {
    event.preventDefault()
    load()
  }

  const cancelBooking = async (booking: Booking) => {
    if (!window.confirm(`Hủy đơn ${booking.bookingCode}? Ghế sẽ được trả lại và vé bị vô hiệu hóa.`)) return
    setCancelling(true)
    try {
      const updated = await adminService.cancelBooking(booking.bookingCode)
      setSelected((current) => current?.bookingCode === updated.bookingCode ? updated : current)
      load()
    } catch (err) {
      window.alert(errorMessage(err, 'Không thể hủy đơn'))
    } finally {
      setCancelling(false)
    }
  }

  return (
    <div>
      <ConsolePageHeader
        eyebrow="Giao dịch"
        title="Quản lý đơn đặt vé"
        description="Tra cứu đơn theo mã, số điện thoại hoặc tên khách. Hủy đơn sẽ trả ghế về trạng thái trống."
      />

      <form className="console-toolbar" onSubmit={submitSearch}>
        <div className="input-wrap" style={{ flex: 1, minWidth: 220 }}>
          <Icon name="search" size={16} />
          <input
            className="input"
            placeholder="Mã đơn, SĐT hoặc tên khách…"
            value={q}
            onChange={(event) => setQ(event.target.value)}
          />
        </div>
        <select className="select" value={status} onChange={(event) => setStatus(event.target.value)}>
          <option value="">Tất cả trạng thái</option>
          <option value="PENDING">Chờ thanh toán</option>
          <option value="CONFIRMED">Đã xác nhận</option>
          <option value="EXPIRED">Hết hạn</option>
          <option value="CANCELLED">Đã hủy</option>
        </select>
        <input className="input" type="date" value={date} onChange={(event) => setDate(event.target.value)} aria-label="Lọc theo ngày" />
        <button type="submit" className="btn btn-ghost btn-sm">
          <Icon name="search" size={15} /> Tìm
        </button>
        <span className="console-toolbar__spacer" />
        <span className="badge badge-muted">{bookings.length} đơn</span>
      </form>

      {loading ? (
        <div className="console-card console-empty">Đang tải danh sách đơn…</div>
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
                <th>Mã đơn</th>
                <th>Khách hàng</th>
                <th>Phim / suất chiếu</th>
                <th>Ghế</th>
                <th>Kênh</th>
                <th className="num">Tổng tiền</th>
                <th>Thanh toán</th>
                <th>Trạng thái</th>
                <th className="num">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {bookings.length === 0 ? (
                <tr>
                  <td colSpan={9}>
                    <div className="console-empty">
                      <strong>Không có đơn phù hợp</strong>
                      Thử đổi bộ lọc hoặc từ khóa tìm kiếm.
                    </div>
                  </td>
                </tr>
              ) : bookings.map((booking) => (
                <tr key={booking.id}>
                  <td className="mono">{booking.bookingCode}</td>
                  <td>
                    <div className="cell-main">{booking.customerName || 'Khách lẻ'}</div>
                    <div className="cell-sub">{booking.customerPhone || '—'}</div>
                  </td>
                  <td>
                    <div className="cell-main">{booking.showtime.movieTitle}</div>
                    <div className="cell-sub">
                      {formatTime(booking.showtime.startTime)} · {booking.showtime.cinemaName}
                    </div>
                  </td>
                  <td>
                    <span className="badge badge-accent">
                      {booking.seats.map((seat) => seat.seatCode).join(', ') || '—'}
                    </span>
                  </td>
                  <td><StatusBadge status={booking.channel} /></td>
                  <td className="num">{formatVnd(booking.totalAmount)}</td>
                  <td>
                    <div className="cell-main"><StatusBadge status={booking.paymentMethod} /></div>
                    <div className="cell-sub"><StatusBadge status={booking.paymentStatus} /></div>
                  </td>
                  <td><StatusBadge status={booking.status} /></td>
                  <td>
                    <div className="cell-actions">
                      <button type="button" className="btn btn-ghost btn-sm" onClick={() => setSelected(booking)}>
                        <Icon name="eye" size={14} /> Xem
                      </button>
                      {booking.status !== 'CANCELLED' && (
                        <button type="button" className="btn btn-danger btn-sm" onClick={() => cancelBooking(booking)} disabled={cancelling}>
                          <Icon name="close" size={14} /> Hủy
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal
        open={Boolean(selected)}
        title="Chi tiết đơn đặt vé"
        subtitle={selected ? `Mã đơn ${selected.bookingCode}` : ''}
        onClose={() => setSelected(null)}
        size="lg"
        footer={
          <>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setSelected(null)}>Đóng</button>
            {selected && selected.status !== 'CANCELLED' && (
              <button type="button" className="btn btn-danger btn-sm" onClick={() => cancelBooking(selected)} disabled={cancelling}>
                {cancelling ? 'Đang hủy…' : 'Hủy đơn'}
              </button>
            )}
          </>
        }
      >
        {selected && (
          <div>
            <div className="console-grid console-grid--2">
              <div className="console-card" style={{ background: 'var(--surface-2)' }}>
                <div className="console-card__head">
                  <div>
                    <h3>Khách hàng</h3>
                    <p>{selected.customerName || 'Khách lẻ'}</p>
                  </div>
                  <StatusBadge status={selected.channel} />
                </div>
                <div className="pos-line"><span>Điện thoại</span><strong>{selected.customerPhone || '—'}</strong></div>
                <div className="pos-line"><span>Ngày tạo</span><strong>{formatDateTime(selected.createdAt)}</strong></div>
                <div className="pos-line"><span>Trạng thái</span><strong><StatusBadge status={selected.status} /></strong></div>
              </div>
              <div className="console-card" style={{ background: 'var(--surface-2)' }}>
                <div className="console-card__head">
                  <div>
                    <h3>Suất chiếu</h3>
                    <p>{selected.showtime.movieTitle}</p>
                  </div>
                </div>
                <div className="pos-line"><span>Rạp</span><strong>{selected.showtime.cinemaName}</strong></div>
                <div className="pos-line"><span>Phòng</span><strong>{selected.showtime.auditoriumName}</strong></div>
                <div className="pos-line"><span>Thời gian</span><strong>{formatDateTime(selected.showtime.startTime)}</strong></div>
              </div>
            </div>

            <div className="console-card" style={{ marginTop: 14, background: 'var(--surface-2)' }}>
              <div className="console-card__head">
                <div>
                  <h3>Ghế & thanh toán</h3>
                  <p>{selected.seats.map((seat) => seat.seatCode).join(', ') || 'Chưa có ghế'}</p>
                </div>
                <StatusBadge status={selected.ticketStatus} />
              </div>
              <div className="pos-line"><span>Tiền ghế</span><strong>{formatVnd(selected.seatAmount)}</strong></div>
              <div className="pos-line"><span>Bắp nước</span><strong>{formatVnd(selected.snackAmount)}</strong></div>
              {selected.snackNote && <div className="pos-line"><span>Ghi chú</span><strong>{selected.snackNote}</strong></div>}
              <div className="pos-line"><span>Phương thức</span><strong><StatusBadge status={selected.paymentMethod} /></strong></div>
              <div className="pos-line"><span>Trạng thái thanh toán</span><strong><StatusBadge status={selected.paymentStatus} /></strong></div>
              <div className="pos-line"><span>Mã vé</span><strong className="mono">{selected.ticketCode || '—'}</strong></div>
              <div className="pos-line pos-line--total"><span>Tổng cộng</span><strong>{formatVnd(selected.totalAmount)}</strong></div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
