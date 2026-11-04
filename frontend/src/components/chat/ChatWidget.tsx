import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import axios from 'axios'
import { Icon } from '../ui/Icon'
import { aiService } from '../../services/aiService'
import type { AiChatMessage } from '../../services/aiService'

interface Message {
  id: number
  role: 'ai' | 'user'
  text: string
  time: string
  error?: boolean
}

const suggestions = [
  'Phim nào đang chiếu?',
  'Suất chiếu hôm nay',
  'Gợi ý phim hay cho tối nay',
  'Giá vé ở CineX',
]

const greeting: Message = {
  id: 1,
  role: 'ai',
  text: 'Xin chào! Mình là CineX AI. Mình có thể gợi ý phim, tra suất chiếu và giải đáp thắc mắc về đặt vé. Bạn muốn xem gì hôm nay?',
  time: now(),
}

function now() {
  return new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(new Date())
}

function errorText(error: unknown) {
  if (axios.isAxiosError(error)) {
    const apiMessage = (error.response?.data as { message?: string } | undefined)?.message
    if (apiMessage) return apiMessage
    if (error.code === 'ECONNABORTED') return 'Trợ lý AI phản hồi quá lâu, bạn thử lại nhé.'
    if (!error.response) return 'Không kết nối được máy chủ CineX, bạn kiểm tra mạng giúp mình nhé.'
  }
  return error instanceof Error ? error.message : 'Có lỗi khi gọi trợ lý AI.'
}

/** Render **đậm** và xuống dòng đơn giản, tránh hiển thị Markdown thô. */
function formatText(text: string) {
  return text.split('\n').map((line, lineIndex) => (
    <span key={lineIndex}>
      {line.split(/(\*\*.*?\*\*)/g).filter(Boolean).map((part, partIndex) =>
        part.startsWith('**') && part.endsWith('**') ? (
          <strong key={partIndex}>{part.slice(2, -2)}</strong>
        ) : (
          <span key={partIndex}>{part}</span>
        ),
      )}
      {lineIndex < text.split('\n').length - 1 && <br />}
    </span>
  ))
}

export function openChat() {
  window.dispatchEvent(new CustomEvent('cinex:open-chat'))
}

export function ChatWidget() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState<Message[]>([greeting])
  const [input, setInput] = useState('')
  const [typing, setTyping] = useState(false)
  const bodyRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handler = () => setOpen(true)
    window.addEventListener('cinex:open-chat', handler)
    return () => window.removeEventListener('cinex:open-chat', handler)
  }, [])

  useEffect(() => {
    if (open) {
      bodyRef.current?.scrollTo({ top: bodyRef.current.scrollHeight, behavior: 'smooth' })
    }
  }, [open, messages, typing])

  const send = useCallback(
    async (text: string) => {
      const value = text.trim()
      if (!value || typing) return

      const history: AiChatMessage[] = messages
        .filter((message) => !message.error)
        .map((message) => ({
          role: message.role === 'ai' ? 'assistant' : 'user',
          content: message.text,
        }))

      setMessages((current) => [
        ...current,
        { id: Date.now(), role: 'user', text: value, time: now() },
      ])
      setInput('')
      setTyping(true)

      try {
        const result = await aiService.chat(value, history)
        const reply = result?.reply?.trim()
        if (!reply) throw new Error('Trợ lý AI không trả về nội dung.')
        setMessages((current) => [
          ...current,
          { id: Date.now() + 1, role: 'ai', text: reply, time: now() },
        ])
      } catch (error) {
        setMessages((current) => [
          ...current,
          {
            id: Date.now() + 1,
            role: 'ai',
            text: `⚠️ ${errorText(error)}`,
            time: now(),
            error: true,
          },
        ])
      } finally {
        setTyping(false)
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
        <div className="chat-widget__panel" role="dialog" aria-label="CineX AI">
          <div className="chat-head">
            <span className="chat-head__avatar">
              <Icon name="sparkles" size={21} />
            </span>
            <div>
              <div className="chat-head__name">CineX AI</div>
              <div className="chat-head__status">Đang trực tuyến</div>
            </div>
            <button
              type="button"
              className="input-action"
              style={{ marginLeft: 'auto' }}
              onClick={() => setOpen(false)}
              aria-label="Đóng chat"
            >
              <Icon name="close" size={17} />
            </button>
          </div>

          <div className="chat-body" ref={bodyRef} aria-live="polite">
            {messages.map((message) => (
              <div key={message.id} className={`chat-msg ${message.role === 'ai' ? 'is-ai' : 'is-user'}`}>
                {formatText(message.text)}
                <div className="chat-time">{message.time}</div>
              </div>
            ))}
            {typing && (
              <div className="chat-msg is-ai">
                <span className="muted">CineX AI đang trả lời...</span>
              </div>
            )}
          </div>

          <div className="chat-suggestions">
            {suggestions.map((suggestion) => (
              <button
                key={suggestion}
                type="button"
                className="chip"
                disabled={typing}
                onClick={() => void send(suggestion)}
              >
                {suggestion}
              </button>
            ))}
          </div>

          <form className="chat-compose" onSubmit={handleSubmit}>
            <input
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder="Hỏi CineX AI bất cứ điều gì..."
              aria-label="Tin nhắn"
              maxLength={2000}
            />
            <button type="submit" aria-label="Gửi tin nhắn" disabled={!input.trim() || typing}>
              <Icon name="send" size={17} />
            </button>
          </form>
        </div>
      )}

      <button
        type="button"
        className="chat-widget__bubble"
        onClick={() => setOpen((value) => !value)}
        aria-expanded={open}
        aria-label={open ? 'Đóng CineX AI' : 'Mở CineX AI'}
      >
        <Icon name={open ? 'close' : 'chat'} size={24} />
      </button>
    </>
  )
}
