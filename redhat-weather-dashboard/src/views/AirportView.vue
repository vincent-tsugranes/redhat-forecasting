<template>
  <div class="container">
    <h1>{{ $t('airport.title') }}</h1>

    <div class="card search-card">
      <h2>{{ $t('airport.selectAirport') }}</h2>
      <div class="search-wrapper">
        <label for="airport-search" class="sr-only">{{ $t('airport.searchAriaLabel') }}</label>
        <input
          id="airport-search"
          v-model="searchQuery"
          type="text"
          :placeholder="$t('airport.searchPlaceholder')"
          class="search-input"
          role="combobox"
          aria-autocomplete="list"
          :aria-expanded="searchResults.length > 0 && showResults && !!searchQuery"
          aria-controls="airport-listbox"
          :aria-activedescendant="activeDescendant"
          @input="onSearchInput"
          @focus="showResults = true"
          @keydown="onSearchKeydown"
        />
        <ul
          v-if="searchResults.length > 0 && showResults && searchQuery"
          id="airport-listbox"
          class="search-results"
          role="listbox"
        >
          <li
            v-for="(airport, index) in searchResults"
            :id="'airport-option-' + airport.id"
            :key="airport.id"
            class="search-result-item"
            :class="{ 'search-result-active': highlightedIndex === index }"
            role="option"
            :aria-selected="highlightedIndex === index"
            @click="selectAirport(airport)"
          >
            <div class="result-code">{{ airport.airportCode }}</div>
            <div class="result-name">{{ airport.name }}</div>
            <div class="result-location">{{ airport.state ? airport.state + ', ' : '' }}{{ airport.country }}</div>
          </li>
        </ul>
      </div>
      <button
        v-if="selectedAirportCode"
        :disabled="refreshing"
        style="margin-top: 10px"
        @click="refreshWeather"
      >
        {{ refreshing ? $t('airport.refreshing') : $t('airport.refreshWeather') }}
      </button>
    </div>

    <AirportSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="metar" class="card">
      <h2>{{ $t('airport.metar') }}</h2>
      <div class="weather-report">
        <div class="report-header">
          <strong>{{ selectedAirportCode }}</strong>
          <span>{{ formatDate(metar.observationTime) }}</span>
        </div>
        <div v-if="metar.fetchedAt" style="margin-bottom: 10px">
          <FreshnessBadge :fetched-at="metar.fetchedAt" data-type="airport" />
        </div>
        <DecodedWeather :raw-text="metar.rawText" type="metar">
          <div class="report-raw">{{ metar.rawText }}</div>
          <div class="report-details">
            <div
              v-if="metar.flightCategory"
              class="flight-category"
              :class="'category-' + metar.flightCategory"
              :aria-label="'Flight category: ' + metar.flightCategory"
            >
              {{ metar.flightCategory }}
            </div>
            <div v-if="metar.temperatureCelsius != null">
              <span aria-hidden="true">🌡️</span> {{ Math.round(metar.temperatureCelsius) }}°C
            </div>
            <div v-if="metar.dewpointCelsius != null">
              <span aria-hidden="true">💧</span> Dew {{ Math.round(metar.dewpointCelsius) }}°C
            </div>
            <div v-if="metar.windSpeedKnots != null">
              <span aria-hidden="true">💨</span>
              {{ metar.windDirection != null ? metar.windDirection + '° at ' : ''
              }}{{ metar.windSpeedKnots }} kts{{
                metar.windGustKnots ? ', gusts ' + metar.windGustKnots + ' kts' : ''
              }}
            </div>
            <div v-if="metar.visibilityMiles != null">
              <span aria-hidden="true">👁️</span> Visibility: {{ metar.visibilityMiles }} mi
            </div>
            <div v-if="metar.ceilingFeet != null">
              <span aria-hidden="true">☁️</span> Ceiling {{ metar.ceilingFeet }} ft
            </div>
            <div v-if="metar.altimeterInches != null">
              <span aria-hidden="true">📊</span> Altimeter {{ metar.altimeterInches }} inHg
            </div>
            <div v-if="metar.skyCondition">
              <span aria-hidden="true">🌤️</span> Sky: {{ metar.skyCondition }}
            </div>
            <div v-if="metar.weatherConditions">
              <span aria-hidden="true">🌧️</span> {{ metar.weatherConditions }}
            </div>
          </div>
        </DecodedWeather>
      </div>
    </div>

    <div v-if="taf" class="card">
      <h2>{{ $t('airport.taf') }}</h2>
      <div class="weather-report">
        <div class="report-header">
          <strong>{{ selectedAirportCode }}</strong>
          <span>{{ formatDate(taf.observationTime) }}</span>
        </div>
        <div v-if="taf.fetchedAt" style="margin-bottom: 10px">
          <FreshnessBadge :fetched-at="taf.fetchedAt" data-type="airport" />
        </div>
        <DecodedWeather :raw-text="taf.rawText" type="taf">
          <div class="report-raw">{{ taf.rawText }}</div>
        </DecodedWeather>
      </div>
    </div>

    <!-- Airport detail panels (only when an airport is selected and loaded) -->
    <template v-if="!loading && selectedAirportCode && (metar || taf)">

      <!-- Delay status banner -->
      <div v-if="airportDelay && airportDelay.isDelayed" class="card delay-banner">
        <div class="delay-header">
          <span class="delay-icon" aria-hidden="true">&#x26A0;&#xFE0F;</span>
          <strong>{{ airportDelay.airportCode }} — {{ airportDelay.delayType }} Delay</strong>
        </div>
        <div class="delay-details">
          <span v-if="airportDelay.avgDelayMinutes">Avg: {{ airportDelay.avgDelayMinutes }} min</span>
          <span v-if="airportDelay.reason">Reason: {{ airportDelay.reason }}</span>
          <span v-if="airportDelay.trend" class="trend-badge" :class="'trend-' + airportDelay.trend">{{ airportDelay.trend }}</span>
        </div>
      </div>

      <div class="airport-detail-grid">
        <!-- Left column: map + forecast + history -->
        <div class="detail-main">
          <!-- Mini map -->
          <div v-if="selectedAirport" class="card">
            <h2>Location</h2>
            <div ref="miniMapContainer" class="mini-map"></div>
            <div class="location-meta">
              {{ selectedAirport.latitude.toFixed(4) }}{{ selectedAirport.latitude >= 0 ? 'N' : 'S' }},
              {{ Math.abs(selectedAirport.longitude).toFixed(4) }}{{ selectedAirport.longitude >= 0 ? 'E' : 'W' }}
              <span v-if="selectedAirport.state || selectedAirport.country" class="location-region">
                {{ selectedAirport.state ? selectedAirport.state + ', ' : '' }}{{ selectedAirport.country }}
              </span>
            </div>
          </div>

          <!-- Hourly forecast -->
          <div v-if="forecasts.length > 0" class="card">
            <HourlyTimeline :forecasts="forecasts" />
          </div>

          <!-- Historical chart -->
          <HistoricalChart v-if="selectedAirport?.id" :location-id="selectedAirport.id" />
        </div>

        <!-- Right column: solar, links, nearby -->
        <div class="detail-sidebar">
          <!-- Solar data -->
          <SolarPanel v-if="selectedAirport?.id" :location-id="selectedAirport.id" />

          <!-- Charts & resources -->
          <div v-if="isUsAirport" class="card">
            <h3>Charts &amp; Resources</h3>
            <div class="resource-links">
              <a
                :href="`https://flightaware.com/resources/airport/${selectedAirportCode}/procedures`"
                target="_blank"
                rel="noopener noreferrer"
                class="resource-link"
              >
                <span class="resource-icon" aria-hidden="true">&#x1F4C4;</span>
                <div>
                  <div class="resource-title">Airport Procedures</div>
                  <div class="resource-desc">Instrument approaches &amp; diagrams</div>
                </div>
              </a>
              <a
                :href="`https://skyvector.com/airport/${faaCode}`"
                target="_blank"
                rel="noopener noreferrer"
                class="resource-link"
              >
                <span class="resource-icon" aria-hidden="true">&#x1F5FA;&#xFE0F;</span>
                <div>
                  <div class="resource-title">SkyVector Chart</div>
                  <div class="resource-desc">Sectional &amp; IFR charts</div>
                </div>
              </a>
              <a
                :href="`https://www.airnav.com/airport/${selectedAirportCode}`"
                target="_blank"
                rel="noopener noreferrer"
                class="resource-link"
              >
                <span class="resource-icon" aria-hidden="true">&#x2139;&#xFE0F;</span>
                <div>
                  <div class="resource-title">AirNav Info</div>
                  <div class="resource-desc">Runways, frequencies, FBOs</div>
                </div>
              </a>
            </div>
          </div>

          <!-- Nearby PIREPs -->
          <div v-if="nearbyPireps.length > 0" class="card">
            <h3>Nearby Pilot Reports</h3>
            <div class="nearby-list">
              <div v-for="p in nearbyPireps" :key="p.id" class="nearby-item">
                <div class="nearby-header">
                  <span class="nearby-type">{{ p.reportType }}</span>
                  <span class="nearby-time">{{ formatDate(p.observationTime) }}</span>
                </div>
                <div v-if="p.altitudeFt" class="nearby-detail">Alt: {{ p.altitudeFt.toLocaleString() }} ft</div>
                <div v-if="p.turbulenceIntensity" class="nearby-detail nearby-hazard">Turbulence: {{ p.turbulenceIntensity }}</div>
                <div v-if="p.icingIntensity" class="nearby-detail nearby-hazard">Icing: {{ p.icingIntensity }}</div>
                <div v-if="p.skyCondition" class="nearby-detail">Sky: {{ p.skyCondition }}</div>
              </div>
            </div>
          </div>

          <!-- Active SIGMETs -->
          <div v-if="nearbySigmets.length > 0" class="card">
            <h3>Active SIGMETs</h3>
            <div class="nearby-list">
              <div v-for="s in nearbySigmets" :key="s.id" class="nearby-item">
                <div class="nearby-header">
                  <span class="nearby-type">{{ s.sigmetType }}</span>
                  <span v-if="s.hazard" class="nearby-hazard">{{ s.hazard }}</span>
                </div>
                <div class="nearby-detail">Valid: {{ formatDate(s.validTimeFrom) }} - {{ formatDate(s.validTimeTo) }}</div>
                <div v-if="s.severity" class="nearby-detail">Severity: {{ s.severity }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div v-if="!loading && !metar && selectedAirportCode" class="card">
      <p>{{ $t('airport.noData') }}</p>
      <p>{{ $t('airport.noDataHint') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, onUnmounted, watch, nextTick, markRaw } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import weatherService, { type AirportWeather, type WeatherForecast, type Location } from '../services/weatherService'
import { formatDate } from '../utils/dateUtils'
import FreshnessBadge from '../components/FreshnessBadge.vue'
import DecodedWeather from '../components/DecodedWeather.vue'
import HourlyTimeline from '../components/HourlyTimeline.vue'
import HistoricalChart from '../components/HistoricalChart.vue'
import SolarPanel from '../components/SolarPanel.vue'
import AirportSkeleton from '../components/skeletons/AirportSkeleton.vue'

const route = useRoute()
const store = useWeatherStore()
const { airports, pireps, sigmets, delays } = storeToRefs(store)

const selectedAirportCode = ref<string>('')
const selectedAirport = ref<Location | null>(null)
const metar = ref<AirportWeather | null>(null)
const taf = ref<AirportWeather | null>(null)
const forecasts = ref<WeatherForecast[]>([])
const loading = ref(false)
const refreshing = ref(false)
const error = ref<string | null>(null)
let refreshTimeout: ReturnType<typeof setTimeout> | null = null

// Mini map
const miniMapContainer = ref<HTMLElement | null>(null)
let miniMap: L.Map | null = null
let miniMapMarker: L.Marker | null = null

// Search state
const searchQuery = ref('')
const searchResults = ref<Location[]>([])
const showResults = ref(false)
const highlightedIndex = ref(-1)

const activeDescendant = computed(() => {
  if (highlightedIndex.value >= 0 && highlightedIndex.value < searchResults.value.length) {
    return 'airport-option-' + searchResults.value[highlightedIndex.value].id
  }
  return undefined
})

function onSearchInput() {
  highlightedIndex.value = -1
  if (!searchQuery.value || searchQuery.value.length < 1) {
    searchResults.value = []
    return
  }

  const query = searchQuery.value.toLowerCase()
  searchResults.value = airports.value
    .filter(
      (apt) =>
        apt.airportCode?.toLowerCase().includes(query) ||
        apt.name?.toLowerCase().includes(query),
    )
    .slice(0, 50)
  showResults.value = true
}

function onSearchKeydown(event: KeyboardEvent) {
  if (!showResults.value || searchResults.value.length === 0) return

  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      highlightedIndex.value = Math.min(highlightedIndex.value + 1, searchResults.value.length - 1)
      break
    case 'ArrowUp':
      event.preventDefault()
      highlightedIndex.value = Math.max(highlightedIndex.value - 1, 0)
      break
    case 'Enter':
      event.preventDefault()
      if (highlightedIndex.value >= 0) {
        selectAirport(searchResults.value[highlightedIndex.value])
      }
      break
    case 'Escape':
      event.preventDefault()
      showResults.value = false
      highlightedIndex.value = -1
      break
  }
}

function selectAirport(airport: Location) {
  selectedAirportCode.value = airport.airportCode || ''
  selectedAirport.value = airport
  searchQuery.value = `${airport.airportCode} - ${airport.name}`
  searchResults.value = []
  showResults.value = false
  loadAirportWeather()
}

async function loadAirportWeather() {
  if (!selectedAirportCode.value) return

  loading.value = true
  error.value = null
  metar.value = null
  taf.value = null
  forecasts.value = []

  try {
    const promises: Promise<unknown>[] = [
      weatherService.getLatestMetar(selectedAirportCode.value),
      weatherService.getLatestTaf(selectedAirportCode.value),
    ]

    if (selectedAirport.value?.id) {
      promises.push(weatherService.getForecastsByLocation(selectedAirport.value.id))
    }

    const [metarData, tafData, forecastData] = await Promise.allSettled(promises)

    if (metarData.status === 'fulfilled') {
      metar.value = metarData.value as AirportWeather
    }

    if (tafData.status === 'fulfilled') {
      taf.value = tafData.value as AirportWeather
    }

    if (forecastData && forecastData.status === 'fulfilled') {
      forecasts.value = forecastData.value as WeatherForecast[]
    }

    if (metarData.status === 'rejected' && tafData.status === 'rejected') {
      error.value = 'No weather data available for this airport'
    }
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Failed to load airport weather'
  } finally {
    loading.value = false
    nextTick(() => updateMiniMap())
  }
}

function updateMiniMap() {
  const apt = selectedAirport.value
  if (!apt?.latitude || !apt?.longitude) return

  if (!miniMapContainer.value) return

  if (!miniMap) {
    miniMap = markRaw(L.map(miniMapContainer.value, {
      zoomControl: false,
      attributionControl: false,
      dragging: false,
      scrollWheelZoom: false,
      doubleClickZoom: false,
      touchZoom: false,
    }).setView([apt.latitude, apt.longitude], 10)) as unknown as L.Map

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
    }).addTo(miniMap)
  } else {
    miniMap.setView([apt.latitude, apt.longitude], 10)
  }

  if (miniMapMarker) {
    miniMapMarker.remove()
  }
  miniMapMarker = L.marker([apt.latitude, apt.longitude]).addTo(miniMap)
  miniMapMarker.bindPopup(`<strong>${apt.airportCode}</strong><br/>${apt.name}`)
}

// Nearby PIREPs (within ~100nm / ~1.5 degrees)
const nearbyPireps = computed(() => {
  const apt = selectedAirport.value
  if (!apt?.latitude || !apt?.longitude) return []
  const range = 1.5
  return pireps.value.filter(p =>
    Math.abs(p.latitude - apt.latitude) < range &&
    Math.abs(p.longitude - apt.longitude) < range
  ).slice(0, 10)
})

// Nearby SIGMETs — these have geojson but no simple lat/lon center, so check rawText or firName
const nearbySigmets = computed(() => {
  // Show all active SIGMETs since we can't easily filter by proximity without geojson parsing
  return sigmets.value.slice(0, 5)
})

// Delay status for this airport
const airportDelay = computed(() => {
  if (!selectedAirportCode.value) return null
  return delays.value.find(d => d.airportCode === selectedAirportCode.value) || null
})

// FAA chart links (US airports starting with K)
const isUsAirport = computed(() => selectedAirportCode.value.startsWith('K') && selectedAirportCode.value.length === 4)
const faaCode = computed(() => isUsAirport.value ? selectedAirportCode.value.substring(1) : '')

async function refreshWeather() {
  if (!selectedAirportCode.value) return

  refreshing.value = true
  error.value = null

  try {
    await weatherService.refreshAirportWeather(selectedAirportCode.value)
    refreshTimeout = setTimeout(() => {
      loadAirportWeather()
      refreshing.value = false
    }, 2000)
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Failed to refresh airport weather'
    refreshing.value = false
  }
}

function selectByCode(code: string) {
  const airport = airports.value.find(a => a.airportCode === code)
  if (airport) {
    selectAirport(airport)
  } else {
    selectedAirportCode.value = code
    selectedAirport.value = null
    searchQuery.value = code
    loadAirportWeather()
  }
}

onMounted(async () => {
  await store.fetchAirports()
  // Also ensure pireps/sigmets/delays are loaded for the nearby sections
  store.fetchPireps()
  store.fetchSigmets()
  store.fetchDelays()
  const code = route.query.code as string | undefined
  if (code) selectByCode(code)
})

watch(() => route.query.code, (newCode) => {
  if (newCode && typeof newCode === 'string') selectByCode(newCode)
})

onBeforeUnmount(() => {
  if (miniMap) {
    miniMap.remove()
    miniMap = null
    miniMapMarker = null
  }
})

onUnmounted(() => {
  if (refreshTimeout) {
    clearTimeout(refreshTimeout)
    refreshTimeout = null
  }
})
</script>

<style scoped>
.search-card {
  position: relative;
}

.search-wrapper {
  position: relative;
}

.search-input {
  width: 100%;
  padding: 10px 16px;
  font-size: 16px;
  border: 2px solid var(--border-color, #ddd);
  border-radius: 8px;
  outline: none;
  transition: border-color 0.3s;
  box-sizing: border-box;
  background: var(--bg-input, #fff);
  color: var(--text-primary, #333);
}

.search-input:focus {
  border-color: var(--accent);
}

.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 4px;
  background: var(--bg-card, white);
  border: 1px solid var(--border-color, #ddd);
  border-radius: 8px;
  box-shadow: 0 4px 12px var(--shadow-md, rgba(0, 0, 0, 0.15));
  max-height: 300px;
  overflow-y: auto;
  z-index: 100;
  list-style: none;
  padding: 0;
  margin: 4px 0 0 0;
}

.search-result-item {
  padding: 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--border-light, #eee);
  transition: background-color 0.2s;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item:hover,
.search-result-active {
  background-color: var(--bg-code, #f5f5f5);
}

.result-code {
  font-weight: 700;
  font-size: 14px;
  color: var(--accent);
}

.result-name {
  font-weight: 500;
  font-size: 14px;
}

.result-location {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-top: 2px;
}

.weather-report {
  background: var(--bg-code, #f9f9f9);
  border-radius: 8px;
  padding: 12px;
}

.report-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 1rem;
}

.report-raw {
  background: var(--bg-input, #fff);
  border: 1px solid var(--border-color, #ddd);
  border-radius: 4px;
  padding: 8px;
  font-family: monospace;
  margin: 8px 0;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--text-primary, #333);
}

.report-details {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.flight-category {
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: bold;
  color: white;
}

.category-VFR {
  background: var(--flight-vfr);
}

.category-MVFR {
  background: var(--flight-mvfr);
}

.category-IFR {
  background: var(--flight-ifr);
  color: #333;
}

.category-LIFR {
  background: var(--flight-lifr);
}

/* Delay banner */
.delay-banner {
  border-left: 4px solid var(--color-warning, #f0ad4e);
  background: var(--bg-warning, #fff8e1);
}

.delay-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  margin-bottom: 6px;
}

.delay-icon {
  font-size: 18px;
}

.delay-details {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--text-secondary, #666);
}

.trend-badge {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  text-transform: capitalize;
}

.trend-increasing { background: var(--severity-high, #e53935); color: white; }
.trend-decreasing { background: var(--color-success, #43a047); color: white; }
.trend-stable { background: var(--text-muted, #999); color: white; }

/* Two-column detail grid */
.airport-detail-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 16px;
  margin-top: 12px;
}

.detail-main,
.detail-sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* Mini map */
.mini-map {
  height: 200px;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 8px;
}

.location-meta {
  font-size: 13px;
  color: var(--text-secondary, #666);
  font-family: monospace;
}

.location-region {
  margin-left: 8px;
  font-family: inherit;
  color: var(--text-primary, #333);
}

/* Resource links */
.resource-links {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.resource-link {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  text-decoration: none;
  color: var(--text-primary, #333);
  background: var(--bg-code, #f5f5f5);
  transition: background 0.2s;
}

.resource-link:hover {
  background: var(--bg-hover, #eee);
}

.resource-icon {
  font-size: 20px;
  flex-shrink: 0;
  line-height: 1;
  margin-top: 2px;
}

.resource-title {
  font-weight: 600;
  font-size: 13px;
}

.resource-desc {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-top: 1px;
}

/* Nearby PIREPs / SIGMETs */
.nearby-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nearby-item {
  padding: 8px 10px;
  border-radius: 6px;
  background: var(--bg-code, #f5f5f5);
  font-size: 13px;
}

.nearby-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.nearby-type {
  font-weight: 600;
  font-size: 12px;
  text-transform: uppercase;
  color: var(--accent);
}

.nearby-time {
  font-size: 11px;
  color: var(--text-muted, #999);
}

.nearby-detail {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-top: 2px;
}

.nearby-hazard {
  color: var(--severity-high, #e53935);
  font-weight: 600;
}

@media (max-width: 768px) {
  .airport-detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
