<template>
  <div class="airport-map-container">
    <div class="search-bar">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="🔍 Search airports by name, code, or location..."
        class="search-input"
        @input="onSearchInput"
      />
      <div v-if="searchResults.length > 0 && searchQuery" class="search-results">
        <div
          v-for="airport in searchResults"
          :key="airport.id"
          class="search-result-item"
          @click="selectAirport(airport)"
        >
          <div class="airport-code">{{ airport.airportCode }}</div>
          <div class="airport-info">
            <div class="airport-name">{{ airport.name }}</div>
            <div class="airport-location">{{ airport.state }}, {{ airport.country }}</div>
          </div>
        </div>
      </div>
    </div>
    <div ref="mapContainer" class="map-container"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import 'leaflet.markercluster/dist/MarkerCluster.css'
import 'leaflet.markercluster/dist/MarkerCluster.Default.css'
import 'leaflet.markercluster'
import weatherService, { type Location } from '../services/weatherService'

const mapContainer = ref<HTMLElement | null>(null)
const searchQuery = ref('')
const searchResults = ref<Location[]>([])
const airports = ref<Location[]>([])
const map = ref<L.Map | null>(null)
const markerClusterGroup = ref<L.MarkerClusterGroup | null>(null)

async function loadAirports() {
  try {
    airports.value = await weatherService.getAirports()
    if (map.value && airports.value.length > 0) {
      initializeMarkers()
    }
  } catch (error) {
    console.error('Error loading airports:', error)
  }
}

function initializeMap() {
  if (!mapContainer.value) return

  map.value = L.map(mapContainer.value).setView([20, 0], 2)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors',
    maxZoom: 18,
  }).addTo(map.value)

  // Create marker cluster group
  markerClusterGroup.value = L.markerClusterGroup({
    chunkedLoading: true,
    maxClusterRadius: 80,
    spiderfyOnMaxZoom: true,
    showCoverageOnHover: false,
    zoomToBoundsOnClick: true,
  })

  map.value.addLayer(markerClusterGroup.value)
}

async function fetchWeatherForAirport(airportCode: string) {
  try {
    // Fetch latest METAR (current airport weather conditions)
    const metar = await weatherService.getLatestMetar(airportCode)
    return metar
  } catch (error) {
    console.error('Error fetching airport weather:', error)
    return null
  }
}

function createPopupContent(airport: Location, lat: number, lon: number) {
  return `
    <div class="airport-popup">
      <div class="popup-header">
        <strong>${airport.airportCode}</strong>
      </div>
      <div class="popup-name">${airport.name}</div>
      <div class="popup-location">${airport.state || ''}, ${airport.country || ''}</div>
      <div class="popup-coords">${lat.toFixed(4)}, ${lon.toFixed(4)}</div>
      <div class="weather-container">
        <div class="weather-loading">⏳ Loading weather...</div>
      </div>
    </div>
  `
}

function updatePopupWithWeather(popup: L.Popup, weather: any, airport: Location) {
  const popupElement = popup.getElement()
  if (!popupElement) return

  const weatherContainer = popupElement.querySelector('.weather-container')
  if (!weatherContainer) return

  if (weather) {
    // Convert Celsius to Fahrenheit
    const tempC = weather.temperatureCelsius
    const tempF = tempC !== null && tempC !== undefined ? Math.round((tempC * 9/5) + 32) : null
    const windKnots = weather.windSpeedKnots
    const windMph = windKnots ? Math.round(windKnots * 1.151) : null
    const visibility = weather.visibilityMiles
    const flightCategory = weather.flightCategory
    const observationTime = weather.observationTime ? new Date(weather.observationTime).toLocaleString() : null

    // Flight category colors
    const categoryColors: Record<string, string> = {
      'VFR': '#4caf50',
      'MVFR': '#2196f3',
      'IFR': '#ff9800',
      'LIFR': '#f44336'
    }

    weatherContainer.innerHTML = `
      <div class="weather-divider"></div>
      <div class="weather-title">✈️ METAR Conditions</div>
      <div class="weather-info">
        ${tempC !== null && tempC !== undefined ? `
          <div class="weather-item">
            <span class="weather-label">Temperature:</span>
            <span class="weather-value">${tempF}°F / ${tempC}°C</span>
          </div>
        ` : ''}
        ${windMph ? `
          <div class="weather-item">
            <span class="weather-label">Wind:</span>
            <span class="weather-value">${windMph} mph (${windKnots} kts)</span>
          </div>
        ` : ''}
        ${visibility !== null && visibility !== undefined ? `
          <div class="weather-item">
            <span class="weather-label">Visibility:</span>
            <span class="weather-value">${visibility} mi</span>
          </div>
        ` : ''}
        ${flightCategory ? `
          <div class="weather-item">
            <span class="weather-label">Flight Category:</span>
            <span class="weather-value" style="color: ${categoryColors[flightCategory] || '#666'}; font-weight: bold;">
              ${flightCategory}
            </span>
          </div>
        ` : ''}
        ${observationTime ? `
          <div class="weather-time">Updated: ${observationTime}</div>
        ` : ''}
      </div>
    `
  } else {
    // Show helpful airport info instead when weather unavailable
    weatherContainer.innerHTML = `
      <div class="weather-divider"></div>
      <div class="weather-info-alt">
        <div class="info-note">
          <span class="info-icon">📡</span>
          <span>Weather data updates every 15 minutes</span>
        </div>
        <div class="weather-hint">Click refresh or check back soon for live METAR conditions</div>
      </div>
    `
  }
}

function initializeMarkers() {
  if (!markerClusterGroup.value) return

  markerClusterGroup.value.clearLayers()

  const markers: L.Marker[] = []

  airports.value.forEach((airport) => {
    const lat = Number(airport.latitude)
    const lon = Number(airport.longitude)

    if (!isNaN(lat) && !isNaN(lon)) {
      const marker = L.marker([lat, lon], {
        icon: L.divIcon({
          className: 'airport-marker',
          html: '<div class="marker-icon">✈️</div>',
          iconSize: [20, 20],
        }),
      })

      const popupContent = createPopupContent(airport, lat, lon)
      const popup = L.popup().setContent(popupContent)
      marker.bindPopup(popup)

      // Fetch weather when popup opens
      marker.on('popupopen', async () => {
        if (airport.airportCode) {
          const weather = await fetchWeatherForAirport(airport.airportCode)
          updatePopupWithWeather(popup, weather, airport)
        }
      })

      markers.push(marker)
    }
  })

  markerClusterGroup.value.addLayers(markers)
}

function onSearchInput() {
  if (!searchQuery.value || searchQuery.value.length < 2) {
    searchResults.value = []
    return
  }

  const query = searchQuery.value.toLowerCase()
  searchResults.value = airports.value
    .filter(
      (airport) =>
        airport.name?.toLowerCase().includes(query) ||
        airport.airportCode?.toLowerCase().includes(query) ||
        airport.state?.toLowerCase().includes(query) ||
        airport.country?.toLowerCase().includes(query)
    )
    .slice(0, 10) // Limit to 10 results
}

function selectAirport(airport: Location) {
  if (!map.value) return

  const lat = Number(airport.latitude)
  const lon = Number(airport.longitude)

  if (!isNaN(lat) && !isNaN(lon)) {
    map.value.setView([lat, lon], 12)
    searchQuery.value = `${airport.airportCode} - ${airport.name}`
    searchResults.value = []

    // Find and open the popup for this airport
    if (markerClusterGroup.value) {
      markerClusterGroup.value.eachLayer((layer) => {
        if (layer instanceof L.Marker) {
          const markerLatLng = layer.getLatLng()
          if (Math.abs(markerLatLng.lat - lat) < 0.0001 && Math.abs(markerLatLng.lng - lon) < 0.0001) {
            layer.openPopup()
          }
        }
      })
    }
  }
}

onMounted(async () => {
  initializeMap()
  await loadAirports()
})
</script>

<style scoped>
.airport-map-container {
  position: relative;
  width: 100%;
  height: 600px;
}

.search-bar {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  width: 90%;
  max-width: 600px;
}

.search-input {
  width: 100%;
  padding: 12px 20px;
  font-size: 16px;
  border: 2px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  outline: none;
  transition: border-color 0.3s;
  background: white;
}

.search-input:focus {
  border-color: #ee0000;
}

.search-results {
  margin-top: 8px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  max-height: 400px;
  overflow-y: auto;
}

.search-result-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #eee;
  transition: background-color 0.2s;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item:hover {
  background-color: #f5f5f5;
}

.airport-code {
  font-weight: bold;
  color: #ee0000;
  min-width: 50px;
  font-size: 14px;
}

.airport-info {
  flex: 1;
}

.airport-name {
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 2px;
}

.airport-location {
  font-size: 12px;
  color: #666;
}

.map-container {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.airport-marker) {
  background: none;
  border: none;
}

:deep(.marker-icon) {
  font-size: 16px;
  text-align: center;
  line-height: 20px;
}

:deep(.airport-popup) {
  padding: 4px;
}

:deep(.popup-header) {
  font-size: 16px;
  color: #ee0000;
  margin-bottom: 4px;
}

:deep(.popup-name) {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}

:deep(.popup-location) {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

:deep(.popup-coords) {
  font-size: 11px;
  color: #999;
  font-family: monospace;
}

:deep(.weather-container) {
  margin-top: 8px;
}

:deep(.weather-divider) {
  height: 1px;
  background: #e0e0e0;
  margin: 8px 0;
}

:deep(.weather-loading) {
  font-size: 12px;
  color: #666;
  text-align: center;
  padding: 8px 0;
}

:deep(.weather-error) {
  font-size: 12px;
  color: #f44336;
  text-align: center;
  padding: 8px 0;
}

:deep(.weather-title) {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

:deep(.weather-info) {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

:deep(.weather-item) {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  gap: 8px;
}

:deep(.weather-label) {
  color: #666;
  font-weight: 500;
}

:deep(.weather-value) {
  color: #333;
  font-weight: 600;
  text-align: right;
}

:deep(.weather-time) {
  font-size: 11px;
  color: #999;
  margin-top: 8px;
  padding-top: 6px;
  border-top: 1px solid #eee;
  text-align: center;
}

:deep(.weather-info-alt) {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 8px 0;
}

:deep(.info-note) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  background: #f5f5f5;
  border-radius: 6px;
  font-size: 13px;
  color: #666;
}

:deep(.info-icon) {
  font-size: 16px;
}

:deep(.weather-hint) {
  font-size: 12px;
  color: #999;
  text-align: center;
  font-style: italic;
}

:deep(.leaflet-popup-content-wrapper) {
  border-radius: 8px;
  min-width: 260px;
}

:deep(.marker-cluster) {
  background-color: rgba(238, 0, 0, 0.6);
}

:deep(.marker-cluster div) {
  background-color: rgba(238, 0, 0, 0.8);
  color: white;
  font-weight: bold;
}
</style>
