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
      <div v-if="showRadar" class="radar-controls">
        <select v-model="radarProduct" class="radar-select" :aria-label="$t('map.radarProduct')">
          <option value="nexrad-n0q-900913">{{ $t('map.radarBaseReflectivity') }}</option>
          <option value="nexrad-n0r-900913">{{ $t('map.radarCompositeReflectivity') }}</option>
          <option value="nexrad-n0u-900913">{{ $t('map.radarBaseVelocity') }}</option>
          <option value="nexrad-q2-900913">{{ $t('map.radarPrecipitation') }}</option>
        </select>
        <label class="opacity-control">
          {{ $t('map.radarOpacity') }}
          <input v-model.number="radarOpacity" type="range" min="10" max="90" step="10" class="opacity-slider" />
          <span class="opacity-value">{{ radarOpacity }}%</span>
        </label>
      </div>
    </div>
    <div
      ref="mapContainer"
      class="map-container"
      role="application"
      aria-label="Unified weather map"
    ></div>
    <div class="map-legend">
      <div v-if="showAirports" class="legend-section">
        <div class="legend-title">Flight Category</div>
        <div class="legend-item"><span class="legend-dot" style="background: #4caf50"></span> VFR</div>
        <div class="legend-item"><span class="legend-dot" style="background: #2196f3"></span> MVFR</div>
        <div class="legend-item"><span class="legend-dot" style="background: #ff9800"></span> IFR</div>
        <div class="legend-item"><span class="legend-dot" style="background: #f44336"></span> LIFR</div>
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
import weatherService, { type AirportWeather, type Location } from '../services/weatherService'
import { formatDate, formatRelativeTime, getFreshnessLevel } from '../utils/dateUtils'

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
const radarProduct = ref('nexrad-n0q-900913')
const radarOpacity = ref(50)

const CATEGORY_COLORS: Record<number, string> = {
  0: '#007bff', 1: '#ffc107', 2: '#ff9800', 3: '#ff5722', 4: '#f44336', 5: '#9c27b0',
}

const FLIGHT_CATEGORY_COLORS: Record<string, string> = {
  VFR: '#4caf50',
  MVFR: '#2196f3',
  IFR: '#ff9800',
  LIFR: '#f44336',
}
const DEFAULT_MARKER_COLOR = '#2196f3'

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
    `https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/${radarProduct.value}/{z}/{x}/{y}.png`,
    { attribution: 'NEXRAD radar data &copy; Iowa State University', opacity: radarOpacity.value / 100, maxZoom: 18 },
  )

  if (showAirports.value) airportLayer.value.addTo(map.value)
  if (showEarthquakes.value) earthquakeLayer.value.addTo(map.value)
  if (showHurricanes.value) hurricaneLayer.value.addTo(map.value)

  placeAirportMarkers()
  placeEarthquakeMarkers()
  placeHurricaneMarkers()
}

function createAirportPopupContent(airport: Location) {
  return `
    <div class="airport-popup">
      <div class="popup-header"><strong>${airport.airportCode || ''}</strong> - ${airport.name}</div>
      <div class="popup-location">${airport.state || ''}${airport.state && airport.country ? ', ' : ''}${airport.country || ''}</div>
      <div class="weather-container">
        <div class="weather-loading">Loading weather...</div>
      </div>
    </div>
  `
}

function degreesToCompass(deg: number): string {
  const dirs = ['N','NNE','NE','ENE','E','ESE','SE','SSE','S','SSW','SW','WSW','W','WNW','NW','NNW']
  return dirs[Math.round(deg / 22.5) % 16]
}

async function fetchAirportWeather(code: string): Promise<{ metar: AirportWeather | null; taf: AirportWeather | null }> {
  const [metarResult, tafResult] = await Promise.allSettled([
    weatherService.getLatestMetar(code),
    weatherService.getLatestTaf(code),
  ])
  return {
    metar: metarResult.status === 'fulfilled' ? metarResult.value : null,
    taf: tafResult.status === 'fulfilled' ? tafResult.value : null,
  }
}

function updateAirportPopup(popup: L.Popup, metar: AirportWeather | null, taf: AirportWeather | null) {
  const el = popup.getElement()
  if (!el) return
  const container = el.querySelector('.weather-container')
  if (!container) return

  if (!metar && !taf) {
    container.innerHTML = `
      <div class="weather-divider"></div>
      <div class="weather-no-data">No weather data available. Check back soon.</div>
    `
    return
  }

  let html = '<div class="weather-divider"></div>'

  if (metar) {
    const tempC = metar.temperatureCelsius
    const tempF = tempC != null ? Math.round((tempC * 9) / 5 + 32) : null
    const dewC = metar.dewpointCelsius
    const dewF = dewC != null ? Math.round((dewC * 9) / 5 + 32) : null
    const windKnots = metar.windSpeedKnots
    const windMph = windKnots ? Math.round(windKnots * 1.151) : null
    const gustKnots = metar.windGustKnots
    const gustMph = gustKnots ? Math.round(gustKnots * 1.151) : null
    const windDir = metar.windDirection
    const windDirStr = windDir != null ? degreesToCompass(windDir) : null

    let windStr = ''
    if (windMph) {
      windStr = `${windMph} mph`
      if (windDirStr) windStr += ` from ${windDirStr}`
      if (gustMph) windStr += `, gusts ${gustMph}`
    }

    const observationTime = metar.observationTime ? new Date(metar.observationTime).toLocaleString() : null
    const fetchedAt = metar.fetchedAt || null
    const relativeTime = fetchedAt ? formatRelativeTime(fetchedAt) : null
    const freshnessLevel = fetchedAt ? getFreshnessLevel(fetchedAt, 'airport') : 'fresh'
    const flightCategory = metar.flightCategory
    const catColor = flightCategory ? (FLIGHT_CATEGORY_COLORS[flightCategory] || '#666') : '#666'

    html += '<div class="weather-title">METAR Conditions</div><div class="weather-info">'
    if (tempF != null) html += `<div class="weather-item"><span class="weather-label">Temperature:</span><span class="weather-value">${tempF}°F / ${tempC}°C</span></div>`
    if (dewF != null) html += `<div class="weather-item"><span class="weather-label">Dew Point:</span><span class="weather-value">${dewF}°F / ${dewC}°C</span></div>`
    if (windStr) html += `<div class="weather-item"><span class="weather-label">Wind:</span><span class="weather-value">${windStr}</span></div>`
    if (metar.visibilityMiles != null) html += `<div class="weather-item"><span class="weather-label">Visibility:</span><span class="weather-value">${metar.visibilityMiles} mi</span></div>`
    if (metar.ceilingFeet != null) html += `<div class="weather-item"><span class="weather-label">Ceiling:</span><span class="weather-value">${metar.ceilingFeet} ft</span></div>`
    if (metar.skyCondition) html += `<div class="weather-item"><span class="weather-label">Sky:</span><span class="weather-value">${metar.skyCondition}</span></div>`
    if (metar.weatherConditions) html += `<div class="weather-item"><span class="weather-label">Weather:</span><span class="weather-value">${metar.weatherConditions}</span></div>`
    if (flightCategory) html += `<div class="weather-item"><span class="weather-label">Flight Cat:</span><span class="weather-value" style="color:${catColor};font-weight:bold">${flightCategory}</span></div>`
    html += '</div>'
    if (observationTime) {
      html += `<div class="weather-time">Observed: ${observationTime}`
      if (relativeTime) html += ` <span class="freshness-indicator freshness-${freshnessLevel}">${relativeTime}</span>`
      html += '</div>'
    }
  }

  if (taf) {
    html += '<div class="weather-divider"></div>'
    html += '<div class="weather-title">TAF Forecast</div>'
    html += `<div class="taf-raw">${taf.rawText || 'No TAF text available'}</div>`
  }

  container.innerHTML = html
}

function placeAirportMarkers() {
  if (!airportLayer.value) return
  airportLayer.value.clearLayers()

  for (const apt of airports.value) {
    if (!apt.latitude || !apt.longitude) continue
    const marker = L.circleMarker([apt.latitude, apt.longitude], {
      radius: 4,
      fillColor: DEFAULT_MARKER_COLOR,
      color: '#fff',
      weight: 1,
      fillOpacity: 0.8,
    })

    const popupContent = createAirportPopupContent(apt)
    const popup = L.popup({ maxWidth: 320 }).setContent(popupContent)
    marker.bindPopup(popup)

    marker.on('popupopen', async () => {
      if (apt.airportCode) {
        const { metar, taf } = await fetchAirportWeather(apt.airportCode)
        updateAirportPopup(popup, metar, taf)
        if (metar?.flightCategory) {
          marker.setStyle({ fillColor: FLIGHT_CATEGORY_COLORS[metar.flightCategory] || DEFAULT_MARKER_COLOR })
        }
      }
    })

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

watch(radarProduct, (product) => {
  if (!map.value || !radarLayer.value) return
  const wasVisible = map.value.hasLayer(radarLayer.value)
  if (wasVisible) map.value.removeLayer(radarLayer.value)
  radarLayer.value = L.tileLayer(
    `https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/${product}/{z}/{x}/{y}.png`,
    { attribution: 'NEXRAD radar data &copy; Iowa State University', opacity: radarOpacity.value / 100, maxZoom: 18 },
  )
  if (wasVisible) map.value.addLayer(radarLayer.value)
})

watch(radarOpacity, (opacity) => {
  if (radarLayer.value) radarLayer.value.setOpacity(opacity / 100)
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
  right: 10px;
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

.radar-controls {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-left: 22px;
  margin-top: 2px;
}

.radar-select {
  font-size: 11px;
  padding: 3px 6px;
  border: 1px solid var(--border-light, #ccc);
  border-radius: 4px;
  background: var(--bg-card, white);
  color: var(--text-primary, #333);
  cursor: pointer;
}

.opacity-control {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--text-secondary, #666);
  cursor: default;
}

.opacity-slider {
  width: 60px;
  height: 4px;
  accent-color: #ee0000;
  cursor: pointer;
}

.opacity-value {
  font-size: 10px;
  font-weight: 600;
  min-width: 28px;
  color: var(--text-primary, #333);
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

:deep(.airport-popup) {
  padding: 4px;
}

:deep(.popup-header) {
  font-size: 14px;
  color: #ee0000;
  font-weight: 600;
  margin-bottom: 2px;
}

:deep(.popup-location) {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-bottom: 4px;
}

:deep(.weather-container) {
  margin-top: 4px;
}

:deep(.weather-divider) {
  height: 1px;
  background: var(--border-light, #e0e0e0);
  margin: 8px 0;
}

:deep(.weather-loading) {
  font-size: 12px;
  color: var(--text-secondary, #666);
  text-align: center;
  padding: 8px 0;
}

:deep(.weather-no-data) {
  font-size: 12px;
  color: var(--text-secondary, #999);
  text-align: center;
  padding: 8px 0;
  font-style: italic;
}

:deep(.weather-title) {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #333);
  margin-bottom: 6px;
}

:deep(.weather-info) {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

:deep(.weather-item) {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  gap: 8px;
}

:deep(.weather-label) {
  color: var(--text-secondary, #666);
  font-weight: 500;
}

:deep(.weather-value) {
  color: var(--text-primary, #333);
  font-weight: 600;
  text-align: right;
}

:deep(.weather-time) {
  font-size: 11px;
  color: var(--text-secondary, #999);
  margin-top: 6px;
  padding-top: 4px;
  border-top: 1px solid var(--border-light, #eee);
  text-align: center;
}

:deep(.freshness-indicator) {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 600;
  margin-left: 4px;
}

:deep(.freshness-fresh) {
  background: #e8f5e9;
  color: #2e7d32;
}

:deep(.freshness-aging) {
  background: #fff3e0;
  color: #e65100;
}

:deep(.freshness-stale) {
  background: #ffebee;
  color: #c62828;
}

:deep(.taf-raw) {
  font-family: monospace;
  font-size: 11px;
  background: var(--bg-input, #f5f5f5);
  border: 1px solid var(--border-light, #e0e0e0);
  border-radius: 4px;
  padding: 8px;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--text-primary, #333);
  max-height: 120px;
  overflow-y: auto;
}

:deep(.leaflet-popup-content-wrapper) {
  border-radius: 8px;
}

:deep(.leaflet-popup-content) {
  margin: 10px 12px;
}
</style>
