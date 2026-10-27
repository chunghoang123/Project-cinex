export function formatVnd(value: number): string {
  return `${new Intl.NumberFormat('vi-VN').format(value)}đ`
}

export function formatShortVnd(value: number): string {
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1).replace('.0', '')}tr`
  if (value >= 1_000) return `${Math.round(value / 1_000)}k`
  return `${value}`
}

export function formatDuration(minutes: number): string {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h}h ${m.toString().padStart(2, '0')}m` : `${m}m`
}

export function todayLabel(): string {
  return new Intl.DateTimeFormat('vi-VN', {
    weekday: 'long',
    day: '2-digit',
    month: '2-digit',
  }).format(new Date())
}

export function buildDates(count = 7) {
  const days = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']
  return Array.from({ length: count }).map((_, index) => {
    const date = new Date()
    date.setDate(date.getDate() + index)
    return {
      key: date.toISOString().slice(0, 10),
      weekday: days[date.getDay()],
      day: date.getDate().toString().padStart(2, '0'),
      month: `${date.getMonth() + 1}`.padStart(2, '0'),
      label: `${date.getDate().toString().padStart(2, '0')}/${(date.getMonth() + 1).toString().padStart(2, '0')}`,
    }
  })
}

export function minutesToTime(minutes: number): string {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`
}

export function addMinutes(time: string, minutes: number): string {
  const [h, m] = time.split(':').map(Number)
  return minutesToTime(h * 60 + m + minutes)
}

export function cx(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(' ')
}
