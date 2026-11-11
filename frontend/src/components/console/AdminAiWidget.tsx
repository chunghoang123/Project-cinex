import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Icon } from '../ui/Icon'
import { adminService } from '../../services/adminService'
import type { AdminAiActionResult } from '../../services/adminService'
import { errorMessage } from '../../utils/consoleFormat'

interface AdminMessage {
  id: number
  role: 'ai' | 'user'
  text: string
  time: string
  error?: boolean
  actions?: AdminAiActionResult[]
}

const QUICK_PROMPTS = [
  'Tự động cập nhật suất chiếu cho 10 ngày tới',
  'Tạo phòng Screen 3 có 10 hàng 12 cột ở rạp Landmark 81',
  'Thêm phim Lật Mặt 7 đang chiếu, 138 phút, 13+',
  'Thêm suất chiếu Dune: Part Two ngày mai lúc 18:00 và 21:00 ở Landmark 81 giá 95k',
]

const GREETING: AdminMessage = {
  id: 1,
  role: 'ai',
  text: 'Xin chào Quản trị viên! Mình là **CineX Admin Copilot**.\nBạn chỉ cần gõ yêu cầu tự nhiên để mình thực thi ngay vào hệ thống:\n- **Tạo phòng chiếu** (tự động sinh ghế STANDARD/VIP/COUPLE)\n- **Thêm phim mới** (tự động bổ sung poster & trailer nếu có)\n- **Tạo / cập nhật suất chiếu** hoặc **tự động phủ lịch chiếu 10 ngày** (hệ thống tự động cập nhật định kỳ 5 ngày/lần).',
  time: '',
}

function nowTime() {
  return new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(new Date())
}

function formatMarkdownText(text: string) {
  return text.split('\n').map((line, lineIndex, allLines) => (
    <span key={lineIndex}>
      {line
        .split(/(\*\*.*?\*\*)/g)
        .filter(Boolean)
        .map((part, partIndex) =>
          part.startsWith('**') && part.endsWith('**') ? (
            <strong key={partIndex}>{part.slice(2, -2)}</strong>
          ) : (
            <span key={partIndex}>{part}</span>
          ),
        )}
      {lineIndex < allLines.length - 1 && <br />}
    </span>
  ))
}

export function openAdminAi(initialPrompt?: string) {
  window.dispatchEvent(
    new CustomEvent('cinex:open-admin-ai', { detail: { prompt: initialPrompt || '' } }),
  )
}

export function notifyAdminDataRefresh() {
  window.dispatchEvent(new CustomEvent('cinex:admin-refresh'))
}

export function AdminAiWidget() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<AdminMessage[]>([GREETING])
  const [input, setInput] = useState('')
  const [typing, setTyping] = useState(false)
  const bodyRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    const handler = (event: Event) => {
      const custom = event as CustomEvent<{ prompt?: string }>
      setOpen(true)
      if (custom.detail?.prompt) {
        setInput(custom.detail.prompt)
      }
      window.setTimeout(() => inputRef.current?.focus(), 60)
    }
    window.addEventListener('cinex:open-admin-ai', handler)
    return () => window.removeEventListener('cinex:open-admin-ai', handler)
  }, [])

  useEffect(() => {
    if (open) {
      bodyRef.current?.scrollTo({ top: bodyRef.current.scrollHeight, behavior: 'smooth' })
    }
  }, [open, messages, typing])

  useEffect(() => {
    if (!open) return
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('keydown', onKey)
    inputRef.current?.focus()
    return () => document.removeEventListener('keydown', onKey)
  }, [open])

  const send = useCallback(
    async (rawText: string) => {
      const value = rawText.trim()
      if (!value || typing) return

      const history = messages
        .filter((msg) => !msg.error)
        .map((msg) => ({
          role: (msg.role === 'ai' ? 'assistant' : 'user') as 'assistant' | 'user',
          content: msg.text,
        }))

      setMessages((current) => [
        ...current,
        { id: Date.now(), role: 'user', text: value, time: nowTime() },
      ])
      setInput('')
      setTyping(true)

      try {
        const res = await adminService.chatAi(value, history)
        const reply = res?.reply?.trim() || 'Đã xử lý yêu cầu của bạn.'
        setMessages((current) => [
          ...current,
          {
            id: Date.now() + 1,
            role: 'ai',
            text: reply,
            time: nowTime(),
            actions: res?.actions || [],
          },
        ])
        if (res?.dataChanged) {
          notifyAdminDataRefresh()
        }
      } catch (err) {
        setMessages((current) => [
          ...current,
          {
            id: Date.now() + 1,
            role: 'ai',
            text: `Không thể thực hiện yêu cầu: ${errorMessage(err, 'Lỗi kết nối trợ lý AI quản trị')}`,
            time: nowTime(),
            error: true,
          },
        ])
      } finally {
        setTyping(false)
        inputRef.current?.focus()
      }
    },
    [messages, typing],
  )

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    void send(input)
  }

  return (
    <>
      {open && (
        <section className="admin-ai-panel" role="dialog" aria-label="CineX Admin Copilot">
          <header className="admin-ai-panel__head">
            <div className="admin-ai-panel__brand">
              <span className="admin-ai-panel__avatar">
                <Icon name="sparkles" size={18} />
              </span>
              <div>
                <strong>CineX Admin Copilot</strong>
                <span>Tạo phòng · Thêm phim · Xếp suất chiếu tự động</span>
              </div>
            </div>
            <button
              type="button"
              className="icon-btn"
              onClick={() => setOpen(false)}
              aria-label="Đóng trợ lý AI"
            >
              <Icon name="close" size={18} />
            </button>
          </header>

          <div className="admin-ai-panel__body" ref={bodyRef} aria-live="polite">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`admin-ai-msg ${msg.role === 'ai' ? 'is-ai' : 'is-user'} ${msg.error ? 'is-error' : ''}`}
              >
                <div className="admin-ai-msg__text">{formatMarkdownText(msg.text)}</div>

                {msg.actions && msg.actions.length > 0 && (
                  <div className="admin-ai-actions">
                    {msg.actions.map((action, index) => (
                      <div
                        key={`${action.type}-${index}`}
                        className={`admin-ai-action-card ${action.success ? 'is-success' : 'is-failed'}`}
                      >
                        <div className="admin-ai-action-card__head">
                          <Icon name={action.success ? 'check-circle' : 'alert'} size={15} />
                          <strong>{action.title}</strong>
                          <span className="badge badge-muted">{action.type}</span>
                        </div>
                        <p>{action.detail}</p>
                      </div>
                    ))}
                  </div>
                )}

                {msg.time && <div className="admin-ai-msg__time">{msg.time}</div>}
              </div>
            ))}

            {typing && (
              <div className="admin-ai-msg is-ai">
                <div className="chat-typing" aria-label="Admin AI đang xử lý">
                  <span />
                  <span />
                  <span />
                </div>
              </div>
            )}
          </div>

          <div className="admin-ai-panel__chips">
            {QUICK_PROMPTS.map((prompt) => (
              <button
                key={prompt}
                type="button"
                className="admin-ai-chip"
                disabled={typing}
                onClick={() => void send(prompt)}
              >
                {prompt}
              </button>
            ))}
          </div>

          <form className="admin-ai-panel__form" onSubmit={handleSubmit}>
            <input
              ref={inputRef}
              className="input"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Yêu cầu tạo phòng, thêm phim, tạo lịch chiếu..."
              maxLength={2000}
              autoComplete="off"
            />
            <button
              type="submit"
              className="btn btn-primary btn-sm"
              disabled={!input.trim() || typing}
              aria-label="Gửi lệnh cho AI"
            >
              <Icon name="send" size={16} />
            </button>
          </form>
        </section>
      )}

      <button
        type="button"
        className="admin-ai-fab"
        onClick={() => setOpen((prev) => !prev)}
        aria-expanded={open}
        aria-label={open ? 'Đóng trợ lý AI quản trị' : 'Mở trợ lý AI quản trị'}
        title="Trợ lý AI Quản trị (Tạo phòng, thêm phim, xếp lịch chiếu)"
      >
        <Icon name={open ? 'close' : 'sparkles'} size={20} />
        <span>AI Quản trị</span>
      </button>
    </>
  )
}
