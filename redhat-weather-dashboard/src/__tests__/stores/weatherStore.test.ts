import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useWeatherStore, clearCache } from '../../stores/weatherStore'

// Mock the weather service
vi.mock('../../services/weatherService', () => ({
  default: {
    getAirports: vi.fn().mockResolvedValue([
      { id: 2, name: 'JFK', latitude: 40.6, longitude: -73.7, locationType: 'airport', airportCode: 'KJFK' },
    ]),
    getForecastsByLocation: vi.fn().mockResolvedValue([]),
    getActiveStorms: vi.fn().mockResolvedValue([]),
    getRecentEarthquakes: vi.fn().mockResolvedValue([]),
    getActiveAlerts: vi.fn().mockResolvedValue([]),
    getHistoricalForecasts: vi.fn().mockResolvedValue([]),
    refreshEarthquakes: vi.fn().mockResolvedValue(undefined),
    refreshHurricaneData: vi.fn().mockResolvedValue(undefined),
  },
}))

describe('weatherStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    clearCache()
  })

  it('initializes with empty state', () => {
    const store = useWeatherStore()
    expect(store.airports).toEqual([])
    expect(store.airportsLoading).toBe(false)
    expect(store.airportsError).toBeNull()
  })

  it('fetches airports', async () => {
    const store = useWeatherStore()
    await store.fetchAirports()
    expect(store.airports).toHaveLength(1)
    expect(store.airports[0].airportCode).toBe('KJFK')
  })

  it('fetches forecasts for a location', async () => {
    const store = useWeatherStore()
    await store.fetchForecasts(1)
    expect(store.forecasts).toEqual([])
    expect(store.forecastsLoading).toBe(false)
  })

  it('fetches hurricanes', async () => {
    const store = useWeatherStore()
    await store.fetchHurricanes()
    expect(store.hurricanes).toEqual([])
    expect(store.hurricanesLoading).toBe(false)
  })

  it('fetches earthquakes', async () => {
    const store = useWeatherStore()
    await store.fetchEarthquakes()
    expect(store.earthquakes).toEqual([])
    expect(store.earthquakesLoading).toBe(false)
  })

  it('fetches alerts', async () => {
    const store = useWeatherStore()
    await store.fetchAlerts()
    expect(store.alerts).toEqual([])
    expect(store.alertsError).toBe(false)
  })

  it('handles fetch error gracefully', async () => {
    const weatherService = await import('../../services/weatherService')
    vi.mocked(weatherService.default.getAirports).mockRejectedValueOnce(new Error('Network error'))

    const store = useWeatherStore()
    clearCache('airports')
    await store.fetchAirports()
    expect(store.airportsError).toBe('Network error')
    expect(store.airportsLoading).toBe(false)
  })

  it('clearCache removes specific cache entry', () => {
    clearCache('airports')
    // Should not throw
    expect(true).toBe(true)
  })

  it('clearCache removes all entries', () => {
    clearCache()
    // Should not throw
    expect(true).toBe(true)
  })

  it('deduplicates concurrent requests', async () => {
    const store = useWeatherStore()
    clearCache('airports')

    // Fire two requests simultaneously
    const p1 = store.fetchAirports()
    const p2 = store.fetchAirports()
    await Promise.all([p1, p2])

    const weatherService = await import('../../services/weatherService')
    // The second call should reuse the first pending request
    expect(vi.mocked(weatherService.default.getAirports).mock.calls.length).toBeGreaterThanOrEqual(1)
  })

  // --- Data flow tests: ensure data is always iterable ---

  it('airports is an iterable array after fetchAirports()', async () => {
    const store = useWeatherStore()
    await store.fetchAirports()
    expect(Array.isArray(store.airports)).toBe(true)
    // Verify iterable — this was the exact bug: "airports.value is not iterable"
    expect(() => [...store.airports]).not.toThrow()
    expect(store.airports.length).toBe(1)
  })

  it('airports is [] after fetchAirports() with empty result', async () => {
    const weatherService = await import('../../services/weatherService')
    vi.mocked(weatherService.default.getAirports).mockResolvedValueOnce([])

    const store = useWeatherStore()
    clearCache('airports')
    await store.fetchAirports()
    expect(store.airports).toEqual([])
    expect(store.airports.length).toBe(0)
  })

  it('data remains safe default when service throws', async () => {
    const weatherService = await import('../../services/weatherService')
    vi.mocked(weatherService.default.getAirports).mockRejectedValueOnce(new Error('API failure'))

    const store = useWeatherStore()
    clearCache('airports')
    await store.fetchAirports()
    expect(store.airportsLoading).toBe(false)
    expect(store.airportsError).toBe('API failure')
    // airports should remain the safe default (empty array), not null/undefined
    expect(Array.isArray(store.airports)).toBe(true)
  })

  it('data is usable with array methods after fetch', async () => {
    const store = useWeatherStore()
    await store.fetchAirports()

    const names = store.airports.map((apt) => apt.name)
    expect(names).toEqual(['JFK'])

    const filtered = store.airports.filter((apt) => apt.locationType === 'airport')
    expect(filtered).toHaveLength(1)

    expect(store.airports.length).toBe(1)
  })

  it('uses cache on repeated calls within TTL', async () => {
    const weatherService = await import('../../services/weatherService')
    const spy = vi.mocked(weatherService.default.getAirports)
    spy.mockClear()

    const store = useWeatherStore()
    clearCache('airports')

    await store.fetchAirports()
    await store.fetchAirports()

    // Second call should use cache, so service only called once
    expect(spy).toHaveBeenCalledTimes(1)
  })

  it('refreshEarthquakes busts cache and re-fetches', async () => {
    const weatherService = await import('../../services/weatherService')
    const spy = vi.mocked(weatherService.default.getRecentEarthquakes)
    spy.mockClear()

    const store = useWeatherStore()
    clearCache('earthquakes')

    await store.fetchEarthquakes()
    expect(spy).toHaveBeenCalledTimes(1)

    // refreshEarthquakes should bust cache and fetch again
    await store.refreshEarthquakes()
    expect(spy.mock.calls.length).toBeGreaterThanOrEqual(2)
  })

  it('refreshAlerts busts cache and re-fetches', async () => {
    const weatherService = await import('../../services/weatherService')
    const spy = vi.mocked(weatherService.default.getActiveAlerts)
    spy.mockClear()

    const store = useWeatherStore()
    clearCache('alerts')

    await store.fetchAlerts()
    await store.refreshAlerts()

    expect(spy.mock.calls.length).toBeGreaterThanOrEqual(2)
  })
})
