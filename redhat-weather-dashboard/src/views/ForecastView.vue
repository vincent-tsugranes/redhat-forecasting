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
    </div>

    <div v-if="selectedLocation" class="selected-location-header">
      <h2>{{ selectedLocation.name }}, {{ selectedLocation.state || selectedLocation.country }}</h2>
      <FavoriteButton
        :active="isFavorite(selectedLocation.id)"
        @toggle="toggleFavorite(selectedLocation)"
      />
    </div>

    <SolarPanel v-if="selectedLocationId" :location-id="Number(selectedLocationId)" />
    <ClimateNormalsCard
      v-if="selectedLocationId"
      :location-id="Number(selectedLocationId)"
      :current-temp="forecasts.length > 0 ? forecasts[0].temperatureFahrenheit : null"
    />

    <ForecastSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <DailyForecastCards v-if="forecasts.length > 0" :forecasts="forecasts" />
    <HourlyTimeline v-if="forecasts.length > 0" :forecasts="forecasts" />

    <div v-if="forecasts.length > 0" class="card">
      <h2>{{ $t('forecast.forecastTrends') }}</h2>
      <ErrorBoundary>
        <ForecastChart :forecasts="forecasts" />
      </ErrorBoundary>
    </div>

    <ErrorBoundary>
      <WindRoseChart v-if="forecasts.length > 0" :forecasts="forecasts" />
    </ErrorBoundary>

    <div v-if="forecasts.length > 0" class="card">
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
            <span aria-hidden="true">📥</span> {{ $t('forecast.exportCSV') }}
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
              <th>Conditions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="forecast in sortedForecasts" :key="forecast.id">
              <td class="td-nowrap">{{ formatDate(forecast.validFrom) }}</td>
              <td>{{ formatTemp(forecast.temperatureFahrenheit) }}</td>
              <td>{{ formatSpeed(forecast.windSpeedMph) }}</td>
              <td>{{ forecast.precipitationProbability != null ? forecast.precipitationProbability + '%' : '-' }}</td>
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

    <HistoricalChart
      v-if="selectedLocationId"
      :location-id="Number(selectedLocationId)"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import { useUnitPreferences } from '../composables/useUnitPreferences'
import { type Location } from '../services/weatherService'
import { formatDate } from '../utils/dateUtils'
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
  locations,
  forecasts,
  forecastsLoading: loading,
  forecastsError: error,
} = storeToRefs(store)

const { isFavorite, addFavorite, removeFavorite } = useFavorites()

const selectedLocationId = ref<string>('')
const selectedLocation = ref<Location | null>(null)
const searchQuery = ref('')
const searchResults = ref<Location[]>([])
const showResults = ref(false)
const highlightedIndex = ref(-1)

const activeDescendant = computed(() => {
  if (highlightedIndex.value >= 0 && highlightedIndex.value < searchResults.value.length) {
    return 'search-option-' + searchResults.value[highlightedIndex.value].id
  }
  return undefined
})

function onSearchInput() {
  highlightedIndex.value = -1
  if (!searchQuery.value || searchQuery.value.length < 2) {
    searchResults.value = []
    return
  }

  const query = searchQuery.value.toLowerCase()
  searchResults.value = locations.value
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
  store.fetchForecasts(Number(selectedLocationId.value))
}

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
  if (forecastSortKey.value !== key) return '⇅'
  return forecastSortDir.value === 'asc' ? '↑' : '↓'
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
  await store.fetchLocations()
  const qid = route.query.locationId
  if (qid) {
    const loc = locations.value.find((l) => l.id === Number(qid))
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
