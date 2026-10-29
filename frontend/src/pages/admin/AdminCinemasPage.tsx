import { useEffect, useState } from 'react'
import { ConsolePageHeader } from '../../components/console/ConsolePageHeader'
import { Modal } from '../../components/console/Modal'
import { Icon } from '../../components/ui/Icon'
import { adminService } from '../../services/adminService'
import type { Auditorium, AuditoriumPayload, Cinema, CinemaPayload } from '../../types/Admin'
import { errorMessage } from '../../utils/consoleFormat'

const EMPTY_CINEMA: CinemaPayload = { name: '', address: '', phone: '' }
const EMPTY_AUDITORIUM: AuditoriumPayload = { name: '', totalRows: 8, columnsPerRow: 12 }

export function AdminCinemasPage() {
  const [cinemas, setCinemas] = useState<Cinema[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [cinemaModalOpen, setCinemaModalOpen] = useState(false)
  const [editingCinema, setEditingCinema] = useState<Cinema | null>(null)
  const [cinemaForm, setCinemaForm] = useState<CinemaPayload>(EMPTY_CINEMA)
  const [cinemaError, setCinemaError] = useState('')
  const [savingCinema, setSavingCinema] = useState(false)

  const [auditoriumModalOpen, setAuditoriumModalOpen] = useState(false)
  const [selectedCinema, setSelectedCinema] = useState<Cinema | null>(null)
  const [auditoriums, setAuditoriums] = useState<Auditorium[]>([])
  const [auditoriumLoading, setAuditoriumLoading] = useState(false)
  const [auditoriumError, setAuditoriumError] = useState('')
  const [auditoriumForm, setAuditoriumForm] = useState<AuditoriumPayload>(EMPTY_AUDITORIUM)
  const [editingAuditorium, setEditingAuditorium] = useState<Auditorium | null>(null)
  const [savingAuditorium, setSavingAuditorium] = useState(false)

  const load = () => {
    setLoading(true)
    setError('')
    adminService.getCinemas()
      .then(setCinemas)
      .catch((err) => setError(errorMessage(err, 'Không tải được danh sách rạp')))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [])

  const openCreateCinema = () => {
    setEditingCinema(null)
    setCinemaForm(EMPTY_CINEMA)
    setCinemaError('')
    setCinemaModalOpen(true)
  }

  const openEditCinema = (cinema: Cinema) => {
    setEditingCinema(cinema)
    setCinemaForm({ name: cinema.name, address: cinema.address, phone: cinema.phone })
    setCinemaError('')
    setCinemaModalOpen(true)
  }

  const saveCinema = async () => {
    if (!cinemaForm.name.trim()) {
      setCinemaError('Vui lòng nhập tên rạp')
      return
    }
    setSavingCinema(true)
    setCinemaError('')
    try {
      if (editingCinema) {
        await adminService.updateCinema(editingCinema.id, cinemaForm)
      } else {
        await adminService.createCinema(cinemaForm)
      }
      setCinemaModalOpen(false)
      load()
    } catch (err) {
      setCinemaError(errorMessage(err, 'Không lưu được rạp'))
    } finally {
      setSavingCinema(false)
    }
  }

  const deactivateCinema = async (cinema: Cinema) => {
    if (!window.confirm(`Ngừng hoạt động rạp "${cinema.name}"?`)) return
    try {
      await adminService.deleteCinema(cinema.id)
      load()
    } catch (err) {
      window.alert(errorMessage(err, 'Không thể ngừng hoạt động rạp'))
    }
  }

  const openAuditoriums = async (cinema: Cinema) => {
    setSelectedCinema(cinema)
    setAuditoriumModalOpen(true)
    setAuditoriumForm(EMPTY_AUDITORIUM)
    setEditingAuditorium(null)
    setAuditoriumError('')
    await loadAuditoriums(cinema.id)
  }

  const loadAuditoriums = async (cinemaId: number) => {
    setAuditoriumLoading(true)
    setAuditoriumError('')
    try {
      setAuditoriums(await adminService.getAuditoriums(cinemaId))
    } catch (err) {
      setAuditoriumError(errorMessage(err, 'Không tải được phòng chiếu'))
    } finally {
      setAuditoriumLoading(false)
    }
  }

  const saveAuditorium = async () => {
    if (!selectedCinema) return
    if (!auditoriumForm.name.trim()) {
      setAuditoriumError('Vui lòng nhập tên phòng chiếu')
      return
    }
    setSavingAuditorium(true)
    setAuditoriumError('')
    try {
      if (editingAuditorium) {
        await adminService.updateAuditorium(editingAuditorium.id, auditoriumForm)
      } else {
        await adminService.createAuditorium(selectedCinema.id, auditoriumForm)
      }
      setAuditoriumForm(EMPTY_AUDITORIUM)
      setEditingAuditorium(null)
      await loadAuditoriums(selectedCinema.id)
    } catch (err) {
      setAuditoriumError(errorMessage(err, 'Không lưu được phòng chiếu'))
    } finally {
      setSavingAuditorium(false)
    }
  }

  const editAuditorium = (auditorium: Auditorium) => {
    setEditingAuditorium(auditorium)
    setAuditoriumForm({
      name: auditorium.name,
      totalRows: auditorium.totalRows,
      columnsPerRow: auditorium.columnsPerRow,
    })
  }

  const regenerateSeats = async (auditorium: Auditorium) => {
    try {
      await adminService.generateSeats(auditorium.id)
      if (selectedCinema) await loadAuditoriums(selectedCinema.id)
    } catch (err) {
      window.alert(errorMessage(err, 'Không thể tạo ghế'))
    }
  }

  const deactivateAuditorium = async (auditorium: Auditorium) => {
    if (!window.confirm(`Ngừng hoạt động phòng "${auditorium.name}"?`)) return
    try {
      await adminService.deleteAuditorium(auditorium.id)
      if (selectedCinema) await loadAuditoriums(selectedCinema.id)
    } catch (err) {
      window.alert(errorMessage(err, 'Không thể ngừng hoạt động phòng chiếu'))
    }
  }

  return (
    <div>
      <ConsolePageHeader
        eyebrow="Hạ tầng rạp"
        title="Rạp & phòng chiếu"
        description="Quản lý cụm rạp, phòng chiếu và sơ đồ ghế. Mỗi phòng chiếu được tạo ghế tự động theo số hàng và số cột."
        actions={
          <button type="button" className="btn btn-primary btn-sm" onClick={openCreateCinema}>
            <Icon name="plus" size={16} /> Thêm rạp
          </button>
        }
      />

      {loading ? (
        <div className="console-card console-empty">Đang tải danh sách rạp…</div>
      ) : error ? (
        <div className="console-card console-empty">
          <strong>Không tải được dữ liệu</strong>
          {error}
          <div style={{ marginTop: 14 }}>
            <button type="button" className="btn btn-ghost btn-sm" onClick={load}>Thử lại</button>
          </div>
        </div>
      ) : cinemas.length === 0 ? (
        <div className="console-card console-empty">
          <strong>Chưa có rạp nào</strong>
          Thêm rạp đầu tiên để bắt đầu cấu hình phòng chiếu.
        </div>
      ) : (
        <div className="console-grid console-grid--2">
          {cinemas.map((cinema) => (
            <div className="console-card" key={cinema.id}>
              <div className="console-card__head">
                <div>
                  <h2>{cinema.name}</h2>
                  <p>{cinema.address || 'Chưa cập nhật địa chỉ'}</p>
                </div>
                <span className={`badge ${cinema.active ? 'badge-success' : 'badge-muted'}`}>
                  {cinema.active ? 'Đang hoạt động' : 'Đã ngừng'}
                </span>
              </div>
              <div className="pos-line">
                <span>Điện thoại</span>
                <strong>{cinema.phone || '—'}</strong>
              </div>
              <div className="pos-line">
                <span>Mã rạp</span>
                <strong>#{cinema.id}</strong>
              </div>
              <div className="console-page-head__actions" style={{ marginTop: 14 }}>
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => openAuditoriums(cinema)}>
                  <Icon name="screen" size={15} /> Phòng chiếu
                </button>
                <button type="button" className="btn btn-ghost btn-sm" onClick={() => openEditCinema(cinema)}>
                  <Icon name="edit" size={15} /> Sửa
                </button>
                {cinema.active && (
                  <button type="button" className="btn btn-danger btn-sm" onClick={() => deactivateCinema(cinema)}>
                    <Icon name="trash" size={15} /> Ngừng
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal
        open={cinemaModalOpen}
        title={editingCinema ? 'Cập nhật rạp' : 'Thêm rạp mới'}
        subtitle={editingCinema ? `Đang sửa rạp #${editingCinema.id}` : 'Tạo cụm rạp mới trong hệ thống'}
        onClose={() => setCinemaModalOpen(false)}
        footer={
          <>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setCinemaModalOpen(false)}>Hủy</button>
            <button type="button" className="btn btn-primary btn-sm" onClick={saveCinema} disabled={savingCinema}>
              {savingCinema ? 'Đang lưu…' : editingCinema ? 'Lưu thay đổi' : 'Thêm rạp'}
            </button>
          </>
        }
      >
        {cinemaError && <div className="form-error" style={{ marginBottom: 14 }}>{cinemaError}</div>}
        <div className="console-form-grid">
          <div className="field field--full">
            <label>Tên rạp</label>
            <input className="input" value={cinemaForm.name} onChange={(e) => setCinemaForm({ ...cinemaForm, name: e.target.value })} placeholder="Cinex Landmark 81" />
          </div>
          <div className="field field--full">
            <label>Địa chỉ</label>
            <input className="input" value={cinemaForm.address} onChange={(e) => setCinemaForm({ ...cinemaForm, address: e.target.value })} placeholder="Số nhà, đường, quận, thành phố" />
          </div>
          <div className="field field--full">
            <label>Điện thoại</label>
            <input className="input" value={cinemaForm.phone} onChange={(e) => setCinemaForm({ ...cinemaForm, phone: e.target.value })} placeholder="1900 6017" />
          </div>
        </div>
      </Modal>

      <Modal
        open={auditoriumModalOpen}
        title={selectedCinema ? `Phòng chiếu · ${selectedCinema.name}` : 'Phòng chiếu'}
        subtitle="Tạo phòng mới hoặc cập nhật kích thước để hệ thống sinh ghế tự động"
        onClose={() => setAuditoriumModalOpen(false)}
        size="lg"
      >
        {auditoriumError && <div className="form-error" style={{ marginBottom: 14 }}>{auditoriumError}</div>}

        <div className="console-card" style={{ background: 'var(--surface-2)', marginBottom: 16 }}>
          <div className="console-card__head">
            <div>
              <h3>{editingAuditorium ? 'Cập nhật phòng chiếu' : 'Thêm phòng chiếu'}</h3>
              <p>Ghế được đặt tên theo hàng và cột, ví dụ A1, A2…</p>
            </div>
            {editingAuditorium && (
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={() => {
                  setEditingAuditorium(null)
                  setAuditoriumForm(EMPTY_AUDITORIUM)
                }}
              >
                Hủy sửa
              </button>
            )}
          </div>
          <div className="console-form-grid">
            <div className="field">
              <label>Tên phòng</label>
              <input className="input" value={auditoriumForm.name} onChange={(e) => setAuditoriumForm({ ...auditoriumForm, name: e.target.value })} placeholder="Screen 1" />
            </div>
            <div className="field">
              <label>Số hàng</label>
              <input className="input" type="number" min={1} max={30} value={auditoriumForm.totalRows} onChange={(e) => setAuditoriumForm({ ...auditoriumForm, totalRows: Number(e.target.value) })} />
            </div>
            <div className="field">
              <label>Số cột</label>
              <input className="input" type="number" min={1} max={30} value={auditoriumForm.columnsPerRow} onChange={(e) => setAuditoriumForm({ ...auditoriumForm, columnsPerRow: Number(e.target.value) })} />
            </div>
            <div className="field" style={{ justifyContent: 'flex-end' }}>
              <button type="button" className="btn btn-primary btn-sm" onClick={saveAuditorium} disabled={savingAuditorium}>
                {savingAuditorium ? 'Đang lưu…' : editingAuditorium ? 'Lưu phòng' : 'Thêm phòng'}
              </button>
            </div>
          </div>
        </div>

        {auditoriumLoading ? (
          <div className="console-empty">Đang tải phòng chiếu…</div>
        ) : auditoriums.length === 0 ? (
          <div className="console-empty">
            <strong>Chưa có phòng chiếu</strong>
            Tạo phòng đầu tiên bằng biểu mẫu phía trên.
          </div>
        ) : (
          <div className="auditorium-list">
            {auditoriums.map((auditorium) => (
              <div className="auditorium-item" key={auditorium.id}>
                <div>
                  <strong>{auditorium.name}</strong>
                  <span>{auditorium.totalRows} hàng × {auditorium.columnsPerRow} cột · {auditorium.seatCount} ghế</span>
                </div>
                <div className="cell-actions">
                  <button type="button" className="btn btn-ghost btn-sm" onClick={() => editAuditorium(auditorium)}>
                    <Icon name="edit" size={14} /> Sửa
                  </button>
                  <button type="button" className="btn btn-ghost btn-sm" onClick={() => regenerateSeats(auditorium)}>
                    <Icon name="refresh" size={14} /> Tạo ghế
                  </button>
                  {auditorium.active && (
                    <button type="button" className="btn btn-danger btn-sm" onClick={() => deactivateAuditorium(auditorium)}>
                      <Icon name="trash" size={14} />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </Modal>
    </div>
  )
}
