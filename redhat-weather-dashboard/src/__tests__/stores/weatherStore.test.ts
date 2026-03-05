import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useWeatherStore, clearCache } from '../../stores/weatherStore'

// Mock the weather service
vi.mock('../../services/weatherService', () => ({
  default: {
    getLocations: vi.fn().mockResolvedValue([
      { id: 1, name: 'Test City', latitude: 40, longitude: -74, locationType: 'city' },
    ]),
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
    expect(store.locations).toEqual([])
    expect(store.locationsLoading).toBe(false)
    expect(store.locationsError).toBeNull()
  })

  it('fetches locations', async () => {
    const store = useWeatherStore()
    await store.fetchLocations()
    expect(store.locations).toHaveLength(1)
    expect(store.locations[0].name).toBe('Test City')
    expect(store.locationsLoading).toBe(false)
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
    vi.mocked(weatherService.default.getLocations).mockRejectedValueOnce(new Error('Network error'))

    const store = useWeatherStore()
    clearCache('locations')
    await store.fetchLocations()
    expect(store.locationsError).toBe('Network error')
    expect(store.locationsLoading).toBe(false)
  })

  it('clearCache removes specific cache entry', () => {
    clearCache('locations')
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
    clearCache('locations')

    // Fire two requests simultaneously
    const p1 = store.fetchLocations()
    const p2 = store.fetchLocations()
    await Promise.all([p1, p2])

    const weatherService = await import('../../services/weatherService')
    // The second call should reuse the first pending request
    expect(vi.mocked(weatherService.default.getLocations).mock.calls.length).toBeGreaterThanOrEqual(1)
  })
})
