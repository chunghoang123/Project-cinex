import { useEffect, useState } from 'react'
import { ConsolePageHeader } from '../../components/console/ConsolePageHeader'
import { Modal } from '../../components/console/Modal'
import { StatusBadge } from '../../components/console/StatusBadge'
import { Icon } from '../../components/ui/Icon'
import { snacks } from '../../data/mockData'
import { catalogService } from '../../services/catalogService'
import { staffService } from '../../services/staffService'
import type { Cinema, Showtime, ShowtimeSeat, StaffSale } from '../../types/Admin'
import { formatVnd } from '../../utils/format'
import { errorMessage, formatDateTime, formatTime, todayIso } from '../../utils/consoleFormat'

type PaymentMethod = 'CASH' | 'CARD' | 'TRANSFER'

const PAYMENT_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Tiền mặt',
  CARD: 'Thẻ',
  TRANSFER: 'Chuyển khoản',
}

export function StaffPosPage() {
  const [date, setDate] = useState(todayIso())
  const [cinemas, setCinemas] = useState<Cinema[]>([])
  const [cinemaId, setCinemaId] = useState<number | ''>('')
  const [showtimes, setShowtimes] = useState<Showtime[]>([])
  const [showtimesLoading, setShowtimesLoading] = useState(false)
  const [selectedShowtime, setSelectedShowtime] = useState<Showtime | null>(null)

  const [seats, setSeats] = useState<ShowtimeSeat[]>([])
  const [seatsLoading, setSeatsLoading] = useState(false)
  const [seatVersion, setSeatVersion] = useState(0)
  const [selectedSeatIds, setSelectedSeatIds] = useState<number[]>([])

  const [customerName, setCustomerName] = useState('')
  const [customerPhone, setCustomerPhone] = useState('')
  const [snackQty, setSnackQty] = useState<Record<string, number>>({})
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CASH')

  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [sale, setSale] = useState<StaffSale | null>(null)
  const [successOpen, setSuccessOpen] = useState(false)

  const fetchShowtimes = async () => {
    setShowtimesLoading(true)
    try {
      const items = await staffService.getShowtimes({
        date,
        cinemaId: cinemaId === '' ? undefined : Number(cinemaId),
      })
      setShowtimes(items)
      setSelectedShowtime((current) => {
        if (current && items.some((item) => item.id === current.id)) return current
        return items[0] ?? null
      })
    } catch (err) {
      setError(errorMessage(err, 'Không tải được suất chiếu'))
    } finally {
      setShowtimesLoading(false)
    }
  }

  useEffect(() => {
    catalogService.getCinemas().then(setCinemas).catch(() => undefined)
  }, [])

  useEffect(() => {
    fetchShowtimes()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [date, cinemaId])

  useEffect(() => {
    if (!selectedShowtime) {
      setSeats([])
      return
    }
    setSeatsLoading(true)
    setError('')
    staffService.getSeats(selectedShowtime.id)
      .then((items) => {
        setSeats(items)
        setSelectedSeatIds([])
      })
      .catch((err) => setError(errorMessage(err, 'Không tải được sơ đồ ghế')))
      .finally(() => setSeatsLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedShowtime?.id, seatVersion])

  const selectedSeats = seats.filter((seat) => selectedSeatIds.includes(seat.id))
  const seatTotal = selectedSeats.reduce((sum, seat) => sum + Number(seat.price), 0)
  const snackAmount = snacks.reduce(
    (sum, snack) => sum + snack.price * (snackQty[snack.id] || 0),
    0
  )
  const totalAmount = seatTotal + snackAmount
  const snackNote = snacks
    .filter((snack) => (snackQty[snack.id] || 0) > 0)
    .map((snack) => `${snack.name} x${snackQty[snack.id]}`)
    .join(', ')

  const rows = Array.from(new Set(seats.map((seat) => seat.rowNumber))).sort((a, b) => a - b)
  const columnCount = Math.max(...seats.map((seat) => seat.columnNumber), 12)

  const toggleSeat = (seat: ShowtimeSeat) => {
    if (seat.status !== 'AVAILABLE') return
    setSelectedSeatIds((current) =>
      current.includes(seat.id)
        ? current.filter((id) => id !== seat.id)
        : [...current, seat.id]
    )
  }

  const changeSnack = (id: string, delta: number) => {
    setSnackQty((current) => {
      const next = Math.max(0, (current[id] || 0) + delta)
      return { ...current, [id]: next }
    })
  }

  const resetSale = () => {
    setSelectedSeatIds([])
    setCustomerName('')
    setCustomerPhone('')
    setSnackQty({})
    setPaymentMethod('CASH')
    setError('')
    setSeatVersion((value) => value + 1)
    fetchShowtimes()
  }

  const submitSale = async () => {
    if (!selectedShowtime) {
      setError('Vui lòng chọn suất chiếu')
      return
    }
    if (selectedSeatIds.length === 0) {
      setError('Vui lòng chọn ít nhất một ghế')
      return
    }
    setSubmitting(true)
    setError('')
    try {
      const result = await staffService.createSale({
        showtimeId: selectedShowtime.id,
        seatIds: selectedSeatIds,
        customerName: customerName.trim() || 'Khách lẻ',
        customerPhone: customerPhone.trim() || undefined,
        paymentMethod,
        snackAmount,
        snackNote: snackNote || undefined,
      })
      setSale(result)
      setSuccessOpen(true)
      resetSale()
    } catch (err) {
      setError(errorMessage(err, 'Không thể hoàn tất bán vé'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div>
      <ConsolePageHeader
        eyebrow="POS bán vé"
        title="Bán vé tại quầy"
        description="Chọn suất chiếu, ghế, bắp nước và thanh toán. Vé điện tử được phát hành ngay sau giao dịch."
      />

      <div className="pos-layout">
        <aside className="pos-sidebar">
          <div className="console-card pos-filter">
            <div className="field">
              <label>Ngày chiếu</label>
              <input className="input" type="date" value={date} onChange={(event) => setDate(event.target.value)} />
            </div>
            <div className="field">
              <label>Rạp</label>
              <select
                className="select"
                value={cinemaId}
                onChange={(event) => setCinemaId(event.target.value === '' ? '' : Number(event.target.value))}
              >
                <option value="">Tất cả rạp</option>
                {cinemas.map((cinema) => <option key={cinema.id} value={cinema.id}>{cinema.name}</option>)}
              </select>
            </div>
          </div>

          <div className="console-card">
            <div className="console-card__head">
              <div>
                <h2>Suất chiếu</h2>
                <p>{showtimesLoading ? 'Đang tải…' : `${showtimes.length} suất khả dụng`}</p>
              </div>
              <button type="button" className="icon-btn" onClick={fetchShowtimes} aria-label="Tải lại">
                <Icon name="refresh" size={16} />
              </button>
            </div>

            {showtimesLoading ? (
              <div className="console-empty">Đang tải suất chiếu…</div>
            ) : showtimes.length === 0 ? (
              <div className="console-empty">
                <strong>Không có suất chiếu</strong>
                Chọn ngày hoặc rạp khác.
              </div>
            ) : (
              <div className="pos-showtime-list">
                {showtimes.map((showtime) => (
                  <button
                    type="button"
                    key={showtime.id}
                    className={`pos-showtime ${selectedShowtime?.id === showtime.id ? 'is-active' : ''}`}
                    onClick={() => setSelectedShowtime(showtime)}
                  >
                    <div>
                      <div className="pos-showtime__time">{formatTime(showtime.startTime)}</div>
                      <div className="pos-showtime__meta">{showtime.movieTitle}</div>
                      <div className="pos-showtime__meta">{showtime.cinemaName} · {showtime.auditoriumName}</div>
                    </div>
                    <span className="pos-showtime__seats">{showtime.availableSeats}/{showtime.totalSeats}<br />ghế</span>
                  </button>
                ))}
              </div>
            )}
          </div>
        </aside>

        <section className="pos-work">
          <div className="pos-work__main">
            {!selectedShowtime ? (
              <div className="console-card console-empty">
                <strong>Chọn một suất chiếu để bắt đầu</strong>
                Danh sách suất chiếu nằm ở cột bên trái.
              </div>
            ) : (
              <>
                <div className="console-card">
                  <div className="console-card__head">
                    <div>
                      <h2>{selectedShowtime.movieTitle}</h2>
                      <p>
                        {formatDateTime(selectedShowtime.startTime)} · {selectedShowtime.cinemaName} · {selectedShowtime.auditoriumName}
                      </p>
                    </div>
                    <StatusBadge status={selectedShowtime.status} />
                  </div>

                  <div className="pos-screen-label">Màn hình</div>
                  <div className="pos-screen" />

                  {seatsLoading ? (
                    <div className="console-empty">Đang tải sơ đồ ghế…</div>
                  ) : seats.length === 0 ? (
                    <div className="console-empty">
                      <strong>Suất chiếu chưa có ghế</strong>
                      Vui lòng kiểm tra cấu hình phòng chiếu.
                    </div>
                  ) : (
                    <div className="pos-seat-map">
                      {rows.map((row) => {
                        const rowSeats = seats
                          .filter((seat) => seat.rowNumber === row)
                          .sort((a, b) => a.columnNumber - b.columnNumber)
                        const rowLabel = rowSeats[0]?.seatCode.replace(/\d+$/, '') ?? String(row)
                        return (
                          <div
                            className="pos-seat-row"
                            key={row}
                            style={{ gridTemplateColumns: `22px repeat(${columnCount}, minmax(24px, 1fr))` }}
                          >
                            <span className="pos-seat-row__label">{rowLabel}</span>
                            {rowSeats.map((seat) => {
                              const selected = selectedSeatIds.includes(seat.id)
                              const disabled = seat.status !== 'AVAILABLE'
                              return (
                                <button
                                  type="button"
                                  key={seat.id}
                                  className={[
                                    'pos-seat',
                                    seat.seatType === 'VIP' ? 'is-vip' : '',
                                    seat.seatType === 'COUPLE' ? 'is-couple' : '',
                                    selected ? 'is-selected' : '',
                                    seat.status === 'BOOKED' ? 'is-booked' : '',
                                    seat.status === 'HELD' ? 'is-held' : '',
                                  ].filter(Boolean).join(' ')}
                                  disabled={disabled}
                                  onClick={() => toggleSeat(seat)}
                                  title={`${seat.seatCode} · ${seat.seatType} · ${formatVnd(Number(seat.price))}`}
                                >
                                  {seat.seatCode}
                                </button>
                              )
                            })}
                          </div>
                        )
                      })}
                    </div>
                  )}

                  <div className="pos-legend">
                    <span><i /> Trống</span>
                    <span><i className="is-selected" /> Đang chọn</span>
                    <span><i className="is-vip" /> VIP</span>
                    <span><i className="is-couple" /> Couple</span>
                    <span><i className="is-booked" /> Đã bán</span>
                  </div>
                </div>

                <div className="console-card">
                  <div className="console-card__head">
                    <div>
                      <h2>Thông tin khách</h2>
                      <p>Không bắt buộc · dùng để tra cứu đơn sau này</p>
                    </div>
                  </div>
                  <div className="console-form-grid">
                    <div className="field">
                      <label>Tên khách hàng</label>
                      <input className="input" value={customerName} onChange={(event) => setCustomerName(event.target.value)} placeholder="Khách lẻ" />
                    </div>
                    <div className="field">
                      <label>Số điện thoại</label>
                      <input className="input" value={customerPhone} onChange={(event) => setCustomerPhone(event.target.value)} placeholder="09xx xxx xxx" />
                    </div>
                  </div>
                </div>
              </>
            )}
          </div>

          <aside className="pos-cart">
            <div className="console-card">
              <div className="console-card__head">
                <div>
                  <h2>Đơn bán vé</h2>
                  <p>{selectedSeats.length} ghế · {snackNote ? 'có bắp nước' : 'chưa có bắp nước'}</p>
                </div>
                <Icon name="receipt" size={18} />
              </div>

              <div className="pos-cart-section">
                <h3>Ghế đã chọn</h3>
                <div className="pos-selected-seats">
                  {selectedSeats.length === 0 ? (
                    <em>Chưa chọn ghế</em>
                  ) : selectedSeats.map((seat) => (
                    <span key={seat.id}>{seat.seatCode}</span>
                  ))}
                </div>
              </div>

              <div className="pos-cart-section">
                <h3>Bắp nước</h3>
                <div className="pos-snacks">
                  {snacks.map((snack) => (
                    <div className="pos-snack" key={snack.id}>
                      <div>
                        <strong>{snack.name}</strong>
                        <span>{formatVnd(snack.price)}</span>
                      </div>
                      <div className="pos-snack__qty">
                        <button type="button" onClick={() => changeSnack(snack.id, -1)} aria-label={`Giảm ${snack.name}`}>
                          <Icon name="close" size={11} />
                        </button>
                        <b>{snackQty[snack.id] || 0}</b>
                        <button type="button" onClick={() => changeSnack(snack.id, 1)} aria-label={`Thêm ${snack.name}`}>
                          <Icon name="plus" size={11} />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="pos-cart-section">
                <h3>Thanh toán</h3>
                <div className="pos-payment-methods">
                  {(Object.keys(PAYMENT_LABELS) as PaymentMethod[]).map((method) => (
                    <button
                      type="button"
                      key={method}
                      className={`pos-payment-method ${paymentMethod === method ? 'is-active' : ''}`}
                      onClick={() => setPaymentMethod(method)}
                    >
                      <Icon name={method === 'CASH' ? 'cash' : method === 'CARD' ? 'credit-card' : 'globe'} size={17} />
                      {PAYMENT_LABELS[method]}
                    </button>
                  ))}
                </div>
              </div>

              <div className="pos-cart-section">
                <div className="pos-line"><span>Tiền ghế</span><strong>{formatVnd(seatTotal)}</strong></div>
                <div className="pos-line"><span>Bắp nước</span><strong>{formatVnd(snackAmount)}</strong></div>
                <div className="pos-line pos-line--total"><span>Tổng cộng</span><strong>{formatVnd(totalAmount)}</strong></div>
              </div>

              {error && <div className="form-error" style={{ marginBottom: 12 }}>{error}</div>}

              <button
                type="button"
                className="btn btn-gradient btn-block"
                onClick={submitSale}
                disabled={submitting || !selectedShowtime || selectedSeatIds.length === 0}
              >
                <Icon name="ticket" size={17} />
                {submitting ? 'Đang xử lý…' : 'Xác nhận bán vé'}
              </button>
            </div>
          </aside>
        </section>
      </div>

      <Modal
        open={successOpen}
        title="Bán vé thành công"
        subtitle="Vé đã được phát hành và ghế đã được cập nhật"
        onClose={() => setSuccessOpen(false)}
        size="sm"
        footer={
          <>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => window.print()}>
              <Icon name="printer" size={15} /> In vé
            </button>
            <button type="button" className="btn btn-primary btn-sm" onClick={() => setSuccessOpen(false)}>
              Bán vé tiếp
            </button>
          </>
        }
      >
        {sale && (
          <div className="pos-success">
            <div className="pos-success__icon"><Icon name="check-circle" size={30} /></div>
            <h3>Đã phát hành vé</h3>
            <p>{sale.booking.customerName || 'Khách lẻ'} · {sale.booking.seats.length} ghế</p>
            <div className="pos-ticket-code">{sale.ticketCode}</div>
            <div className="pos-success__meta">
              <div><span>Mã đơn</span><strong>{sale.booking.bookingCode}</strong></div>
              <div><span>Phim</span><strong>{sale.booking.showtime.movieTitle}</strong></div>
              <div><span>Suất chiếu</span><strong>{formatDateTime(sale.booking.showtime.startTime)}</strong></div>
              <div><span>Rạp / phòng</span><strong>{sale.booking.showtime.cinemaName} · {sale.booking.showtime.auditoriumName}</strong></div>
              <div><span>Ghế</span><strong>{sale.booking.seats.map((seat) => seat.seatCode).join(', ')}</strong></div>
              <div><span>Thanh toán</span><strong>{formatVnd(sale.booking.totalAmount)} · {PAYMENT_LABELS[(sale.booking.paymentMethod as PaymentMethod) || 'CASH']}</strong></div>
            </div>

            <div className="pos-print-area">
              <h2>CINEX</h2>
              <p>Vé xem phim · {sale.ticketCode}</p>
              <div>Phim: {sale.booking.showtime.movieTitle}</div>
              <div>Suất: {formatDateTime(sale.booking.showtime.startTime)}</div>
              <div>Rạp: {sale.booking.showtime.cinemaName} · {sale.booking.showtime.auditoriumName}</div>
              <div>Ghế: {sale.booking.seats.map((seat) => seat.seatCode).join(', ')}</div>
              <div>Tổng: {formatVnd(sale.booking.totalAmount)}</div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
