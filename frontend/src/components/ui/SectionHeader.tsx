import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Icon } from './Icon'

interface SectionHeaderProps {
  title: string
  action?: string
  actionTo?: string
  children?: ReactNode
}

export function SectionHeader({ title, action, actionTo, children }: SectionHeaderProps) {
  return (
    <div className="section-head">
      <h2 className="section-head__title">{title}</h2>
      {children}
      {action && actionTo && (
        <Link to={actionTo} className="section-head__link">
          {action}
          <Icon name="chevron-right" size={14} />
        </Link>
      )}
    </div>
  )
}
