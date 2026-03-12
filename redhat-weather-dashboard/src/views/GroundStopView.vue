<template>
  <div class="container">
    <h1>{{ $t('groundStop.title') }}</h1>

    <div class="card">
      <div class="card-header-row">
        <h2>{{ $t('groundStop.activePrograms') }}</h2>
        <div class="header-actions">
          <input
            v-model="filterText"
            type="text"
            class="filter-input"
            :placeholder="$t('groundStop.filterPlaceholder')"
            aria-label="Filter by airport code or name"
          />
          <button class="btn-sm" :disabled="refreshing" @click="refreshData">
            {{ refreshing ? $t('airport.refreshing') : $t('groundStop.refreshData') }}
          </button>
        </div>
      </div>
    </div>

    <TableSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="!loading && groundStops.length > 0" class="stats-bar">
      <span class="stat-chip stat-alert">
        <span aria-hidden="true">🛑</span> {{ filteredStops.length }} {{ $t('groundStop.active') }}
      </span>
      <span v-if="gdpCount > 0" class="stat-chip">
        <span aria-hidden="true">⏳</span> {{ gdpCount }} {{ $t('groundStop.gdps') }}
      </span>
      <span v-if="gsCount > 0" class="stat-chip stat-alert">
        <span aria-hidden="true">🚫</span> {{ gsCount }} {{ $t('groundStop.groundStops') }}
      </span>
    </div>

    <ErrorBoundary>
      <div v-if="filteredStops.length > 0" class="card">
        <div class="table-controls">
          <span class="table-meta">{{ filteredStops.length }} of {{ groundStops.length }} programs</span>
        </div>
        <div class="table-wrapper">
          <table class="data-table" aria-label="Active ground stops and ground delay programs">
            <thead>
              <tr>
                <th role="columnheader" tabindex="0" @click="toggleSort('airportCode')" @keydown.enter.prevent="toggleSort('airportCode')" @keydown.space.prevent="toggleSort('airportCode')">
                  {{ $t('groundStop.airport') }}
                  <span class="sort-indicator" :class="{ active: sortKey === 'airportCode' }">{{ sortIcon('airportCode') }}</span>
                </th>
                <th>{{ $t('groundStop.name') }}</th>
                <th role="columnheader" tabindex="0" @click="toggleSort('programType')" @keydown.enter.prevent="toggleSort('programType')" @keydown.space.prevent="toggleSort('programType')">
                  {{ $t('groundStop.program') }}
                  <span class="sort-indicator" :class="{ active: sortKey === 'programType' }">{{ sortIcon('programType') }}</span>
                </th>
                <th>{{ $t('groundStop.reason') }}</th>
                <th role="columnheader" tabindex="0" @click="toggleSort('avgDelayMinutes')" @keydown.enter.prevent="toggleSort('avgDelayMinutes')" @keydown.space.prevent="toggleSort('avgDelayMinutes')">
                  {{ $t('groundStop.avgDelay') }}
                  <span class="sort-indicator" :class="{ active: sortKey === 'avgDelayMinutes' }">{{ sortIcon('avgDelayMinutes') }}</span>
                </th>
                <th>{{ $t('groundStop.maxDelay') }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="gs in sortedStops" :key="gs.id">
                <td><strong>{{ gs.airportCode }}</strong></td>
                <td>{{ gs.airportName || '-' }}</td>
                <td>
                  <span class="type-badge" :class="isGroundStop(gs) ? 'type-stop' : 'type-gdp'">
                    {{ gs.programType }}
                  </span>
                </td>
                <td class="td-truncate">{{ gs.reason || '-' }}</td>
                <td>{{ gs.avgDelayMinutes ? gs.avgDelayMinutes + ' min' : '-' }}</td>
                <td>{{ gs.maxDelayMinutes ? gs.maxDelayMinutes + ' min' : '-' }}</td>
                <td>
                  <FreshnessBadge v-if="gs.fetchedAt" :fetched-at="gs.fetchedAt" data-type="groundStop" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </ErrorBoundary>

    <div v-if="!loading && groundStops.length === 0" class="card">
      <p><span aria-hidden="true">✅</span> {{ $t('groundStop.noActive') }}</p>
      <p>{{ $t('groundStop.autoFetch') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import type { GroundStop } from '../services/weatherService'
import TableSkeleton from '../components/skeletons/TableSkeleton.vue'
import FreshnessBadge from '../components/FreshnessBadge.vue'
import ErrorBoundary from '../components/ErrorBoundary.vue'

const store = useWeatherStore()
const toast = useToast()
const { groundStops, groundStopsLoading: loading, groundStopsError: error } = storeToRefs(store)

const refreshing = ref(false)
const filterText = ref('')
const sortKey = ref<string>('airportCode')
const sortDir = ref<'asc' | 'desc'>('asc')

function isGroundStop(gs: GroundStop) {
  const t = gs.programType?.toLowerCase() || ''
  return t.includes('stop') || t === 'gs'
}

const gdpCount = computed(() => groundStops.value.filter(gs => {
  const t = gs.programType?.toLowerCase() || ''
  return t.includes('delay') || t === 'gdp'
}).length)

const gsCount = computed(() => groundStops.value.filter(gs => isGroundStop(gs)).length)

const filteredStops = computed(() => {
  if (!filterText.value) return groundStops.value
  const q = filterText.value.toLowerCase()
  return groundStops.value.filter(gs =>
    gs.airportCode?.toLowerCase().includes(q) || gs.airportName?.toLowerCase().includes(q)
  )
})

const sortedStops = computed(() => {
  const data = [...filteredStops.value]
  const dir = sortDir.value === 'asc' ? 1 : -1
  const key = sortKey.value as keyof GroundStop
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

function toggleSort(key: string) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = key === 'avgDelayMinutes' ? 'desc' : 'asc'
  }
}

function sortIcon(key: string) {
  if (sortKey.value !== key) return '⇅'
  return sortDir.value === 'asc' ? '↑' : '↓'
}

async function refreshData() {
  refreshing.value = true
  try {
    await store.refreshGroundStops()
    toast.success('Ground stop data refreshed')
  } catch {
    toast.error('Failed to refresh ground stop data')
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

.card-header-row h2 { margin-bottom: 0; }

.header-actions {
  display: flex;
  gap: 6px;
  align-items: center;
}

.filter-input {
  padding: 4px 8px;
  font-size: 13px;
  border: 1px solid var(--border-color, #ddd);
  border-radius: 4px;
  background: var(--bg-card, white);
  color: var(--text-primary, #333);
  width: 160px;
}

.stats-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.table-controls { margin-bottom: 8px; }

.td-truncate {
  max-width: 250px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
  font-size: 10px;
  color: white;
  white-space: nowrap;
}

.type-stop { background: #f44336; }
.type-gdp { background: #ff9800; }

.sort-indicator {
  font-size: 11px;
  opacity: 0.4;
  margin-left: 2px;
}
.sort-indicator.active { opacity: 1; }
</style>
