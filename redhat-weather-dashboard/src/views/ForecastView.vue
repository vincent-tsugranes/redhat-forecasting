<template>
  <div class="container">
    <h1>Weather Forecasts</h1>

    <AlertsBanner />

    <div class="card search-card">
      <h2>Search Location</h2>
      <div class="search-wrapper">
        <label for="location-search" class="sr-only">Search locations by name or state</label>
        <input
          id="location-search"
          v-model="searchQuery"
          type="text"
          placeholder="Search locations by name or state..."
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
        <ul v-if="searchResults.length > 0 && showResults && searchQuery" id="search-listbox" class="search-results" role="listbox">
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
    </div>

    <div v-if="loading" class="loading">Loading forecasts...</div>
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="forecasts.length > 0" class="card">
      <h2>Forecast Trends</h2>
      <ForecastChart :forecasts="forecasts" />
    </div>

    <div v-if="forecasts.length > 0" class="card">
      <h2>Forecast Data</h2>
      <p><strong>{{ forecasts.length }}</strong> forecast periods available</p>
      <p v-if="forecasts[0]">Source: <strong>{{ forecasts[0].source.toUpperCase() }}</strong></p>
      <p v-if="forecasts[0]?.fetchedAt">
        Data fetched: <FreshnessBadge :fetched-at="forecasts[0].fetchedAt" data-type="forecast" />
      </p>

      <div v-for="forecast in forecasts.slice(0, 10)" :key="forecast.id" class="forecast-item">
        <div class="forecast-header">
          <strong>{{ formatDate(forecast.validFrom) }}</strong> - {{ formatDate(forecast.validTo) }}
        </div>
        <div class="forecast-details">
          <div><span aria-hidden="true">🌡️</span> {{ forecast.temperatureFahrenheit }}°F ({{ forecast.temperatureCelsius }}°C)</div>
          <div><span aria-hidden="true">💨</span> Wind: {{ forecast.windSpeedMph }} mph</div>
          <div v-if="forecast.precipitationProbability != null"><span aria-hidden="true">☔</span> Precip: {{ forecast.precipitationProbability }}%</div>
          <div v-if="forecast.humidity != null"><span aria-hidden="true">💧</span> Humidity: {{ forecast.humidity }}%</div>
        </div>
        <div class="forecast-description">
          {{ forecast.weatherDescription || forecast.weatherShortDescription }}
        </div>
      </div>
    </div>

    <div v-else-if="!loading && selectedLocationId" class="card">
      <p>No forecast data available for this location yet.</p>
      <p>The scheduler will fetch data automatically within 30 minutes.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import weatherService, { type Location, type WeatherForecast } from '../services/weatherService'
import { formatDate } from '../utils/dateUtils'
import FreshnessBadge from '../components/FreshnessBadge.vue'
import ForecastChart from '../components/ForecastChart.vue'
import AlertsBanner from '../components/AlertsBanner.vue'

const locations = ref<Location[]>([])
const forecasts = ref<WeatherForecast[]>([])
const selectedLocationId = ref<string>('')
const searchQuery = ref('')
const searchResults = ref<Location[]>([])
const showResults = ref(false)
const highlightedIndex = ref(-1)
const loading = ref(false)
const error = ref<string | null>(null)

const activeDescendant = computed(() => {
  if (highlightedIndex.value >= 0 && highlightedIndex.value < searchResults.value.length) {
    return 'search-option-' + searchResults.value[highlightedIndex.value].id
  }
  return undefined
})

async function loadLocations() {
  try {
    locations.value = await weatherService.getLocations()
  } catch (err: any) {
    error.value = err.message || 'Failed to load locations'
  }
}

function onSearchInput() {
  highlightedIndex.value = -1
  if (!searchQuery.value || searchQuery.value.length < 2) {
    searchResults.value = []
    return
  }

  const query = searchQuery.value.toLowerCase()
  searchResults.value = locations.value
    .filter(
      (loc) =>
        loc.name?.toLowerCase().includes(query) ||
        loc.state?.toLowerCase().includes(query)
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
  searchQuery.value = `${loc.name}, ${loc.state || loc.country || ''}`
  searchResults.value = []
  showResults.value = false
  loadForecasts()
}

async function loadForecasts() {
  if (!selectedLocationId.value) return

  loading.value = true
  error.value = null

  try {
    forecasts.value = await weatherService.getForecastsByLocation(Number(selectedLocationId.value))
  } catch (err: any) {
    error.value = err.message || 'Failed to load forecasts'
    console.error('Error loading forecasts:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadLocations()
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
  border-color: #ee0000;
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

.forecast-item {
  border: 1px solid var(--border-light, #eee);
  border-radius: 8px;
  padding: 15px;
  margin: 10px 0;
  background: var(--bg-code, #f9f9f9);
}

.forecast-header {
  font-size: 1.1rem;
  color: #ee0000;
  margin-bottom: 10px;
}

.forecast-details {
  display: flex;
  gap: 20px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.forecast-details div {
  color: var(--text-secondary, #666);
}

.forecast-description {
  margin-top: 10px;
  color: var(--text-primary, #333);
  font-style: italic;
}
</style>
