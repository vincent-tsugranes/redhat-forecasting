<template>
  <div class="container">
    <div class="dashboard-header">
      <h1>{{ $t('dashboard.title') }}</h1>
      <div class="header-actions">
        <span v-if="lastRefreshed" class="last-refresh">
          Updated {{ formatRelativeTime(lastRefreshed) }}
        </span>
        <button class="btn-sm btn-icon" :aria-label="$t('dashboard.refreshAriaLabel')" @click="handleRefresh">
          <span aria-hidden="true">🔄</span> {{ $t('dashboard.refreshData') }}
        </button>
      </div>
    </div>

    <!-- Quick airport search -->
    <div class="quick-search-bar">
      <label for="dash-airport-search" class="sr-only">Search airports</label>
      <input
        id="dash-airport-search"
        v-model="quickSearch"
        type="text"
        placeholder="Quick airport lookup (e.g. KJFK)..."
        class="quick-search-input"
        @input="onQuickSearch"
        @keydown.enter="goToFirstResult"
        @keydown.escape="quickSearchResults = []"
      />
      <ul v-if="quickSearchResults.length > 0 && quickSearch" class="quick-search-results">
        <li
          v-for="apt in quickSearchResults"
          :key="apt.id"
          class="quick-search-item"
          @click="goToAirport(apt.airportCode!)"
        >
          <strong>{{ apt.airportCode }}</strong> {{ apt.name }}
        </li>
      </ul>
    </div>

    <DataStatusCard />
    <FavoriteWeatherCards />

    <DashboardSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="!loading" class="stats-bar">
      <span class="stat-chip">
        <span aria-hidden="true">✈️</span> <strong>{{ airports.length.toLocaleString() }}</strong> Airports
      </span>
      <span class="stat-chip" v-if="hurricanes.length > 0" :class="{ 'stat-alert': true }">
        <span aria-hidden="true">🌀</span> <strong>{{ hurricanes.length }}</strong> Storms
      </span>
      <span class="stat-chip">
        <span aria-hidden="true">🌍</span> <strong>{{ earthquakes.length }}</strong> Quakes
      </span>
      <span class="stat-chip" v-if="alerts.length > 0" :class="{ 'stat-alert': true }">
        <span aria-hidden="true">⚠️</span> <strong>{{ alerts.length }}</strong> Alerts
      </span>
      <span class="stat-chip">
        <span aria-hidden="true">📋</span> <strong>{{ pireps.length }}</strong> PIREPs
      </span>
      <span class="stat-chip">
        <span aria-hidden="true">🚨</span> <strong>{{ sigmets.length }}</strong> SIGMETs
      </span>
      <span class="stat-chip" v-if="tfrs.length > 0">
        <span aria-hidden="true">🚫</span> <strong>{{ tfrs.length }}</strong> TFRs
      </span>
      <span class="stat-chip" v-if="cwas.length > 0">
        <span aria-hidden="true">📡</span> <strong>{{ cwas.length }}</strong> CWAs
      </span>
      <span class="stat-chip" v-if="groundStops.length > 0" :class="{ 'stat-alert': true }">
        <span aria-hidden="true">🛑</span> <strong>{{ groundStops.length }}</strong> Ground Stops
      </span>
      <span class="stat-chip" v-if="volcanicAsh.length > 0" :class="{ 'stat-alert': true }">
        <span aria-hidden="true">🌋</span> <strong>{{ volcanicAsh.length }}</strong> Volcanic Ash
      </span>
      <span class="stat-chip" v-if="lightning.length > 0">
        <span aria-hidden="true">⚡</span> <strong>{{ lightning.length }}</strong> Lightning
      </span>
      <span class="stat-chip" v-if="delayedAirports > 0" :class="{ 'stat-alert': true }">
        <span aria-hidden="true">⏱️</span> <strong>{{ delayedAirports }}</strong> Delays
      </span>
    </div>

    <div v-if="!loading" class="dashboard-grid">
      <div class="dashboard-main">

        <!-- Weather alerts summary -->
        <div v-if="alerts.length > 0" class="card alerts-summary-card">
          <div class="card-header-row">
            <h2><span aria-hidden="true">⚠️</span> Active Alerts</h2>
            <span class="alert-count-badge" :class="'severity-' + highestAlertSeverity">{{ alerts.length }}</span>
          </div>
          <div class="alerts-summary-list">
            <div
              v-for="a in alerts.slice(0, 5)"
              :key="a.id"
              class="alert-summary-item"
              :class="'alert-' + (a.severity || 'unknown').toLowerCase()"
            >
              <div class="alert-summary-header">
                <span class="alert-event">{{ a.event }}</span>
                <span v-if="a.severity" class="alert-sev-badge" :class="'sev-' + a.severity.toLowerCase()">{{ a.severity }}</span>
              </div>
              <div v-if="a.headline" class="alert-headline">{{ a.headline }}</div>
              <div v-if="a.areaDesc" class="alert-area">{{ a.areaDesc }}</div>
            </div>
            <div v-if="alerts.length > 5" class="alerts-more">
              +{{ alerts.length - 5 }} more alerts
            </div>
          </div>
        </div>

        <!-- Airport delays & ground stops -->
        <div v-if="delayedAirports > 0 || groundStops.length > 0" class="card">
          <div class="card-header-row">
            <h2><span aria-hidden="true">⏱️</span> Delays &amp; Ground Stops</h2>
            <router-link to="/delays">
              <button class="btn-sm btn-outline">View All</button>
            </router-link>
          </div>
          <!-- Ground stops -->
          <div v-if="groundStops.length > 0" class="gs-list">
            <div v-for="gs in groundStops" :key="gs.id" class="gs-item">
              <span class="gs-icon" aria-hidden="true">🛑</span>
              <strong>{{ gs.airportCode }}</strong>
              <span class="gs-label">Ground Stop</span>
              <span v-if="gs.reason" class="gs-reason">{{ gs.reason }}</span>
            </div>
          </div>
          <!-- Delayed airports -->
          <div v-if="delayedAirportsList.length > 0" class="delay-list">
            <div v-for="d in delayedAirportsList" :key="d.id" class="delay-item">
              <router-link :to="{ path: '/airports', query: { code: d.airportCode } }" class="delay-code">{{ d.airportCode }}</router-link>
              <span class="delay-type-label">{{ d.delayType }}</span>
              <span v-if="d.avgDelayMinutes" class="delay-mins">{{ d.avgDelayMinutes }} min</span>
              <span v-if="d.trend" class="trend-badge" :class="'trend-' + d.trend">{{ d.trend }}</span>
            </div>
          </div>
        </div>

        <!-- Recent earthquakes table -->
        <div class="card">
          <div class="card-header-row">
            <h2><span aria-hidden="true">🌍</span> {{ $t('dashboard.earthquakeMonitor') }}</h2>
            <router-link to="/earthquakes">
              <button class="btn-sm btn-outline">{{ $t('dashboard.viewEarthquakes') }}</button>
            </router-link>
          </div>
          <div v-if="earthquakes.length > 0" class="table-wrapper">
            <table class="data-table" aria-label="Recent earthquakes">
              <thead>
                <tr>
                  <th>Mag</th>
                  <th>Location</th>
                  <th>Depth</th>
                  <th>Time</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="eq in earthquakes.slice(0, 5)" :key="eq.id">
                  <td>
                    <span class="magnitude-badge" :class="getMagnitudeClass(eq.magnitude)">M{{ eq.magnitude }}</span>
                  </td>
                  <td class="td-truncate">{{ eq.place }}</td>
                  <td>{{ eq.depthKm }} km</td>
                  <td class="td-nowrap">{{ formatRelativeTime(eq.eventTime) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="empty-state">No recent earthquakes</p>
        </div>

        <!-- Active storms -->
        <div class="card">
          <div class="card-header-row">
            <h2><span aria-hidden="true">🌀</span> {{ $t('dashboard.hurricaneTracking') }}</h2>
            <router-link to="/hurricanes">
              <button class="btn-sm btn-outline">{{ $t('dashboard.viewHurricanes') }}</button>
            </router-link>
          </div>
          <div v-if="hurricanes.length > 0" class="table-wrapper">
            <table class="data-table" aria-label="Active tropical storms">
              <thead>
                <tr>
                  <th>Storm</th>
                  <th>Category</th>
                  <th>Winds</th>
                  <th>Pressure</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="storm in hurricanes" :key="storm.id">
                  <td class="storm-name-cell">{{ storm.stormName || storm.stormId }}</td>
                  <td>
                    <span class="category-badge" :class="'cat-' + (storm.category ?? 0)">
                      {{ storm.category === 0 ? 'TS' : 'Cat ' + storm.category }}
                    </span>
                  </td>
                  <td>{{ storm.maxSustainedWindsMph }} mph</td>
                  <td>{{ storm.minCentralPressureMb }} mb</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="empty-state">No active tropical systems</p>
        </div>

        <!-- PIREP hazard summary -->
        <div v-if="pireps.length > 0" class="card">
          <div class="card-header-row">
            <h2><span aria-hidden="true">📋</span> PIREP Hazard Summary</h2>
            <router-link to="/pireps">
              <button class="btn-sm btn-outline">View PIREPs</button>
            </router-link>
          </div>
          <div class="hazard-summary">
            <div v-if="pirepHazards.turbulence > 0" class="hazard-chip hazard-turb">
              <span class="hazard-count">{{ pirepHazards.turbulence }}</span>
              <span>Turbulence</span>
            </div>
            <div v-if="pirepHazards.icing > 0" class="hazard-chip hazard-ice">
              <span class="hazard-count">{{ pirepHazards.icing }}</span>
              <span>Icing</span>
            </div>
            <div v-if="pirepHazards.weather > 0" class="hazard-chip hazard-wx">
              <span class="hazard-count">{{ pirepHazards.weather }}</span>
              <span>Weather</span>
            </div>
            <div class="hazard-chip hazard-total">
              <span class="hazard-count">{{ pireps.length }}</span>
              <span>Total Reports</span>
            </div>
          </div>
        </div>

        <!-- TFR breakdown -->
        <div v-if="tfrs.length > 0" class="card">
          <div class="card-header-row">
            <h2><span aria-hidden="true">🚫</span> Active TFRs</h2>
            <router-link to="/tfrs">
              <button class="btn-sm btn-outline">View TFRs</button>
            </router-link>
          </div>
          <div class="tfr-breakdown">
            <div v-for="(count, type) in tfrByType" :key="type" class="tfr-type-chip">
              <span class="tfr-type-count">{{ count }}</span>
              <span class="tfr-type-label">{{ type }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right column: map + space weather -->
      <div class="dashboard-sidebar">
        <!-- Space weather card -->
        <div v-if="spaceWeather" class="card space-weather-card">
          <h3><span aria-hidden="true">☀️</span> Space Weather</h3>
          <div class="sw-grid">
            <div class="sw-item">
              <div class="sw-label">Kp Index</div>
              <div class="sw-value" :class="'kp-' + getKpClass(spaceWeather.kpIndex)">{{ spaceWeather.kpIndex }}</div>
              <div class="sw-sublabel">{{ spaceWeather.kpLevel }}</div>
            </div>
            <div v-if="spaceWeather.solarWindSpeed" class="sw-item">
              <div class="sw-label">Solar Wind</div>
              <div class="sw-value">{{ spaceWeather.solarWindSpeed }}</div>
              <div class="sw-sublabel">km/s</div>
            </div>
            <div v-if="spaceWeather.geomagneticStormLevel" class="sw-item">
              <div class="sw-label">Geomagnetic</div>
              <div class="sw-value sw-geo">{{ spaceWeather.geomagneticStormLevel }}</div>
            </div>
            <div v-if="spaceWeather.auroraChance" class="sw-item">
              <div class="sw-label">Aurora</div>
              <div class="sw-value">{{ spaceWeather.auroraChance }}</div>
            </div>
          </div>
          <div v-if="spaceWeather.alerts.length > 0" class="sw-alerts">
            <div v-for="(sa, i) in spaceWeather.alerts.slice(0, 2)" :key="i" class="sw-alert-item">
              {{ sa.message }}
            </div>
          </div>
        </div>

        <div class="card map-card">
          <h2><span aria-hidden="true">🗺️</span> {{ $t('dashboard.globalMap') }}</h2>
          <div class="dashboard-map">
            <ErrorBoundary>
              <UnifiedMap />
            </ErrorBoundary>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import { formatRelativeTime } from '../utils/dateUtils'
import UnifiedMap from '../components/UnifiedMap.vue'
import ErrorBoundary from '../components/ErrorBoundary.vue'
import DataStatusCard from '../components/DataStatusCard.vue'
import DashboardSkeleton from '../components/skeletons/DashboardSkeleton.vue'
import FavoriteWeatherCards from '../components/FavoriteWeatherCards.vue'

const router = useRouter()
const store = useWeatherStore()
const {
  airports,
  hurricanes,
  earthquakes,
  alerts,
  pireps,
  sigmets,
  tfrs,
  cwas,
  delays,
  groundStops,
  volcanicAsh,
  lightning,
  spaceWeather,
  airportsLoading: loading,
  airportsError: error,
} = storeToRefs(store)
const toast = useToast()

const lastRefreshed = ref<string | null>(null)
const quickSearch = ref('')
const quickSearchResults = ref<{ id: number; airportCode?: string; name: string }[]>([])
let autoRefreshInterval: ReturnType<typeof setInterval> | null = null

const delayedAirports = computed(() => delays.value.filter(d => d.isDelayed).length)
const delayedAirportsList = computed(() => delays.value.filter(d => d.isDelayed).slice(0, 8))

const highestAlertSeverity = computed(() => {
  const rank: Record<string, number> = { extreme: 4, severe: 3, moderate: 2, minor: 1 }
  let highest = 'unknown'
  let highestRank = 0
  for (const a of alerts.value) {
    const sev = (a.severity || 'unknown').toLowerCase()
    if ((rank[sev] || 0) > highestRank) {
      highestRank = rank[sev] || 0
      highest = sev
    }
  }
  return highest
})

// PIREP hazard aggregation
const pirepHazards = computed(() => {
  let turbulence = 0
  let icing = 0
  let weather = 0
  for (const p of pireps.value) {
    if (p.turbulenceIntensity) turbulence++
    if (p.icingIntensity) icing++
    if (p.weatherConditions) weather++
  }
  return { turbulence, icing, weather }
})

// TFR type breakdown
const tfrByType = computed(() => {
  const counts: Record<string, number> = {}
  for (const t of tfrs.value) {
    const type = t.tfrType || 'Other'
    counts[type] = (counts[type] || 0) + 1
  }
  return counts
})

// Quick airport search
function onQuickSearch() {
  if (!quickSearch.value || quickSearch.value.length < 1) {
    quickSearchResults.value = []
    return
  }
  const q = quickSearch.value.toLowerCase()
  quickSearchResults.value = airports.value
    .filter(a => a.airportCode?.toLowerCase().includes(q) || a.name?.toLowerCase().includes(q))
    .slice(0, 8)
}

function goToAirport(code: string) {
  quickSearch.value = ''
  quickSearchResults.value = []
  router.push({ path: '/airports', query: { code } })
}

function goToFirstResult() {
  if (quickSearchResults.value.length > 0 && quickSearchResults.value[0].airportCode) {
    goToAirport(quickSearchResults.value[0].airportCode)
  }
}

function getMagnitudeClass(magnitude: number) {
  if (magnitude >= 7) return 'mag-major'
  if (magnitude >= 5) return 'mag-strong'
  if (magnitude >= 4) return 'mag-moderate'
  return 'mag-light'
}

function getKpClass(kp: number): string {
  if (kp >= 7) return 'extreme'
  if (kp >= 5) return 'storm'
  if (kp >= 4) return 'active'
  return 'quiet'
}

async function handleRefresh() {
  try {
    await store.fetchAirports()
    lastRefreshed.value = new Date().toISOString()
    toast.success('Data refreshed successfully')
  } catch {
    toast.error('Failed to refresh data')
  }
}

onMounted(async () => {
  // Critical data first
  await Promise.all([
    store.fetchAirports(),
    store.fetchEarthquakes(),
    store.fetchHurricanes(),
  ])
  lastRefreshed.value = new Date().toISOString()

  // Secondary data
  store.fetchAlerts()
  store.fetchPireps()
  store.fetchSigmets()
  store.fetchDelays()
  store.fetchSpaceWeather()

  // Tertiary data
  store.fetchTfrs()
  store.fetchCwas()
  store.fetchGroundStops()
  store.fetchVolcanicAsh()
  store.fetchLightning()

  // Auto-refresh every 5 minutes
  autoRefreshInterval = setInterval(() => {
    store.fetchAlerts()
    store.fetchDelays()
    store.fetchGroundStops()
    store.fetchSpaceWeather()
    lastRefreshed.value = new Date().toISOString()
  }, 5 * 60_000)
})

onUnmounted(() => {
  if (autoRefreshInterval) {
    clearInterval(autoRefreshInterval)
    autoRefreshInterval = null
  }
})
</script>

<style scoped>
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.dashboard-header h1 {
  margin-bottom: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.last-refresh {
  font-size: 11px;
  color: var(--text-muted, #999);
}

/* Quick search */
.quick-search-bar {
  position: relative;
  margin-bottom: 12px;
}

.quick-search-input {
  width: 100%;
  padding: 8px 14px;
  font-size: 14px;
  border: 1px solid var(--border-color, #ddd);
  border-radius: 8px;
  outline: none;
  background: var(--bg-input, #fff);
  color: var(--text-primary, #333);
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.quick-search-input:focus {
  border-color: var(--accent);
}

.quick-search-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 2px;
  background: var(--bg-card, white);
  border: 1px solid var(--border-color, #ddd);
  border-radius: 6px;
  box-shadow: 0 4px 12px var(--shadow-md, rgba(0, 0, 0, 0.15));
  z-index: 100;
  list-style: none;
  padding: 0;
  margin: 2px 0 0;
  max-height: 240px;
  overflow-y: auto;
}

.quick-search-item {
  padding: 6px 12px;
  cursor: pointer;
  font-size: 13px;
  border-bottom: 1px solid var(--border-light, #eee);
  transition: background 0.15s;
}

.quick-search-item:last-child { border-bottom: none; }
.quick-search-item:hover { background: var(--bg-code, #f5f5f5); }
.quick-search-item strong { color: var(--accent); margin-right: 6px; }

/* Compact stats bar */
.stats-bar {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  padding: 2px 0;
  flex-wrap: wrap;
}

.stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 4px 8px;
  background: var(--bg-card, white);
  border-radius: 12px;
  font-size: 11px;
  color: var(--text-secondary, #666);
  white-space: nowrap;
  box-shadow: 0 1px 3px var(--shadow, rgba(0, 0, 0, 0.08));
}

.stat-chip strong {
  color: var(--text-primary, #333);
  font-size: 12px;
}

.stat-chip.stat-alert {
  background: var(--alert-bg);
  color: var(--alert-text);
}

.stat-chip.stat-alert strong {
  color: var(--alert-text);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.card-header-row h2 {
  margin-bottom: 0;
  font-size: 1.1rem;
}

.empty-state {
  color: var(--text-secondary, #666);
  text-align: center;
  padding: 12px;
  font-style: italic;
}

/* Alerts summary */
.alerts-summary-card {
  border-left: 4px solid var(--severity-strong, #e53935);
}

.alert-count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
  color: white;
}

.severity-extreme { background: #b71c1c; }
.severity-severe { background: var(--aging-color, #ff9800); }
.severity-moderate { background: var(--color-warning, #f0ad4e); }
.severity-minor { background: #0d47a1; }
.severity-unknown { background: var(--text-muted, #999); }

.alerts-summary-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.alert-summary-item {
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 13px;
  border-left: 3px solid var(--border-color, #ddd);
}

.alert-extreme { border-left-color: #b71c1c; background: rgba(183, 28, 28, 0.05); }
.alert-severe { border-left-color: var(--aging-color, #ff9800); background: rgba(255, 152, 0, 0.05); }
.alert-moderate { border-left-color: var(--color-warning, #f0ad4e); background: rgba(240, 173, 78, 0.05); }
.alert-minor { border-left-color: #0d47a1; background: rgba(13, 71, 161, 0.05); }

.alert-summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
}

.alert-event {
  font-weight: 600;
}

.alert-sev-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 3px;
  color: white;
  text-transform: uppercase;
}

.sev-extreme { background: #b71c1c; }
.sev-severe { background: var(--aging-color, #ff9800); }
.sev-moderate { background: var(--color-warning, #f0ad4e); color: #333; }
.sev-minor { background: #0d47a1; }

.alert-headline {
  font-size: 12px;
  color: var(--text-secondary, #666);
  line-height: 1.3;
}

.alert-area {
  font-size: 11px;
  color: var(--text-muted, #999);
  margin-top: 2px;
}

.alerts-more {
  text-align: center;
  font-size: 12px;
  color: var(--text-muted, #999);
  padding: 4px;
}

/* Delays & ground stops */
.gs-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
}

.gs-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 6px;
  background: rgba(183, 28, 28, 0.06);
  font-size: 13px;
}

.gs-icon { font-size: 14px; }
.gs-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--severity-high, #e53935);
  text-transform: uppercase;
}

.gs-reason {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-left: auto;
}

.delay-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.delay-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 6px;
  background: var(--bg-code, #f5f5f5);
  font-size: 12px;
}

.delay-code {
  font-weight: 700;
  font-family: monospace;
  color: var(--accent);
  text-decoration: none;
}

.delay-code:hover { text-decoration: underline; }

.delay-type-label {
  color: var(--text-secondary, #666);
}

.delay-mins {
  font-weight: 600;
  color: var(--text-primary, #333);
}

.trend-badge {
  display: inline-block;
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 600;
  text-transform: capitalize;
  color: white;
}

.trend-increasing { background: var(--severity-high, #e53935); }
.trend-decreasing { background: var(--color-success, #43a047); }
.trend-stable { background: var(--text-muted, #999); }

/* PIREP hazard summary */
.hazard-summary {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.hazard-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  background: var(--bg-code, #f5f5f5);
}

.hazard-count {
  font-weight: 700;
  font-size: 16px;
}

.hazard-turb { color: var(--aging-color, #ff9800); }
.hazard-turb .hazard-count { color: var(--aging-color, #ff9800); }
.hazard-ice { color: #0d47a1; }
.hazard-ice .hazard-count { color: #0d47a1; }
.hazard-wx { color: var(--text-secondary, #666); }
.hazard-total { color: var(--text-muted, #999); }

/* TFR breakdown */
.tfr-breakdown {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tfr-type-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 8px;
  background: var(--bg-code, #f5f5f5);
  font-size: 12px;
}

.tfr-type-count {
  font-weight: 700;
  font-size: 16px;
  color: var(--severity-high, #e53935);
}

.tfr-type-label {
  color: var(--text-secondary, #666);
}

/* Magnitude badges */
.magnitude-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
  color: white;
  font-size: 12px;
  white-space: nowrap;
}

.mag-light { background: var(--severity-light); }
.mag-moderate { background: var(--severity-moderate); }
.mag-strong { background: var(--severity-strong); }
.mag-major { background: var(--severity-major); }

/* Category badges */
.category-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
  color: white;
  font-size: 12px;
  white-space: nowrap;
}

.cat-0 { background: var(--storm-ts); }
.cat-1 { background: var(--storm-cat1); color: var(--storm-cat1-text); }
.cat-2 { background: var(--storm-cat2); }
.cat-3 { background: var(--storm-cat3); }
.cat-4 { background: var(--storm-cat4); }
.cat-5 { background: var(--storm-cat5); }

.storm-name-cell {
  font-weight: 600;
  color: var(--accent);
}

.td-truncate {
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.td-nowrap {
  white-space: nowrap;
}

/* Space weather card */
.space-weather-card h3 {
  margin-bottom: 8px;
  font-size: 1rem;
}

.sw-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.sw-item {
  text-align: center;
  padding: 8px 6px;
  background: var(--bg-code, #f9f9f9);
  border-radius: 6px;
}

.sw-label {
  font-size: 10px;
  text-transform: uppercase;
  color: var(--text-muted, #999);
  margin-bottom: 2px;
}

.sw-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary, #333);
}

.sw-sublabel {
  font-size: 11px;
  color: var(--text-secondary, #666);
}

.sw-geo {
  font-size: 14px;
}

.kp-quiet { color: var(--color-success, #43a047); }
.kp-active { color: var(--color-warning, #f0ad4e); }
.kp-storm { color: var(--aging-color, #ff9800); }
.kp-extreme { color: var(--severity-high, #e53935); }

.sw-alerts {
  margin-top: 8px;
  border-top: 1px solid var(--border-light, #eee);
  padding-top: 6px;
}

.sw-alert-item {
  font-size: 11px;
  color: var(--text-secondary, #666);
  padding: 3px 0;
  line-height: 1.3;
}

/* Sidebar map */
.map-card {
  position: sticky;
  top: 20px;
}

.map-card h2 {
  margin-bottom: 8px;
  font-size: 1.1rem;
}

.dashboard-map {
  height: 500px;
  border-radius: 8px;
  overflow: hidden;
}

@media (max-width: 1024px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-map {
    height: 400px;
  }
}

@media (max-width: 768px) {
  .stat-chip {
    padding: 3px 6px;
    font-size: 10px;
  }

  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .header-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>
