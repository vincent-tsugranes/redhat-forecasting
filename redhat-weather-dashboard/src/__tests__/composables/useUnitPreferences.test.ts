import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useUnitPreferences } from '../../composables/useUnitPreferences'

// Provide a working localStorage mock for this test
const store: Record<string, string> = {}
vi.stubGlobal('localStorage', {
  getItem: (key: string) => store[key] ?? null,
  setItem: (key: string, value: string) => { store[key] = value },
  removeItem: (key: string) => { delete store[key] },
})

describe('useUnitPreferences', () => {
  beforeEach(() => {
    Object.keys(store).forEach((k) => delete store[k])
    // Reset to defaults
    const { tempUnit, speedUnit, timeFormat } = useUnitPreferences()
    tempUnit.value = 'F'
    speedUnit.value = 'mph'
    timeFormat.value = '12h'
  })

  it('returns default units', () => {
    const { tempUnit, speedUnit, timeFormat } = useUnitPreferences()
    expect(tempUnit.value).toBe('F')
    expect(speedUnit.value).toBe('mph')
    expect(timeFormat.value).toBe('12h')
  })

  it('converts temperature to Fahrenheit by default', () => {
    const { convertTemp } = useUnitPreferences()
    expect(convertTemp(72)).toBe(72)
  })

  it('converts temperature to Celsius when set', () => {
    const { tempUnit, convertTemp } = useUnitPreferences()
    tempUnit.value = 'C'
    expect(convertTemp(32)).toBe(0)
    expect(convertTemp(212)).toBe(100)
  })

  it('formats temperature with unit symbol', () => {
    const { formatTemp, tempUnit } = useUnitPreferences()
    expect(formatTemp(72)).toBe('72\u00b0F')
    tempUnit.value = 'C'
    expect(formatTemp(72)).toBe('22\u00b0C')
  })

  it('converts speed to mph by default', () => {
    const { convertSpeed } = useUnitPreferences()
    expect(convertSpeed(10)).toBe(10)
  })

  it('converts speed to km/h when set', () => {
    const { speedUnit, convertSpeed } = useUnitPreferences()
    speedUnit.value = 'kmh'
    expect(convertSpeed(10)).toBe(16)
  })

  it('formats speed with unit label', () => {
    const { formatSpeed, speedUnit } = useUnitPreferences()
    expect(formatSpeed(10)).toBe('10 mph')
    speedUnit.value = 'kmh'
    expect(formatSpeed(10)).toBe('16 km/h')
  })
})
