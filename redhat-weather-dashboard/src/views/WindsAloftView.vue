<template>
  <div class="container">
    <h1>{{ $t('windsAloft.title') }}</h1>

    <div class="card">
      <div class="card-header-row">
        <h2>{{ $t('windsAloft.currentForecasts') }}</h2>
        <div class="header-actions">
          <select v-model="filterAltitude" class="filter-select" aria-label="Filter by altitude">
            <option value="all">All Altitudes</option>
            <option v-for="alt in standardAltitudes" :key="alt" :value="alt">{{ alt.toLocaleString() }} ft</option>
          </select>
          <input
            v-model="searchStation"
            type="text"
            class="filter-input"
            placeholder="Search station..."
            aria-label="Search by station ID"
          />
          <button class="btn-sm" :disabled="refreshing" @click="refreshData">
            {{ refreshing ? $t('airport.refreshing') : $t('windsAloft.refreshData') }}
          </button>
        </div>
      </div>
    </div>

    <TableSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <ErrorBoundary>
    <div v-if="filteredWinds.length > 0" class="card">
      <div class="table-controls">
        <span class="table-meta">{{ filteredWinds.length }} forecast entries ({{ stationCount }} stations)</span>
      </div>
      <div class="table-wrapper">
        <table class="data-table" aria-label="Winds and temperatures aloft forecasts">
          <thead>
            <tr>
              <th>Station</th>
              <th>Altitude</th>
              <th>Wind Dir</th>
              <th>Wind Spd</th>
              <th>Temp</th>
              <th>Valid</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="w in paginatedWinds" :key="w.id">
              <td><strong>{{ w.stationId }}</strong></td>
              <td class="td-nowrap">{{ w.altitudeFt.toLocaleString() }} ft</td>
              <td>
                <span v-if="w.windDirection != null" class="wind-dir">
                  {{ w.windDirection }}°
                  <span class="wind-arrow" :style="{ transform: `rotate(${w.windDirection}deg)` }">↑</span>
                </span>
                <span v-else>Calm</span>
              </td>
              <td>
                <span v-if="w.windSpeedKnots != null" :class="windSpeedClass(w.windSpeedKnots)">
                  {{ w.windSpeedKnots }} kt
                </span>
                <span v-else>-</span>
              </td>
              <td>
                <span v-if="w.temperatureCelsius != null" :class="tempClass(w.temperatureCelsius)">
                  {{ w.temperatureCelsius }}°C
                </span>
                <span v-else>-</span>
              </td>
              <td class="td-nowrap">{{ formatRelativeTime(w.validTime) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="totalPages > 1" class="pagination">
        <button class="btn-sm" :disabled="page <= 1" @click="page--">Prev</button>
        <span class="page-info">Page {{ page }} of {{ totalPages }}</span>
        <button class="btn-sm" :disabled="page >= totalPages" @click="page++">Next</button>
      </div>
    </div>

    </ErrorBoundary>

    <div v-if="!loading && windsAloft.length === 0" class="card">
      <p>{{ $t('windsAloft.noData') }}</p>
      <p>{{ $t('windsAloft.autoFetch') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import { formatRelativeTime } from '../utils/dateUtils'
import TableSkeleton from '../components/skeletons/TableSkeleton.vue'
import ErrorBoundary from '../components/ErrorBoundary.vue'

const store = useWeatherStore()
const toast = useToast()
const { windsAloft, windsAloftLoading: loading, windsAloftError: error } = storeToRefs(store)

const refreshing = ref(false)
const filterAltitude = ref('all')
const searchStation = ref('')
const page = ref(1)
const pageSize = 50

const standardAltitudes = [3000, 6000, 9000, 12000, 18000, 24000, 30000, 34000, 39000]

const filteredWinds = computed(() => {
  let result = windsAloft.value
  if (filterAltitude.value !== 'all') {
    result = result.filter(w => w.altitudeFt === Number(filterAltitude.value))
  }
  if (searchStation.value) {
    const q = searchStation.value.toUpperCase()
    result = result.filter(w => w.stationId.includes(q))
  }
  return result
})

const stationCount = computed(() => {
  return new Set(filteredWinds.value.map(w => w.stationId)).size
})

const totalPages = computed(() => Math.ceil(filteredWinds.value.length / pageSize))

const paginatedWinds = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredWinds.value.slice(start, start + pageSize)
})

function windSpeedClass(speed: number) {
  if (speed >= 50) return 'wind-extreme'
  if (speed >= 30) return 'wind-strong'
  if (speed >= 15) return 'wind-moderate'
  return 'wind-light'
}

function tempClass(temp: number) {
  if (temp <= -40) return 'temp-extreme-cold'
  if (temp <= -20) return 'temp-cold'
  if (temp >= 30) return 'temp-hot'
  return ''
}

async function refreshData() {
  refreshing.value = true
  try {
    await store.refreshWindsAloft()
    toast.success('Winds aloft data refreshed')
  } catch {
    toast.error('Failed to refresh winds aloft data')
  } finally {
    refreshing.value = false
  }
}

onMounted(() => {
  store.fetchWindsAloft()
})
</script>

<style scoped>
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.card-header-row h2 { margin-bottom: 0; }

.header-actions {
  display: flex;
  gap: 6px;
  align-items: center;
}

.filter-select, .filter-input {
  padding: 4px 8px;
  font-size: 13px;
  border: 1px solid var(--border-color, #ddd);
  border-radius: 4px;
  background: var(--bg-card, white);
  color: var(--text-primary, #333);
}

.filter-input {
  width: 120px;
}

.table-controls { margin-bottom: 8px; }
.td-nowrap { white-space: nowrap; }

.wind-dir {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.wind-arrow {
  display: inline-block;
  font-size: 14px;
  transition: transform 0.2s;
}

.wind-light { color: #4caf50; }
.wind-moderate { color: #ff9800; font-weight: 600; }
.wind-strong { color: #f44336; font-weight: 700; }
.wind-extreme { color: #9c27b0; font-weight: 700; }

.temp-extreme-cold { color: #1565c0; font-weight: 700; }
.temp-cold { color: #42a5f5; }
.temp-hot { color: #f44336; }

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 12px;
}

.page-info {
  font-size: 13px;
  color: var(--text-secondary, #666);
}
</style>
