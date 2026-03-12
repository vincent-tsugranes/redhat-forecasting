<template>
  <div class="container">
    <h1>{{ $t('pirep.title') }}</h1>

    <div class="card">
      <div class="card-header-row">
        <h2>{{ $t('pirep.recentReports') }}</h2>
        <div class="header-actions">
          <select v-model="filterType" class="filter-select" aria-label="Filter by hazard type">
            <option value="all">All Reports</option>
            <option value="turbulence">Turbulence</option>
            <option value="icing">Icing</option>
          </select>
          <button class="btn-sm" :disabled="refreshing" @click="refreshData">
            {{ refreshing ? $t('airport.refreshing') : $t('pirep.refreshData') }}
          </button>
        </div>
      </div>
    </div>

    <ErrorBoundary>
      <PirepMap v-if="filteredPireps.length > 0" :pireps="filteredPireps" />
    </ErrorBoundary>

    <TableSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="filteredPireps.length > 0" class="card">
      <div class="table-controls">
        <span class="table-meta">{{ filteredPireps.length }} pilot reports</span>
      </div>
      <div class="table-wrapper">
        <table class="data-table" aria-label="Pilot Reports">
          <thead>
            <tr>
              <th role="columnheader" tabindex="0" @click="toggleSort('observationTime')" @keydown.enter.prevent="toggleSort('observationTime')" @keydown.space.prevent="toggleSort('observationTime')">
                Time <span class="sort-indicator" :class="{ active: sortKey === 'observationTime' }">{{ sortIcon('observationTime') }}</span>
              </th>
              <th role="columnheader" tabindex="0" @click="toggleSort('altitudeFt')" @keydown.enter.prevent="toggleSort('altitudeFt')" @keydown.space.prevent="toggleSort('altitudeFt')">
                Altitude <span class="sort-indicator" :class="{ active: sortKey === 'altitudeFt' }">{{ sortIcon('altitudeFt') }}</span>
              </th>
              <th>Turbulence</th>
              <th>Icing</th>
              <th>Aircraft</th>
              <th>Conditions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="pirep in sortedPireps" :key="pirep.id">
              <td class="td-nowrap">{{ formatDate(pirep.observationTime) }}</td>
              <td>{{ pirep.altitudeFt ? pirep.altitudeFt.toLocaleString() + ' ft' : '-' }}</td>
              <td>
                <span v-if="pirep.turbulenceIntensity" class="hazard-badge" :class="'turb-' + pirep.turbulenceIntensity">
                  {{ pirep.turbulenceIntensity }}
                </span>
                <span v-else>-</span>
              </td>
              <td>
                <span v-if="pirep.icingIntensity" class="hazard-badge" :class="'ice-' + pirep.icingIntensity">
                  {{ pirep.icingIntensity }}
                </span>
                <span v-else>-</span>
              </td>
              <td>{{ pirep.aircraftType || '-' }}</td>
              <td class="td-truncate">{{ pirep.skyCondition || pirep.weatherConditions || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else-if="!loading && pireps.length === 0" class="card">
      <p>{{ $t('pirep.noRecent') }}</p>
      <p>{{ $t('pirep.autoFetch') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import { formatDate } from '../utils/dateUtils'
import PirepMap from '../components/PirepMap.vue'
import ErrorBoundary from '../components/ErrorBoundary.vue'
import TableSkeleton from '../components/skeletons/TableSkeleton.vue'

const store = useWeatherStore()
const toast = useToast()
const { pireps, pirepsLoading: loading, pirepsError: error } = storeToRefs(store)

const refreshing = ref(false)
const filterType = ref('all')
const sortKey = ref('observationTime')
const sortDir = ref<'asc' | 'desc'>('desc')

const filteredPireps = computed(() => {
  if (filterType.value === 'turbulence') {
    return pireps.value.filter(p => p.turbulenceIntensity && p.turbulenceIntensity !== 'NEG')
  }
  if (filterType.value === 'icing') {
    return pireps.value.filter(p => p.icingIntensity && p.icingIntensity !== 'NEG')
  }
  return pireps.value
})

function toggleSort(key: string) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = key === 'observationTime' ? 'desc' : 'asc'
  }
}

function sortIcon(key: string) {
  if (sortKey.value !== key) return '⇅'
  return sortDir.value === 'asc' ? '↑' : '↓'
}

const sortedPireps = computed(() => {
  const data = [...filteredPireps.value]
  const dir = sortDir.value === 'asc' ? 1 : -1
  const key = sortKey.value as keyof (typeof data)[0]
  data.sort((a, b) => {
    const av = a[key]
    const bv = b[key]
    if (av == null && bv == null) return 0
    if (av == null) return 1
    if (bv == null) return -1
    if (typeof av === 'string' && typeof bv === 'string') return av.localeCompare(bv) * dir
    return ((av as number) - (bv as number)) * dir
  })
  return data
})

async function refreshData() {
  refreshing.value = true
  try {
    await store.refreshPireps()
    toast.success('PIREP data refreshed')
  } catch {
    toast.error('Failed to refresh PIREP data')
  } finally {
    refreshing.value = false
  }
}


</script>

<style scoped>
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.card-header-row h2 {
  margin-bottom: 0;
}

.header-actions {
  display: flex;
  gap: 6px;
  align-items: center;
}

.filter-select {
  padding: 4px 8px;
  font-size: 13px;
  border: 1px solid var(--border-color, #ddd);
  border-radius: 4px;
  background: var(--bg-card, white);
  color: var(--text-primary, #333);
}

.table-controls {
  margin-bottom: 8px;
}

.td-nowrap { white-space: nowrap; }
.td-truncate {
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hazard-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
  font-size: 11px;
  text-transform: uppercase;
  color: white;
}

.turb-NEG, .ice-NEG { background: #4caf50; }
.turb-LGT, .ice-TRC, .ice-LGT { background: #8bc34a; }
.turb-MOD, .ice-MOD { background: #ff9800; }
.turb-SEV, .ice-SEV { background: #f44336; }
.turb-EXTRM { background: #9c27b0; }
</style>
