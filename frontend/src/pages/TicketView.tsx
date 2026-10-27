import { Link, useParams } from 'react-router-dom'
import { bookings, cinemas, movies, showtimesFor } from '../data/mockData'
import { formatVnd } from '../utils/format'
import { Icon } from '../components/ui/Icon'
import { PageBar } from '../components/ui/PageBar'
import { QrCode } from '../components/ui/QrCode'

export function TicketView() {
  const { bookingId } = useParams()
  const storedCode = sessionStorage.getItem('ticketCode')
  const code = bookingId ?? storedCode ?? bookings[0].code
  const booking = bookings.find((item) => item.code === code)

  const storedMovieId = sessionStorage.getItem('ticketMovieId')
  const storedCinemaId = sessionStorage.getItem('ticketCinemaId')
  const storedShowtimeId = sessionStorage.getItem('ticketShowtimeId')
  const storedSeats: string[] = JSON.parse(sessionStorage.getItem('ticketSeats') || '[]')
  const storedTotal = Number(sessionStorage.getItem('ticketTotal') || 0)

  const movie = movies.find((item) => String(item.id) === storedMovieId) ?? movies[0]
  const cinema = cinemas.find((item) => String(item.id) === storedCinemaId) ?? cinemas[0]
  const showtime = showtimesFor(movie.id).find((item) => String(item.id) === storedShowtimeId) ?? showtimesFor(movie.id)[0]

  const title = booking?.movieTitle ?? movie.title
  const cinemaName = booking?.cinema ?? cinema.name
  const hall = booking?.hall ?? showtime.hall
  const showDate = booking?.date ?? 'Hôm nay'
  const showTime = booking?.time ?? showtime.time
  const seats = booking?.seats ?? (storedSeats.length > 0 ? storedSeats : ['D5', 'D6'])
  const total = booking?.total ?? (storedTotal || 380000)

  const handleShare = async () => {
    const text = `Vé Cinex ${code} - ${title} - ${showTime} - Ghế ${seats.join(', ')}`
    if (navigator.share) {
      await navigator.share({ title: 'Vé Cinex', text })
    } else {
      await navigator.clipboard?.writeText(text)
    }
  }

  const handleDownload = () => {
    const text = [
      'CINEX E-TICKET',
      `Mã vé: ${code}`,
      `Phim: ${title}`,
      `Rạp: ${cinemaName}`,
      `Phòng: ${hall}`,
      `Ngày: ${showDate}`,
      `Suất chiếu: ${showTime}`,
      `Ghế: ${seats.join(', ')}`,
      `Tổng tiền: ${formatVnd(total)}`,
    ].join('\n')
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${code}.txt`
    link.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="container">
      <PageBar title="Vé của tôi" backTo="/history" />

      <div className="ticket-wrap">
        <div className="eticket">
          <div className="eticket__top">
            <div className="eticket__brand">
              <span className="brand">
                <span className="brand__mark">
                  <Icon name="film" size={18} />
                </span>
                <span>
                  Cine<em>x</em>
                </span>
              </span>
              <span className="badge badge-success">Đã thanh toán</span>
            </div>
            <span className="eyebrow">E-Ticket · {code}</span>
            <h1 className="eticket__title">{title}</h1>
            <div className="eticket__grid">
              <div>
                <span>Rạp</span>
                <strong>{cinemaName}</strong>
              </div>
              <div>
                <span>Phòng chiếu</span>
                <strong>{hall}</strong>
              </div>
              <div>
                <span>Ngày</span>
                <strong>{showDate}</strong>
              </div>
              <div>
                <span>Giờ</span>
                <strong>{showTime}</strong>
              </div>
            </div>
          </div>

          <div className="eticket__seat">
            <span>Ghế</span>
            <strong>{seats.join(' · ')}</strong>
          </div>

          <div className="qr-box">
            <QrCode value={code} size={150} />
          </div>

          <div className="eticket__foot">
            Quét mã QR tại quầy để nhận vé. Vui lòng có mặt trước 15 phút.
          </div>

          <span className="eticket__notch left" />
          <span className="eticket__notch right" />
        </div>

        <aside className="ticket-actions">
          <div className="surface card-pad">
            <h2 className="h3" style={{ marginBottom: 12 }}>
              Thông tin vé
            </h2>
            <div className="summary-line">
              <span>Mã vé</span>
              <strong>{code}</strong>
            </div>
            <div className="summary-line">
              <span>Ghế</span>
              <strong>{seats.join(', ')}</strong>
            </div>
            <div className="summary-line">
              <span>Tổng tiền</span>
              <strong>{formatVnd(total)}</strong>
            </div>
            <div className="summary-line">
              <span>Trạng thái</span>
              <strong style={{ color: 'var(--success)' }}>Đã thanh toán</strong>
            </div>

            <div className="stack gap-10" style={{ marginTop: 14 }}>
              <button type="button" className="btn btn-primary btn-block" onClick={handleDownload}>
                <Icon name="download" size={16} />
                Tải vé
              </button>
              <button type="button" className="btn btn-ghost btn-block" onClick={handleShare}>
                <Icon name="share" size={16} />
                Chia sẻ
              </button>
              <button type="button" className="btn btn-outline btn-block" onClick={() => window.print()}>
                <Icon name="ticket" size={16} />
                In vé
              </button>
            </div>
          </div>

          <Link to="/history" className="btn btn-ghost btn-block">
            Xem tất cả vé
          </Link>
          <Link to="/" className="btn btn-outline btn-block">
            Về trang chủ
          </Link>
        </aside>
      </div>
    </div>
  )
}
