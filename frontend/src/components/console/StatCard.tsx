import { Icon, type IconName } from '../ui/Icon'

interface StatCardProps {
  icon: IconName
  label: string
  value: string
  hint?: string
  tone?: 'accent' | 'violet' | 'green' | 'amber'
}

export function StatCard({ icon, label, value, hint, tone = 'accent' }: StatCardProps) {
  return (
    <div className={`console-stat console-stat--${tone}`}>
      <span className="console-stat__icon">
        <Icon name={icon} size={19} />
      </span>
      <div>
        <span className="console-stat__label">{label}</span>
        <strong>{value}</strong>
        {hint && <small>{hint}</small>}
      </div>
    </div>
  )
}
