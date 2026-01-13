<template>
  <div class="container">
    <h1>Weather Forecasts</h1>

    <div class="card">
      <h2>Select Location</h2>
      <select v-model="selectedLocationId" @change="loadForecasts" style="width: 100%; padding: 10px; font-size: 16px;">
        <option value="">-- Select a location --</option>
        <option v-for="loc in locations" :key="loc.id" :value="loc.id">
          {{ loc.name }}, {{ loc.state }}
        </option>
      </select>
    </div>

    <div v-if="loading" class="loading">Loading forecasts...</div>
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="forecasts.length > 0" class="card">
      <h2>Forecast Data</h2>
      <p><strong>{{ forecasts.length }}</strong> forecast periods available</p>
      <p v-if="forecasts[0]">Source: <strong>{{ forecasts[0].source.toUpperCase() }}</strong></p>

      <div v-for="forecast in forecasts.slice(0, 10)" :key="forecast.id" class="forecast-item">
        <div class="forecast-header">
          <strong>{{ formatDate(forecast.validFrom) }}</strong> - {{ formatDate(forecast.validTo) }}
        </div>
        <div class="forecast-details">
          <div>🌡️ {{ forecast.temperatureFahrenheit }}°F ({{ forecast.temperatureCelsius }}°C)</div>
          <div>💨 Wind: {{ forecast.windSpeedMph }} mph</div>
          <div v-if="forecast.precipitationProbability">☔ Precipitation: {{ forecast.precipitationProbability }}%</div>
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
import { ref, onMounted } from 'vue'
import weatherService, { type Location, type WeatherForecast } from '../services/weatherService'

const locations = ref<Location[]>([])
const forecasts = ref<WeatherForecast[]>([])
const selectedLocationId = ref<string>('')
const loading = ref(false)
const error = ref<string | null>(null)

async function loadLocations() {
  try {
    locations.value = await weatherService.getLocations()
  } catch (err: any) {
    error.value = err.message || 'Failed to load locations'
  }
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

function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadLocations()
})
</script>

<style scoped>
.forecast-item {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 15px;
  margin: 10px 0;
  background: #f9f9f9;
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
  color: #666;
}

.forecast-description {
  margin-top: 10px;
  color: #333;
  font-style: italic;
}
</style>
