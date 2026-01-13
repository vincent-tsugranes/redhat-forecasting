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
        <div class="report-raw">{{ metar.rawText }}</div>
        <div class="report-details">
          <div v-if="metar.flightCategory" class="flight-category" :class="'category-' + metar.flightCategory">
            {{ metar.flightCategory }}
          </div>
          <div v-if="metar.temperatureCelsius">🌡️ {{ Math.round(metar.temperatureCelsius) }}°C</div>
          <div v-if="metar.windSpeedKnots">💨 {{ metar.windSpeedKnots }} knots</div>
          <div v-if="metar.visibilityMiles">👁️ {{ metar.visibilityMiles }} mi</div>
          <div v-if="metar.ceilingFeet">☁️ {{ metar.ceilingFeet }} ft</div>
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
import { ref, onMounted } from 'vue'
import weatherService, { type Location, type AirportWeather } from '../services/weatherService'

const airports = ref<Location[]>([])
const selectedAirportCode = ref<string>('')
const metar = ref<AirportWeather | null>(null)
const taf = ref<AirportWeather | null>(null)
const loading = ref(false)
const refreshing = ref(false)
const error = ref<string | null>(null)

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
    setTimeout(() => {
      loadAirportWeather()
      refreshing.value = false
    }, 2000)
  } catch (err: any) {
    error.value = err.message || 'Failed to refresh airport weather'
    refreshing.value = false
  }
}

function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadAirports()
})
</script>

<style scoped>
.weather-report {
  background: #f9f9f9;
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
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 10px;
  font-family: monospace;
  margin: 10px 0;
  white-space: pre-wrap;
  word-break: break-all;
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
