import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ConsolePageHeader } from '../../components/console/ConsolePageHeader'
import { Modal } from '../../components/console/Modal'
import { StatusBadge } from '../../components/console/StatusBadge'
import { Icon } from '../../components/ui/Icon'
import { adminService, youtubeService } from '../../services/adminService'
import type { YoutubeTrailer } from '../../services/adminService'
import type { Movie } from '../../types/Movie'
import { formatDuration } from '../../utils/format'
import { errorMessage } from '../../utils/consoleFormat'

interface MovieForm {
  title: string
  description: string
  posterUrl: string
  trailerUrl: string
  durationMinutes: number
  ageRating: number
  genre: string
  status: 'NOW_SHOWING' | 'COMING_SOON'
}

const EMPTY_FORM: MovieForm = {
  title: '',
  description: '',
  posterUrl: '',
  trailerUrl: '',
  durationMinutes: 120,
  ageRating: 13,
  genre: '',
  status: 'NOW_SHOWING',
}

export function AdminMoviesPage() {
  const [movies, setMovies] = useState<Movie[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Movie | null>(null)
  const [form, setForm] = useState<MovieForm>(EMPTY_FORM)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')
  const [search, setSearch] = useState('')
  const [trailerQuery, setTrailerQuery] = useState('')
  const [trailerResults, setTrailerResults] = useState<YoutubeTrailer[]>([])
  const [trailerLoading, setTrailerLoading] = useState(false)
  const [trailerError, setTrailerError] = useState('')

  const load = () => {
    setLoading(true)
    setError('')
    adminService.getMovies()
      .then(setMovies)
      .catch((err) => setError(errorMessage(err, 'Không tải được danh sách phim')))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
    const onRefresh = () => load()
    window.addEventListener('cinex:admin-refresh', onRefresh)
    return () => window.removeEventListener('cinex:admin-refresh', onRefresh)
  }, [])

  const openCreate = () => {
    setEditing(null)
    setForm(EMPTY_FORM)
    setFormError('')
    setTrailerQuery('')
    setTrailerResults([])
    setTrailerError('')
    setModalOpen(true)
  }

  const openEdit = (movie: Movie) => {
    setEditing(movie)
    setForm({
      title: movie.title,
      description: movie.description || '',
      posterUrl: movie.posterUrl || '',
      trailerUrl: movie.trailerUrl || '',
      durationMinutes: movie.durationMinutes || 120,
      ageRating: movie.ageRating || 0,
      genre: movie.genre || '',
      status: movie.status,
    })
    setFormError('')
    setTrailerQuery(movie.title)
    setTrailerResults([])
    setTrailerError('')
    setModalOpen(true)
  }

  const updateForm = <K extends keyof MovieForm>(key: K, value: MovieForm[K]) => {
    setForm((current) => ({ ...current, [key]: value }))
  }

  const searchTrailers = async (event: FormEvent) => {
    event.preventDefault()
    const query = trailerQuery.trim()
    if (!query) return
    setTrailerLoading(true)
    setTrailerError('')
    setTrailerResults([])
    try {
      const results = await youtubeService.searchTrailers(query, 8)
      if (results.length === 0) {
        setTrailerError('Không tìm thấy video YouTube nào phù hợp. Thử từ khoá khác.')
      } else {
        setTrailerResults(results)
      }
    } catch (err) {
      setTrailerError(
        errorMessage(err, 'Không tìm được trailer. Kiểm tra YOUTUBE_API_KEY đã cấu hình trên server chưa?'),
      )
    } finally {
      setTrailerLoading(false)
    }
  }

  const selectTrailer = (trailer: YoutubeTrailer) => {
    updateForm('trailerUrl', trailer.embedUrl)
    setTrailerResults([])
    setTrailerError('')
  }

  const save = async () => {
    const cleanTitle = form.title.trim().replace(/\s+/g, ' ')
    if (!cleanTitle) {
      setFormError('Vui lòng nhập tên phim')
      return
    }
    setSaving(true)
    setFormError('')
    try {
      const payload = {
        ...form,
        durationMinutes: Number(form.durationMinutes),
        ageRating: Number(form.ageRating),
      }
      if (editing) {
        await adminService.updateMovie(editing.id, payload)
      } else {
        await adminService.createMovie(payload)
      }
      setModalOpen(false)
      load()
    } catch (err) {
      setFormError(errorMessage(err, 'Không lưu được phim'))
    } finally {
      setSaving(false)
    }
  }

  const deleteMovie = async (movie: Movie) => {
    if (!window.confirm(`Xóa phim "${movie.title}" khỏi hệ thống?`)) return
    try {
      await adminService.deleteMovie(movie.id)
      load()
    } catch (err) {
      window.alert(errorMessage(err, 'Không thể ẩn phim'))
    }
  }

  const filtered = movies.filter((movie) => {
    const keyword = search.trim().toLowerCase()
    if (!keyword) return true
    return movie.title.toLowerCase().includes(keyword) || (movie.genre || '').toLowerCase().includes(keyword)
  })

  return (
    <div>
      <ConsolePageHeader
        eyebrow="Danh mục phim"
        title="Quản lý phim"
        description="Thêm, cập nhật và xóa phim khỏi hệ thống."
        actions={
          <>
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              onClick={() => openAdminAi('Thêm phim Lật Mặt 7 đang chiếu, 138 phút, 13+')}
            >
              <Icon name="sparkles" size={15} /> AI thêm phim
            </button>
            <button type="button" className="btn btn-primary btn-sm" onClick={openCreate}>
              <Icon name="plus" size={16} /> Thêm phim
            </button>
          </>
        }
      />

      <div className="console-toolbar">
        <div className="input-wrap" style={{ flex: 1, maxWidth: 320 }}>
          <Icon name="search" size={16} />
          <input
            className="input"
            placeholder="Tìm theo tên phim hoặc thể loại…"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
        <span className="console-toolbar__spacer" />
        <span className="badge badge-muted">{movies.length} phim</span>
      </div>

      {loading ? (
        <div className="console-card console-empty">Đang tải danh sách phim…</div>
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
                <th>Phim</th>
                <th>Thể loại</th>
                <th>Thời lượng</th>
                <th>Độ tuổi</th>
                <th>Trạng thái</th>
                <th className="num">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={7}>
                    <div className="console-empty">
                      <strong>Không có phim phù hợp</strong>
                      Thử từ khóa khác hoặc thêm phim mới.
                    </div>
                  </td>
                </tr>
              ) : filtered.map((movie) => (
                <tr key={movie.id}>
                  <td>
                    <div className="admin-movie-cell">
                      {movie.posterUrl ? (
                        <img src={movie.posterUrl} alt={movie.title} />
                      ) : (
                        <span className="console-avatar" style={{ width: 34, height: 48, borderRadius: 6 }}>
                          <Icon name="film" size={16} />
                        </span>
                      )}
                      <div>
                        <strong>{movie.title}</strong>
                        <span>#{movie.id}</span>
                      </div>
                    </div>
                  </td>
                  <td>{movie.genre || '—'}</td>
                  <td>{movie.durationMinutes ? formatDuration(movie.durationMinutes) : '—'}</td>
                  <td>{movie.ageRating ? `T${movie.ageRating}` : 'P'}</td>
                  <td><StatusBadge status={movie.status} /></td>
                  <td>
                    <span className={`badge ${movie.active ? 'badge-success' : 'badge-muted'}`}>
                      {movie.active ? 'Đang hiển thị' : 'Đã ẩn'}
                    </span>
                  </td>
                  <td>
                    <div className="cell-actions">
                      <button type="button" className="btn btn-ghost btn-sm" onClick={() => openEdit(movie)}>
                        <Icon name="edit" size={14} /> Sửa
                      </button>
                      <button type="button" className="btn btn-danger btn-sm" onClick={() => deleteMovie(movie)}>
                        <Icon name="trash" size={14} /> Xóa
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
        title={editing ? 'Cập nhật phim' : 'Thêm phim mới'}
        subtitle={editing ? `Đang sửa phim #${editing.id}` : 'Điền thông tin phim để đưa vào danh mục'}
        onClose={() => setModalOpen(false)}
        size="lg"
        footer={
          <>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setModalOpen(false)}>
              Hủy
            </button>
            <button type="button" className="btn btn-primary btn-sm" onClick={save} disabled={saving}>
              {saving ? 'Đang lưu…' : editing ? 'Lưu thay đổi' : 'Thêm phim'}
            </button>
          </>
        }
      >
        {formError && <div className="form-error" style={{ marginBottom: 14 }}>{formError}</div>}
        <div className="console-form-grid">
          <div className="field field--full">
            <label>Tên phim</label>
            <input className="input" value={form.title} onChange={(e) => updateForm('title', e.target.value)} placeholder="Ví dụ: Dune: Part Two" />
          </div>
          <div className="field">
            <label>Thể loại</label>
            <input className="input" value={form.genre} onChange={(e) => updateForm('genre', e.target.value)} placeholder="Sci-Fi, Adventure" />
          </div>
          <div className="field">
            <label>Trạng thái</label>
            <select className="select" value={form.status} onChange={(e) => updateForm('status', e.target.value as MovieForm['status'])}>
              <option value="NOW_SHOWING">Đang chiếu</option>
              <option value="COMING_SOON">Sắp chiếu</option>
            </select>
          </div>
          <div className="field">
            <label>Thời lượng (phút)</label>
            <input className="input" type="number" min={1} value={form.durationMinutes} onChange={(e) => updateForm('durationMinutes', Number(e.target.value))} />
          </div>
          <div className="field">
            <label>Độ tuổi</label>
            <input className="input" type="number" min={0} value={form.ageRating} onChange={(e) => updateForm('ageRating', Number(e.target.value))} />
          </div>
          <div className="field field--full">
            <label>URL poster</label>
            <input className="input" value={form.posterUrl} onChange={(e) => updateForm('posterUrl', e.target.value)} placeholder="https://…" />
          </div>
          <div className="field field--full">
            <label>URL trailer (dán watch/shorts/youtu.be đều được, hệ thống tự đổi sang embed)</label>
            <input className="input" value={form.trailerUrl} onChange={(e) => updateForm('trailerUrl', e.target.value)} placeholder="https://www.youtube.com/embed/…" />
          </div>
          <form className="field field--full" onSubmit={searchTrailers}>
            <label>Tìm trailer trên YouTube</label>
            <div style={{ display: 'flex', gap: 8 }}>
              <input
                className="input"
                value={trailerQuery}
                onChange={(e) => setTrailerQuery(e.target.value)}
                placeholder="Tên phim + official trailer"
                style={{ flex: 1 }}
              />
              <button type="submit" className="btn btn-ghost btn-sm" disabled={trailerLoading || !trailerQuery.trim()}>
                {trailerLoading ? 'Đang tìm…' : 'Tìm trailer'}
              </button>
            </div>
            {trailerError && <p className="field-error" style={{ marginTop: 8 }}>{trailerError}</p>}
            {trailerResults.length > 0 && (
              <div style={{ marginTop: 10, display: 'grid', gap: 8 }}>
                {trailerResults.map((trailer) => (
                  <button
                    key={trailer.videoId}
                    type="button"
                    className="btn btn-ghost btn-sm"
                    style={{ justifyContent: 'flex-start', textAlign: 'left', alignItems: 'center' }}
                    onClick={() => selectTrailer(trailer)}
                    title={`Dùng trailer này: ${trailer.title}`}
                  >
                    <img
                      src={trailer.thumbnailUrl}
                      alt=""
                      width={72}
                      height={40}
                      style={{ borderRadius: 6, objectFit: 'cover', flex: 'none' }}
                      loading="lazy"
                    />
                    <span style={{ flex: 1, minWidth: 0 }}>
                      <span style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {trailer.title}
                      </span>
                      <small className="muted">{trailer.channelTitle} · Bấm để dùng</small>
                    </span>
                  </button>
                ))}
              </div>
            )}
          </form>
          <div className="field field--full">
            <label>Mô tả</label>
            <textarea className="textarea" value={form.description} onChange={(e) => updateForm('description', e.target.value)} placeholder="Nội dung ngắn về phim…" />
          </div>
        </div>
      </Modal>
    </div>
  )
}
