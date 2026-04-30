import { ref } from 'vue'
import { defineStore } from 'pinia'
import weatherService, {
  type Location,
  type WeatherForecast,
  type Hurricane,
  type Earthquake,
  type WeatherAlert,
  type Pirep,
  type Sigmet,
  type Cwa,
  type WindsAloft,
  type Tfr,
  type AirportDelay,
  type GroundStop,
  type VolcanicAshAdvisory,
  type LightningStrike,
  type SpaceWeather,
  type AirQuality,
  type AstronomicalData,
  type DashboardData,
} from '../services/weatherService'

const CACHE_TTLS: Record<string, number> = {
  airports: 30 * 60_000,
  alerts: 5 * 60_000,
  hurricanes: 5 * 60_000,
  earthquakes: 10 * 60_000,
  pireps: 5 * 60_000,
  sigmets: 5 * 60_000,
  cwas: 5 * 60_000,
  windsAloft: 30 * 60_000,
  tfrs: 10 * 60_000,
  delays: 2 * 60_000,
  groundStops: 2 * 60_000,
  volcanicAsh: 5 * 60_000,
  lightning: 2 * 60_000,
  spaceWeather: 5 * 60_000,
  airQuality: 10 * 60_000,
  astronomical: 30 * 60_000,
}
const DEFAULT_CACHE_TTL = 60_000 // 1 min fallback (forecasts, history, etc.)
const pendingRequests = new Map<string, Promise<unknown>>()
const cache = new Map<string, { data: unknown; timestamp: number }>()

function isCacheValid(key: string): boolean {
  const entry = cache.get(key)
  if (!entry) return false
  const ttl = CACHE_TTLS[key] ?? DEFAULT_CACHE_TTL
  return Date.now() - entry.timestamp < ttl
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

  // --- PIREPs ---
  const pireps = ref<Pirep[]>([])
  const pirepsLoading = ref(false)
  const pirepsError = ref<string | null>(null)

  async function fetchPireps() {
    pirepsLoading.value = true
    pirepsError.value = null
    try {
      pireps.value = await deduplicatedFetch('pireps', () => weatherService.getRecentPireps())
    } catch (err: unknown) {
      pirepsError.value = err instanceof Error ? err.message : 'Failed to load PIREPs'
    } finally {
      pirepsLoading.value = false
    }
  }

  // --- SIGMETs ---
  const sigmets = ref<Sigmet[]>([])
  const sigmetsLoading = ref(false)
  const sigmetsError = ref<string | null>(null)

  async function fetchSigmets() {
    sigmetsLoading.value = true
    sigmetsError.value = null
    try {
      sigmets.value = await deduplicatedFetch('sigmets', () => weatherService.getActiveSigmets())
    } catch (err: unknown) {
      sigmetsError.value = err instanceof Error ? err.message : 'Failed to load SIGMETs'
    } finally {
      sigmetsLoading.value = false
    }
  }

  // --- CWAs ---
  const cwas = ref<Cwa[]>([])
  const cwasLoading = ref(false)
  const cwasError = ref<string | null>(null)

  async function fetchCwas() {
    cwasLoading.value = true
    cwasError.value = null
    try {
      cwas.value = await deduplicatedFetch('cwas', () => weatherService.getActiveCwas())
    } catch (err: unknown) {
      cwasError.value = err instanceof Error ? err.message : 'Failed to load CWAs'
    } finally {
      cwasLoading.value = false
    }
  }

  // --- Winds Aloft ---
  const windsAloft = ref<WindsAloft[]>([])
  const windsAloftLoading = ref(false)
  const windsAloftError = ref<string | null>(null)

  async function fetchWindsAloft() {
    windsAloftLoading.value = true
    windsAloftError.value = null
    try {
      windsAloft.value = await deduplicatedFetch('windsAloft', () => weatherService.getLatestWindsAloft())
    } catch (err: unknown) {
      windsAloftError.value = err instanceof Error ? err.message : 'Failed to load winds aloft'
    } finally {
      windsAloftLoading.value = false
    }
  }

  // --- TFRs ---
  const tfrs = ref<Tfr[]>([])
  const tfrsLoading = ref(false)
  const tfrsError = ref<string | null>(null)

  async function fetchTfrs() {
    tfrsLoading.value = true
    tfrsError.value = null
    try {
      tfrs.value = await deduplicatedFetch('tfrs', () => weatherService.getActiveTfrs())
    } catch (err: unknown) {
      tfrsError.value = err instanceof Error ? err.message : 'Failed to load TFRs'
    } finally {
      tfrsLoading.value = false
    }
  }

  // --- Airport Delays ---
  const delays = ref<AirportDelay[]>([])
  const delaysLoading = ref(false)
  const delaysError = ref<string | null>(null)

  async function fetchDelays() {
    delaysLoading.value = true
    delaysError.value = null
    try {
      delays.value = await deduplicatedFetch('delays', () => weatherService.getActiveDelays())
    } catch (err: unknown) {
      delaysError.value = err instanceof Error ? err.message : 'Failed to load airport delays'
    } finally {
      delaysLoading.value = false
    }
  }

  // --- Ground Stops ---
  const groundStops = ref<GroundStop[]>([])
  const groundStopsLoading = ref(false)
  const groundStopsError = ref<string | null>(null)

  async function fetchGroundStops() {
    groundStopsLoading.value = true
    groundStopsError.value = null
    try {
      groundStops.value = await deduplicatedFetch('groundStops', () => weatherService.getActiveGroundStops())
    } catch (err: unknown) {
      groundStopsError.value = err instanceof Error ? err.message : 'Failed to load ground stops'
    } finally {
      groundStopsLoading.value = false
    }
  }

  // --- Volcanic Ash ---
  const volcanicAsh = ref<VolcanicAshAdvisory[]>([])
  const volcanicAshLoading = ref(false)
  const volcanicAshError = ref<string | null>(null)

  async function fetchVolcanicAsh() {
    volcanicAshLoading.value = true
    volcanicAshError.value = null
    try {
      volcanicAsh.value = await deduplicatedFetch('volcanicAsh', () => weatherService.getActiveVolcanicAsh())
    } catch (err: unknown) {
      volcanicAshError.value = err instanceof Error ? err.message : 'Failed to load volcanic ash advisories'
    } finally {
      volcanicAshLoading.value = false
    }
  }

  // --- Lightning ---
  const lightning = ref<LightningStrike[]>([])
  const lightningLoading = ref(false)
  const lightningError = ref<string | null>(null)

  async function fetchLightning() {
    lightningLoading.value = true
    lightningError.value = null
    try {
      lightning.value = await deduplicatedFetch('lightning', () => weatherService.getRecentLightning())
    } catch (err: unknown) {
      lightningError.value = err instanceof Error ? err.message : 'Failed to load lightning data'
    } finally {
      lightningLoading.value = false
    }
  }

  // --- Space Weather ---
  const spaceWeather = ref<SpaceWeather | null>(null)
  const spaceWeatherLoading = ref(false)
  const spaceWeatherError = ref<string | null>(null)

  async function fetchSpaceWeather() {
    spaceWeatherLoading.value = true
    spaceWeatherError.value = null
    try {
      spaceWeather.value = await deduplicatedFetch('spaceWeather', () => weatherService.getSpaceWeather())
    } catch (err: unknown) {
      spaceWeatherError.value = err instanceof Error ? err.message : 'Failed to load space weather'
    } finally {
      spaceWeatherLoading.value = false
    }
  }

  // --- Astronomical ---
  const astronomical = ref<AstronomicalData | null>(null)
  const astronomicalLoading = ref(false)
  const astronomicalError = ref<string | null>(null)

  async function fetchAstronomical(lat: number, lon: number) {
    astronomicalLoading.value = true
    astronomicalError.value = null
    try {
      astronomical.value = await deduplicatedFetch(`astronomical-${lat}-${lon}`, () =>
        weatherService.getAstronomicalData(lat, lon),
      )
    } catch (err: unknown) {
      astronomicalError.value = err instanceof Error ? err.message : 'Failed to load astronomical data'
    } finally {
      astronomicalLoading.value = false
    }
  }

  // --- Air Quality ---
  const airQuality = ref<AirQuality | null>(null)
  const airQualityLoading = ref(false)
  const airQualityError = ref<string | null>(null)

  async function fetchAirQuality(locationId?: number) {
    airQualityLoading.value = true
    airQualityError.value = null
    try {
      if (locationId) {
        airQuality.value = await deduplicatedFetch(`airQuality-${locationId}`, () =>
          weatherService.getAirQuality(locationId),
        )
      }
    } catch (err: unknown) {
      airQualityError.value = err instanceof Error ? err.message : 'Failed to load air quality data'
    } finally {
      airQualityLoading.value = false
    }
  }

  async function refreshAirQuality() {
    clearCache('airQuality')
    await weatherService.refreshAirQuality()
    await new Promise((r) => setTimeout(r, 2000))
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

  async function refreshSpaceWeather() {
    clearCache('spaceWeather')
    await fetchSpaceWeather()
  }

  async function refreshPireps() {
    clearCache('pireps')
    await weatherService.refreshPireps()
    await new Promise((r) => setTimeout(r, 2000))
    await fetchPireps()
  }

  async function refreshSigmets() {
    clearCache('sigmets')
    await weatherService.refreshSigmets()
    await new Promise((r) => setTimeout(r, 2000))
    await fetchSigmets()
  }

  async function refreshCwas() {
    clearCache('cwas')
    await weatherService.refreshCwas()
    await new Promise((r) => setTimeout(r, 2000))
    await fetchCwas()
  }

  async function refreshWindsAloft() {
    clearCache('windsAloft')
    await weatherService.refreshWindsAloft()
    await new Promise((r) => setTimeout(r, 2000))
    await fetchWindsAloft()
  }

  async function refreshTfrs() {
    clearCache('tfrs')
    await weatherService.refreshTfrs()
    await new Promise((r) => setTimeout(r, 2000))
    await fetchTfrs()
  }

  async function refreshDelays() {
    clearCache('delays')
    await weatherService.refreshDelays()
    await new Promise((r) => setTimeout(r, 2000))
    await fetchDelays()
  }

  async function refreshGroundStops() {
    clearCache('groundStops')
    await weatherService.refreshGroundStops()
    await new Promise((r) => setTimeout(r, 2000))
    await fetchGroundStops()
  }

  async function refreshVolcanicAsh() {
    clearCache('volcanicAsh')
    await weatherService.refreshVolcanicAsh()
    await new Promise((r) => setTimeout(r, 2000))
    await fetchVolcanicAsh()
  }

  async function refreshLightning() {
    clearCache('lightning')
    await weatherService.refreshLightning()
    await new Promise((r) => setTimeout(r, 2000))
    await fetchLightning()
  }

  // --- Composite dashboard fetch (single HTTP request for all 12 sources) ---
  async function fetchDashboardData() {
    earthquakesLoading.value = true
    hurricanesLoading.value = true
    pirepsLoading.value = true
    sigmetsLoading.value = true
    cwasLoading.value = true
    tfrsLoading.value = true
    delaysLoading.value = true
    groundStopsLoading.value = true
    volcanicAshLoading.value = true
    lightningLoading.value = true
    spaceWeatherLoading.value = true

    try {
      const data: DashboardData = await weatherService.getDashboardData()

      earthquakes.value = data.earthquakes
      hurricanes.value = data.hurricanes
      alerts.value = data.alerts
      pireps.value = data.pireps
      sigmets.value = data.sigmets
      cwas.value = data.cwas
      tfrs.value = data.tfrs
      delays.value = data.delays
      groundStops.value = data.groundStops
      volcanicAsh.value = data.volcanicAsh
      lightning.value = data.lightning
      spaceWeather.value = data.spaceWeather

      // Populate cache so individual fetch*() calls see fresh data
      const now = Date.now()
      cache.set('earthquakes', { data: data.earthquakes, timestamp: now })
      cache.set('hurricanes', { data: data.hurricanes, timestamp: now })
      cache.set('alerts', { data: data.alerts, timestamp: now })
      cache.set('pireps', { data: data.pireps, timestamp: now })
      cache.set('sigmets', { data: data.sigmets, timestamp: now })
      cache.set('cwas', { data: data.cwas, timestamp: now })
      cache.set('tfrs', { data: data.tfrs, timestamp: now })
      cache.set('delays', { data: data.delays, timestamp: now })
      cache.set('groundStops', { data: data.groundStops, timestamp: now })
      cache.set('volcanicAsh', { data: data.volcanicAsh, timestamp: now })
      cache.set('lightning', { data: data.lightning, timestamp: now })
      cache.set('spaceWeather', { data: data.spaceWeather, timestamp: now })
    } finally {
      earthquakesLoading.value = false
      hurricanesLoading.value = false
      pirepsLoading.value = false
      sigmetsLoading.value = false
      cwasLoading.value = false
      tfrsLoading.value = false
      delaysLoading.value = false
      groundStopsLoading.value = false
      volcanicAshLoading.value = false
      lightningLoading.value = false
      spaceWeatherLoading.value = false
    }
  }

  // --- SSE update handler (patches a single data source from a live stream event) ---
  function applySSEUpdate(eventName: string, data: unknown) {
    const now = Date.now()
    const handlers: Record<string, () => void> = {
      earthquakes: () => { earthquakes.value = data as Earthquake[]; cache.set('earthquakes', { data, timestamp: now }) },
      hurricanes: () => { hurricanes.value = data as Hurricane[]; cache.set('hurricanes', { data, timestamp: now }) },
      alerts: () => { alerts.value = data as WeatherAlert[]; cache.set('alerts', { data, timestamp: now }) },
      pireps: () => { pireps.value = data as Pirep[]; cache.set('pireps', { data, timestamp: now }) },
      sigmets: () => { sigmets.value = data as Sigmet[]; cache.set('sigmets', { data, timestamp: now }) },
      cwas: () => { cwas.value = data as Cwa[]; cache.set('cwas', { data, timestamp: now }) },
      tfrs: () => { tfrs.value = data as Tfr[]; cache.set('tfrs', { data, timestamp: now }) },
      delays: () => { delays.value = data as AirportDelay[]; cache.set('delays', { data, timestamp: now }) },
      groundStops: () => { groundStops.value = data as GroundStop[]; cache.set('groundStops', { data, timestamp: now }) },
      volcanicAsh: () => { volcanicAsh.value = data as VolcanicAshAdvisory[]; cache.set('volcanicAsh', { data, timestamp: now }) },
      lightning: () => { lightning.value = data as LightningStrike[]; cache.set('lightning', { data, timestamp: now }) },
      spaceWeather: () => { spaceWeather.value = data as SpaceWeather | null; cache.set('spaceWeather', { data, timestamp: now }) },
    }
    handlers[eventName]?.()
  }

  return {
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
    // PIREPs
    pireps,
    pirepsLoading,
    pirepsError,
    fetchPireps,
    refreshPireps,
    // SIGMETs
    sigmets,
    sigmetsLoading,
    sigmetsError,
    fetchSigmets,
    refreshSigmets,
    // CWAs
    cwas,
    cwasLoading,
    cwasError,
    fetchCwas,
    refreshCwas,
    // Winds Aloft
    windsAloft,
    windsAloftLoading,
    windsAloftError,
    fetchWindsAloft,
    refreshWindsAloft,
    // TFRs
    tfrs,
    tfrsLoading,
    tfrsError,
    fetchTfrs,
    refreshTfrs,
    // Delays
    delays,
    delaysLoading,
    delaysError,
    fetchDelays,
    refreshDelays,
    // Ground Stops
    groundStops,
    groundStopsLoading,
    groundStopsError,
    fetchGroundStops,
    refreshGroundStops,
    // Volcanic Ash
    volcanicAsh,
    volcanicAshLoading,
    volcanicAshError,
    fetchVolcanicAsh,
    refreshVolcanicAsh,
    // Lightning
    lightning,
    lightningLoading,
    lightningError,
    fetchLightning,
    refreshLightning,
    // Space Weather
    spaceWeather,
    spaceWeatherLoading,
    spaceWeatherError,
    fetchSpaceWeather,
    refreshSpaceWeather,
    // Astronomical
    astronomical,
    astronomicalLoading,
    astronomicalError,
    fetchAstronomical,
    // Air Quality
    airQuality,
    airQualityLoading,
    airQualityError,
    fetchAirQuality,
    refreshAirQuality,
    // Alerts
    alerts,
    alertsError,
    fetchAlerts,
    refreshAlerts,
    // Dashboard composite
    fetchDashboardData,
    applySSEUpdate,
  }
})
