import { useCallback, useEffect, useRef, useState } from 'react'
import { ConsolePageHeader } from '../../components/console/ConsolePageHeader'
import { StatusBadge } from '../../components/console/StatusBadge'
import { Icon } from '../../components/ui/Icon'
import { staffService } from '../../services/staffService'
import type { TicketValidation } from '../../types/Admin'
import { formatVnd } from '../../utils/format'
import { errorMessage, formatDateTime } from '../../utils/consoleFormat'

export function StaffValidatePage() {
  const [code, setCode] = useState('')
  const codeRef = useRef('')
  const videoRef = useRef<HTMLVideoElement | null>(null)
  const streamRef = useRef<MediaStream | null>(null)
  const rafRef = useRef(0)
  const cancelledRef = useRef(false)
  const [result, setResult] = useState<TicketValidation | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [cameraOn, setCameraOn] = useState(false)
  const [cameraError, setCameraError] = useState('')

  const stopCamera = useCallback(() => {
    cancelledRef.current = true
    cancelAnimationFrame(rafRef.current)
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
    if (videoRef.current) {
      videoRef.current.srcObject = null
      videoRef.current.pause()
    }
    setCameraOn(false)
  }, [])

  const runValidate = useCallback(async (raw: string) => {
    const value = raw.trim()
    if (!value) {
      setError('Vui lòng nhập mã vé')
      return
    }
    setLoading(true)
    setError('')
    setResult(null)
    try {
      setResult(await staffService.validateTicket(value))
    } catch (err) {
      setError(errorMessage(err, 'Không thể soát vé'))
    } finally {
      setLoading(false)
    }
  }, [])

  const startCamera = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
      streamRef.current = stream
      const video = videoRef.current
      if (video) {
        video.srcObject = stream
        await video.play()
      }
      setCameraError('')
      setCameraOn(true)
    } catch (err) {
      setCameraError(errorMessage(err, 'Không mở được camera để quét mã QR'))
    }
  }

  useEffect(() => {
    if (!cameraOn) return
    return () => stopCamera()
  }, [cameraOn, stopCamera])

  useEffect(() => {
    if (!cameraOn) return
    const video = videoRef.current
    if (!video) return
    type DetectorInstance = {
      detect: (source: CanvasImageSource) => Promise<Array<{ rawValue?: string }>>
    }
    type DetectorCtor = new (options?: { formats?: string[] }) => DetectorInstance
    const DetectorCtor = (window as unknown as { BarcodeDetector?: DetectorCtor }).BarcodeDetector
    if (!DetectorCtor) {
      setCameraError('Trình duyệt hiện chưa hỗ trợ camera quét mã QR')
      setCameraOn(false)
      return
    }
    const detector = new DetectorCtor({ formats: ['qr_code'] })
    cancelledRef.current = false
    const tick = async () => {
      const width = video.videoWidth
      const height = video.videoHeight
      if (width && height) {
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        canvas.getContext('2d')?.drawImage(video, 0, 0, width, height)
        const codes = await detector.detect(canvas)
        if (cancelledRef.current) return
        if (codes && codes.length > 0) {
          const raw = String(codes[0].rawValue || '')
          cancelAnimationFrame(rafRef.current)
          const ticketCode = raw.startsWith('CINEX|') ? raw.split('|')[1] || raw : raw
          stopCamera()
          void runValidate(ticketCode)
          return
        }
      }
      rafRef.current = requestAnimationFrame(tick)
    }
    rafRef.current = requestAnimationFrame(tick)
    return () => {
      cancelledRef.current = true
      cancelAnimationFrame(rafRef.current)
    }
  }, [cameraOn, stopCamera, runValidate])

  const validate = (event: React.FormEvent) => {
    event.preventDefault()
    void runValidate(codeRef.current)
  }

  const handleCodeChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value.toUpperCase()
    setCode(value)
    codeRef.current = value
  }

  return (
    <div>
      <ConsolePageHeader
        eyebrow="Kiểm soát vé"
        title="Soát vé tại cổng"
        description="Quét mã QR vé điện tử hoặc nhập mã vé để xác thực và đánh dấu đã sử dụng. Mỗi vé chỉ có hiệu lực một lần."
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
              onChange={handleCodeChange}
              placeholder="TKDEMO0001"
              autoFocus
            />
            <button type="submit" className="btn btn-primary" disabled={loading}>
              <Icon name="scan" size={17} />
              {loading ? 'Đang kiểm tra…' : 'Soát vé'}
            </button>
          </form>

          <button
            type="button"
            className="btn btn-outline btn-block"
            style={{ marginTop: 12 }}
            onClick={cameraOn ? stopCamera : startCamera}
            disabled={loading}
          >
            <Icon name="scan" size={17} />
            {cameraOn ? 'Tắt camera' : 'Bật camera quét QR vé điện tử'}
          </button>
          {cameraError && <div className="form-error" style={{ marginTop: 10 }}>{cameraError}</div>}
          {cameraOn && (
            <video ref={videoRef} className="scan-video" muted playsInline style={{ width: '100%', borderRadius: 12, background: '#000', marginTop: 10 }} />
          )}

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

              {result.valid && result.booking && (
                <button type="button" className="btn btn-outline btn-sm" style={{ marginTop: 14 }} onClick={() => window.print()}>
                  <Icon name="printer" size={15} /> In vé
                </button>
              )}
            </div>
          )}

          {result?.valid && result.booking && (
            <div className="pos-print-area">
              <h2>CINEX</h2>
              <p>Vé xem phim · {result.ticketCode}</p>
              <div>Phim: {result.booking.showtime.movieTitle}</div>
              <div>Suất: {formatDateTime(result.booking.showtime.startTime)}</div>
              <div>Rạp: {result.booking.showtime.cinemaName} · {result.booking.showtime.auditoriumName}</div>
              <div>Ghế: {result.booking.seats.map((seat) => seat.seatCode).join(', ')}</div>
              <div>Tổng: {formatVnd(result.booking.totalAmount)}</div>
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
