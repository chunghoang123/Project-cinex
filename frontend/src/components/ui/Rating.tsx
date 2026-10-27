import { Icon } from './Icon'

interface RatingProps {
  value: number
  size?: number
  showValue?: boolean
}

export function Rating({ value, size = 13, showValue = true }: RatingProps) {
  return (
    <span className="rating">
      <Icon name="star" size={size} filled />
      {showValue && value.toFixed(1)}
    </span>
  )
}
