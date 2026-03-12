<template>
  <div class="container">
    <h1>{{ $t('forecast.title') }}</h1>

    <AlertsBanner />

    <div class="card search-card">
      <h2>{{ $t('forecast.searchLocation') }}</h2>
      <div class="search-wrapper">
        <label for="location-search" class="sr-only">{{ $t('forecast.searchAriaLabel') }}</label>
        <input
          id="location-search"
          v-model="searchQuery"
          type="text"
          :placeholder="$t('forecast.searchPlaceholder')"
          class="search-input"
          role="combobox"
          aria-autocomplete="list"
          :aria-expanded="searchResults.length > 0 && showResults && !!searchQuery"
          aria-controls="search-listbox"
          :aria-activedescendant="activeDescendant"
          @input="onSearchInput"
          @focus="showResults = true"
          @keydown="onSearchKeydown"
        />
        <ul
          v-if="searchResults.length > 0 && showResults && searchQuery"
          id="search-listbox"
          class="search-results"
          role="listbox"
        >
          <li
            v-for="(loc, index) in searchResults"
            :id="'search-option-' + loc.id"
            :key="loc.id"
            class="search-result-item"
            :class="{ 'search-result-active': highlightedIndex === index }"
            role="option"
            :aria-selected="highlightedIndex === index"
            @click="selectLocation(loc)"
          >
            <div class="result-name">{{ loc.name }}</div>
            <div class="result-state">{{ loc.state }}, {{ loc.country }}</div>
          </li>
        </ul>
      </div>

      <!-- Multi-location comparison -->
      <div v-if="selectedLocation" class="compare-section">
        <label for="compare-search" class="compare-label">Compare with:</label>
        <div class="compare-wrapper">
          <input
            id="compare-search"
            v-model="compareQuery"
            type="text"
            placeholder="Add location to compare..."
            class="search-input compare-input"
            @input="onCompareInput"
            @focus="showCompareResults = true"
            @keydown="onCompareKeydown"
          />
          <ul v-if="compareResults.length > 0 && showCompareResults && compareQuery" class="search-results">
            <li
              v-for="(loc, index) in compareResults"
              :key="loc.id"
              class="search-result-item"
              :class="{ 'search-result-active': compareHighlightedIndex === index }"
              @click="addCompareLocation(loc)"
            >
              <div class="result-name">{{ loc.name }}</div>
              <div class="result-state">{{ loc.state }}, {{ loc.country }}</div>
            </li>
          </ul>
        </div>
        <div v-if="compareLocations.length > 0" class="compare-tags">
          <span v-for="cl in compareLocations" :key="cl.id" class="compare-tag">
            {{ cl.name }}
            <button class="compare-remove" @click="removeCompareLocation(cl.id)">&times;</button>
          </span>
        </div>
      </div>
    </div>

    <div v-if="selectedLocation" class="selected-location-header">
      <h2>{{ selectedLocation.name }}, {{ selectedLocation.state || selectedLocation.country }}</h2>
      <FavoriteButton
        :active="isFavorite(selectedLocation.id)"
        @toggle="toggleFavorite(selectedLocation)"
      />
    </div>

    <!-- Section navigation -->
    <nav v-if="selectedLocation && forecasts.length > 0" class="section-nav" aria-label="Forecast sections">
      <a href="#section-daily" class="section-nav-link">Daily</a>
      <a href="#section-hourly" class="section-nav-link">Hourly</a>
      <a v-if="yesterdayComparison" href="#section-comparison" class="section-nav-link">vs Yesterday</a>
      <a v-if="compareLocations.length > 0" href="#section-multicompare" class="section-nav-link">Compare</a>
      <a href="#section-trends" class="section-nav-link">Trends</a>
      <a href="#section-wind" class="section-nav-link">Wind Rose</a>
      <a href="#section-data" class="section-nav-link">Data</a>
      <a href="#section-history" class="section-nav-link">History</a>
    </nav>

    <!-- Forecast freshness indicator -->
    <div v-if="forecasts.length > 0 && forecasts[0]?.fetchedAt" class="freshness-bar">
      <FreshnessBadge :fetched-at="forecasts[0].fetchedAt" data-type="forecast" />
      <span class="freshness-source">{{ forecasts[0].source.toUpperCase() }}</span>
      <span class="freshness-age">{{ forecastAge }}</span>
    </div>

    <SolarPanel v-if="selectedLocationId" :location-id="Number(selectedLocationId)" />
    <ClimateNormalsCard
      v-if="selectedLocationId"
      :location-id="Number(selectedLocationId)"
      :current-temp="forecasts.length > 0 ? forecasts[0].temperatureFahrenheit : null"
    />

    <ForecastSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <!-- Weather icon legend (collapsible) -->
    <div v-if="forecasts.length > 0" class="icon-legend-wrapper">
      <button class="btn-text icon-legend-toggle" @click="showIconLegend = !showIconLegend">
        {{ showIconLegend ? 'Hide' : 'Show' }} Icon Legend
      </button>
      <div v-if="showIconLegend" class="icon-legend">
        <span class="legend-item"><span class="legend-icon">&#x2600;&#xFE0F;</span> Clear</span>
        <span class="legend-item"><span class="legend-icon">&#x26C5;</span> Partly Cloudy</span>
        <span class="legend-item"><span class="legend-icon">&#x2601;&#xFE0F;</span> Cloudy</span>
        <span class="legend-item"><span class="legend-icon">&#x1F326;&#xFE0F;</span> Drizzle</span>
        <span class="legend-item"><span class="legend-icon">&#x1F327;&#xFE0F;</span> Rain</span>
        <span class="legend-item"><span class="legend-icon">&#x26C8;&#xFE0F;</span> Thunderstorm</span>
        <span class="legend-item"><span class="legend-icon">&#x1F328;&#xFE0F;</span> Snow</span>
        <span class="legend-item"><span class="legend-icon">&#x1F32B;&#xFE0F;</span> Fog/Haze</span>
        <span class="legend-item"><span class="legend-icon">&#x1F4A8;</span> Windy</span>
      </div>
    </div>

    <div id="section-daily">
      <DailyForecastCards v-if="forecasts.length > 0" :forecasts="forecasts" />
    </div>

    <HourlyTimeline v-if="forecasts.length > 0" :forecasts="forecasts" />

    <!-- Forecast comparison: today vs yesterday -->
    <div v-if="yesterdayComparison" id="section-comparison" class="card comparison-card">
      <h2>Today vs Yesterday</h2>
      <div class="comparison-grid">
        <div class="comparison-item">
          <div class="comparison-label">High Temp</div>
          <div class="comparison-values">
            <span>Today: {{ formatTemp(yesterdayComparison.todayHigh) }}</span>
            <span :class="yesterdayComparison.highDiff > 0 ? 'diff-warmer' : 'diff-cooler'">
              {{ yesterdayComparison.highDiff > 0 ? '+' : '' }}{{ yesterdayComparison.highDiff }}&deg;
            </span>
          </div>
        </div>
        <div class="comparison-item">
          <div class="comparison-label">Low Temp</div>
          <div class="comparison-values">
            <span>Today: {{ formatTemp(yesterdayComparison.todayLow) }}</span>
            <span :class="yesterdayComparison.lowDiff > 0 ? 'diff-warmer' : 'diff-cooler'">
              {{ yesterdayComparison.lowDiff > 0 ? '+' : '' }}{{ yesterdayComparison.lowDiff }}&deg;
            </span>
          </div>
        </div>
        <div class="comparison-item">
          <div class="comparison-label">Wind</div>
          <div class="comparison-values">
            <span>Today: {{ formatSpeed(yesterdayComparison.todayWind) }}</span>
            <span :class="yesterdayComparison.windDiff > 0 ? 'diff-windier' : 'diff-calmer'">
              {{ yesterdayComparison.windLabel }}
            </span>
          </div>
        </div>
        <div v-if="yesterdayComparison.todayPrecip != null" class="comparison-item">
          <div class="comparison-label">Precip Chance</div>
          <div class="comparison-values">
            <span>Today: {{ yesterdayComparison.todayPrecip }}%</span>
            <span>Yesterday: {{ yesterdayComparison.yesterdayPrecip ?? 0 }}%</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Multi-location comparison -->
    <div v-if="compareLocations.length > 0 && compareForecasts.size > 0" id="section-multicompare" class="card">
      <h2>Location Comparison</h2>
      <div class="table-wrapper">
        <table class="data-table" aria-label="Location comparison">
          <thead>
            <tr>
              <th>Location</th>
              <th>High</th>
              <th>Low</th>
              <th>Wind</th>
              <th>Precip</th>
              <th>Conditions</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="td-nowrap"><strong>{{ selectedLocation!.name }}</strong></td>
              <td>{{ primaryTodaySummary ? formatTemp(primaryTodaySummary.high) : '-' }}</td>
              <td>{{ primaryTodaySummary ? formatTemp(primaryTodaySummary.low) : '-' }}</td>
              <td>{{ primaryTodaySummary ? formatSpeed(primaryTodaySummary.wind) : '-' }}</td>
              <td>{{ primaryTodaySummary?.precip != null ? primaryTodaySummary.precip + '%' : '-' }}</td>
              <td class="td-conditions">{{ primaryTodaySummary?.condition || '-' }}</td>
            </tr>
            <tr v-for="cl in compareLocations" :key="cl.id">
              <td class="td-nowrap">{{ cl.name }}</td>
              <template v-if="getCompareSummary(cl.id)">
                <td>{{ formatTemp(getCompareSummary(cl.id)!.high) }}</td>
                <td>{{ formatTemp(getCompareSummary(cl.id)!.low) }}</td>
                <td>{{ formatSpeed(getCompareSummary(cl.id)!.wind) }}</td>
                <td>{{ getCompareSummary(cl.id)!.precip != null ? getCompareSummary(cl.id)!.precip + '%' : '-' }}</td>
                <td class="td-conditions">{{ getCompareSummary(cl.id)!.condition }}</td>
              </template>
              <template v-else>
                <td colspan="5" class="empty-state">Loading...</td>
              </template>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="forecasts.length > 0" id="section-trends" class="card">
      <h2>{{ $t('forecast.forecastTrends') }}</h2>
      <ErrorBoundary>
        <ForecastChart :forecasts="forecasts" />
      </ErrorBoundary>
    </div>

    <div id="section-wind">
      <ErrorBoundary>
        <WindRoseChart v-if="forecasts.length > 0" :forecasts="forecasts" />
      </ErrorBoundary>
    </div>

    <div v-if="forecasts.length > 0" id="section-data" class="card">
      <div class="forecast-data-header">
        <h2>{{ $t('forecast.forecastData') }}</h2>
        <div class="header-actions">
          <span class="table-meta">
            <strong>{{ forecasts.length }}</strong> periods
            <template v-if="forecasts[0]"> &middot; {{ forecasts[0].source.toUpperCase() }}</template>
          </span>
          <span v-if="forecasts[0]?.fetchedAt">
            <FreshnessBadge :fetched-at="forecasts[0].fetchedAt" data-type="forecast" />
          </span>
          <button
            class="btn-sm btn-icon btn-outline"
            :aria-label="$t('forecast.exportAriaLabel')"
            @click="handleExport"
          >
            <span aria-hidden="true">&#x1F4E5;</span> {{ $t('forecast.exportCSV') }}
          </button>
        </div>
      </div>

      <div class="table-wrapper">
        <table class="data-table" aria-label="Forecast data periods">
          <thead>
            <tr>
              <th role="columnheader" tabindex="0" aria-sort="none" @click="toggleForecastSort('validFrom')" @keydown.enter.prevent="toggleForecastSort('validFrom')" @keydown.space.prevent="toggleForecastSort('validFrom')">
                Period
                <span class="sort-indicator" :class="{ active: forecastSortKey === 'validFrom' }">{{ forecastSortIcon('validFrom') }}</span>
              </th>
              <th role="columnheader" tabindex="0" aria-sort="none" @click="toggleForecastSort('temperatureFahrenheit')" @keydown.enter.prevent="toggleForecastSort('temperatureFahrenheit')" @keydown.space.prevent="toggleForecastSort('temperatureFahrenheit')">
                Temp
                <span class="sort-indicator" :class="{ active: forecastSortKey === 'temperatureFahrenheit' }">{{ forecastSortIcon('temperatureFahrenheit') }}</span>
              </th>
              <th role="columnheader" tabindex="0" aria-sort="none" @click="toggleForecastSort('windSpeedMph')" @keydown.enter.prevent="toggleForecastSort('windSpeedMph')" @keydown.space.prevent="toggleForecastSort('windSpeedMph')">
                Wind
                <span class="sort-indicator" :class="{ active: forecastSortKey === 'windSpeedMph' }">{{ forecastSortIcon('windSpeedMph') }}</span>
              </th>
              <th role="columnheader" tabindex="0" aria-sort="none" @click="toggleForecastSort('precipitationProbability')" @keydown.enter.prevent="toggleForecastSort('precipitationProbability')" @keydown.space.prevent="toggleForecastSort('precipitationProbability')">
                Precip
                <span class="sort-indicator" :class="{ active: forecastSortKey === 'precipitationProbability' }">{{ forecastSortIcon('precipitationProbability') }}</span>
              </th>
              <th>Humidity</th>
              <th>Feels Like</th>
              <th>Conditions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="forecast in sortedForecasts" :key="forecast.id">
              <td class="td-nowrap">{{ formatDate(forecast.validFrom) }}</td>
              <td>{{ formatTemp(forecast.temperatureFahrenheit) }}</td>
              <td>
                <span v-if="forecast.windDirection != null" class="wind-arrow-sm" :style="{ transform: 'rotate(' + (forecast.windDirection + 180) + 'deg)' }">&#x2191;</span>
                {{ formatSpeed(forecast.windSpeedMph) }}
                <span v-if="forecast.windDirection != null" class="wind-dir-sm">{{ compassLabel(forecast.windDirection) }}</span>
              </td>
              <td>{{ forecast.precipitationProbability != null ? forecast.precipitationProbability + '%' : '-' }}</td>
              <td>{{ forecast.humidity != null ? forecast.humidity + '%' : '-' }}</td>
              <td>{{ getFeelsLikeLabel(forecast) }}</td>
              <td class="td-conditions">{{ forecast.weatherShortDescription || forecast.weatherDescription || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="!loading && selectedLocationId && forecasts.length === 0" class="card">
      <p>{{ $t('forecast.noData') }}</p>
      <p>{{ $t('forecast.schedulerNote') }}</p>
    </div>

    <div id="section-history">
      <HistoricalChart
        v-if="selectedLocationId"
        :location-id="Number(selectedLocationId)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import { useUnitPreferences } from '../composables/useUnitPreferences'
import { type Location, type WeatherForecast } from '../services/weatherService'
import weatherService from '../services/weatherService'
import { formatDate, formatRelativeTime } from '../utils/dateUtils'
import { computeFeelsLike, degreesToCompass } from '../utils/weatherCalc'
import FreshnessBadge from '../components/FreshnessBadge.vue'
import ForecastChart from '../components/ForecastChart.vue'
import AlertsBanner from '../components/AlertsBanner.vue'
import ForecastSkeleton from '../components/skeletons/ForecastSkeleton.vue'
import FavoriteButton from '../components/FavoriteButton.vue'
import DailyForecastCards from '../components/DailyForecastCards.vue'
import HourlyTimeline from '../components/HourlyTimeline.vue'
import HistoricalChart from '../components/HistoricalChart.vue'
import SolarPanel from '../components/SolarPanel.vue'
import ClimateNormalsCard from '../components/ClimateNormalsCard.vue'
import WindRoseChart from '../components/WindRoseChart.vue'
import ErrorBoundary from '../components/ErrorBoundary.vue'
import { useFavorites } from '../composables/useFavorites'
import { exportForecastsToCSV } from '../utils/exportUtils'

const route = useRoute()
const store = useWeatherStore()
const toast = useToast()
const { formatTemp, formatSpeed } = useUnitPreferences()
const {
  airports,
  forecasts,
  forecastsLoading: loading,
  forecastsError: error,
  historicalForecasts,
} = storeToRefs(store)

const { isFavorite, addFavorite, removeFavorite } = useFavorites()

const selectedLocationId = ref<string>('')
const selectedLocation = ref<Location | null>(null)
const searchQuery = ref('')
const searchResults = ref<Location[]>([])
const showResults = ref(false)
const highlightedIndex = ref(-1)
const showIconLegend = ref(false)

// Compare state
const compareQuery = ref('')
const compareResults = ref<Location[]>([])
const showCompareResults = ref(false)
const compareHighlightedIndex = ref(-1)
const compareLocations = ref<Location[]>([])
const compareForecasts = ref<Map<number, WeatherForecast[]>>(new Map())

const activeDescendant = computed(() => {
  if (highlightedIndex.value >= 0 && highlightedIndex.value < searchResults.value.length) {
    return 'search-option-' + searchResults.value[highlightedIndex.value].id
  }
  return undefined
})

// Forecast freshness age
const forecastAge = computed(() => {
  if (!forecasts.value.length || !forecasts.value[0]?.fetchedAt) return ''
  return formatRelativeTime(forecasts.value[0].fetchedAt)
})

function onSearchInput() {
  highlightedIndex.value = -1
  if (!searchQuery.value || searchQuery.value.length < 2) {
    searchResults.value = []
    return
  }

  const query = searchQuery.value.toLowerCase()
  searchResults.value = airports.value
    .filter(
      (loc) => loc.name?.toLowerCase().includes(query) || loc.state?.toLowerCase().includes(query),
    )
    .slice(0, 10)
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
        selectLocation(searchResults.value[highlightedIndex.value])
      }
      break
    case 'Escape':
      event.preventDefault()
      showResults.value = false
      highlightedIndex.value = -1
      break
  }
}

function selectLocation(loc: Location) {
  selectedLocationId.value = String(loc.id)
  selectedLocation.value = loc
  searchQuery.value = `${loc.name}, ${loc.state || loc.country || ''}`
  searchResults.value = []
  showResults.value = false
  compareLocations.value = []
  compareForecasts.value = new Map()
  store.fetchForecasts(Number(selectedLocationId.value))
  store.fetchHistoricalForecasts(Number(selectedLocationId.value), 2)
}

// --- Compare search ---
function onCompareInput() {
  compareHighlightedIndex.value = -1
  if (!compareQuery.value || compareQuery.value.length < 2) {
    compareResults.value = []
    return
  }
  const query = compareQuery.value.toLowerCase()
  const excludeIds = new Set([Number(selectedLocationId.value), ...compareLocations.value.map((l) => l.id)])
  compareResults.value = airports.value
    .filter((loc) => !excludeIds.has(loc.id) && (loc.name?.toLowerCase().includes(query) || loc.state?.toLowerCase().includes(query)))
    .slice(0, 10)
  showCompareResults.value = true
}

function onCompareKeydown(event: KeyboardEvent) {
  if (!showCompareResults.value || compareResults.value.length === 0) return
  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      compareHighlightedIndex.value = Math.min(compareHighlightedIndex.value + 1, compareResults.value.length - 1)
      break
    case 'ArrowUp':
      event.preventDefault()
      compareHighlightedIndex.value = Math.max(compareHighlightedIndex.value - 1, 0)
      break
    case 'Enter':
      event.preventDefault()
      if (compareHighlightedIndex.value >= 0) addCompareLocation(compareResults.value[compareHighlightedIndex.value])
      break
    case 'Escape':
      event.preventDefault()
      showCompareResults.value = false
      break
  }
}

async function addCompareLocation(loc: Location) {
  if (compareLocations.value.length >= 3) return
  compareLocations.value.push(loc)
  compareQuery.value = ''
  compareResults.value = []
  showCompareResults.value = false
  try {
    const data = await weatherService.getForecastsByLocation(loc.id)
    compareForecasts.value = new Map(compareForecasts.value).set(loc.id, data)
  } catch {
    // silently fail — row will show "Loading..."
  }
}

function removeCompareLocation(id: number) {
  compareLocations.value = compareLocations.value.filter((l) => l.id !== id)
  const updated = new Map(compareForecasts.value)
  updated.delete(id)
  compareForecasts.value = updated
}

interface DaySummary {
  high: number
  low: number
  wind: number
  precip: number | null
  condition: string
}

function getTodaySummary(forecastList: WeatherForecast[]): DaySummary | null {
  const today = new Date().toISOString().substring(0, 10)
  const todayItems = forecastList.filter((f) => f.validFrom.substring(0, 10) === today)
  if (todayItems.length === 0) return null
  let high = -Infinity, low = Infinity, totalWind = 0, maxPrecip: number | null = null
  let condition = ''
  for (const f of todayItems) {
    high = Math.max(high, f.temperatureFahrenheit)
    low = Math.min(low, f.temperatureFahrenheit)
    totalWind += f.windSpeedMph
    if (f.precipitationProbability != null) maxPrecip = Math.max(maxPrecip ?? 0, f.precipitationProbability)
    if (!condition) condition = f.weatherShortDescription || f.weatherDescription || ''
  }
  return { high, low, wind: Math.round(totalWind / todayItems.length), precip: maxPrecip, condition }
}

const primaryTodaySummary = computed(() => getTodaySummary(forecasts.value))

function getCompareSummary(locId: number): DaySummary | null {
  const data = compareForecasts.value.get(locId)
  if (!data) return null
  return getTodaySummary(data)
}

// --- Today vs yesterday comparison ---
const yesterdayComparison = computed(() => {
  if (forecasts.value.length === 0 || historicalForecasts.value.length === 0) return null

  const today = new Date().toISOString().substring(0, 10)
  const yesterday = new Date(Date.now() - 86400000).toISOString().substring(0, 10)

  const todayItems = forecasts.value.filter((f) => f.validFrom.substring(0, 10) === today)
  const yesterdayItems = historicalForecasts.value.filter((f) => f.validFrom.substring(0, 10) === yesterday)

  if (todayItems.length === 0 || yesterdayItems.length === 0) return null

  const todayHigh = Math.max(...todayItems.map((f) => f.temperatureFahrenheit))
  const todayLow = Math.min(...todayItems.map((f) => f.temperatureFahrenheit))
  const todayWind = Math.round(todayItems.reduce((s, f) => s + f.windSpeedMph, 0) / todayItems.length)
  const todayPrecip = todayItems.some((f) => f.precipitationProbability != null)
    ? Math.max(...todayItems.map((f) => f.precipitationProbability ?? 0))
    : null

  const yesterdayHigh = Math.max(...yesterdayItems.map((f) => f.temperatureFahrenheit))
  const yesterdayLow = Math.min(...yesterdayItems.map((f) => f.temperatureFahrenheit))
  const yesterdayWind = Math.round(yesterdayItems.reduce((s, f) => s + f.windSpeedMph, 0) / yesterdayItems.length)
  const yesterdayPrecip = yesterdayItems.some((f) => f.precipitationProbability != null)
    ? Math.max(...yesterdayItems.map((f) => f.precipitationProbability ?? 0))
    : null

  const highDiff = Math.round(todayHigh - yesterdayHigh)
  const lowDiff = Math.round(todayLow - yesterdayLow)
  const windDiff = todayWind - yesterdayWind

  return {
    todayHigh, todayLow, todayWind, todayPrecip,
    yesterdayHigh, yesterdayLow, yesterdayWind, yesterdayPrecip,
    highDiff, lowDiff, windDiff,
    windLabel: windDiff > 2 ? `+${windDiff} mph windier` : windDiff < -2 ? `${windDiff} mph calmer` : 'Similar',
  }
})

// --- Sort ---
const forecastSortKey = ref('validFrom')
const forecastSortDir = ref<'asc' | 'desc'>('asc')

function toggleForecastSort(key: string) {
  if (forecastSortKey.value === key) {
    forecastSortDir.value = forecastSortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    forecastSortKey.value = key
    forecastSortDir.value = key === 'validFrom' ? 'asc' : 'desc'
  }
}

function forecastSortIcon(key: string) {
  if (forecastSortKey.value !== key) return '\u21C5'
  return forecastSortDir.value === 'asc' ? '\u2191' : '\u2193'
}

const sortedForecasts = computed(() => {
  const data = [...forecasts.value]
  const dir = forecastSortDir.value === 'asc' ? 1 : -1
  const key = forecastSortKey.value as keyof (typeof data)[0]

  data.sort((a, b) => {
    const av = a[key]
    const bv = b[key]
    if (av == null && bv == null) return 0
    if (av == null) return 1
    if (bv == null) return -1
    if (typeof av === 'string' && typeof bv === 'string') return av.localeCompare(bv) * dir
    return ((av as number) - (bv as number)) * dir
  })

  return data
})

function compassLabel(deg: number): string {
  return degreesToCompass(deg)
}

function getFeelsLikeLabel(f: WeatherForecast): string {
  const fl = computeFeelsLike(f.temperatureFahrenheit, f.windSpeedMph, f.humidity ?? null)
  if (fl == null || Math.abs(fl - f.temperatureFahrenheit) < 3) return '-'
  return formatTemp(fl)
}

function handleExport() {
  if (selectedLocation.value && forecasts.value.length > 0) {
    exportForecastsToCSV(forecasts.value, selectedLocation.value.name)
    toast.success('Forecast data exported successfully')
  }
}

function toggleFavorite(loc: Location) {
  if (isFavorite(loc.id)) {
    removeFavorite(loc.id)
  } else {
    addFavorite({ id: loc.id, name: loc.name, state: loc.state })
  }
}

onMounted(async () => {
  await store.fetchAirports()
  const qid = route.query.locationId
  if (qid) {
    const loc = airports.value.find((l) => l.id === Number(qid))
    if (loc) {
      selectLocation(loc)
    }
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
}

.search-result-item {
  padding: 10px 16px;
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

.result-name {
  font-weight: 500;
  font-size: 14px;
}

.result-state {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-top: 2px;
}

/* Compare section */
.compare-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light, #eee);
}

.compare-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary, #666);
  margin-bottom: 6px;
  display: block;
}

.compare-wrapper {
  position: relative;
}

.compare-input {
  font-size: 14px;
  padding: 8px 12px;
}

.compare-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.compare-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: var(--accent);
  color: white;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
}

.compare-remove {
  background: none;
  border: none;
  color: white;
  font-size: 14px;
  cursor: pointer;
  padding: 0 2px;
  line-height: 1;
  opacity: 0.8;
}

.compare-remove:hover {
  opacity: 1;
}

/* Section navigation */
.section-nav {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  padding: 8px 0;
  margin-bottom: 12px;
  position: sticky;
  top: 0;
  z-index: 50;
  background: var(--bg-primary, #f5f5f5);
}

.section-nav-link {
  padding: 5px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  text-decoration: none;
  color: var(--text-secondary, #666);
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-color, #ddd);
  transition: all 0.2s;
}

.section-nav-link:hover {
  color: var(--accent);
  border-color: var(--accent);
}

/* Freshness bar */
.freshness-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-color, #ddd);
  border-radius: 8px;
  margin-bottom: 12px;
  font-size: 12px;
}

.freshness-source {
  font-weight: 600;
  color: var(--text-primary, #333);
}

.freshness-age {
  color: var(--text-muted, #999);
}

/* Icon legend */
.icon-legend-wrapper {
  margin-bottom: 12px;
}

.icon-legend-toggle {
  font-size: 12px;
  color: var(--text-secondary, #666);
  cursor: pointer;
  background: none;
  border: none;
  padding: 4px 0;
  text-decoration: underline;
}

.icon-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 10px 14px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-color, #ddd);
  border-radius: 8px;
  margin-top: 6px;
}

.icon-legend .legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.icon-legend .legend-icon {
  font-size: 16px;
}

/* Comparison card */
.comparison-card {
  border-left: 4px solid var(--accent);
}

.comparison-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.comparison-item {
  padding: 8px 12px;
  background: var(--bg-secondary, #f9f9f9);
  border-radius: 8px;
}

.comparison-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  color: var(--text-muted, #999);
  margin-bottom: 4px;
}

.comparison-values {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.diff-warmer {
  color: #e53935;
  font-weight: 600;
}

.diff-cooler {
  color: #1e88e5;
  font-weight: 600;
}

.diff-windier {
  color: #ff9800;
  font-weight: 600;
}

.diff-calmer {
  color: #4caf50;
  font-weight: 600;
}

/* Forecast data table enhancements */
.forecast-data-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.forecast-data-header h2 {
  margin-bottom: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.td-conditions {
  max-width: 240px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.td-nowrap {
  white-space: nowrap;
}

.wind-arrow-sm {
  display: inline-block;
  font-size: 11px;
  line-height: 1;
}

.wind-dir-sm {
  font-size: 10px;
  color: var(--text-muted, #999);
  margin-left: 2px;
}

.selected-location-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.selected-location-header h2 {
  margin-bottom: 0;
}
</style>
