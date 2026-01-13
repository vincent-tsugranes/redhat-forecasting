<template>
  <div class="container">
    <h1>Weather Dashboard</h1>

    <div v-if="loading" class="loading">Loading weather data...</div>
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="!loading" class="grid">
      <div class="card">
        <h2>📍 Locations</h2>
        <p><strong>{{ locations.length }}</strong> locations monitored</p>
        <ul v-if="locations.length > 0" style="list-style: none; padding: 0;">
          <li v-for="loc in locations.slice(0, 5)" :key="loc.id">
            {{ loc.name }}, {{ loc.state }}
          </li>
        </ul>
      </div>

      <div class="card">
        <h2>🌤️ Weather Forecasts</h2>
        <p>Automatic updates every 30 minutes from NOAA</p>
        <button @click="loadData">Refresh Data</button>
      </div>

      <div class="card">
        <h2>✈️ Airport Weather</h2>
        <p>METAR/TAF updates every 15 minutes</p>
        <router-link to="/airports">
          <button>View Airports</button>
        </router-link>
      </div>

      <div class="card">
        <h2>🌀 Hurricane Tracking</h2>
        <p>Active tropical systems monitoring</p>
        <router-link to="/hurricanes">
          <button>View Hurricanes</button>
        </router-link>
      </div>
    </div>

    <div v-if="!loading" class="card" style="margin-top: 20px;">
      <h2>🗺️ Global Airport Map</h2>
      <p style="margin-bottom: 15px; color: #666;">
        Explore {{ locations.length }} airports worldwide. Click clusters to zoom in, or search for a specific airport.
      </p>
      <AirportMap />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import weatherService, { type Location } from '../services/weatherService'
import AirportMap from '../components/AirportMap.vue'

const locations = ref<Location[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

async function loadData() {
  loading.value = true
  error.value = null

  try {
    locations.value = await weatherService.getLocations()
  } catch (err: any) {
    error.value = err.message || 'Failed to load locations'
    console.error('Error loading locations:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
h1 {
  color: #ee0000;
  margin-bottom: 30px;
}

h2 {
  font-size: 1.5rem;
  margin-bottom: 10px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

button {
  margin-top: 10px;
}

ul {
  margin-top: 10px;
}

ul li {
  padding: 5px 0;
  color: #666;
}
</style>
