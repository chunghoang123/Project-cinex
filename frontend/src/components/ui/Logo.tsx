import { Link } from 'react-router-dom'
import { Icon } from './Icon'

interface LogoProps {
  to?: string
  compact?: boolean
}

export function Logo({ to = '/', compact = false }: LogoProps) {
  return (
    <Link to={to} className="brand" aria-label="Cinex - Trang chủ">
      <span className="brand__mark">
        <Icon name="film" size={18} />
      </span>
      {!compact && (
        <span>
          Cine<em>x</em>
        </span>
      )}
    </Link>
  )
}
