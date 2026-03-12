<template>
  <div class="container">
    <div class="dashboard-header">
      <h1>{{ $t('dashboard.title') }}</h1>
      <button class="btn-sm btn-icon" :aria-label="$t('dashboard.refreshAriaLabel')" @click="handleRefresh">
        <span aria-hidden="true">🔄</span> {{ $t('dashboard.refreshData') }}
      </button>
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
      </div>

      <!-- Right column: map -->
      <div class="dashboard-sidebar">
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
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import { formatRelativeTime } from '../utils/dateUtils'
import UnifiedMap from '../components/UnifiedMap.vue'
import ErrorBoundary from '../components/ErrorBoundary.vue'
import DataStatusCard from '../components/DataStatusCard.vue'
import DashboardSkeleton from '../components/skeletons/DashboardSkeleton.vue'
import FavoriteWeatherCards from '../components/FavoriteWeatherCards.vue'

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
  airportsLoading: loading,
  airportsError: error,
} = storeToRefs(store)
const toast = useToast()

const delayedAirports = computed(() => delays.value.filter(d => d.isDelayed).length)

function getMagnitudeClass(magnitude: number) {
  if (magnitude >= 7) return 'mag-major'
  if (magnitude >= 5) return 'mag-strong'
  if (magnitude >= 4) return 'mag-moderate'
  return 'mag-light'
}

async function handleRefresh() {
  try {
    await store.fetchAirports()
    toast.success('Data refreshed successfully')
  } catch {
    toast.error('Failed to refresh data')
  }
}

onMounted(async () => {
  // Critical data first - shown directly on the dashboard
  await Promise.all([
    store.fetchAirports(),
    store.fetchEarthquakes(),
    store.fetchHurricanes(),
  ])
  // Secondary data - used for stat chips only, load without blocking
  store.fetchAlerts()
  store.fetchPireps()
  store.fetchSigmets()
  store.fetchDelays()
  // Tertiary data - least critical
  store.fetchTfrs()
  store.fetchCwas()
  store.fetchGroundStops()
  store.fetchVolcanicAsh()
  store.fetchLightning()
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

/* Compact stats bar */
.stats-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  padding: 2px 0;
}

.stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  background: var(--bg-card, white);
  border-radius: 16px;
  font-size: 12px;
  color: var(--text-secondary, #666);
  white-space: nowrap;
  box-shadow: 0 1px 3px var(--shadow, rgba(0, 0, 0, 0.08));
}

.stat-chip strong {
  color: var(--text-primary, #333);
  font-size: 13px;
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
  gap: 20px;
  align-items: start;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-header-row h2 {
  margin-bottom: 0;
  font-size: 1.2rem;
}

.empty-state {
  color: var(--text-secondary, #666);
  text-align: center;
  padding: 20px;
  font-style: italic;
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

/* Sidebar map */
.map-card {
  position: sticky;
  top: 20px;
}

.map-card h2 {
  margin-bottom: 12px;
  font-size: 1.2rem;
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
  .stats-bar {
    gap: 6px;
  }

  .stat-chip {
    padding: 4px 8px;
    font-size: 11px;
  }

  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
