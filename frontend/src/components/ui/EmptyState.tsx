import type { ReactNode } from 'react'
import { Icon, type IconName } from './Icon'

interface EmptyStateProps {
  icon?: IconName
  title: string
  description?: string
  action?: ReactNode
}

export function EmptyState({ icon = 'film', title, description, action }: EmptyStateProps) {
  return (
    <div className="empty-state">
      <Icon name={icon} size={38} />
      <strong>{title}</strong>
      {description && <p>{description}</p>}
      {action}
    </div>
  )
}
