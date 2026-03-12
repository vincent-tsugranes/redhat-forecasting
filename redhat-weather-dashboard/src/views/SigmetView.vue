<template>
  <div class="container">
    <h1>{{ $t('sigmet.title') }}</h1>

    <div class="card">
      <div class="card-header-row">
        <h2>{{ $t('sigmet.activeAdvisories') }}</h2>
        <div class="header-actions">
          <select v-model="filterScope" class="filter-select" aria-label="Filter by scope">
            <option value="all">All Scopes</option>
            <option value="DOMESTIC">Domestic</option>
            <option value="INTERNATIONAL">International</option>
          </select>
          <select v-model="filterType" class="filter-select" aria-label="Filter by advisory type">
            <option value="all">All</option>
            <option value="SIGMET">SIGMETs</option>
            <option value="AIRMET">AIRMETs</option>
          </select>
          <select v-model="filterHazard" class="filter-select" aria-label="Filter by hazard">
            <option value="all">All Hazards</option>
            <option value="TURB">Turbulence</option>
            <option value="ICE">Icing</option>
            <option value="IFR">IFR</option>
            <option value="MTN OBSCN">Mtn Obscuration</option>
            <option value="CONVECTIVE">Convective</option>
          </select>
          <button class="btn-sm" :disabled="refreshing" @click="refreshData">
            {{ refreshing ? $t('airport.refreshing') : $t('sigmet.refreshData') }}
          </button>
        </div>
      </div>
    </div>

    <TableSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="filteredSigmets.length > 0" class="card">
      <div class="table-controls">
        <span class="table-meta">{{ filteredSigmets.length }} active advisories</span>
      </div>
      <div class="table-wrapper">
        <table class="data-table" aria-label="Active SIGMETs and AIRMETs">
          <thead>
            <tr>
              <th>Type</th>
              <th>Hazard</th>
              <th>Severity</th>
              <th>Altitude</th>
              <th>Valid From</th>
              <th>Valid To</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="sigmet in filteredSigmets" :key="sigmet.id" :class="{ 'row-expired-soon': isExpiringSoon(sigmet) }">
              <td>
                <span class="type-badge" :class="sigmet.sigmetType === 'SIGMET' ? 'type-sigmet' : 'type-airmet'">
                  {{ sigmet.sigmetType }}
                </span>
                <span v-if="sigmet.scope === 'INTERNATIONAL'" class="scope-badge">INTL</span>
              </td>
              <td>{{ sigmet.hazard || '-' }}<span v-if="sigmet.firName" class="fir-label"> ({{ sigmet.firName }})</span></td>
              <td>{{ sigmet.severity || '-' }}</td>
              <td class="td-nowrap">
                <template v-if="sigmet.altitudeLowFt || sigmet.altitudeHighFt">
                  {{ sigmet.altitudeLowFt ? sigmet.altitudeLowFt.toLocaleString() : 'SFC' }} -
                  {{ sigmet.altitudeHighFt ? sigmet.altitudeHighFt.toLocaleString() : 'UNL' }} ft
                </template>
                <span v-else>-</span>
              </td>
              <td class="td-nowrap">{{ formatDate(sigmet.validTimeFrom) }}</td>
              <td class="td-nowrap">{{ formatDate(sigmet.validTimeTo) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Raw text cards -->
    <div v-for="sigmet in filteredSigmets" :key="'card-' + sigmet.id" class="card sigmet-card">
      <div class="sigmet-header">
        <span class="type-badge" :class="sigmet.sigmetType === 'SIGMET' ? 'type-sigmet' : 'type-airmet'">
          {{ sigmet.sigmetType }}
        </span>
        <strong>{{ sigmet.hazard || 'Advisory' }}</strong>
        <span v-if="sigmet.severity" class="severity-label">{{ sigmet.severity }}</span>
      </div>
      <pre v-if="sigmet.rawText" class="raw-text">{{ sigmet.rawText }}</pre>
      <div class="sigmet-validity">
        Valid: {{ formatDate(sigmet.validTimeFrom) }} - {{ formatDate(sigmet.validTimeTo) }}
      </div>
    </div>

    <div v-if="!loading && sigmets.length === 0" class="card">
      <p>{{ $t('sigmet.noActive') }}</p>
      <p>{{ $t('sigmet.autoFetch') }}</p>
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
const store = useWeatherStore()
const toast = useToast()
const { sigmets, sigmetsLoading: loading, sigmetsError: error } = storeToRefs(store)

const refreshing = ref(false)
const filterScope = ref('all')
const filterType = ref('all')
const filterHazard = ref('all')

const filteredSigmets = computed(() => {
  let result = sigmets.value
  if (filterScope.value !== 'all') {
    result = result.filter(s => s.scope === filterScope.value)
  }
  if (filterType.value !== 'all') {
    result = result.filter(s => s.sigmetType === filterType.value)
  }
  if (filterHazard.value !== 'all') {
    result = result.filter(s => s.hazard?.includes(filterHazard.value))
  }
  return result
})

function isExpiringSoon(sigmet: { validTimeTo: string }) {
  const expiresIn = new Date(sigmet.validTimeTo).getTime() - Date.now()
  return expiresIn > 0 && expiresIn < 60 * 60 * 1000 // less than 1 hour
}

async function refreshData() {
  refreshing.value = true
  try {
    await store.refreshSigmets()
    toast.success('SIGMET/AIRMET data refreshed')
  } catch {
    toast.error('Failed to refresh SIGMET data')
  } finally {
    refreshing.value = false
  }
}

onMounted(() => {
  store.fetchSigmets()
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

.type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
  font-size: 11px;
  color: white;
}

.type-sigmet { background: #f44336; }
.type-airmet { background: #ff9800; }

.severity-label {
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.sigmet-card { margin-bottom: 12px; }

.sigmet-header {
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

.sigmet-validity {
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.scope-badge {
  display: inline-block;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 9px;
  font-weight: bold;
  color: white;
  background: #7b1fa2;
  margin-left: 4px;
  vertical-align: middle;
}

.fir-label {
  font-size: 11px;
  color: var(--text-secondary, #666);
}

.row-expired-soon {
  background: #fff8e1;
}
</style>
