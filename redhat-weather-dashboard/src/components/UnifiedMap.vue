<template>
  <div class="unified-map-wrapper">
    <div class="map-search-bar">
      <label for="unified-map-search" class="sr-only">Search map</label>
      <input
        id="unified-map-search"
        v-model="searchQuery"
        type="text"
        :placeholder="$t('map.searchPlaceholder')"
        class="search-input"
        @input="onSearchInput"
        @focus="showResults = true"
        @keydown.escape="closeSearch"
      />
      <div v-if="searchResults.length > 0 && showResults && searchQuery.length >= 2" class="search-results" role="listbox" aria-label="Map search results">
        <div
          v-for="result in searchResults"
          :key="result.key"
          class="search-result-item"
          role="option"
          tabindex="0"
          @click="selectResult(result)"
          @keydown.enter.prevent="selectResult(result)"
          @keydown.space.prevent="selectResult(result)"
        >
          <span class="result-icon" aria-hidden="true">{{ result.icon }}</span>
          <div class="result-info">
            <div class="result-title">{{ result.title }}</div>
            <div class="result-subtitle">{{ result.subtitle }}</div>
          </div>
        </div>
      </div>
    </div>
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
        <input v-model="showPireps" type="checkbox" />
        <span aria-hidden="true">📋</span> {{ $t('map.layerPireps') }}
      </label>
      <label class="layer-toggle">
        <input v-model="showSigmets" type="checkbox" />
        <span aria-hidden="true">🚨</span> {{ $t('map.layerSigmets') }}
      </label>
      <label class="layer-toggle">
        <input v-model="showCwas" type="checkbox" />
        <span aria-hidden="true">📡</span> {{ $t('map.layerCwas') }}
      </label>
      <label class="layer-toggle">
        <input v-model="showTfrs" type="checkbox" />
        <span aria-hidden="true">🚫</span> {{ $t('map.layerTfrs') }}
      </label>
      <label class="layer-toggle">
        <input v-model="showGroundStops" type="checkbox" />
        <span aria-hidden="true">🛑</span> {{ $t('map.layerGroundStops') }}
      </label>
      <label class="layer-toggle">
        <input v-model="showVolcanicAsh" type="checkbox" />
        <span aria-hidden="true">🌋</span> {{ $t('map.layerVolcanicAsh') }}
      </label>
      <label class="layer-toggle">
        <input v-model="showLightning" type="checkbox" />
        <span aria-hidden="true">⚡</span> {{ $t('map.layerLightning') }}
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
        <div class="legend-item"><span class="legend-dot dot-vfr"></span> VFR</div>
        <div class="legend-item"><span class="legend-dot dot-mvfr"></span> MVFR</div>
        <div class="legend-item"><span class="legend-dot dot-ifr"></span> IFR</div>
        <div class="legend-item"><span class="legend-dot dot-lifr"></span> LIFR</div>
      </div>
      <div v-if="showEarthquakes" class="legend-section">
        <div class="legend-title">Magnitude</div>
        <div class="legend-item"><span class="legend-dot dot-mag-low"></span> 2.5-4</div>
        <div class="legend-item"><span class="legend-dot dot-mag-med"></span> 4-5</div>
        <div class="legend-item"><span class="legend-dot dot-mag-high"></span> 5-7</div>
        <div class="legend-item"><span class="legend-dot dot-mag-major"></span> 7+</div>
      </div>
      <div v-if="showHurricanes" class="legend-section">
        <div class="legend-title">Storm Category</div>
        <div class="legend-item"><span class="legend-dot dot-storm-ts"></span> TS</div>
        <div class="legend-item"><span class="legend-dot dot-storm-cat12"></span> Cat 1-2</div>
        <div class="legend-item"><span class="legend-dot dot-storm-cat34"></span> Cat 3-4</div>
        <div class="legend-item"><span class="legend-dot dot-storm-cat5"></span> Cat 5</div>
      </div>
      <div v-if="showPireps" class="legend-section">
        <div class="legend-title">PIREP Severity</div>
        <div class="legend-item"><span class="legend-dot dot-pirep-light"></span> None/Light</div>
        <div class="legend-item"><span class="legend-dot dot-pirep-mod"></span> Moderate</div>
        <div class="legend-item"><span class="legend-dot dot-pirep-sev"></span> Severe</div>
        <div class="legend-item"><span class="legend-dot dot-pirep-ext"></span> Extreme</div>
      </div>
      <div v-if="showSigmets" class="legend-section">
        <div class="legend-title">SIGMETs</div>
        <div class="legend-item"><span class="legend-swatch swatch-sigmet"></span> Active Area</div>
      </div>
      <div v-if="showCwas" class="legend-section">
        <div class="legend-title">CWAs</div>
        <div class="legend-item"><span class="legend-swatch swatch-cwa"></span> Active Area</div>
      </div>
      <div v-if="showTfrs" class="legend-section">
        <div class="legend-title">TFRs</div>
        <div class="legend-item"><span class="legend-swatch swatch-tfr"></span> Restricted Area</div>
      </div>
      <div v-if="showGroundStops" class="legend-section">
        <div class="legend-title">Ground Stops</div>
        <div class="legend-item"><span class="legend-dot dot-gs-stop"></span> Ground Stop</div>
        <div class="legend-item"><span class="legend-dot dot-gs-delay"></span> Ground Delay</div>
      </div>
      <div v-if="showVolcanicAsh" class="legend-section">
        <div class="legend-title">Volcanic Ash</div>
        <div class="legend-item"><span class="legend-swatch swatch-ash"></span> Ash Cloud</div>
      </div>
      <div v-if="showLightning" class="legend-section">
        <div class="legend-title">Lightning</div>
        <div class="legend-item"><span class="legend-dot dot-lightning"></span> Recent Strike</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, shallowRef, watch, onMounted, onBeforeUnmount, nextTick, markRaw } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useWeatherStore } from '../stores/weatherStore'
import weatherService, { type AirportWeather, type Location } from '../services/weatherService'
import { formatDate, formatRelativeTime, getFreshnessLevel } from '../utils/dateUtils'

const router = useRouter()
const store = useWeatherStore()
const { airports, earthquakes, hurricanes, pireps, sigmets, cwas, tfrs, groundStops, volcanicAsh, lightning } = storeToRefs(store)

const mapContainer = ref<HTMLElement | null>(null)
const map = shallowRef<L.Map | null>(null)

const airportLayer = shallowRef<L.LayerGroup | null>(null)
const earthquakeLayer = shallowRef<L.LayerGroup | null>(null)
const hurricaneLayer = shallowRef<L.LayerGroup | null>(null)
const pirepLayer = shallowRef<L.LayerGroup | null>(null)
const sigmetLayer = shallowRef<L.LayerGroup | null>(null)
const cwaLayer = shallowRef<L.LayerGroup | null>(null)
const tfrLayer = shallowRef<L.LayerGroup | null>(null)
const groundStopLayer = shallowRef<L.LayerGroup | null>(null)
const volcanicAshLayer = shallowRef<L.LayerGroup | null>(null)
const lightningLayer = shallowRef<L.LayerGroup | null>(null)
const radarLayer = shallowRef<L.TileLayer | null>(null)

const showAirports = ref(true)
const showEarthquakes = ref(true)
const showHurricanes = ref(true)
const showPireps = ref(false)
const showSigmets = ref(false)
const showCwas = ref(false)
const showTfrs = ref(false)
const showGroundStops = ref(false)
const showVolcanicAsh = ref(false)
const showLightning = ref(false)
const showRadar = ref(false)
const radarProduct = ref('nexrad-n0q-900913')
const radarOpacity = ref(50)

function getStormTypeName(basin?: string): string {
  const types: Record<string, string> = { AT: 'Hurricane', EP: 'Hurricane', CP: 'Hurricane', WP: 'Typhoon', IO: 'Cyclone', SH: 'Cyclone' }
  return types[basin || 'AT'] || 'Hurricane'
}

// Search state
const searchQuery = ref('')
const showResults = ref(false)

interface SearchResult {
  key: string
  icon: string
  title: string
  subtitle: string
  lat: number
  lng: number
  zoom: number
  type: 'airport' | 'earthquake' | 'hurricane' | 'pirep' | 'sigmet' | 'cwa' | 'tfr' | 'groundStop' | 'volcanicAsh' | 'lightning'
}

const searchResults = ref<SearchResult[]>([])

function onSearchInput() {
  if (!searchQuery.value || searchQuery.value.length < 2) {
    searchResults.value = []
    return
  }

  const query = searchQuery.value.toLowerCase()
  const results: SearchResult[] = []

  // Search airports
  if (showAirports.value) {
    for (const apt of airports.value) {
      if (!apt.latitude || !apt.longitude) continue
      if (
        apt.name?.toLowerCase().includes(query) ||
        apt.airportCode?.toLowerCase().includes(query) ||
        apt.state?.toLowerCase().includes(query) ||
        apt.country?.toLowerCase().includes(query)
      ) {
        results.push({
          key: `airport-${apt.id}`,
          icon: '✈️',
          title: `${apt.airportCode || ''} - ${apt.name}`,
          subtitle: [apt.state, apt.country].filter(Boolean).join(', '),
          lat: apt.latitude,
          lng: apt.longitude,
          zoom: 12,
          type: 'airport',
        })
      }
      if (results.length >= 15) break
    }
  }

  // Search earthquakes
  if (showEarthquakes.value && results.length < 15) {
    for (const eq of earthquakes.value) {
      if (eq.place?.toLowerCase().includes(query)) {
        results.push({
          key: `eq-${eq.id}`,
          icon: '🌍',
          title: `M${eq.magnitude} - ${eq.place}`,
          subtitle: `Depth: ${eq.depthKm} km`,
          lat: eq.latitude,
          lng: eq.longitude,
          zoom: 8,
          type: 'earthquake',
        })
      }
      if (results.length >= 15) break
    }
  }

  // Search hurricanes
  if (showHurricanes.value && results.length < 15) {
    for (const storm of hurricanes.value) {
      const name = storm.stormName || storm.stormId || ''
      if (name.toLowerCase().includes(query)) {
        results.push({
          key: `storm-${storm.id}`,
          icon: '🌀',
          title: name,
          subtitle: storm.category != null
            ? (storm.category === 0 ? 'Tropical Storm' : `${getStormTypeName(storm.basin)} Cat ${storm.category}`)
            : 'Active Storm',
          lat: storm.latitude,
          lng: storm.longitude,
          zoom: 6,
          type: 'hurricane',
        })
      }
    }
  }

  // Search TFRs
  if (showTfrs.value && results.length < 15) {
    for (const tfr of tfrs.value) {
      if (!tfr.latitude || !tfr.longitude) continue
      if (
        tfr.notamId?.toLowerCase().includes(query) ||
        tfr.facility?.toLowerCase().includes(query) ||
        tfr.tfrType?.toLowerCase().includes(query) ||
        tfr.description?.toLowerCase().includes(query)
      ) {
        results.push({
          key: `tfr-${tfr.id}`,
          icon: '🚫',
          title: `TFR ${tfr.notamId}`,
          subtitle: `${tfr.tfrType} - ${tfr.facility}${tfr.state ? ', ' + tfr.state : ''}`,
          lat: tfr.latitude,
          lng: tfr.longitude,
          zoom: 10,
          type: 'tfr',
        })
      }
      if (results.length >= 15) break
    }
  }

  // Search ground stops
  if (showGroundStops.value && results.length < 15) {
    for (const gs of groundStops.value) {
      if (
        gs.airportCode?.toLowerCase().includes(query) ||
        gs.airportName?.toLowerCase().includes(query) ||
        gs.programType?.toLowerCase().includes(query)
      ) {
        // Ground stops don't have lat/lon, so look up airport coords
        const apt = airports.value.find(a => a.airportCode === gs.airportCode)
        if (apt?.latitude && apt?.longitude) {
          results.push({
            key: `gs-${gs.id}`,
            icon: '🛑',
            title: `${gs.airportCode} - ${gs.programType}`,
            subtitle: gs.reason || gs.airportName || '',
            lat: apt.latitude,
            lng: apt.longitude,
            zoom: 10,
            type: 'groundStop',
          })
        }
      }
      if (results.length >= 15) break
    }
  }

  searchResults.value = results.slice(0, 15)
  showResults.value = true
}

function selectResult(result: SearchResult) {
  if (!map.value) return
  map.value.flyTo([result.lat, result.lng], result.zoom)
  searchQuery.value = result.title
  showResults.value = false

  // Open the popup for the matching marker
  const layerMap: Record<string, L.LayerGroup | null> = {
    airport: airportLayer.value,
    earthquake: earthquakeLayer.value,
    hurricane: hurricaneLayer.value,
    pirep: pirepLayer.value,
    sigmet: sigmetLayer.value,
    cwa: cwaLayer.value,
    tfr: tfrLayer.value,
    groundStop: groundStopLayer.value,
    volcanicAsh: volcanicAshLayer.value,
    lightning: lightningLayer.value,
  }
  const layer = layerMap[result.type] ?? null

  if (layer) {
    layer.eachLayer((l: L.Layer) => {
      const latLng = (l as L.CircleMarker).getLatLng?.() ?? (l as L.Marker).getLatLng?.()
      if (latLng && Math.abs(latLng.lat - result.lat) < 0.0001 && Math.abs(latLng.lng - result.lng) < 0.0001) {
        setTimeout(() => (l as L.CircleMarker).openPopup(), 500)
      }
    })
  }
}

function closeSearch() {
  showResults.value = false
}

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

  map.value = markRaw(L.map(mapContainer.value, { preferCanvas: true }).setView([30, -40], 3))

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
    maxZoom: 18,
  }).addTo(map.value)

  airportLayer.value = markRaw(L.layerGroup())
  earthquakeLayer.value = markRaw(L.layerGroup())
  hurricaneLayer.value = markRaw(L.layerGroup())
  pirepLayer.value = markRaw(L.layerGroup())
  sigmetLayer.value = markRaw(L.layerGroup())
  cwaLayer.value = markRaw(L.layerGroup())
  tfrLayer.value = markRaw(L.layerGroup())
  groundStopLayer.value = markRaw(L.layerGroup())
  volcanicAshLayer.value = markRaw(L.layerGroup())
  lightningLayer.value = markRaw(L.layerGroup())
  radarLayer.value = markRaw(L.tileLayer(
    `https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/${radarProduct.value}/{z}/{x}/{y}.png`,
    { attribution: 'NEXRAD radar data &copy; Iowa State University', opacity: radarOpacity.value / 100, maxZoom: 18 },
  ))

  if (showAirports.value) airportLayer.value.addTo(map.value)
  if (showEarthquakes.value) earthquakeLayer.value.addTo(map.value)
  if (showHurricanes.value) hurricaneLayer.value.addTo(map.value)
  if (showPireps.value) pirepLayer.value.addTo(map.value)
  if (showSigmets.value) sigmetLayer.value.addTo(map.value)
  if (showCwas.value) cwaLayer.value.addTo(map.value)
  if (showTfrs.value) tfrLayer.value.addTo(map.value)
  if (showGroundStops.value) groundStopLayer.value.addTo(map.value)
  if (showVolcanicAsh.value) volcanicAshLayer.value.addTo(map.value)
  if (showLightning.value) lightningLayer.value.addTo(map.value)

  // Intercept airport detail links in popups to use Vue Router
  mapContainer.value.addEventListener('click', (e: Event) => {
    const target = (e.target as HTMLElement).closest('[data-airport-link]')
    if (target) {
      e.preventDefault()
      const code = target.getAttribute('data-airport-link')
      if (code) router.push({ path: '/airports', query: { code } })
    }
  })

  placeAirportMarkers()
  placeEarthquakeMarkers()
  placeHurricaneMarkers()
  placePirepMarkers()
  placeSigmetPolygons()
  placeCwaPolygons()
  placeTfrPolygons()
}

function createAirportPopupContent(airport: Location) {
  const code = airport.airportCode || ''
  const detailUrl = code ? `/airports?code=${encodeURIComponent(code)}` : ''
  return `
    <div class="airport-popup">
      <div class="popup-header"><strong>${code}</strong> - ${airport.name}</div>
      <div class="popup-location">${airport.state || ''}${airport.state && airport.country ? ', ' : ''}${airport.country || ''}</div>
      <div class="weather-container">
        <div class="weather-loading">Loading weather...</div>
      </div>
      ${detailUrl ? `<a href="${detailUrl}" class="popup-detail-link" data-airport-link="${code}">View full details &rarr;</a>` : ''}
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

function createWeatherItem(label: string, value: string, style?: string): HTMLElement {
  const item = document.createElement('div')
  item.className = 'weather-item'
  const labelEl = document.createElement('span')
  labelEl.className = 'weather-label'
  labelEl.textContent = label
  const valueEl = document.createElement('span')
  valueEl.className = 'weather-value'
  valueEl.textContent = value
  if (style) valueEl.setAttribute('style', style)
  item.appendChild(labelEl)
  item.appendChild(valueEl)
  return item
}

function updateAirportPopup(popup: L.Popup, metar: AirportWeather | null, taf: AirportWeather | null) {
  const el = popup.getElement()
  if (!el) return
  const container = el.querySelector('.weather-container')
  if (!container) return

  container.textContent = ''

  const divider = document.createElement('div')
  divider.className = 'weather-divider'
  container.appendChild(divider)

  if (!metar && !taf) {
    const noData = document.createElement('div')
    noData.className = 'weather-no-data'
    noData.textContent = 'No weather data available. Check back soon.'
    container.appendChild(noData)
    return
  }

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

    const title = document.createElement('div')
    title.className = 'weather-title'
    title.textContent = 'METAR Conditions'
    container.appendChild(title)

    const info = document.createElement('div')
    info.className = 'weather-info'
    if (tempF != null) info.appendChild(createWeatherItem('Temperature:', `${tempF}°F / ${tempC}°C`))
    if (dewF != null) info.appendChild(createWeatherItem('Dew Point:', `${dewF}°F / ${dewC}°C`))
    if (windStr) info.appendChild(createWeatherItem('Wind:', windStr))
    if (metar.visibilityMiles != null) info.appendChild(createWeatherItem('Visibility:', `${metar.visibilityMiles} mi`))
    if (metar.ceilingFeet != null) info.appendChild(createWeatherItem('Ceiling:', `${metar.ceilingFeet} ft`))
    if (metar.skyCondition) info.appendChild(createWeatherItem('Sky:', metar.skyCondition))
    if (metar.weatherConditions) info.appendChild(createWeatherItem('Weather:', metar.weatherConditions))
    if (flightCategory) {
      info.appendChild(createWeatherItem('Flight Cat:', flightCategory, `color:${catColor};font-weight:bold`))
    }
    container.appendChild(info)

    if (observationTime) {
      const timeEl = document.createElement('div')
      timeEl.className = 'weather-time'
      timeEl.textContent = `Observed: ${observationTime}`
      if (relativeTime) {
        const badge = document.createElement('span')
        badge.className = `freshness-indicator freshness-${freshnessLevel}`
        badge.textContent = relativeTime
        timeEl.appendChild(document.createTextNode(' '))
        timeEl.appendChild(badge)
      }
      container.appendChild(timeEl)
    }
  }

  if (taf) {
    const tafDivider = document.createElement('div')
    tafDivider.className = 'weather-divider'
    container.appendChild(tafDivider)

    const tafTitle = document.createElement('div')
    tafTitle.className = 'weather-title'
    tafTitle.textContent = 'TAF Forecast'
    container.appendChild(tafTitle)

    const tafRaw = document.createElement('div')
    tafRaw.className = 'taf-raw'
    tafRaw.textContent = taf.rawText || 'No TAF text available'
    container.appendChild(tafRaw)
  }
}

// Track the latest call to cancel stale async placement
let airportPlacementId = 0

async function placeAirportMarkers() {
  if (!airportLayer.value) return
  airportLayer.value.clearLayers()

  const layer = airportLayer.value
  const data = airports.value
  const currentId = ++airportPlacementId
  const BATCH_SIZE = 500

  for (let i = 0; i < data.length; i++) {
    // Yield to main thread every BATCH_SIZE markers
    if (i > 0 && i % BATCH_SIZE === 0) {
      await new Promise(r => setTimeout(r, 0))
      // Abort if a newer placement started
      if (airportPlacementId !== currentId) return
    }

    const apt = data[i]
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

    layer.addLayer(marker)
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
      ${storm.category != null ? (storm.category === 0 ? 'Tropical Storm' : `${getStormTypeName(storm.basin)} Cat ${storm.category}`) : 'N/A'}<br/>
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

const PIREP_SEVERITY_COLORS: Record<string, string> = {
  NEG: '#4caf50', NONE: '#4caf50', LGT: '#4caf50', 'NEG-LGT': '#4caf50',
  MOD: '#ff9800', 'LGT-MOD': '#ff9800', 'MOD-SEV': '#f44336',
  SEV: '#f44336', EXTRM: '#9c27b0', HVY: '#9c27b0',
}

function getPirepColor(pirep: { turbulenceIntensity?: string; icingIntensity?: string }): string {
  return PIREP_SEVERITY_COLORS[pirep.turbulenceIntensity ?? '']
    ?? PIREP_SEVERITY_COLORS[pirep.icingIntensity ?? '']
    ?? '#4caf50'
}

function placePirepMarkers() {
  if (!pirepLayer.value) return
  pirepLayer.value.clearLayers()

  for (const p of pireps.value) {
    if (!p.latitude || !p.longitude) continue
    const color = getPirepColor(p)
    const marker = L.circleMarker([p.latitude, p.longitude], {
      radius: 5,
      fillColor: color,
      color: '#fff',
      weight: 1,
      fillOpacity: 0.8,
    })
    const turbStr = p.turbulenceIntensity ? `Turb: ${p.turbulenceIntensity}` : ''
    const iceStr = p.icingIntensity ? `Ice: ${p.icingIntensity}` : ''
    const altStr = p.altitudeFt ? `${p.altitudeFt.toLocaleString()} ft` : ''
    marker.bindPopup(`
      <strong>${p.reportType}</strong>${altStr ? ' at ' + altStr : ''}<br/>
      ${[turbStr, iceStr].filter(Boolean).join(' | ')}<br/>
      ${p.aircraftType ? 'Aircraft: ' + p.aircraftType + '<br/>' : ''}
      ${formatRelativeTime(p.observationTime)}
    `)
    pirepLayer.value.addLayer(marker)
  }
}

function placeGeoJsonPolygons(
  layer: L.LayerGroup,
  items: { geojson?: object; [key: string]: unknown }[],
  color: string,
  popupFn: (item: Record<string, unknown>) => string,
) {
  layer.clearLayers()
  for (const item of items) {
    if (!item.geojson) continue
    try {
      const geoLayer = L.geoJSON(item.geojson as GeoJSON.GeoJsonObject, {
        style: {
          color,
          weight: 2,
          fillColor: color,
          fillOpacity: 0.2,
        },
      })
      geoLayer.bindPopup(popupFn(item as Record<string, unknown>))
      layer.addLayer(geoLayer)
    } catch {
      // skip invalid GeoJSON
    }
  }
}

function placeSigmetPolygons() {
  if (!sigmetLayer.value) return
  placeGeoJsonPolygons(
    sigmetLayer.value,
    sigmets.value,
    '#f44336',
    (s) => `
      <strong>SIGMET ${s.sigmetId}</strong><br/>
      ${s.hazard ? 'Hazard: ' + s.hazard + '<br/>' : ''}
      ${s.severity ? 'Severity: ' + s.severity + '<br/>' : ''}
      ${s.altitudeLowFt || s.altitudeHighFt ? 'Alt: ' + (s.altitudeLowFt ?? '?') + ' - ' + (s.altitudeHighFt ?? '?') + ' ft<br/>' : ''}
      ${s.scope === 'INTERNATIONAL' && s.firName ? 'FIR: ' + s.firName + '<br/>' : ''}
      Valid: ${formatRelativeTime(s.validTimeFrom as string)} to ${formatRelativeTime(s.validTimeTo as string)}
    `,
  )
}

function placeCwaPolygons() {
  if (!cwaLayer.value) return
  placeGeoJsonPolygons(
    cwaLayer.value,
    cwas.value,
    '#ff9800',
    (c) => `
      <strong>CWA ${c.cwaId}</strong> (${c.artcc})<br/>
      ${c.hazard ? 'Hazard: ' + c.hazard + '<br/>' : ''}
      ${c.severity ? 'Severity: ' + c.severity + '<br/>' : ''}
      Valid: ${formatRelativeTime(c.validTimeFrom as string)} to ${formatRelativeTime(c.validTimeTo as string)}
    `,
  )
}

function placeTfrPolygons() {
  if (!tfrLayer.value) return
  tfrLayer.value.clearLayers()

  for (const tfr of tfrs.value) {
    if (tfr.geojson) {
      try {
        const geoLayer = L.geoJSON(tfr.geojson as GeoJSON.GeoJsonObject, {
          style: {
            color: '#d32f2f',
            weight: 2,
            fillColor: '#d32f2f',
            fillOpacity: 0.2,
            dashArray: '5, 5',
          },
        })
        geoLayer.bindPopup(`
          <strong>TFR ${tfr.notamId}</strong>${tfr.isNew ? ' <span style="color:#4caf50;font-weight:bold">NEW</span>' : ''}<br/>
          Type: ${tfr.tfrType}<br/>
          Facility: ${tfr.facility}${tfr.state ? ', ' + tfr.state : ''}<br/>
          ${tfr.description ? '<small>' + tfr.description.slice(0, 200) + (tfr.description.length > 200 ? '...' : '') + '</small>' : ''}
        `)
        tfrLayer.value.addLayer(geoLayer)
      } catch {
        // skip invalid GeoJSON
      }
    } else if (tfr.latitude && tfr.longitude) {
      // Fallback: point marker if no polygon
      const marker = L.circleMarker([tfr.latitude, tfr.longitude], {
        radius: 8,
        fillColor: '#d32f2f',
        color: '#fff',
        weight: 2,
        fillOpacity: 0.6,
      })
      marker.bindPopup(`
        <strong>TFR ${tfr.notamId}</strong>${tfr.isNew ? ' <span style="color:#4caf50;font-weight:bold">NEW</span>' : ''}<br/>
        Type: ${tfr.tfrType}<br/>
        Facility: ${tfr.facility}${tfr.state ? ', ' + tfr.state : ''}
      `)
      tfrLayer.value.addLayer(marker)
    }
  }
}

function placeGroundStopMarkers() {
  if (!groundStopLayer.value) return
  groundStopLayer.value.clearLayers()

  for (const gs of groundStops.value) {
    // Look up airport coords
    const apt = airports.value.find(a => a.airportCode === gs.airportCode)
    if (!apt?.latitude || !apt?.longitude) continue

    const isStop = gs.programType.toLowerCase().includes('stop')
    const color = isStop ? '#d32f2f' : '#ff9800'
    const marker = L.circleMarker([apt.latitude, apt.longitude], {
      radius: 10,
      fillColor: color,
      color: '#fff',
      weight: 2,
      fillOpacity: 0.8,
    })
    marker.bindPopup(`
      <strong>${gs.airportCode}</strong> - ${gs.programType}<br/>
      ${gs.airportName ? gs.airportName + '<br/>' : ''}
      ${gs.reason ? 'Reason: ' + gs.reason + '<br/>' : ''}
      ${gs.avgDelayMinutes ? 'Avg Delay: ' + gs.avgDelayMinutes + ' min<br/>' : ''}
      ${gs.maxDelayMinutes ? 'Max Delay: ' + gs.maxDelayMinutes + ' min' : ''}
    `)
    groundStopLayer.value.addLayer(marker)
  }
}

function placeVolcanicAshPolygons() {
  if (!volcanicAshLayer.value) return
  placeGeoJsonPolygons(
    volcanicAshLayer.value,
    volcanicAsh.value,
    '#795548',
    (v) => `
      <strong>Volcanic Ash Advisory</strong><br/>
      ${v.volcanoName ? 'Volcano: ' + v.volcanoName + '<br/>' : ''}
      ${v.firName ? 'FIR: ' + v.firName + '<br/>' : ''}
      ${v.hazard ? 'Hazard: ' + v.hazard + '<br/>' : ''}
      ${v.altitudeLowFt || v.altitudeHighFt ? 'Alt: ' + (v.altitudeLowFt ?? '?') + ' - ' + (v.altitudeHighFt ?? '?') + ' ft<br/>' : ''}
      Valid: ${formatRelativeTime(v.validTimeFrom as string)} to ${formatRelativeTime(v.validTimeTo as string)}
    `,
  )
}

function placeLightningMarkers() {
  if (!lightningLayer.value) return
  lightningLayer.value.clearLayers()

  for (const strike of lightning.value) {
    const marker = L.circleMarker([strike.latitude, strike.longitude], {
      radius: 3,
      fillColor: '#ffeb3b',
      color: '#f57f17',
      weight: 1,
      fillOpacity: 0.9,
    })
    marker.bindPopup(`
      <strong>Lightning Strike</strong><br/>
      ${strike.amplitudeKa ? 'Amplitude: ' + strike.amplitudeKa.toFixed(1) + ' kA<br/>' : ''}
      ${strike.strikeType ? 'Type: ' + strike.strikeType + '<br/>' : ''}
      ${formatRelativeTime(strike.strikeTime)}
    `)
    lightningLayer.value.addLayer(marker)
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

watch(showPireps, (visible) => {
  if (!map.value || !pirepLayer.value) return
  if (visible) {
    map.value.addLayer(pirepLayer.value)
    store.fetchPireps()
  } else {
    map.value.removeLayer(pirepLayer.value)
  }
})

watch(showSigmets, (visible) => {
  if (!map.value || !sigmetLayer.value) return
  if (visible) {
    map.value.addLayer(sigmetLayer.value)
    store.fetchSigmets()
  } else {
    map.value.removeLayer(sigmetLayer.value)
  }
})

watch(showCwas, (visible) => {
  if (!map.value || !cwaLayer.value) return
  if (visible) {
    map.value.addLayer(cwaLayer.value)
    store.fetchCwas()
  } else {
    map.value.removeLayer(cwaLayer.value)
  }
})

watch(showTfrs, (visible) => {
  if (!map.value || !tfrLayer.value) return
  if (visible) {
    map.value.addLayer(tfrLayer.value)
    store.fetchTfrs()
  } else {
    map.value.removeLayer(tfrLayer.value)
  }
})

watch(showGroundStops, (visible) => {
  if (!map.value || !groundStopLayer.value) return
  if (visible) {
    map.value.addLayer(groundStopLayer.value)
    store.fetchGroundStops()
  } else {
    map.value.removeLayer(groundStopLayer.value)
  }
})

watch(showVolcanicAsh, (visible) => {
  if (!map.value || !volcanicAshLayer.value) return
  if (visible) {
    map.value.addLayer(volcanicAshLayer.value)
    store.fetchVolcanicAsh()
  } else {
    map.value.removeLayer(volcanicAshLayer.value)
  }
})

watch(showLightning, (visible) => {
  if (!map.value || !lightningLayer.value) return
  if (visible) {
    map.value.addLayer(lightningLayer.value)
    store.fetchLightning()
  } else {
    map.value.removeLayer(lightningLayer.value)
  }
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
  radarLayer.value = markRaw(L.tileLayer(
    `https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/${product}/{z}/{x}/{y}.png`,
    { attribution: 'NEXRAD radar data &copy; Iowa State University', opacity: radarOpacity.value / 100, maxZoom: 18 },
  ))
  if (wasVisible) map.value.addLayer(radarLayer.value)
})

watch(radarOpacity, (opacity) => {
  if (radarLayer.value) radarLayer.value.setOpacity(opacity / 100)
})

// Re-render markers when data changes (shallow watch — store replaces arrays, not mutates)
watch(airports, placeAirportMarkers)
watch(earthquakes, placeEarthquakeMarkers)
watch(hurricanes, placeHurricaneMarkers)
watch(pireps, placePirepMarkers)
watch(sigmets, placeSigmetPolygons)
watch(cwas, placeCwaPolygons)
watch(tfrs, placeTfrPolygons)
watch(groundStops, placeGroundStopMarkers)
watch(volcanicAsh, placeVolcanicAshPolygons)
watch(lightning, placeLightningMarkers)

onMounted(() => {
  nextTick(() => initMap())
})

onBeforeUnmount(() => {
  if (map.value) {
    map.value.remove()
    map.value = null
  }
  airportLayer.value = null
  earthquakeLayer.value = null
  hurricaneLayer.value = null
  pirepLayer.value = null
  sigmetLayer.value = null
  cwaLayer.value = null
  tfrLayer.value = null
  groundStopLayer.value = null
  volcanicAshLayer.value = null
  lightningLayer.value = null
  radarLayer.value = null
})
</script>

<style scoped>
.unified-map-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.map-search-bar {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  width: 90%;
  max-width: 500px;
}

.map-search-bar .search-input {
  width: 100%;
  padding: 10px 16px;
  font-size: 14px;
  border: 2px solid var(--border-light, #ddd);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  outline: none;
  transition: border-color 0.3s;
  background: var(--bg-card, white);
  color: var(--text-primary, #333);
}

.map-search-bar .search-input:focus {
  border-color: var(--accent);
}

.search-results {
  margin-top: 4px;
  background: var(--bg-card, white);
  border: 1px solid var(--border-light, #ddd);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  max-height: 320px;
  overflow-y: auto;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  cursor: pointer;
  border-bottom: 1px solid var(--border-light, #eee);
  transition: background-color 0.15s;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item:hover {
  background-color: var(--bg-code, #f5f5f5);
}

.result-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.result-info {
  flex: 1;
  min-width: 0;
}

.result-title {
  font-weight: 500;
  font-size: 13px;
  color: var(--text-primary, #333);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-subtitle {
  font-size: 11px;
  color: var(--text-secondary, #666);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
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
  accent-color: var(--accent);
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
  accent-color: var(--accent);
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

.legend-swatch {
  display: inline-block;
  width: 14px;
  height: 10px;
  border-radius: 2px;
  flex-shrink: 0;
}

/* Flight category dots */
.dot-vfr { background: #4caf50; }
.dot-mvfr { background: #2196f3; }
.dot-ifr { background: #ff9800; }
.dot-lifr { background: #f44336; }

/* Magnitude dots */
.dot-mag-low { background: #4caf50; }
.dot-mag-med { background: #ff9800; }
.dot-mag-high { background: #f44336; }
.dot-mag-major { background: #9c27b0; }

/* Storm category dots */
.dot-storm-ts { background: #007bff; }
.dot-storm-cat12 { background: #ffc107; }
.dot-storm-cat34 { background: #f44336; }
.dot-storm-cat5 { background: #9c27b0; }

/* PIREP severity dots */
.dot-pirep-light { background: #4caf50; }
.dot-pirep-mod { background: #ff9800; }
.dot-pirep-sev { background: #f44336; }
.dot-pirep-ext { background: #9c27b0; }

/* Ground stop dots */
.dot-gs-stop { background: #d32f2f; }
.dot-gs-delay { background: #ff9800; }

/* Lightning dot */
.dot-lightning { background: #ffeb3b; }

/* Area swatches */
.swatch-sigmet { background: rgba(244, 67, 54, 0.25); border: 2px solid #f44336; }
.swatch-cwa { background: rgba(255, 152, 0, 0.25); border: 2px solid #ff9800; }
.swatch-tfr { background: rgba(211, 47, 47, 0.2); border: 2px solid #d32f2f; }
.swatch-ash { background: rgba(121, 85, 72, 0.25); border: 2px solid #795548; }

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
  color: var(--accent);
  font-weight: 600;
  margin-bottom: 2px;
}

:deep(.popup-location) {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-bottom: 4px;
}

:deep(.popup-detail-link) {
  display: block;
  margin-top: 8px;
  padding-top: 6px;
  border-top: 1px solid var(--border-light, #e0e0e0);
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
  text-decoration: none;
  cursor: pointer;
}

:deep(.popup-detail-link:hover) {
  text-decoration: underline;
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
