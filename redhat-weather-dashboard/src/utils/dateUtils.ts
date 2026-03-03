export type DataType = 'forecast' | 'airport' | 'hurricane'
export type FreshnessLevel = 'fresh' | 'aging' | 'stale'

// Thresholds in minutes, matched to scheduler refresh intervals
const STALENESS_THRESHOLDS: Record<DataType, { aging: number; stale: number }> = {
  forecast:  { aging: 25, stale: 45 },   // refreshes every 30 min
  airport:   { aging: 12, stale: 20 },   // refreshes every 15 min
  hurricane: { aging: 50, stale: 90 },   // refreshes every 60 min
}

export function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

export function formatRelativeTime(dateString: string): string {
  const now = Date.now()
  const then = new Date(dateString).getTime()
  const diffMs = now - then

  if (diffMs < 0) return 'just now'

  const diffSec = Math.floor(diffMs / 1000)
  if (diffSec < 60) return 'just now'

  const diffMin = Math.floor(diffSec / 60)
  if (diffMin < 60) return `${diffMin} min ago`

  const diffHrs = Math.floor(diffMin / 60)
  if (diffHrs < 24) return `${diffHrs} hr${diffHrs > 1 ? 's' : ''} ago`

  const diffDays = Math.floor(diffHrs / 24)
  return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`
}

export function getFreshnessLevel(dateString: string, dataType: DataType): FreshnessLevel {
  const now = Date.now()
  const then = new Date(dateString).getTime()
  const diffMin = (now - then) / 60000

  const thresholds = STALENESS_THRESHOLDS[dataType]

  if (diffMin < thresholds.aging) return 'fresh'
  if (diffMin < thresholds.stale) return 'aging'
  return 'stale'
}

export function isStale(dateString: string, dataType: DataType): boolean {
  return getFreshnessLevel(dateString, dataType) === 'stale'
}
