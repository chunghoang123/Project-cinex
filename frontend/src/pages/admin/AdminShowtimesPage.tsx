import { useEffect, useState } from 'react'
import { ConsolePageHeader } from '../../components/console/ConsolePageHeader'
import { Modal } from '../../components/console/Modal'
import { StatusBadge } from '../../components/console/StatusBadge'
import { Icon } from '../../components/ui/Icon'
import { adminService } from '../../services/adminService'
import type { Auditorium, Cinema, Showtime } from '../../types/Admin'
import type { Movie } from '../../types/Movie'
import { formatVnd } from '../../utils/format'
import { errorMessage, formatTime, todayIso } from '../../utils/consoleFormat'

interface ShowtimeForm {
  movieId: number | ''
  cinemaId: number | ''
  auditoriumId: number | ''
  date: string
  time: string
  basePrice: number
}

const emptyForm = (date: string): ShowtimeForm => ({
  movieId: '',
  cinemaId: '',
  auditoriumId: '',
  date,
  time: '10:00',
  basePrice: 90000,
})

export function AdminShowtimesPage() {
  const [showtimes, setShowtimes] = useState<Showtime[]>([])
  const [movies, setMovies] = useState<Movie[]>([])
  const [cinemas, setCinemas] = useState<Cinema[]>([])
  const [auditoriums, setAuditoriums] = useState<Auditorium[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [date, setDate] = useState(todayIso())
  const [cinemaFilter, setCinemaFilter] = useState<number | ''>('')
  const [movieFilter, setMovieFilter] = useState<number | ''>('')

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Showtime | null>(null)
  const [form, setForm] = useState<ShowtimeForm>(emptyForm(todayIso()))
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  const loadShowtimes = () => {
    setLoading(true)
    setError('')
    adminService.getShowtimes({
      date,
      cinemaId: cinemaFilter === '' ? undefined : Number(cinemaFilter),
      movieId: movieFilter === '' ? undefined : Number(movieFilter),
    })
      .then(setShowtimes)
      .catch((err) => setError(errorMessage(err, 'Không tải được suất chiếu')))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    Promise.all([adminService.getMovies(), adminService.getCinemas()])
      .then(([movieList, cinemaList]) => {
        setMovies(movieList)
        setCinemas(cinemaList)
      })
      .catch((err) => setError(errorMessage(err, 'Không tải được dữ liệu danh mục')))
  }, [])

  useEffect(() => {
    loadShowtimes()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [date, cinemaFilter, movieFilter])

  useEffect(() => {
    if (form.cinemaId === '') {
      setAuditoriums([])
      return
    }
    adminService.getAuditoriums(Number(form.cinemaId))
      .then((items) => {
        setAuditoriums(items)
        setForm((current) => {
          if (current.auditoriumId !== '' && items.some((item) => item.id === current.auditoriumId)) {
            return current
          }
          return { ...current, auditoriumId: items[0]?.id ?? '' }
        })
      })
      .catch((err) => setFormError(errorMessage(err, 'Không tải được phòng chiếu')))
  }, [form.cinemaId])

  const openCreate = () => {
    setEditing(null)
    setForm({
      ...emptyForm(date),
      movieId: movies[0]?.id ?? '',
      cinemaId: cinemas[0]?.id ?? '',
    })
    setFormError('')
    setModalOpen(true)
  }

  const openEdit = (showtime: Showtime) => {
    const [day, timePart] = showtime.startTime.split('T')
    setEditing(showtime)
    setForm({
      movieId: showtime.movieId,
      cinemaId: showtime.cinemaId,
      auditoriumId: showtime.auditoriumId,
      date: day,
      time: timePart?.slice(0, 5) || '10:00',
      basePrice: showtime.basePrice,
    })
    setFormError('')
    setModalOpen(true)
  }

  const save = async () => {
    if (form.movieId === '' || form.cinemaId === '' || form.auditoriumId === '') {
      setFormError('Vui lòng chọn phim, rạp và phòng chiếu')
      return
    }
    setSaving(true)
    setFormError('')
    try {
      const payload = {
        movieId: Number(form.movieId),
        auditoriumId: Number(form.auditoriumId),
        startTime: `${form.date}T${form.time}:00`,
        basePrice: Number(form.basePrice),
      }
      if (editing) {
        await adminService.updateShowtime(editing.id, payload)
      } else {
        await adminService.createShowtime(payload)
      }
      setModalOpen(false)
      loadShowtimes()
    } catch (err) {
      setFormError(errorMessage(err, 'Không lưu được suất chiếu'))
    } finally {
      setSaving(false)
    }
  }

  const cancelShowtime = async (showtime: Showtime) => {
    if (!window.confirm(`Hủy suất chiếu ${showtime.movieTitle} lúc ${formatTime(showtime.startTime)}?`)) return
    try {
      await adminService.deleteShowtime(showtime.id)
      loadShowtimes()
    } catch (err) {
      window.alert(errorMessage(err, 'Không thể hủy suất chiếu'))
    }
  }

  return (
    <div>
      <ConsolePageHeader
        eyebrow="Lịch chiếu"
        title="Quản lý suất chiếu"
        description="Lên lịch chiếu theo phim, rạp và phòng. Hệ thống tự sinh sơ đồ ghế và tính giá theo loại ghế."
        actions={
          <button type="button" className="btn btn-primary btn-sm" onClick={openCreate}>
            <Icon name="plus" size={16} /> Thêm suất chiếu
          </button>
        }
      />

      <div className="console-toolbar">
        <input
          className="input"
          type="date"
          value={date}
          onChange={(event) => setDate(event.target.value)}
          aria-label="Chọn ngày"
        />
        <select className="select" value={cinemaFilter} onChange={(event) => setCinemaFilter(event.target.value === '' ? '' : Number(event.target.value))}>
          <option value="">Tất cả rạp</option>
          {cinemas.map((cinema) => <option key={cinema.id} value={cinema.id}>{cinema.name}</option>)}
        </select>
        <select className="select" value={movieFilter} onChange={(event) => setMovieFilter(event.target.value === '' ? '' : Number(event.target.value))}>
          <option value="">Tất cả phim</option>
          {movies.map((movie) => <option key={movie.id} value={movie.id}>{movie.title}</option>)}
        </select>
        <span className="console-toolbar__spacer" />
        <span className="badge badge-muted">{showtimes.length} suất</span>
      </div>

      {loading ? (
        <div className="console-card console-empty">Đang tải lịch chiếu…</div>
      ) : error ? (
        <div className="console-card console-empty">
          <strong>Không tải được dữ liệu</strong>
          {error}
          <div style={{ marginTop: 14 }}>
            <button type="button" className="btn btn-ghost btn-sm" onClick={loadShowtimes}>Thử lại</button>
          </div>
        </div>
      ) : (
        <div className="console-table-wrap">
          <table className="console-table">
            <thead>
              <tr>
                <th>Phim</th>
                <th>Rạp / phòng</th>
                <th>Thời gian</th>
                <th className="num">Giá vé</th>
                <th className="num">Ghế trống</th>
                <th>Trạng thái</th>
                <th className="num">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {showtimes.length === 0 ? (
                <tr>
                  <td colSpan={7}>
                    <div className="console-empty">
                      <strong>Không có suất chiếu</strong>
                      Chọn ngày khác hoặc tạo suất chiếu mới.
                    </div>
                  </td>
                </tr>
              ) : showtimes.map((showtime) => (
                <tr key={showtime.id}>
                  <td>
                    <div className="admin-movie-cell">
                      {showtime.posterUrl && <img src={showtime.posterUrl} alt={showtime.movieTitle} />}
                      <div>
                        <strong>{showtime.movieTitle}</strong>
                        <span>{showtime.durationMinutes} phút · #{showtime.id}</span>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div className="cell-main">{showtime.cinemaName}</div>
                    <div className="cell-sub">{showtime.auditoriumName}</div>
                  </td>
                  <td>
                    <div className="cell-main">{formatTime(showtime.startTime)} – {formatTime(showtime.endTime)}</div>
                    <div className="cell-sub">{showtime.startTime.slice(0, 10).split('-').reverse().join('/')}</div>
                  </td>
                  <td className="num">{formatVnd(showtime.basePrice)}</td>
                  <td className="num">
                    <span className={showtime.availableSeats === 0 ? 'badge badge-muted' : 'badge badge-success'}>
                      {showtime.availableSeats}/{showtime.totalSeats}
                    </span>
                  </td>
                  <td><StatusBadge status={showtime.status} /></td>
                  <td>
                    <div className="cell-actions">
                      <button type="button" className="btn btn-ghost btn-sm" onClick={() => openEdit(showtime)}>
                        <Icon name="edit" size={14} /> Sửa
                      </button>
                      <button type="button" className="btn btn-danger btn-sm" onClick={() => cancelShowtime(showtime)}>
                        <Icon name="trash" size={14} /> Hủy
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal
        open={modalOpen}
        title={editing ? 'Cập nhật suất chiếu' : 'Thêm suất chiếu'}
        subtitle={editing ? 'Chỉ có thể sửa suất chiếu chưa có đơn đặt vé' : 'Chọn phim, phòng chiếu và khung giờ'}
        onClose={() => setModalOpen(false)}
        footer={
          <>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setModalOpen(false)}>Hủy</button>
            <button type="button" className="btn btn-primary btn-sm" onClick={save} disabled={saving}>
              {saving ? 'Đang lưu…' : editing ? 'Lưu thay đổi' : 'Tạo suất chiếu'}
            </button>
          </>
        }
      >
        {formError && <div className="form-error" style={{ marginBottom: 14 }}>{formError}</div>}
        <div className="console-form-grid">
          <div className="field field--full">
            <label>Phim</label>
            <select className="select" value={form.movieId} onChange={(e) => setForm({ ...form, movieId: Number(e.target.value) })}>
              <option value="">Chọn phim</option>
              {movies.map((movie) => <option key={movie.id} value={movie.id}>{movie.title}</option>)}
            </select>
          </div>
          <div className="field">
            <label>Rạp</label>
            <select className="select" value={form.cinemaId} onChange={(e) => setForm({ ...form, cinemaId: Number(e.target.value), auditoriumId: '' })}>
              <option value="">Chọn rạp</option>
              {cinemas.map((cinema) => <option key={cinema.id} value={cinema.id}>{cinema.name}</option>)}
            </select>
          </div>
          <div className="field">
            <label>Phòng chiếu</label>
            <select className="select" value={form.auditoriumId} onChange={(e) => setForm({ ...form, auditoriumId: Number(e.target.value) })}>
              <option value="">Chọn phòng</option>
              {auditoriums.map((auditorium) => <option key={auditorium.id} value={auditorium.id}>{auditorium.name} ({auditorium.seatCount} ghế)</option>)}
            </select>
          </div>
          <div className="field">
            <label>Ngày chiếu</label>
            <input className="input" type="date" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} />
          </div>
          <div className="field">
            <label>Giờ bắt đầu</label>
            <input className="input" type="time" value={form.time} onChange={(e) => setForm({ ...form, time: e.target.value })} />
          </div>
          <div className="field">
            <label>Giá vé cơ bản (đ)</label>
            <input className="input" type="number" min={0} step={1000} value={form.basePrice} onChange={(e) => setForm({ ...form, basePrice: Number(e.target.value) })} />
          </div>
        </div>
      </Modal>
    </div>
  )
}
