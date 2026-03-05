<template>
  <div class="unified-map-wrapper">
    <div class="layer-controls">
      <label class="layer-toggle">
        <input v-model="showAirports" type="checkbox" />
        <span aria-hidden="true">✈️</span> {{ $t('map.layerAirports') }}
      </label>
      <label class="layer-toggle">
        <input v-model="showEarthquakes" type="checkbox" />
        <span aria-hidden="true">🌍</span> {{ $t('map.layerEarthquakes') }}
      </label>
      <label class="layer-toggle">
        <input v-model="showHurricanes" type="checkbox" />
        <span aria-hidden="true">🌀</span> {{ $t('map.layerHurricanes') }}
      </label>
      <label class="layer-toggle">
        <input v-model="showRadar" type="checkbox" />
        <span aria-hidden="true">📡</span> {{ $t('map.layerRadar') }}
      </label>
    </div>
    <div
      ref="mapContainer"
      class="map-container"
      role="application"
      aria-label="Unified weather map"
    ></div>
    <div class="map-legend">
      <div v-if="showAirports" class="legend-section">
        <div class="legend-title">Airports</div>
        <div class="legend-item"><span class="legend-dot" style="background: #2196f3"></span> Airport</div>
      </div>
      <div v-if="showEarthquakes" class="legend-section">
        <div class="legend-title">Magnitude</div>
        <div class="legend-item"><span class="legend-dot" style="background: #4caf50"></span> 2.5-4</div>
        <div class="legend-item"><span class="legend-dot" style="background: #ff9800"></span> 4-5</div>
        <div class="legend-item"><span class="legend-dot" style="background: #f44336"></span> 5-7</div>
        <div class="legend-item"><span class="legend-dot" style="background: #9c27b0"></span> 7+</div>
      </div>
      <div v-if="showHurricanes" class="legend-section">
        <div class="legend-title">Storm Category</div>
        <div class="legend-item"><span class="legend-dot" style="background: #007bff"></span> TS</div>
        <div class="legend-item"><span class="legend-dot" style="background: #ffc107"></span> Cat 1-2</div>
        <div class="legend-item"><span class="legend-dot" style="background: #f44336"></span> Cat 3-4</div>
        <div class="legend-item"><span class="legend-dot" style="background: #9c27b0"></span> Cat 5</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, shallowRef, watch, onMounted } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { formatDate } from '../utils/dateUtils'

const store = useWeatherStore()
const { airports, earthquakes, hurricanes } = storeToRefs(store)

const mapContainer = ref<HTMLElement | null>(null)
const map = shallowRef<L.Map | null>(null)

const airportLayer = shallowRef<L.LayerGroup | null>(null)
const earthquakeLayer = shallowRef<L.LayerGroup | null>(null)
const hurricaneLayer = shallowRef<L.LayerGroup | null>(null)
const radarLayer = shallowRef<L.TileLayer | null>(null)

const showAirports = ref(true)
const showEarthquakes = ref(true)
const showHurricanes = ref(true)
const showRadar = ref(false)

const CATEGORY_COLORS: Record<number, string> = {
  0: '#007bff', 1: '#ffc107', 2: '#ff9800', 3: '#ff5722', 4: '#f44336', 5: '#9c27b0',
}

function initMap() {
  if (!mapContainer.value) return

  map.value = L.map(mapContainer.value).setView([30, -40], 3)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
    maxZoom: 18,
  }).addTo(map.value)

  airportLayer.value = L.layerGroup()
  earthquakeLayer.value = L.layerGroup()
  hurricaneLayer.value = L.layerGroup()
  radarLayer.value = L.tileLayer(
    'https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/nexrad-n0q-900913/{z}/{x}/{y}.png',
    { attribution: 'NEXRAD radar data', opacity: 0.5, maxZoom: 18 },
  )

  if (showAirports.value) airportLayer.value.addTo(map.value)
  if (showEarthquakes.value) earthquakeLayer.value.addTo(map.value)
  if (showHurricanes.value) hurricaneLayer.value.addTo(map.value)

  placeAirportMarkers()
  placeEarthquakeMarkers()
  placeHurricaneMarkers()
}

function placeAirportMarkers() {
  if (!airportLayer.value) return
  airportLayer.value.clearLayers()

  for (const apt of airports.value) {
    if (!apt.latitude || !apt.longitude) continue
    const marker = L.circleMarker([apt.latitude, apt.longitude], {
      radius: 4,
      fillColor: '#2196f3',
      color: '#fff',
      weight: 1,
      fillOpacity: 0.8,
    })
    marker.bindPopup(`
      <strong>${apt.airportCode || ''}</strong> - ${apt.name}<br/>
      ${apt.state || ''}, ${apt.country || ''}
    `)
    airportLayer.value.addLayer(marker)
  }
}

function getMagnitudeColor(magnitude: number): string {
  if (magnitude >= 7) return '#9c27b0'
  if (magnitude >= 5) return '#f44336'
  if (magnitude >= 4) return '#ff9800'
  return '#4caf50'
}

function placeEarthquakeMarkers() {
  if (!earthquakeLayer.value) return
  earthquakeLayer.value.clearLayers()

  for (const eq of earthquakes.value) {
    const color = getMagnitudeColor(eq.magnitude)
    const radius = Math.max(eq.magnitude * 3, 5)
    const marker = L.circleMarker([eq.latitude, eq.longitude], {
      radius,
      fillColor: color,
      color: '#fff',
      weight: 1,
      fillOpacity: 0.7,
    })
    marker.bindPopup(`
      <strong style="color:${color}">M${eq.magnitude}</strong> - ${eq.place || 'Unknown'}<br/>
      Depth: ${eq.depthKm} km<br/>
      ${formatDate(eq.eventTime)}
      ${eq.tsunami ? '<br/><strong style="color:#f44336">Tsunami Warning</strong>' : ''}
    `)
    earthquakeLayer.value.addLayer(marker)
  }
}

function placeHurricaneMarkers() {
  if (!hurricaneLayer.value) return
  hurricaneLayer.value.clearLayers()

  for (const storm of hurricanes.value) {
    const color = CATEGORY_COLORS[storm.category ?? 0] ?? CATEGORY_COLORS[0]
    const marker = L.marker([storm.latitude, storm.longitude], {
      icon: L.divIcon({
        className: 'storm-marker-unified',
        html: `<div style="background:${color};width:24px;height:24px;border-radius:50%;border:2px solid ${color};display:flex;align-items:center;justify-content:center;font-size:12px;box-shadow:0 2px 6px rgba(0,0,0,0.3)">🌀</div>`,
        iconSize: [24, 24],
        iconAnchor: [12, 12],
      }),
    })
    marker.bindPopup(`
      <strong>${storm.stormName || storm.stormId}</strong><br/>
      ${storm.category != null ? (storm.category === 0 ? 'Tropical Storm' : `Category ${storm.category}`) : 'N/A'}<br/>
      Winds: ${storm.maxSustainedWindsMph} mph<br/>
      Pressure: ${storm.minCentralPressureMb} mb
    `)
    marker.bindTooltip(storm.stormName || storm.stormId, {
      permanent: true, direction: 'top', offset: [0, -14],
      className: 'storm-label-unified',
    })
    hurricaneLayer.value.addLayer(marker)
  }
}

// Toggle layers on/off
watch(showAirports, (visible) => {
  if (!map.value || !airportLayer.value) return
  if (visible) map.value.addLayer(airportLayer.value)
  else map.value.removeLayer(airportLayer.value)
})

watch(showEarthquakes, (visible) => {
  if (!map.value || !earthquakeLayer.value) return
  if (visible) map.value.addLayer(earthquakeLayer.value)
  else map.value.removeLayer(earthquakeLayer.value)
})

watch(showHurricanes, (visible) => {
  if (!map.value || !hurricaneLayer.value) return
  if (visible) map.value.addLayer(hurricaneLayer.value)
  else map.value.removeLayer(hurricaneLayer.value)
})

watch(showRadar, (visible) => {
  if (!map.value || !radarLayer.value) return
  if (visible) map.value.addLayer(radarLayer.value)
  else map.value.removeLayer(radarLayer.value)
})

// Re-render markers when data changes
watch(airports, placeAirportMarkers, { deep: true })
watch(earthquakes, placeEarthquakeMarkers, { deep: true })
watch(hurricanes, placeHurricaneMarkers, { deep: true })

onMounted(() => {
  store.fetchAirports()
  store.fetchEarthquakes()
  store.fetchHurricanes()
  initMap()
})
</script>

<style scoped>
.unified-map-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.map-container {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
}

.layer-controls {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 1000;
  background: var(--bg-card, white);
  border-radius: 8px;
  padding: 10px 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.layer-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary, #333);
  cursor: pointer;
  white-space: nowrap;
}

.layer-toggle input[type="checkbox"] {
  accent-color: #ee0000;
}

.map-legend {
  position: absolute;
  bottom: 10px;
  right: 10px;
  z-index: 1000;
  background: var(--bg-card, white);
  border-radius: 8px;
  padding: 8px 12px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  font-size: 11px;
  max-height: 300px;
  overflow-y: auto;
}

.legend-section {
  margin-bottom: 8px;
}

.legend-section:last-child {
  margin-bottom: 0;
}

.legend-title {
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  color: var(--text-secondary, #666);
  margin-bottom: 3px;
  font-size: 10px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--text-primary, #333);
  line-height: 1.5;
}

.legend-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

:deep(.storm-marker-unified) {
  background: none !important;
  border: none !important;
}

:deep(.storm-label-unified) {
  font-weight: bold;
  font-size: 11px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border: none;
  border-radius: 4px;
  padding: 2px 6px;
}
</style>
