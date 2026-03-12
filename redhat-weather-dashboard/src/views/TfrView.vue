<template>
  <div class="container">
    <h1>{{ $t('tfr.title') }}</h1>

    <div class="card">
      <div class="card-header-row">
        <h2>{{ $t('tfr.activeRestrictions') }}</h2>
        <div class="header-actions">
          <select v-model="filterType" class="filter-select" aria-label="Filter by TFR type">
            <option value="all">All Types</option>
            <option v-for="t in tfrTypes" :key="t" :value="t">{{ t }}</option>
          </select>
          <select v-model="filterState" class="filter-select" aria-label="Filter by state">
            <option value="all">All States</option>
            <option v-for="s in stateList" :key="s" :value="s">{{ s }}</option>
          </select>
          <button class="btn-sm" :disabled="refreshing" @click="refreshData">
            {{ refreshing ? $t('airport.refreshing') : $t('tfr.refreshData') }}
          </button>
        </div>
      </div>
    </div>

    <TableSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="!loading && tfrs.length > 0" class="stats-bar">
      <span class="stat-chip">
        <span aria-hidden="true">🚫</span> {{ filteredTfrs.length }} Active TFRs
      </span>
      <span v-if="newCount > 0" class="stat-chip stat-alert">
        <span aria-hidden="true">🆕</span> {{ newCount }} New (24h)
      </span>
      <span v-if="securityCount > 0" class="stat-chip stat-alert">
        <span aria-hidden="true">🔒</span> {{ securityCount }} Security
      </span>
      <span v-if="hazardCount > 0" class="stat-chip">
        <span aria-hidden="true">⚠️</span> {{ hazardCount }} Hazards
      </span>
    </div>

    <ErrorBoundary>
    <div v-if="filteredTfrs.length > 0" class="card">
      <div class="table-controls">
        <span class="table-meta">{{ filteredTfrs.length }} restrictions</span>
      </div>
      <div class="table-wrapper">
        <table class="data-table" aria-label="Active Temporary Flight Restrictions">
          <thead>
            <tr>
              <th>NOTAM</th>
              <th>Type</th>
              <th>ARTCC</th>
              <th>State</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="tfr in filteredTfrs" :key="tfr.id" :class="{ 'row-new': tfr.isNew }">
              <td class="td-nowrap">
                <strong>{{ tfr.notamId }}</strong>
                <span v-if="tfr.isNew" class="new-badge">NEW</span>
              </td>
              <td>
                <span class="type-badge" :class="typeClass(tfr.tfrType)">{{ tfr.tfrType }}</span>
              </td>
              <td>{{ tfr.facility }}</td>
              <td>{{ tfr.state || '-' }}</td>
              <td class="td-desc">{{ tfr.description || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    </ErrorBoundary>

    <div v-if="!loading && tfrs.length === 0" class="card">
      <p>{{ $t('tfr.noActive') }}</p>
      <p>{{ $t('tfr.autoFetch') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import TableSkeleton from '../components/skeletons/TableSkeleton.vue'
import ErrorBoundary from '../components/ErrorBoundary.vue'

const store = useWeatherStore()
const toast = useToast()
const { tfrs, tfrsLoading: loading, tfrsError: error } = storeToRefs(store)

const refreshing = ref(false)
const filterType = ref('all')
const filterState = ref('all')

const tfrTypes = computed(() => {
  const set = new Set(tfrs.value.map(t => t.tfrType).filter(Boolean))
  return Array.from(set).sort()
})

const stateList = computed(() => {
  const set = new Set(tfrs.value.map(t => t.state).filter(Boolean))
  return Array.from(set).sort()
})

const filteredTfrs = computed(() => {
  let result = tfrs.value
  if (filterType.value !== 'all') {
    result = result.filter(t => t.tfrType === filterType.value)
  }
  if (filterState.value !== 'all') {
    result = result.filter(t => t.state === filterState.value)
  }
  return result
})

const newCount = computed(() => tfrs.value.filter(t => t.isNew).length)
const securityCount = computed(() => tfrs.value.filter(t => t.tfrType === 'SECURITY').length)
const hazardCount = computed(() => tfrs.value.filter(t => t.tfrType === 'HAZARDS').length)

function typeClass(type: string) {
  switch (type) {
    case 'SECURITY': return 'type-security'
    case 'VIP': return 'type-vip'
    case 'HAZARDS': return 'type-hazard'
    case 'SPACE OPERATIONS': return 'type-space'
    case 'SPECIAL': return 'type-special'
    default: return 'type-other'
  }
}

async function refreshData() {
  refreshing.value = true
  try {
    await store.refreshTfrs()
    toast.success('TFR data refreshed')
  } catch {
    toast.error('Failed to refresh TFR data')
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

.filter-select {
  padding: 4px 8px;
  font-size: 13px;
  border: 1px solid var(--border-color, #ddd);
  border-radius: 4px;
  background: var(--bg-card, white);
  color: var(--text-primary, #333);
}

.stats-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.table-controls { margin-bottom: 8px; }
.td-nowrap { white-space: nowrap; }
.td-desc { max-width: 400px; }

.type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
  font-size: 10px;
  color: white;
  white-space: nowrap;
}

.type-security { background: #f44336; }
.type-vip { background: #9c27b0; }
.type-hazard { background: #ff9800; }
.type-space { background: #2196f3; }
.type-special { background: #607d8b; }
.type-other { background: #795548; }

.new-badge {
  display: inline-block;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 9px;
  font-weight: bold;
  color: white;
  background: #4caf50;
  margin-left: 4px;
  vertical-align: middle;
}

.row-new {
  background: #e8f5e9;
}

</style>
