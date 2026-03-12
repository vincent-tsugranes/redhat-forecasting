<template>
  <div class="container">
    <h1>{{ $t('lightning.title') }}</h1>

    <div class="card">
      <div class="card-header-row">
        <h2>{{ $t('lightning.recentActivity') }}</h2>
        <div class="header-actions">
          <button class="btn-sm" :disabled="refreshing" @click="refreshData">
            {{ refreshing ? $t('airport.refreshing') : $t('lightning.refreshData') }}
          </button>
        </div>
      </div>
    </div>

    <TableSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="!loading" class="stats-bar">
      <span class="stat-chip">
        <span aria-hidden="true">⚡</span> {{ lightning.length }} Strikes (1h)
      </span>
    </div>

    <div v-if="lightning.length > 0" class="card">
      <div class="table-wrapper">
        <table class="data-table" aria-label="Recent lightning strikes">
          <thead>
            <tr>
              <th>Time</th>
              <th>Location</th>
              <th>Amplitude</th>
              <th>Type</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="strike in lightning" :key="strike.id">
              <td class="td-nowrap">{{ formatDate(strike.strikeTime) }}</td>
              <td>{{ strike.latitude.toFixed(2) }}, {{ strike.longitude.toFixed(2) }}</td>
              <td>{{ strike.amplitudeKa ? strike.amplitudeKa.toFixed(1) + ' kA' : '-' }}</td>
              <td>{{ strike.strikeType || 'CG' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else-if="!loading" class="card">
      <p><span aria-hidden="true">✅</span> {{ $t('lightning.noRecent') }}</p>
      <p>{{ $t('lightning.autoFetch') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import { formatDate } from '../utils/dateUtils'
import TableSkeleton from '../components/skeletons/TableSkeleton.vue'

const store = useWeatherStore()
const toast = useToast()
const { lightning, lightningLoading: loading, lightningError: error } = storeToRefs(store)

const refreshing = ref(false)

async function refreshData() {
  refreshing.value = true
  try {
    await store.refreshLightning()
    toast.success('Lightning data refreshed')
  } catch {
    toast.error('Failed to refresh lightning data')
  } finally {
    refreshing.value = false
  }
}

onMounted(() => {
  store.fetchLightning()
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

.td-nowrap { white-space: nowrap; }
</style>
