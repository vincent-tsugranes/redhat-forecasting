import { ref } from 'vue'
import { defineStore } from 'pinia'
import weatherService, {
  type Location,
  type WeatherForecast,
  type Hurricane,
  type Earthquake,
  type WeatherAlert,
} from '../services/weatherService'

const CACHE_TTL = 60_000 // 1 minute
const pendingRequests = new Map<string, Promise<unknown>>()
const cache = new Map<string, { data: unknown; timestamp: number }>()

function isCacheValid(key: string): boolean {
  const entry = cache.get(key)
  return !!entry && Date.now() - entry.timestamp < CACHE_TTL
}

async function deduplicatedFetch<T>(key: string, fetcher: () => Promise<T>): Promise<T> {
  if (isCacheValid(key)) return cache.get(key)!.data as T

  const pending = pendingRequests.get(key)
  if (pending) return pending as Promise<T>

  const promise = fetcher()
    .then((data) => {
      cache.set(key, { data, timestamp: Date.now() })
      pendingRequests.delete(key)
      return data
    })
    .catch((err) => {
      pendingRequests.delete(key)
      throw err
    })

  pendingRequests.set(key, promise)
  return promise
}

export function clearCache(key?: string) {
  if (key) {
    cache.delete(key)
    pendingRequests.delete(key)
  } else {
    cache.clear()
    pendingRequests.clear()
  }
}

export const useWeatherStore = defineStore('weather', () => {
  // --- Locations ---
  const locations = ref<Location[]>([])
  const locationsLoading = ref(false)
  const locationsError = ref<string | null>(null)

  async function fetchLocations() {
    locationsLoading.value = true
    locationsError.value = null
    try {
      locations.value = await deduplicatedFetch('locations', () => weatherService.getLocations())
    } catch (err: unknown) {
      locationsError.value = err instanceof Error ? err.message : 'Failed to load locations'
    } finally {
      locationsLoading.value = false
    }
  }

  // --- Airports ---
  const airports = ref<Location[]>([])
  const airportsLoading = ref(false)
  const airportsError = ref<string | null>(null)

  async function fetchAirports() {
    airportsLoading.value = true
    airportsError.value = null
    try {
      airports.value = await deduplicatedFetch('airports', () => weatherService.getAirports())
    } catch (err: unknown) {
      airportsError.value = err instanceof Error ? err.message : 'Failed to load airports'
    } finally {
      airportsLoading.value = false
    }
  }

  // --- Forecasts ---
  const forecasts = ref<WeatherForecast[]>([])
  const forecastsLoading = ref(false)
  const forecastsError = ref<string | null>(null)

  async function fetchForecasts(locationId: number) {
    forecastsLoading.value = true
    forecastsError.value = null
    try {
      forecasts.value = await deduplicatedFetch(`forecasts-${locationId}`, () =>
        weatherService.getForecastsByLocation(locationId),
      )
    } catch (err: unknown) {
      forecastsError.value = err instanceof Error ? err.message : 'Failed to load forecasts'
    } finally {
      forecastsLoading.value = false
    }
  }

  // --- Hurricanes ---
  const hurricanes = ref<Hurricane[]>([])
  const hurricanesLoading = ref(false)
  const hurricanesError = ref<string | null>(null)

  async function fetchHurricanes() {
    hurricanesLoading.value = true
    hurricanesError.value = null
    try {
      hurricanes.value = await deduplicatedFetch('hurricanes', () =>
        weatherService.getActiveStorms(),
      )
    } catch (err: unknown) {
      hurricanesError.value = err instanceof Error ? err.message : 'Failed to load hurricane data'
    } finally {
      hurricanesLoading.value = false
    }
  }

  // --- Historical Forecasts ---
  const historicalForecasts = ref<WeatherForecast[]>([])
  const historicalLoading = ref(false)
  const historicalError = ref<string | null>(null)

  async function fetchHistoricalForecasts(locationId: number, days: number = 7) {
    historicalLoading.value = true
    historicalError.value = null
    try {
      historicalForecasts.value = await deduplicatedFetch(
        `history-${locationId}-${days}`,
        () => weatherService.getHistoricalForecasts(locationId, days),
      )
    } catch (err: unknown) {
      historicalError.value =
        err instanceof Error ? err.message : 'Failed to load historical data'
    } finally {
      historicalLoading.value = false
    }
  }

  // --- Earthquakes ---
  const earthquakes = ref<Earthquake[]>([])
  const earthquakesLoading = ref(false)
  const earthquakesError = ref<string | null>(null)

  async function fetchEarthquakes() {
    earthquakesLoading.value = true
    earthquakesError.value = null
    try {
      earthquakes.value = await deduplicatedFetch('earthquakes', () =>
        weatherService.getRecentEarthquakes(),
      )
    } catch (err: unknown) {
      earthquakesError.value = err instanceof Error ? err.message : 'Failed to load earthquake data'
    } finally {
      earthquakesLoading.value = false
    }
  }

  // --- Alerts ---
  const alerts = ref<WeatherAlert[]>([])
  const alertsError = ref(false)

  async function fetchAlerts() {
    try {
      alerts.value = await deduplicatedFetch('alerts', () => weatherService.getActiveAlerts())
      alertsError.value = false
    } catch {
      alertsError.value = true
    }
  }

  // --- Refresh helpers (bust cache then re-fetch) ---
  async function refreshEarthquakes() {
    clearCache('earthquakes')
    await weatherService.refreshEarthquakes()
    await new Promise((r) => setTimeout(r, 2000))
    await fetchEarthquakes()
  }

  async function refreshLocations() {
    clearCache('locations')
    await fetchLocations()
  }

  async function refreshHurricanes() {
    clearCache('hurricanes')
    await weatherService.refreshHurricaneData()
    // Wait for backend to process
    await new Promise((r) => setTimeout(r, 2000))
    await fetchHurricanes()
  }

  async function refreshAlerts() {
    clearCache('alerts')
    await fetchAlerts()
  }

  return {
    // Locations
    locations,
    locationsLoading,
    locationsError,
    fetchLocations,
    refreshLocations,
    // Airports
    airports,
    airportsLoading,
    airportsError,
    fetchAirports,
    // Forecasts
    forecasts,
    forecastsLoading,
    forecastsError,
    fetchForecasts,
    // Hurricanes
    hurricanes,
    hurricanesLoading,
    hurricanesError,
    fetchHurricanes,
    refreshHurricanes,
    // Historical
    historicalForecasts,
    historicalLoading,
    historicalError,
    fetchHistoricalForecasts,
    // Earthquakes
    earthquakes,
    earthquakesLoading,
    earthquakesError,
    fetchEarthquakes,
    refreshEarthquakes,
    // Alerts
    alerts,
    alertsError,
    fetchAlerts,
    refreshAlerts,
  }
})
