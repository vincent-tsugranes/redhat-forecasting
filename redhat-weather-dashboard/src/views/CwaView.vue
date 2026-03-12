<template>
  <div class="container">
    <h1>{{ $t('cwa.title') }}</h1>

    <div class="card">
      <div class="card-header-row">
        <h2>{{ $t('cwa.activeAdvisories') }}</h2>
        <div class="header-actions">
          <select v-model="filterArtcc" class="filter-select" aria-label="Filter by ARTCC">
            <option value="all">All ARTCCs</option>
            <option v-for="artcc in artccList" :key="artcc" :value="artcc">{{ artcc }}</option>
          </select>
          <select v-model="filterHazard" class="filter-select" aria-label="Filter by hazard">
            <option value="all">All Hazards</option>
            <option value="TURB">Turbulence</option>
            <option value="ICE">Icing</option>
            <option value="IFR">IFR</option>
            <option value="CONVECTIVE">Convective</option>
          </select>
          <button class="btn-sm" :disabled="refreshing" @click="refreshData">
            {{ refreshing ? $t('airport.refreshing') : $t('cwa.refreshData') }}
          </button>
        </div>
      </div>
    </div>

    <TableSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <ErrorBoundary>
    <div v-if="filteredCwas.length > 0" class="card">
      <div class="table-controls">
        <span class="table-meta">{{ filteredCwas.length }} active advisories</span>
      </div>
      <div class="table-wrapper">
        <table class="data-table" aria-label="Active Center Weather Advisories">
          <thead>
            <tr>
              <th>ARTCC</th>
              <th>Hazard</th>
              <th>Severity</th>
              <th>Altitude</th>
              <th>Valid From</th>
              <th>Valid To</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="cwa in filteredCwas" :key="cwa.id" :class="{ 'row-expired-soon': isExpiringSoon(cwa) }">
              <td>
                <span class="artcc-badge">{{ cwa.artcc }}</span>
              </td>
              <td>{{ cwa.hazard || '-' }}</td>
              <td>{{ cwa.severity || '-' }}</td>
              <td class="td-nowrap">
                <template v-if="cwa.altitudeLowFt || cwa.altitudeHighFt">
                  {{ cwa.altitudeLowFt ? cwa.altitudeLowFt.toLocaleString() : 'SFC' }} -
                  {{ cwa.altitudeHighFt ? cwa.altitudeHighFt.toLocaleString() : 'UNL' }} ft
                </template>
                <span v-else>-</span>
              </td>
              <td class="td-nowrap">{{ formatDate(cwa.validTimeFrom) }}</td>
              <td class="td-nowrap">{{ formatDate(cwa.validTimeTo) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-for="cwa in filteredCwas" :key="'card-' + cwa.id" class="card cwa-card">
      <div class="cwa-header">
        <span class="artcc-badge">{{ cwa.artcc }}</span>
        <strong>{{ cwa.hazard || 'Advisory' }}</strong>
        <span v-if="cwa.severity" class="severity-label">{{ cwa.severity }}</span>
      </div>
      <pre v-if="cwa.rawText" class="raw-text">{{ cwa.rawText }}</pre>
      <div class="cwa-validity">
        Valid: {{ formatDate(cwa.validTimeFrom) }} - {{ formatDate(cwa.validTimeTo) }}
      </div>
    </div>

    </ErrorBoundary>

    <div v-if="!loading && cwas.length === 0" class="card">
      <p>{{ $t('cwa.noActive') }}</p>
      <p>{{ $t('cwa.autoFetch') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import { formatDate } from '../utils/dateUtils'
import TableSkeleton from '../components/skeletons/TableSkeleton.vue'
import ErrorBoundary from '../components/ErrorBoundary.vue'

const store = useWeatherStore()
const toast = useToast()
const { cwas, cwasLoading: loading, cwasError: error } = storeToRefs(store)

const refreshing = ref(false)
const filterArtcc = ref('all')
const filterHazard = ref('all')

const artccList = computed(() => {
  const set = new Set(cwas.value.map(c => c.artcc).filter(Boolean))
  return Array.from(set).sort()
})

const filteredCwas = computed(() => {
  let result = cwas.value
  if (filterArtcc.value !== 'all') {
    result = result.filter(c => c.artcc === filterArtcc.value)
  }
  if (filterHazard.value !== 'all') {
    result = result.filter(c => c.hazard?.includes(filterHazard.value))
  }
  return result
})

function isExpiringSoon(cwa: { validTimeTo: string }) {
  const expiresIn = new Date(cwa.validTimeTo).getTime() - Date.now()
  return expiresIn > 0 && expiresIn < 60 * 60 * 1000
}

async function refreshData() {
  refreshing.value = true
  try {
    await store.refreshCwas()
    toast.success('CWA data refreshed')
  } catch {
    toast.error('Failed to refresh CWA data')
  } finally {
    refreshing.value = false
  }
}

onMounted(() => {
  store.fetchCwas()
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

.filter-select {
  padding: 4px 8px;
  font-size: 13px;
  border: 1px solid var(--border-color, #ddd);
  border-radius: 4px;
  background: var(--bg-card, white);
  color: var(--text-primary, #333);
}

.table-controls { margin-bottom: 8px; }
.td-nowrap { white-space: nowrap; }

.artcc-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
  font-size: 11px;
  color: white;
  background: #1976d2;
}

.severity-label {
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.cwa-card { margin-bottom: 12px; }

.cwa-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.raw-text {
  font-family: monospace;
  font-size: 12px;
  background: var(--bg-input, #f5f5f5);
  border: 1px solid var(--border-light, #e0e0e0);
  border-radius: 4px;
  padding: 10px;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--text-primary, #333);
  margin: 8px 0;
}

.cwa-validity {
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.row-expired-soon {
  background: #fff8e1;
}
</style>
