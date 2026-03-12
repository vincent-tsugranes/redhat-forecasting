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
      <router-link to="/airports" class="stat-chip-link">
        <span class="stat-chip">
          <span aria-hidden="true">✈️</span> <strong>{{ airports.length.toLocaleString() }}</strong> Airports
        </span>
      </router-link>
      <router-link v-if="hurricanes.length > 0" to="/hurricanes" class="stat-chip-link">
        <span class="stat-chip stat-alert">
          <span aria-hidden="true">🌀</span> <strong>{{ hurricanes.length }}</strong> Storms
        </span>
      </router-link>
      <router-link to="/earthquakes" class="stat-chip-link">
        <span class="stat-chip">
          <span aria-hidden="true">🌍</span> <strong>{{ earthquakes.length }}</strong> Quakes
        </span>
      </router-link>
      <span class="stat-chip" v-if="alerts.length > 0" :class="{ 'stat-alert': true }">
        <span aria-hidden="true">⚠️</span> <strong>{{ alerts.length }}</strong> Alerts
      </span>
      <router-link to="/pireps" class="stat-chip-link">
        <span class="stat-chip">
          <span aria-hidden="true">📋</span> <strong>{{ pireps.length }}</strong> PIREPs
        </span>
      </router-link>
      <router-link to="/sigmets" class="stat-chip-link">
        <span class="stat-chip" :class="{ 'stat-alert': worstSigmetSeverity === 'severe' }">
          <span aria-hidden="true">🚨</span> <strong>{{ sigmets.length }}</strong> SIGMETs
        </span>
      </router-link>
      <router-link v-if="tfrs.length > 0" to="/tfrs" class="stat-chip-link">
        <span class="stat-chip">
          <span aria-hidden="true">🚫</span> <strong>{{ tfrs.length }}</strong> TFRs
        </span>
      </router-link>
      <router-link v-if="cwas.length > 0" to="/cwas" class="stat-chip-link">
        <span class="stat-chip">
          <span aria-hidden="true">📡</span> <strong>{{ cwas.length }}</strong> CWAs
        </span>
      </router-link>
      <router-link v-if="groundStops.length > 0" to="/ground-stops" class="stat-chip-link">
        <span class="stat-chip stat-alert">
          <span aria-hidden="true">🛑</span> <strong>{{ groundStops.length }}</strong> Ground Stops
        </span>
      </router-link>
      <router-link v-if="volcanicAsh.length > 0" to="/volcanic-ash" class="stat-chip-link">
        <span class="stat-chip stat-alert">
          <span aria-hidden="true">🌋</span> <strong>{{ volcanicAsh.length }}</strong> Volcanic Ash
        </span>
      </router-link>
      <router-link v-if="lightning.length > 0" to="/lightning" class="stat-chip-link">
        <span class="stat-chip">
          <span aria-hidden="true">⚡</span> <strong>{{ lightning.length }}</strong> Lightning
        </span>
      </router-link>
      <router-link v-if="delayedAirports > 0" to="/delays" class="stat-chip-link">
        <span class="stat-chip stat-alert">
          <span aria-hidden="true">⏱️</span> <strong>{{ delayedAirports }}</strong> Delays
        </span>
      </router-link>
    </div>

    <!-- What's happening now -->
    <div v-if="!loading && situationSummary" class="card situation-card">
      <h2><span aria-hidden="true">📡</span> Situation Summary</h2>
      <p class="situation-text">{{ situationSummary }}</p>
    </div>

    <div v-if="!loading" class="dashboard-grid">
      <div class="dashboard-main">

        <!-- Weather alerts summary -->
        <div v-if="alerts.length > 0" class="card alerts-summary-card">
          <div class="card-header-row">
            <h2><span aria-hidden="true">⚠️</span> Active Alerts</h2>
            <span class="alert-count-badge" :class="'severity-' + highestAlertSeverity">{{ filteredAlerts.length }}/{{ alerts.length }}</span>
          </div>
          <div class="alert-filter-row">
            <button
              v-for="sev in severityOptions"
              :key="sev.value"
              class="alert-filter-btn"
              :class="{ 'filter-active': alertMinSeverity === sev.value, ['sev-' + sev.value]: true }"
              @click="alertMinSeverity = sev.value"
            >{{ sev.label }}</button>
          </div>
          <div class="alerts-summary-list">
            <div
              v-for="a in filteredAlerts.slice(0, 5)"
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
            <div v-if="filteredAlerts.length > 5" class="alerts-more">
              +{{ filteredAlerts.length - 5 }} more alerts
            </div>
            <div v-if="filteredAlerts.length === 0" class="empty-state">
              No {{ alertMinSeverity }}+ alerts active
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
                  <th>Basin</th>
                  <th>Category</th>
                  <th>Winds</th>
                  <th>Pressure</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="storm in hurricanes" :key="storm.id">
                  <td class="storm-name-cell">{{ storm.stormName || storm.stormId }}</td>
                  <td><span v-if="storm.basin" class="basin-tag">{{ storm.basin }}</span></td>
                  <td>
                    <span class="category-badge" :class="'cat-' + (storm.category ?? 0)">
                      {{ storm.category === 0 ? 'TS' : stormTypeName(storm.basin) + ' ' + storm.category }}
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

        <!-- SIGMET/CWA hazard list -->
        <div v-if="sigmets.length > 0 || cwas.length > 0" class="card">
          <div class="card-header-row">
            <h2>
              <span aria-hidden="true">🚨</span> Aviation Hazards
              <span v-if="worstSigmetSeverity === 'severe'" class="hazard-severity-dot severity-severe-dot"></span>
              <span v-else-if="sigmets.some(s => s.sigmetType === 'CONVECTIVE')" class="hazard-severity-dot severity-convective-dot"></span>
            </h2>
            <router-link to="/sigmets">
              <button class="btn-sm btn-outline">View All</button>
            </router-link>
          </div>
          <div class="aviation-hazard-list">
            <div v-for="s in sigmets.slice(0, 5)" :key="s.id" class="aviation-hazard-item" :class="{ 'hazard-convective': s.sigmetType === 'CONVECTIVE' }">
              <span class="hazard-type-badge" :class="s.sigmetType === 'CONVECTIVE' ? 'badge-convective' : 'badge-sigmet'">{{ s.sigmetType === 'CONVECTIVE' ? 'CONV' : 'SIGMET' }}</span>
              <span class="hazard-detail">{{ s.hazard || s.sigmetType }}</span>
              <span v-if="s.severity" class="hazard-sev" :class="'sev-' + s.severity.toLowerCase()">{{ s.severity }}</span>
              <span v-if="s.firName" class="hazard-fir">{{ s.firName }}</span>
            </div>
            <div v-for="c in cwas.slice(0, 3)" :key="c.id" class="aviation-hazard-item">
              <span class="hazard-type-badge badge-cwa">CWA</span>
              <span class="hazard-detail">{{ c.hazard || 'Advisory' }}</span>
              <span class="hazard-fir">{{ c.artcc }}</span>
            </div>
            <div v-if="sigmets.length > 5 || cwas.length > 3" class="alerts-more">
              +{{ Math.max(0, sigmets.length - 5) + Math.max(0, cwas.length - 3) }} more
            </div>
          </div>
        </div>

        <!-- Volcanic ash detail -->
        <div v-if="volcanicAsh.length > 0" class="card">
          <div class="card-header-row">
            <h2><span aria-hidden="true">🌋</span> Volcanic Ash Advisories</h2>
            <router-link to="/volcanic-ash">
              <button class="btn-sm btn-outline">View All</button>
            </router-link>
          </div>
          <div class="volcanic-ash-list">
            <div v-for="va in volcanicAsh.slice(0, 5)" :key="va.id" class="va-item">
              <span class="va-name">{{ va.volcanoName || 'Unknown Volcano' }}</span>
              <span v-if="va.firName" class="va-fir">{{ va.firName }}</span>
              <span v-if="va.severity" class="va-sev" :class="'sev-' + va.severity.toLowerCase()">{{ va.severity }}</span>
              <span v-if="va.altitudeHighFt" class="va-alt">FL{{ Math.round(va.altitudeHighFt / 100) }}</span>
            </div>
          </div>
        </div>

        <!-- Recent activity feed -->
        <div v-if="activityFeed.length > 0" class="card">
          <h2><span aria-hidden="true">🕐</span> Recent Activity</h2>
          <div class="activity-feed">
            <div v-for="item in activityFeed" :key="item.key" class="activity-item">
              <span class="activity-icon" aria-hidden="true">{{ item.icon }}</span>
              <span class="activity-text">{{ item.text }}</span>
              <span class="activity-time">{{ item.timeAgo }}</span>
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
const alertMinSeverity = ref<string>('extreme')
const severityOptions = [
  { value: 'extreme', label: 'Extreme' },
  { value: 'severe', label: 'Severe+' },
  { value: 'moderate', label: 'Moderate+' },
  { value: 'minor', label: 'Minor+' },
  { value: 'all', label: 'All' },
]
const quickSearch = ref('')
const quickSearchResults = ref<{ id: number; airportCode?: string; name: string }[]>([])
let autoRefreshInterval: ReturnType<typeof setInterval> | null = null

const delayedAirports = computed(() => delays.value.filter(d => d.isDelayed).length)
const delayedAirportsList = computed(() => delays.value.filter(d => d.isDelayed).slice(0, 8))

const highestAlertSeverity = computed(() => {
  let highest = 'unknown'
  let highestRank = 0
  for (const a of alerts.value) {
    const sev = (a.severity || 'unknown').toLowerCase()
    if ((severityRank[sev] || 0) > highestRank) {
      highestRank = severityRank[sev] || 0
      highest = sev
    }
  }
  return highest
})

const severityRank: Record<string, number> = { extreme: 4, severe: 3, moderate: 2, minor: 1 }

const filteredAlerts = computed(() => {
  if (alertMinSeverity.value === 'all') return alerts.value
  const minRank = severityRank[alertMinSeverity.value] || 0
  return alerts.value.filter(a => {
    const rank = severityRank[(a.severity || '').toLowerCase()] || 0
    return rank >= minRank
  })
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

// Worst SIGMET severity for chip color
const worstSigmetSeverity = computed(() => {
  for (const s of sigmets.value) {
    if (s.severity?.toLowerCase() === 'severe') return 'severe'
    if (s.sigmetType === 'CONVECTIVE') return 'convective'
  }
  return 'normal'
})

// Situation summary — auto-generated natural-language overview
const situationSummary = computed(() => {
  const parts: string[] = []

  if (hurricanes.value.length > 0) {
    const names = hurricanes.value.map(s => s.stormName || s.stormId).slice(0, 3).join(', ')
    parts.push(`${hurricanes.value.length} active tropical system${hurricanes.value.length > 1 ? 's' : ''} (${names})`)
  }

  const delayed = delays.value.filter(d => d.isDelayed).length
  if (delayed > 0) {
    parts.push(`${delayed} airport${delayed > 1 ? 's' : ''} reporting delays`)
  }

  if (groundStops.value.length > 0) {
    const codes = groundStops.value.map(g => g.airportCode).slice(0, 3).join(', ')
    parts.push(`ground stops at ${codes}`)
  }

  if (alerts.value.length > 0) {
    const extreme = alerts.value.filter(a => a.severity?.toLowerCase() === 'extreme').length
    const severe = alerts.value.filter(a => a.severity?.toLowerCase() === 'severe').length
    if (extreme > 0) parts.push(`${extreme} extreme weather alert${extreme > 1 ? 's' : ''}`)
    else if (severe > 0) parts.push(`${severe} severe weather alert${severe > 1 ? 's' : ''}`)
    else parts.push(`${alerts.value.length} active weather alert${alerts.value.length > 1 ? 's' : ''}`)
  }

  const convSigmets = sigmets.value.filter(s => s.sigmetType === 'CONVECTIVE').length
  if (convSigmets > 0) {
    parts.push(`${convSigmets} convective SIGMET${convSigmets > 1 ? 's' : ''}`)
  } else if (sigmets.value.length > 0) {
    parts.push(`${sigmets.value.length} active SIGMET${sigmets.value.length > 1 ? 's' : ''}`)
  }

  const modTurb = pireps.value.filter(p => {
    const t = (p.turbulenceIntensity || '').toUpperCase()
    return t === 'MOD' || t === 'SEV' || t === 'MOD-SEV' || t === 'EXTRM'
  }).length
  if (modTurb > 0) {
    parts.push(`${modTurb} moderate-or-worse turbulence report${modTurb > 1 ? 's' : ''}`)
  }

  if (earthquakes.value.length > 0) {
    const significant = earthquakes.value.filter(e => e.magnitude >= 5).length
    if (significant > 0) parts.push(`${significant} M5+ earthquake${significant > 1 ? 's' : ''} in the last 24h`)
    else parts.push(`${earthquakes.value.length} earthquake${earthquakes.value.length > 1 ? 's' : ''} recorded`)
  }

  if (volcanicAsh.value.length > 0) {
    const names = volcanicAsh.value.map(v => v.volcanoName).filter(Boolean).slice(0, 2).join(', ')
    parts.push(`volcanic ash advisories${names ? ' (' + names + ')' : ''}`)
  }

  if (parts.length === 0) return 'No significant weather or hazard activity at this time.'
  return parts.join('. ') + '.'
})

// Recent activity feed — aggregated from fetchedAt timestamps across data sources
const activityFeed = computed(() => {
  const items: { key: string; icon: string; text: string; time: number; timeAgo: string }[] = []

  for (const a of alerts.value.slice(0, 3)) {
    if (a.effective) {
      items.push({
        key: `alert-${a.id}`,
        icon: '⚠️',
        text: `${a.event}${a.areaDesc ? ' - ' + a.areaDesc.substring(0, 60) : ''}`,
        time: new Date(a.effective).getTime(),
        timeAgo: formatRelativeTime(a.effective),
      })
    }
  }

  for (const s of hurricanes.value) {
    if (s.fetchedAt) {
      items.push({
        key: `storm-${s.id}`,
        icon: '🌀',
        text: `${s.stormName || s.stormId} advisory updated`,
        time: new Date(s.fetchedAt).getTime(),
        timeAgo: formatRelativeTime(s.fetchedAt),
      })
    }
  }

  for (const gs of groundStops.value) {
    if (gs.fetchedAt) {
      items.push({
        key: `gs-${gs.id}`,
        icon: '🛑',
        text: `Ground stop at ${gs.airportCode}${gs.reason ? ': ' + gs.reason : ''}`,
        time: new Date(gs.fetchedAt).getTime(),
        timeAgo: formatRelativeTime(gs.fetchedAt),
      })
    }
  }

  for (const eq of earthquakes.value.filter(e => e.magnitude >= 4.5).slice(0, 3)) {
    items.push({
      key: `eq-${eq.id}`,
      icon: '🌍',
      text: `M${eq.magnitude} earthquake - ${eq.place}`,
      time: new Date(eq.eventTime).getTime(),
      timeAgo: formatRelativeTime(eq.eventTime),
    })
  }

  for (const s of sigmets.value.filter(s => s.sigmetType === 'CONVECTIVE').slice(0, 2)) {
    if (s.fetchedAt) {
      items.push({
        key: `sigmet-${s.id}`,
        icon: '🚨',
        text: `Convective SIGMET${s.firName ? ' - ' + s.firName : ''}`,
        time: new Date(s.fetchedAt).getTime(),
        timeAgo: formatRelativeTime(s.fetchedAt),
      })
    }
  }

  for (const va of volcanicAsh.value.slice(0, 2)) {
    if (va.fetchedAt) {
      items.push({
        key: `va-${va.id}`,
        icon: '🌋',
        text: `Volcanic ash advisory${va.volcanoName ? ': ' + va.volcanoName : ''}`,
        time: new Date(va.fetchedAt).getTime(),
        timeAgo: formatRelativeTime(va.fetchedAt),
      })
    }
  }

  return items.sort((a, b) => b.time - a.time).slice(0, 10)
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

function stormTypeName(basin?: string): string {
  const types: Record<string, string> = { AT: 'Cat', EP: 'Cat', CP: 'Cat', WP: 'Typh', IO: 'Cyc', SH: 'Cyc' }
  return types[basin || 'AT'] || 'Cat'
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

.alert-filter-row {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
}

.alert-filter-btn {
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid var(--border-color, #ddd);
  border-radius: 4px;
  background: var(--bg-card, white);
  color: var(--text-secondary, #666);
  cursor: pointer;
  transition: all 0.15s;
}

.alert-filter-btn:hover {
  background: var(--bg-code, #f5f5f5);
}

.alert-filter-btn.filter-active {
  color: white;
  border-color: transparent;
}

.alert-filter-btn.filter-active.sev-extreme { background: #b71c1c; }
.alert-filter-btn.filter-active.sev-severe { background: var(--aging-color, #ff9800); }
.alert-filter-btn.filter-active.sev-moderate { background: var(--color-warning, #f0ad4e); color: #333; }
.alert-filter-btn.filter-active.sev-minor { background: #0d47a1; }
.alert-filter-btn.filter-active.sev-all { background: var(--text-muted, #999); }

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

.basin-tag {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 600;
  background: var(--bg-secondary, #f0f0f0);
  color: var(--text-secondary, #666);
  letter-spacing: 0.5px;
}

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

/* Clickable stat chips */
.stat-chip-link {
  text-decoration: none;
}

.stat-chip-link .stat-chip {
  cursor: pointer;
  transition: box-shadow 0.15s, transform 0.1s;
}

.stat-chip-link .stat-chip:hover {
  box-shadow: 0 2px 8px var(--shadow-md, rgba(0, 0, 0, 0.15));
  transform: translateY(-1px);
}

/* Situation summary */
.situation-card {
  border-left: 4px solid var(--accent);
}

.situation-card h2 {
  margin-bottom: 6px;
  font-size: 1rem;
}

.situation-text {
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-primary, #333);
  margin: 0;
}

/* Aviation hazard list */
.aviation-hazard-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.aviation-hazard-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 6px;
  font-size: 13px;
  background: var(--bg-secondary, #f9f9f9);
}

.aviation-hazard-item.hazard-convective {
  background: rgba(255, 152, 0, 0.08);
}

.hazard-type-badge {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 700;
  color: white;
  text-transform: uppercase;
  flex-shrink: 0;
}

.badge-sigmet { background: #e53935; }
.badge-convective { background: #ff9800; }
.badge-cwa { background: #1565c0; }

.hazard-detail {
  font-weight: 500;
  color: var(--text-primary, #333);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hazard-sev {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 3px;
  color: white;
  flex-shrink: 0;
}

.hazard-fir {
  font-size: 11px;
  color: var(--text-muted, #999);
  flex-shrink: 0;
}

.hazard-severity-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-left: 4px;
  vertical-align: middle;
}

.severity-severe-dot { background: #e53935; }
.severity-convective-dot { background: #ff9800; }

/* Volcanic ash list */
.volcanic-ash-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.va-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  background: var(--bg-secondary, #f9f9f9);
  font-size: 13px;
}

.va-name {
  font-weight: 600;
  color: var(--text-primary, #333);
  flex: 1;
}

.va-fir {
  font-size: 11px;
  color: var(--text-muted, #999);
}

.va-sev {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 3px;
  color: white;
}

.va-alt {
  font-size: 11px;
  font-family: monospace;
  color: var(--text-secondary, #666);
  background: var(--bg-code, #f0f0f0);
  padding: 1px 4px;
  border-radius: 3px;
}

/* Activity feed */
.activity-feed {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-light, #eee);
  font-size: 13px;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-icon {
  flex-shrink: 0;
  font-size: 14px;
}

.activity-text {
  flex: 1;
  color: var(--text-primary, #333);
  line-height: 1.3;
}

.activity-time {
  flex-shrink: 0;
  font-size: 11px;
  color: var(--text-muted, #999);
  white-space: nowrap;
}
</style>
