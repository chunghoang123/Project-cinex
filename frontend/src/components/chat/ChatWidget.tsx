import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import { Icon } from '../ui/Icon'
import { aiService } from '../../services/aiService'
import type { AiChatMessage, AiMovieSuggestion } from '../../services/aiService'

interface Message {
  id: number
  role: 'ai' | 'user'
  text: string
  time: string
  error?: boolean
  movies?: AiMovieSuggestion[]
}

interface QuickLink {
  label: string
  to: string
}

const suggestions = [
  'Phim nào đang chiếu?',
  'Suất chiếu hôm nay',
  'Gợi ý phim hay cho tối nay',
  'Giá vé ở CineX',
  'Thanh toán PayOS thế nào?',
]

const quickLinks: QuickLink[] = [
  { label: 'Xem phim', to: '/movies' },
  { label: 'Lịch chiếu', to: '/showtimes' },
  { label: 'Vé của tôi', to: '/history' },
]

const greeting: Message = {
  id: 1,
  role: 'ai',
  text: 'Xin chào! Mình là CineX AI. Mình chỉ hỗ trợ đúng việc đặt vé xem phim: gợi ý phim, tra suất chiếu, giá vé, thanh toán PayOS và vé của bạn. Bạn muốn xem phim gì hôm nay?',
  time: '',
}

function linkForMessage(text: string): QuickLink | null {
  const low = text.toLowerCase()
  if (low.includes('suất chiếu') || low.includes('suất')) return { label: 'Mở lịch chiếu', to: '/showtimes' }
  if (low.includes('giá vé') || low.includes('gia ve')) return { label: 'Xem lịch chiếu & giá', to: '/showtimes' }
  if (low.includes('thanh toán') || low.includes('payos') || low.includes('thanh toan')) return { label: 'Cách đặt vé & thanh toán', to: '/showtimes' }
  if (low.includes('vé của tôi') || low.includes('tra vé') || low.includes('lịch sử vé')) return { label: 'Mở vé của tôi', to: '/history' }
  if (low.includes('phim')) return { label: 'Khám phá phim', to: '/movies' }
  return null
}

function now() {
  return new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(new Date())
}

function errorText(error: unknown) {
  if (axios.isAxiosError(error)) {
    const apiMessage = (error.response?.data as { message?: string } | undefined)?.message
    if (apiMessage) return apiMessage
    if (error.code === 'ECONNABORTED') return 'CineX AI trả lời quá lâu, bạn thử lại nhé.'
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
  const inputRef = useRef<HTMLInputElement>(null)

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
        if (!reply) throw new Error('CineX AI không trả về nội dung.')
        setMessages((current) => [
          ...current,
          { id: Date.now() + 1, role: 'ai', text: reply, time: now(), movies: result?.movies || [] },
        ])
      } catch (error) {
        setMessages((current) => [
          ...current,
          {
            id: Date.now() + 1,
            role: 'ai',
            text: `Rất tiếc, ${errorText(error)}`,
            time: now(),
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

  const closeChat = () => setOpen(false)

  return (
    <>
      {open && (
        <section className="chat-widget__panel" role="dialog" aria-label="CineX AI">
          <header className="chat-head chat-head--panel">
            <span className="chat-head__avatar" aria-hidden="true">
              <Icon name="sparkles" size={20} />
            </span>
            <div className="chat-head__text">
              <div className="chat-head__name">CineX AI</div>
              <div className="chat-head__status">Hỗ trợ đặt vé · đang trực tuyến</div>
            </div>
            <button
              type="button"
              className="chat-head__close"
              onClick={() => setOpen(false)}
              aria-label="Đóng trợ lý"
            >
              <Icon name="close" size={17} />
            </button>
          </header>

          <div className="chat-body" ref={bodyRef} aria-live="polite">
            {messages.map((message) => {
              const link = message.role === 'ai' && !message.error ? linkForMessage(message.text) : null
              return (
                <div key={message.id} className={`chat-msg ${message.role === 'ai' ? 'is-ai' : 'is-user'}`}>
                  <div className="chat-msg__text">{formatText(message.text)}</div>
                  {message.movies && message.movies.length > 0 && (
                    <>
                      <div className="chat-posters">
                        {message.movies.map((movie) => (
                          <Link key={movie.movieId} to={`/movies/${movie.movieId}`} className="chat-poster" title={movie.title} onClick={closeChat}>
                            {movie.posterUrl ? (
                              <img src={movie.posterUrl} alt={movie.title} loading="lazy" />
                            ) : (
                              <span className="chat-poster__fallback">
                                <Icon name="film" size={20} />
                              </span>
                            )}
                            <span className="chat-poster__title">{movie.title}</span>
                            {movie.genre && <span className="chat-poster__genre">{movie.genre}</span>}
                          </Link>
                        ))}
                      </div>
                      <div className="chat-posters-actions">
                        {message.movies.map((movie) => (
                          <Link key={movie.movieId} to={`/showtimes?movieId=${movie.movieId}`} className="chat-cta" onClick={closeChat}>
                            <Icon name="calendar" size={15} />
                            Đặt vé · {movie.title}
                          </Link>
                        ))}
                      </div>
                    </>
                  )}
                  {link && (
                    <Link className="chat-msg__link" to={link.to} onClick={closeChat}>
                      {link.label}
                      <Icon name="arrow-right" size={14} />
                    </Link>
                  )}
                  {message.time && <div className="chat-time">{message.time}</div>}
                </div>
              )
            })}
            {typing && (
              <div className="chat-msg is-ai">
                <div className="chat-typing" aria-label="CineX AI đang trả lời">
                  <span /><span /><span />
                </div>
              </div>
            )}
          </div>

          <div className="chat-quicklinks" aria-label="Lối tắt đặt vé">
            {quickLinks.map((link) => (
              <Link key={link.to} className="chip" to={link.to} onClick={closeChat}>
                <Icon name={link.to === '/movies' ? 'film' : link.to === '/showtimes' ? 'calendar' : 'ticket'} size={14} />
                {link.label}
              </Link>
            ))}
          </div>

          <div className="chat-suggestions" aria-label="Gợi ý câu hỏi">
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
              ref={inputRef}
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder="Hỏi về phim, suất chiếu, giá vé..."
              aria-label="Tin nhắn"
              maxLength={2000}
              autoComplete="off"
            />
            <button type="submit" aria-label="Gửi tin nhắn" disabled={!input.trim() || typing}>
              <Icon name="send" size={17} />
            </button>
          </form>
          <p className="chat-note">Chỉ trả lời về phim &amp; đặt vé tại CineX.</p>
        </section>
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
