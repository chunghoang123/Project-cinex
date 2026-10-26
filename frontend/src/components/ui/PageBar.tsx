import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { Icon } from './Icon'

interface PageBarProps {
  title: string
  backTo?: string
  onBack?: () => void
  actions?: ReactNode
}

export function PageBar({ title, backTo, onBack, actions }: PageBarProps) {
  const navigate = useNavigate()
  return (
    <div className="page-bar">
      <button
        type="button"
        className="page-bar__back"
        aria-label="Quay lại"
        onClick={() => (onBack ? onBack() : backTo ? navigate(backTo) : navigate(-1))}
      >
        <Icon name="chevron-left" size={20} />
      </button>
      <h1 className="page-bar__title">{title}</h1>
      <div className="row gap-8">{actions ?? <span style={{ width: 40 }} />}</div>
    </div>
  )
}
