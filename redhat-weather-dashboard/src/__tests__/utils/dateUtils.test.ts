import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { formatRelativeTime, getFreshnessLevel, isStale } from '../../utils/dateUtils'

describe('formatRelativeTime', () => {
  it('returns "just now" for dates less than 60 seconds ago', () => {
    const now = new Date().toISOString()
    expect(formatRelativeTime(now)).toBe('just now')
  })

  it('returns "just now" for future dates', () => {
    const future = new Date(Date.now() + 60000).toISOString()
    expect(formatRelativeTime(future)).toBe('just now')
  })

  it('returns "X min ago" for dates less than 60 minutes ago', () => {
    const fiveMinAgo = new Date(Date.now() - 5 * 60 * 1000).toISOString()
    expect(formatRelativeTime(fiveMinAgo)).toBe('5 min ago')
  })

  it('returns "X hrs ago" for dates less than 24 hours ago', () => {
    const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString()
    expect(formatRelativeTime(twoHoursAgo)).toBe('2 hrs ago')
  })

  it('returns "1 hr ago" singular', () => {
    const oneHourAgo = new Date(Date.now() - 61 * 60 * 1000).toISOString()
    expect(formatRelativeTime(oneHourAgo)).toBe('1 hr ago')
  })

  it('returns "X days ago" for dates more than 24 hours ago', () => {
    const threeDaysAgo = new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString()
    expect(formatRelativeTime(threeDaysAgo)).toBe('3 days ago')
  })

  it('returns "1 day ago" singular', () => {
    const oneDayAgo = new Date(Date.now() - 25 * 60 * 60 * 1000).toISOString()
    expect(formatRelativeTime(oneDayAgo)).toBe('1 day ago')
  })
})

describe('getFreshnessLevel', () => {
  it('returns "fresh" for forecast data under 25 minutes old', () => {
    const recent = new Date(Date.now() - 10 * 60 * 1000).toISOString()
    expect(getFreshnessLevel(recent, 'forecast')).toBe('fresh')
  })

  it('returns "aging" for forecast data between 25-45 minutes old', () => {
    const aging = new Date(Date.now() - 30 * 60 * 1000).toISOString()
    expect(getFreshnessLevel(aging, 'forecast')).toBe('aging')
  })

  it('returns "stale" for forecast data over 45 minutes old', () => {
    const stale = new Date(Date.now() - 60 * 60 * 1000).toISOString()
    expect(getFreshnessLevel(stale, 'forecast')).toBe('stale')
  })

  it('uses airport thresholds (12/20 min)', () => {
    const fresh = new Date(Date.now() - 5 * 60 * 1000).toISOString()
    const aging = new Date(Date.now() - 15 * 60 * 1000).toISOString()
    const stale = new Date(Date.now() - 25 * 60 * 1000).toISOString()

    expect(getFreshnessLevel(fresh, 'airport')).toBe('fresh')
    expect(getFreshnessLevel(aging, 'airport')).toBe('aging')
    expect(getFreshnessLevel(stale, 'airport')).toBe('stale')
  })

  it('uses hurricane thresholds (50/90 min)', () => {
    const fresh = new Date(Date.now() - 30 * 60 * 1000).toISOString()
    const aging = new Date(Date.now() - 60 * 60 * 1000).toISOString()
    const stale = new Date(Date.now() - 120 * 60 * 1000).toISOString()

    expect(getFreshnessLevel(fresh, 'hurricane')).toBe('fresh')
    expect(getFreshnessLevel(aging, 'hurricane')).toBe('aging')
    expect(getFreshnessLevel(stale, 'hurricane')).toBe('stale')
  })
})

describe('isStale', () => {
  it('returns true when freshness level is stale', () => {
    const old = new Date(Date.now() - 120 * 60 * 1000).toISOString()
    expect(isStale(old, 'forecast')).toBe(true)
  })

  it('returns false when freshness level is fresh', () => {
    const recent = new Date().toISOString()
    expect(isStale(recent, 'forecast')).toBe(false)
  })

  it('returns false when freshness level is aging', () => {
    const aging = new Date(Date.now() - 30 * 60 * 1000).toISOString()
    expect(isStale(aging, 'forecast')).toBe(false)
  })
})
