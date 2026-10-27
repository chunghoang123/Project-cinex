import { useEffect, useState } from 'react'
import { ConsolePageHeader } from '../../components/console/ConsolePageHeader'
import { Modal } from '../../components/console/Modal'
import { StatusBadge } from '../../components/console/StatusBadge'
import { Icon } from '../../components/ui/Icon'
import { adminService } from '../../services/adminService'
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
  }, [])

  const openCreate = () => {
    setEditing(null)
    setForm(EMPTY_FORM)
    setFormError('')
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
    setModalOpen(true)
  }

  const updateForm = <K extends keyof MovieForm>(key: K, value: MovieForm[K]) => {
    setForm((current) => ({ ...current, [key]: value }))
  }

  const save = async () => {
    if (!form.title.trim()) {
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

  const hideMovie = async (movie: Movie) => {
    if (!window.confirm(`Ẩn phim "${movie.title}" khỏi danh sách hiển thị?`)) return
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
        description="Thêm, cập nhật và ẩn phim khỏi hệ thống. Suất chiếu sẽ sử dụng thông tin phim đang hoạt động."
        actions={
          <button type="button" className="btn btn-primary btn-sm" onClick={openCreate}>
            <Icon name="plus" size={16} /> Thêm phim
          </button>
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
                <th>Hiển thị</th>
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
                      {movie.active && (
                        <button type="button" className="btn btn-danger btn-sm" onClick={() => hideMovie(movie)}>
                          <Icon name="trash" size={14} /> Ẩn
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
            <label>URL trailer</label>
            <input className="input" value={form.trailerUrl} onChange={(e) => updateForm('trailerUrl', e.target.value)} placeholder="https://…" />
          </div>
          <div className="field field--full">
            <label>Mô tả</label>
            <textarea className="textarea" value={form.description} onChange={(e) => updateForm('description', e.target.value)} placeholder="Nội dung ngắn về phim…" />
          </div>
        </div>
      </Modal>
    </div>
  )
}
