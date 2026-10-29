interface StatusBadgeProps {
  status?: string
}

const STATUS_MAP: Record<string, { label: string; className: string }> = {
  PENDING: { label: 'Chờ thanh toán', className: 'badge-warning' },
  CONFIRMED: { label: 'Đã xác nhận', className: 'badge-success' },
  EXPIRED: { label: 'Hết hạn', className: 'badge-muted' },
  CANCELLED: { label: 'Đã hủy', className: 'badge-muted' },
  UNUSED: { label: 'Chưa soát', className: 'badge-success' },
  SCANNED: { label: 'Đã soát', className: 'badge-accent' },
  SUCCESS: { label: 'Thành công', className: 'badge-success' },
  REFUNDED: { label: 'Đã hoàn tiền', className: 'badge-warning' },
  FAILED: { label: 'Thất bại', className: 'badge-muted' },
  ONLINE: { label: 'Online', className: 'badge-primary' },
  COUNTER: { label: 'Tại quầy', className: 'badge-accent' },
  CASH: { label: 'Tiền mặt', className: 'badge-success' },
  CARD: { label: 'Thẻ', className: 'badge-primary' },
  TRANSFER: { label: 'Chuyển khoản', className: 'badge-accent' },
  MOMO: { label: 'MoMo', className: 'badge-primary' },
  VNPAY: { label: 'VNPay', className: 'badge-primary' },
  ACTIVE: { label: 'Đang hoạt động', className: 'badge-success' },
  NOW_SHOWING: { label: 'Đang chiếu', className: 'badge-success' },
  COMING_SOON: { label: 'Sắp chiếu', className: 'badge-warning' },
  STANDARD: { label: 'Thường', className: 'badge-muted' },
  VIP: { label: 'VIP', className: 'badge-warning' },
  COUPLE: { label: 'Couple', className: 'badge-primary' },
}

export function StatusBadge({ status }: StatusBadgeProps) {
  if (!status) return <span className="badge badge-muted">—</span>
  const meta = STATUS_MAP[status] ?? { label: status, className: 'badge-muted' }
  return <span className={`badge ${meta.className}`}>{meta.label}</span>
}
