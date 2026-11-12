/** Bỏ dấu tiếng Việt + lowercase để so khớp không phân biệt dấu. */
export function normalizeText(value: string): string {
  return (value ?? '')
    .toLowerCase()
    .replace(/đ/g, 'd')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

export function includesText(haystack: string | undefined | null, needle: string): boolean {
  if (!needle) return true
  if (!haystack) return false
  return normalizeText(haystack).includes(needle)
}
