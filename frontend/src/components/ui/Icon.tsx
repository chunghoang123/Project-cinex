import type { ReactNode, SVGProps } from 'react'

export type IconName =
  | 'home'
  | 'search'
  | 'chat'
  | 'ticket'
  | 'user'
  | 'bell'
  | 'chevron-left'
  | 'chevron-right'
  | 'chevron-down'
  | 'play'
  | 'star'
  | 'location'
  | 'clock'
  | 'calendar'
  | 'seat'
  | 'card'
  | 'shield'
  | 'share'
  | 'settings'
  | 'logout'
  | 'plus'
  | 'check'
  | 'close'
  | 'menu'
  | 'film'
  | 'sparkles'
  | 'send'
  | 'arrow-right'
  | 'arrow-left'
  | 'qr'
  | 'download'
  | 'filter'
  | 'heart'
  | 'edit'
  | 'wallet'
  | 'momo'
  | 'vnpay'
  | 'apple'
  | 'google'
  | 'eye'
  | 'eye-off'
  | 'mail'
  | 'lock'
  | 'phone'
  | 'info'
  | 'alert'
  | 'trash'
  | 'popcorn'
  | 'globe'
  | 'users'
  | 'fire'
  | 'check-circle'
  | 'credit-card'
  | 'ticket-star'
  | 'sparkle'
  | 'compass'
  | 'dashboard'
  | 'chart'
  | 'receipt'
  | 'scan'
  | 'cash'
  | 'screen'
  | 'building'
  | 'printer'
  | 'refresh'
  | 'more'

interface IconProps extends SVGProps<SVGSVGElement> {
  name: IconName
  size?: number
  filled?: boolean
}

const paths: Record<IconName, ReactNode> = {
  home: <><path d="M3 10.5 12 3l9 7.5" /><path d="M5 9.5V21h5v-6h4v6h5V9.5" /></>,
  search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></>,
  chat: <><path d="M21 11.5a8.5 8.5 0 0 1-8.5 8.5H4l2-4.5A8.5 8.5 0 1 1 21 11.5Z" /><path d="M8 10h8M8 14h5" /></>,
  ticket: <><path d="M4 7a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v3a2 2 0 0 0 0 4v3a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-3a2 2 0 0 0 0-4V7Z" /><path d="M13 5v14" /></>,
  user: <><circle cx="12" cy="8" r="4" /><path d="M4 21c0-4 3.6-6 8-6s8 2 8 6" /></>,
  bell: <><path d="M18 9a6 6 0 1 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" /><path d="M10 21h4" /></>,
  'chevron-left': <path d="m15 18-6-6 6-6" />,
  'chevron-right': <path d="m9 18 6-6-6-6" />,
  'chevron-down': <path d="m6 9 6 6 6-6" />,
  play: <path d="M8 5v14l11-7-11-7Z" />,
  star: <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9L12 3Z" />,
  location: <><path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Z" /><circle cx="12" cy="10" r="2.5" /></>,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
  calendar: <><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M8 3v4M16 3v4M3 10h18" /></>,
  seat: <><path d="M6 11V7a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v4" /><path d="M6 11a2 2 0 0 0-2 2v4h16v-4a2 2 0 0 0-2-2" /><path d="M6 17v4M18 17v4" /></>,
  card: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M3 10h18" /></>,
  shield: <><path d="M12 3 4 6v6c0 5 8 9 8 9s8-4 8-9V6l-8-3Z" /><path d="m9 12 2 2 4-4" /></>,
  share: <><circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" /><path d="m8.6 10.5 6.8-4M8.6 13.5l6.8 4" /></>,
  settings: <><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.9.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.9.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.9-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.9V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z" /></>,
  logout: <><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><path d="m16 17 5-5-5-5M21 12H9" /></>,
  plus: <path d="M12 5v14M5 12h14" />,
  check: <path d="m5 12 4 4L19 6" />,
  close: <path d="m6 6 12 12M18 6 6 18" />,
  menu: <path d="M4 7h16M4 12h16M4 17h16" />,
  film: <><rect x="3" y="4" width="18" height="16" rx="2" /><path d="M7 4v16M17 4v16M3 9h4M3 15h4M17 9h4M17 15h4" /></>,
  sparkles: <><path d="m12 3 1.6 4.4L18 9l-4.4 1.6L12 15l-1.6-4.4L6 9l4.4-1.6L12 3Z" /><path d="m19 15 .8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8L19 15Z" /></>,
  send: <path d="m22 2-7 20-4-9-9-4 20-7Z" />,
  'arrow-right': <path d="M5 12h14m-6-6 6 6-6 6" />,
  'arrow-left': <path d="M19 12H5m6 6-6-6 6-6" />,
  qr: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><path d="M14 14h3v3h-3zM20 14h1v1M14 20h1v1M20 20h1v1" /></>,
  download: <><path d="M12 3v12m0 0 4-4m-4 4-4-4" /><path d="M4 21h16" /></>,
  filter: <path d="M4 5h16M7 12h10M10 19h4" />,
  heart: <path d="M20.8 5.6a5.5 5.5 0 0 0-7.8 0L12 6.6l-1-1a5.5 5.5 0 1 0-7.8 7.8L12 22l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z" />,
  edit: <><path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5Z" /></>,
  wallet: <><path d="M20 7V5a2 2 0 0 0-2-2H5a2 2 0 0 0 0 4h15v14H5a2 2 0 0 1-2-2V5" /><path d="M16 13h4" /></>,
  momo: <><rect x="3" y="5" width="18" height="14" rx="4" /><path d="M8 15V9l2.5 3L13 9v6M17 9v6" /></>,
  vnpay: <><circle cx="12" cy="12" r="9" /><path d="m8 8 4 9 4-9" /></>,
  apple: <><path d="M16.5 3c-1.2 0-2.4.7-3.1 1.7-.6.9-.6 2.4 0 3.4.8 1 2 1.6 3.1 1.5 1.2 0 2.4-.7 3-1.7.6-1 .6-2.4 0-3.4-.7-1-1.9-1.6-3-1.5Z" /><path d="M19 9c-1.5-1-3.5-1-5 0-2.4 1.6-3.5 4.4-2.6 7.2.9 2.8 3.4 4.8 5.1 4.8.6 0 1.2-.2 1.7-.4.4-.2.9-.2 1.3 0 .5.2 1 .4 1.5.4" /><path d="M12 9c-.8-1.5-2.3-2.5-4-2.5-2.8 0-5 2.2-5 5 0 3.9 3.2 7.5 5.8 7.5 1 0 1.8-.4 2.7-.4.9 0 1.7.4 2.7.4" /></>,
  google: <><path d="M21 12.2c0-.7-.1-1.4-.2-2H12v3.8h5.1a4.4 4.4 0 0 1-1.9 2.9v2.4h3.1c1.8-1.7 2.7-4.1 2.7-7.1Z" /><path d="M12 21c2.5 0 4.6-.8 6.3-2.3l-3.1-2.4c-.9.6-2 1-3.2 1-2.5 0-4.6-1.7-5.4-3.9H3.4v2.5A9.6 9.6 0 0 0 12 21Z" /><path d="M6.6 13.4a5.8 5.8 0 0 1 0-3.7V7.2H3.4a9.6 9.6 0 0 0 0 8.7l3.2-2.5Z" /><path d="M12 6.2c1.4 0 2.7.5 3.7 1.4l2.7-2.7A9.6 9.6 0 0 0 3.4 7.2l3.2 2.5C7.4 7.8 9.5 6.2 12 6.2Z" /></>,
  eye: <><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" /><circle cx="12" cy="12" r="3" /></>,
  'eye-off': <><path d="m3 3 18 18" /><path d="M10.6 5.2A10.8 10.8 0 0 1 12 5c6.5 0 10 7 10 7a17 17 0 0 1-3 4" /><path d="M6.2 6.2A17.5 17.5 0 0 0 2 12s3.5 7 10 7c1.6 0 3-.4 4.3-1" /><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" /></>,
  mail: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="m3 7 9 6 9-6" /></>,
  lock: <><rect x="4" y="10" width="16" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></>,
  phone: <path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1 1 .4 2 .7 2.9a2 2 0 0 1-.5 2.1L8 10a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.5c.9.3 1.9.6 2.9.7a2 2 0 0 1 1.7 2Z" />,
  info: <><circle cx="12" cy="12" r="9" /><path d="M12 11v5M12 8h.01" /></>,
  alert: <><path d="M10.3 3.9 2.4 18a2 2 0 0 0 1.7 3h15.8a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" /><path d="M12 9v4M12 17h.01" /></>,
  trash: <><path d="M3 6h18M8 6V4h8v2M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" /><path d="M10 11v6M14 11v6" /></>,
  popcorn: <><path d="M5 8h14l-1 13H6L5 8Z" /><path d="M5 8c0-2 1.5-4 3.5-4S12 5 12 8c0-3 1.5-4 3.5-4S19 6 19 8" /></>,
  globe: <><circle cx="12" cy="12" r="9" /><path d="M3 12h18M12 3a15 15 0 0 1 0 18M12 3a15 15 0 0 0 0 18" /></>,
  users: <><circle cx="9" cy="8" r="3.5" /><path d="M2 21c0-3.5 3.1-6 7-6s7 2.5 7 6" /><path d="M16 4.5a3.5 3.5 0 0 1 0 7M17 15c3 .5 5 2.6 5 6" /></>,
  fire: <path d="M12 22c4.4 0 8-3.6 8-8 0-3-1.5-5.5-4-8 .3 2.5-1 4-2 5-1-3-3-5.5-6-7 .3 3.5-4 6.5-4 10 0 4.4 3.6 8 8 8Z" />,
  'check-circle': <><circle cx="12" cy="12" r="9" /><path d="m8 12 2.5 2.5L16 9" /></>,
  'credit-card': <><rect x="2" y="5" width="20" height="14" rx="2" /><path d="M2 10h20M6 15h4" /></>,
  'ticket-star': <><path d="M4 7a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v3a2 2 0 0 0 0 4v3a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-3a2 2 0 0 0 0-4V7Z" /><path d="m12 9 1 2 2 .3-1.5 1.5.4 2.2-1.9-1-1.9 1 .4-2.2L9 11.3l2-.3 1-2Z" /></>,
  sparkle: <path d="m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z" />,
  compass: <><circle cx="12" cy="12" r="9" /><path d="m16 8-2.5 5.5L8 16l2.5-5.5L16 8Z" /></>,
  dashboard: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></>,
  chart: <><path d="M4 20V10M10 20V4M16 20v-7M22 20H2" /></>,
  receipt: <><path d="M5 3h14v18l-3-2-2 2-2-2-2 2-2-2-3 2V3Z" /><path d="M9 8h6M9 12h6M9 16h3" /></>,
  scan: <><path d="M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3M4 12h16" /></>,
  cash: <><rect x="2" y="6" width="20" height="12" rx="2" /><circle cx="12" cy="12" r="3" /><path d="M6 10h.01M18 14h.01" /></>,
  screen: <><rect x="3" y="4" width="18" height="12" rx="2" /><path d="M8 20h8M12 16v4" /></>,
  building: <><rect x="4" y="3" width="16" height="18" rx="1" /><path d="M9 7h2M13 7h2M9 11h2M13 11h2M9 15h2M13 15h2M10 21v-3h4v3" /></>,
  printer: <><path d="M6 9V3h12v6M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2" /><rect x="6" y="14" width="12" height="7" rx="1" /></>,
  refresh: <><path d="M20 11a8 8 0 0 0-14.9-3M4 4v4h4M4 13a8 8 0 0 0 14.9 3M20 20v-4h-4" /></>,
  more: <><circle cx="5" cy="12" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" /></>,
}

export function Icon({ name, size = 20, filled = false, ...props }: IconProps) {
  const isFilled = filled || name === 'star' || name === 'play' || name === 'sparkle' || name === 'sparkles'
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill={isFilled ? 'currentColor' : 'none'}
      stroke="currentColor"
      strokeWidth={isFilled ? 0 : 1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      {paths[name]}
    </svg>
  )
}
