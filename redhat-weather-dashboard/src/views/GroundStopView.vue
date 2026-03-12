<template>
  <div class="container">
    <h1>{{ $t('groundStop.title') }}</h1>

    <div class="card">
      <div class="card-header-row">
        <h2>{{ $t('groundStop.activePrograms') }}</h2>
        <div class="header-actions">
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
        <span aria-hidden="true">🛑</span> {{ groundStops.length }} Active
      </span>
      <span v-if="gdpCount > 0" class="stat-chip">
        <span aria-hidden="true">⏳</span> {{ gdpCount }} GDPs
      </span>
      <span v-if="gsCount > 0" class="stat-chip stat-alert">
        <span aria-hidden="true">🚫</span> {{ gsCount }} Ground Stops
      </span>
    </div>

    <div v-if="groundStops.length > 0" class="card">
      <div class="table-wrapper">
        <table class="data-table" aria-label="Active ground stops and ground delay programs">
          <thead>
            <tr>
              <th>Airport</th>
              <th>Name</th>
              <th>Program</th>
              <th>Reason</th>
              <th>Avg Delay</th>
              <th>Max Delay</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="gs in groundStops" :key="gs.id">
              <td><strong>{{ gs.airportCode }}</strong></td>
              <td>{{ gs.airportName || '-' }}</td>
              <td>
                <span class="type-badge" :class="gs.programType?.includes('Stop') || gs.programType === 'GS' ? 'type-stop' : 'type-gdp'">
                  {{ gs.programType }}
                </span>
              </td>
              <td class="td-truncate">{{ gs.reason || '-' }}</td>
              <td>{{ gs.avgDelayMinutes ? gs.avgDelayMinutes + ' min' : '-' }}</td>
              <td>{{ gs.maxDelayMinutes ? gs.maxDelayMinutes + ' min' : '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else-if="!loading" class="card">
      <p><span aria-hidden="true">✅</span> {{ $t('groundStop.noActive') }}</p>
      <p>{{ $t('groundStop.autoFetch') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import TableSkeleton from '../components/skeletons/TableSkeleton.vue'

const store = useWeatherStore()
const toast = useToast()
const { groundStops, groundStopsLoading: loading, groundStopsError: error } = storeToRefs(store)

const refreshing = ref(false)

const gdpCount = computed(() => groundStops.value.filter(gs => {
  const t = gs.programType?.toLowerCase() || ''
  return t.includes('delay') || t === 'gdp'
}).length)

const gsCount = computed(() => groundStops.value.filter(gs => {
  const t = gs.programType?.toLowerCase() || ''
  return t.includes('stop') || t === 'gs'
}).length)

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

onMounted(() => {
  store.fetchGroundStops()
})
</script>

<style scoped>
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header-row h2 { margin-bottom: 0; }

.header-actions {
  display: flex;
  gap: 6px;
  align-items: center;
}

.stats-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

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
</style>
