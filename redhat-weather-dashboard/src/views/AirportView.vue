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
        <div class="report-raw">{{ taf.rawText }}</div>
      </div>
    </div>

    <div v-if="!loading && !metar && selectedAirportCode" class="card">
      <p>{{ $t('airport.noData') }}</p>
      <p>{{ $t('airport.noDataHint') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import weatherService, { type AirportWeather, type Location } from '../services/weatherService'
import { formatDate } from '../utils/dateUtils'
import FreshnessBadge from '../components/FreshnessBadge.vue'
import AirportSkeleton from '../components/skeletons/AirportSkeleton.vue'

const store = useWeatherStore()
const { airports } = storeToRefs(store)

const selectedAirportCode = ref<string>('')
const metar = ref<AirportWeather | null>(null)
const taf = ref<AirportWeather | null>(null)
const loading = ref(false)
const refreshing = ref(false)
const error = ref<string | null>(null)
let refreshTimeout: ReturnType<typeof setTimeout> | null = null

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

  try {
    const [metarData, tafData] = await Promise.allSettled([
      weatherService.getLatestMetar(selectedAirportCode.value),
      weatherService.getLatestTaf(selectedAirportCode.value),
    ])

    if (metarData.status === 'fulfilled') {
      metar.value = metarData.value
    }

    if (tafData.status === 'fulfilled') {
      taf.value = tafData.value
    }

    if (metarData.status === 'rejected' && tafData.status === 'rejected') {
      error.value = 'No weather data available for this airport'
    }
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Failed to load airport weather'
  } finally {
    loading.value = false
  }
}

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

onMounted(() => {
  store.fetchAirports()
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
  padding: 15px;
}

.report-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 1.1rem;
}

.report-raw {
  background: var(--bg-input, #fff);
  border: 1px solid var(--border-color, #ddd);
  border-radius: 4px;
  padding: 10px;
  font-family: monospace;
  margin: 10px 0;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--text-primary, #333);
}

.report-details {
  display: flex;
  gap: 20px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.flight-category {
  padding: 5px 10px;
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
</style>
