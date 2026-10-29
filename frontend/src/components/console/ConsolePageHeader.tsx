import type { ReactNode } from 'react'

interface ConsolePageHeaderProps {
  eyebrow?: string
  title: string
  description?: string
  actions?: ReactNode
}

export function ConsolePageHeader({ eyebrow, title, description, actions }: ConsolePageHeaderProps) {
  return (
    <div className="console-page-head">
      <div>
        {eyebrow && <span className="console-eyebrow">{eyebrow}</span>}
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      {actions && <div className="console-page-head__actions">{actions}</div>}
    </div>
  )
}
