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

    <div v-if="!loading" class="stats-row">
      <div class="stat-tile">
        <span class="stat-icon" aria-hidden="true">📍</span>
        <div class="stat-body">
          <span class="stat-value">{{ animatedLocationCount }}</span>
          <span class="stat-label">Locations</span>
        </div>
      </div>
      <div class="stat-tile">
        <span class="stat-icon" aria-hidden="true">✈️</span>
        <div class="stat-body">
          <span class="stat-value">{{ airports.length.toLocaleString() }}</span>
          <span class="stat-label">Airports</span>
        </div>
      </div>
      <div class="stat-tile">
        <span class="stat-icon" aria-hidden="true">🌀</span>
        <div class="stat-body">
          <span class="stat-value">{{ hurricanes.length }}</span>
          <span class="stat-label">Active Storms</span>
        </div>
      </div>
      <div class="stat-tile">
        <span class="stat-icon" aria-hidden="true">🌍</span>
        <div class="stat-body">
          <span class="stat-value">{{ earthquakes.length }}</span>
          <span class="stat-label">Earthquakes</span>
        </div>
      </div>
      <div class="stat-tile">
        <span class="stat-icon" aria-hidden="true">⚠️</span>
        <div class="stat-body">
          <span class="stat-value">{{ alerts.length }}</span>
          <span class="stat-label">Active Alerts</span>
        </div>
      </div>
    </div>

    <div v-if="!loading" class="dashboard-grid">
      <!-- Left column: quick-access cards -->
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

        <!-- Quick links row -->
        <div class="quick-links">
          <router-link to="/forecasts" class="quick-link card">
            <span class="ql-icon" aria-hidden="true">🌤️</span>
            <div class="ql-text">
              <strong>{{ $t('dashboard.weatherForecasts') }}</strong>
              <span>{{ $t('dashboard.forecastUpdates') }}</span>
            </div>
          </router-link>
          <router-link to="/airports" class="quick-link card">
            <span class="ql-icon" aria-hidden="true">✈️</span>
            <div class="ql-text">
              <strong>{{ $t('dashboard.airportWeather') }}</strong>
              <span>{{ $t('dashboard.metarUpdates') }}</span>
            </div>
          </router-link>
          <router-link to="/space-weather" class="quick-link card">
            <span class="ql-icon" aria-hidden="true">☀️</span>
            <div class="ql-text">
              <strong>{{ $t('dashboard.spaceWeather') }}</strong>
              <span>{{ $t('dashboard.spaceWeatherUpdates') }}</span>
            </div>
          </router-link>
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
import { useAnimatedNumber } from '../composables/useAnimatedNumber'
import { formatRelativeTime } from '../utils/dateUtils'
import UnifiedMap from '../components/UnifiedMap.vue'
import ErrorBoundary from '../components/ErrorBoundary.vue'
import DataStatusCard from '../components/DataStatusCard.vue'
import DashboardSkeleton from '../components/skeletons/DashboardSkeleton.vue'
import FavoriteWeatherCards from '../components/FavoriteWeatherCards.vue'

const store = useWeatherStore()
const {
  locations,
  airports,
  hurricanes,
  earthquakes,
  alerts,
  locationsLoading: loading,
  locationsError: error,
} = storeToRefs(store)
const toast = useToast()

const locationCount = computed(() => locations.value.length)
const animatedLocationCount = useAnimatedNumber(locationCount)

function getMagnitudeClass(magnitude: number) {
  if (magnitude >= 7) return 'mag-major'
  if (magnitude >= 5) return 'mag-strong'
  if (magnitude >= 4) return 'mag-moderate'
  return 'mag-light'
}

async function handleRefresh() {
  try {
    await store.refreshLocations()
    toast.success('Data refreshed successfully')
  } catch {
    toast.error('Failed to refresh data')
  }
}

onMounted(() => {
  Promise.all([
    store.fetchLocations(),
    store.fetchAirports(),
    store.fetchAlerts(),
    store.fetchEarthquakes(),
    store.fetchHurricanes(),
  ])
})
</script>

<style scoped>
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.dashboard-header h1 {
  margin-bottom: 0;
}

.stats-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.stat-tile {
  flex: 1;
  min-width: 120px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--bg-card, white);
  border-radius: 8px;
  padding: 14px 16px;
  box-shadow: 0 2px 4px var(--shadow, rgba(0, 0, 0, 0.1));
}

.stat-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.stat-body {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--text-primary, #333);
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-top: 2px;
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

.mag-light { background: #4caf50; }
.mag-moderate { background: #ff9800; }
.mag-strong { background: #f44336; }
.mag-major { background: #9c27b0; }

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

.cat-0 { background: #007bff; }
.cat-1 { background: #e6a800; color: #1a1a1a; }
.cat-2 { background: #ff9800; }
.cat-3 { background: #ff5722; }
.cat-4 { background: #f44336; }
.cat-5 { background: #9c27b0; }

.storm-name-cell {
  font-weight: 600;
  color: #ee0000;
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

/* Quick links */
.quick-links {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.quick-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  text-decoration: none;
  color: inherit;
  transition: box-shadow 0.2s, transform 0.15s;
  margin-bottom: 0;
}

.quick-link:hover {
  box-shadow: 0 4px 12px var(--shadow-md, rgba(0, 0, 0, 0.15));
  transform: translateY(-2px);
}

.ql-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.ql-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ql-text strong {
  font-size: 14px;
  color: var(--text-primary, #333);
}

.ql-text span {
  font-size: 12px;
  color: var(--text-secondary, #666);
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

  .quick-links {
    grid-template-columns: 1fr;
  }

  .dashboard-map {
    height: 400px;
  }
}

@media (max-width: 768px) {
  .stats-row {
    gap: 8px;
  }

  .stat-tile {
    min-width: 100px;
    padding: 10px 12px;
  }

  .stat-value {
    font-size: 1.1rem;
  }

  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
