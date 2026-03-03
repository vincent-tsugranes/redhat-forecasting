<template>
  <div class="container">
    <h1>Airport Weather</h1>

    <div class="card">
      <h2>Select Airport</h2>
      <select v-model="selectedAirportCode" @change="loadAirportWeather" style="width: 100%; padding: 10px; font-size: 16px;">
        <option value="">-- Select an airport --</option>
        <option v-for="airport in airports" :key="airport.id" :value="airport.airportCode">
          {{ airport.airportCode }} - {{ airport.name }}
        </option>
      </select>
      <button v-if="selectedAirportCode" @click="refreshWeather" :disabled="refreshing" style="margin-top: 10px;">
        {{ refreshing ? 'Refreshing...' : 'Refresh Weather' }}
      </button>
    </div>

    <div v-if="loading" class="loading">Loading airport weather...</div>
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="metar" class="card">
      <h2>METAR (Current Observation)</h2>
      <div class="weather-report">
        <div class="report-header">
          <strong>{{ selectedAirportCode }}</strong>
          <span>{{ formatDate(metar.observationTime) }}</span>
        </div>
        <div v-if="metar.fetchedAt" style="margin-bottom: 10px;">
          <FreshnessBadge :fetched-at="metar.fetchedAt" data-type="airport" />
        </div>
        <div class="report-raw">{{ metar.rawText }}</div>
        <div class="report-details">
          <div v-if="metar.flightCategory" class="flight-category" :class="'category-' + metar.flightCategory">
            {{ metar.flightCategory }}
          </div>
          <div v-if="metar.temperatureCelsius != null">🌡️ {{ Math.round(metar.temperatureCelsius) }}°C</div>
          <div v-if="metar.dewpointCelsius != null">💧 Dew {{ Math.round(metar.dewpointCelsius) }}°C</div>
          <div v-if="metar.windSpeedKnots != null">
            💨 {{ metar.windDirection != null ? metar.windDirection + '° at ' : '' }}{{ metar.windSpeedKnots }} kts{{ metar.windGustKnots ? ', gusts ' + metar.windGustKnots + ' kts' : '' }}
          </div>
          <div v-if="metar.visibilityMiles != null">👁️ {{ metar.visibilityMiles }} mi</div>
          <div v-if="metar.ceilingFeet != null">☁️ Ceiling {{ metar.ceilingFeet }} ft</div>
          <div v-if="metar.altimeterInches != null">📊 Altimeter {{ metar.altimeterInches }} inHg</div>
          <div v-if="metar.skyCondition">🌤️ Sky: {{ metar.skyCondition }}</div>
          <div v-if="metar.weatherConditions">🌧️ {{ metar.weatherConditions }}</div>
        </div>
      </div>
    </div>

    <div v-if="taf" class="card">
      <h2>TAF (Terminal Aerodrome Forecast)</h2>
      <div class="weather-report">
        <div class="report-header">
          <strong>{{ selectedAirportCode }}</strong>
          <span>{{ formatDate(taf.observationTime) }}</span>
        </div>
        <div v-if="taf.fetchedAt" style="margin-bottom: 10px;">
          <FreshnessBadge :fetched-at="taf.fetchedAt" data-type="airport" />
        </div>
        <div class="report-raw">{{ taf.rawText }}</div>
      </div>
    </div>

    <div v-if="!loading && !metar && selectedAirportCode" class="card">
      <p>No weather data available for this airport yet.</p>
      <p>Click "Refresh Weather" to fetch the latest data.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import weatherService, { type Location, type AirportWeather } from '../services/weatherService'
import { formatDate } from '../utils/dateUtils'
import FreshnessBadge from '../components/FreshnessBadge.vue'

const airports = ref<Location[]>([])
const selectedAirportCode = ref<string>('')
const metar = ref<AirportWeather | null>(null)
const taf = ref<AirportWeather | null>(null)
const loading = ref(false)
const refreshing = ref(false)
const error = ref<string | null>(null)
let refreshTimeout: ReturnType<typeof setTimeout> | null = null

async function loadAirports() {
  try {
    airports.value = await weatherService.getAirports()
  } catch (err: any) {
    error.value = err.message || 'Failed to load airports'
  }
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
      weatherService.getLatestTaf(selectedAirportCode.value)
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
  } catch (err: any) {
    error.value = err.message || 'Failed to load airport weather'
    console.error('Error loading airport weather:', err)
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
    // Wait a moment for the data to be fetched
    refreshTimeout = setTimeout(() => {
      loadAirportWeather().catch((err) => {
        console.error('Error reloading airport weather:', err)
      })
      refreshing.value = false
    }, 2000)
  } catch (err: any) {
    error.value = err.message || 'Failed to refresh airport weather'
    refreshing.value = false
  }
}

onMounted(() => {
  loadAirports()
})

onUnmounted(() => {
  if (refreshTimeout) {
    clearTimeout(refreshTimeout)
    refreshTimeout = null
  }
})
</script>

<style scoped>
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
  background: #28a745;
}

.category-MVFR {
  background: #007bff;
}

.category-IFR {
  background: #ffc107;
  color: #333;
}

.category-LIFR {
  background: #dc3545;
}
</style>
