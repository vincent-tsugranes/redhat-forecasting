<template>
  <div class="container">
    <h1>{{ $t('delay.title') }}</h1>

    <div class="card">
      <div class="card-header-row">
        <h2>{{ $t('delay.currentStatus') }}</h2>
        <div class="header-actions">
          <button class="btn-sm" :disabled="refreshing" @click="refreshData">
            {{ refreshing ? $t('airport.refreshing') : $t('delay.refreshData') }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="card"><p>Loading airport delays...</p></div>
    <div v-if="error" class="error">{{ error }}</div>

    <!-- Summary tiles -->
    <div v-if="!loading" class="stats-row">
      <div class="stat-tile">
        <span class="stat-icon" aria-hidden="true">🔴</span>
        <div class="stat-body">
          <span class="stat-value">{{ delayedCount }}</span>
          <span class="stat-label">Airports Delayed</span>
        </div>
      </div>
      <div class="stat-tile">
        <span class="stat-icon" aria-hidden="true">⏱️</span>
        <div class="stat-body">
          <span class="stat-value">{{ avgDelay ? avgDelay + ' min' : '-' }}</span>
          <span class="stat-label">Avg Delay</span>
        </div>
      </div>
    </div>

    <div v-if="delays.length > 0" class="card">
      <div class="table-wrapper">
        <table class="data-table" aria-label="Airport delays">
          <thead>
            <tr>
              <th>Airport</th>
              <th>Name</th>
              <th>Type</th>
              <th>Reason</th>
              <th>Avg Delay</th>
              <th>Trend</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="delay in delays" :key="delay.id" :class="{ 'row-delayed': delay.isDelayed }">
              <td><strong>{{ delay.airportCode }}</strong></td>
              <td>{{ delay.airportName || '-' }}</td>
              <td>{{ delay.delayType }}</td>
              <td class="td-truncate">{{ delay.reason || '-' }}</td>
              <td>{{ delay.avgDelayMinutes ? delay.avgDelayMinutes + ' min' : '-' }}</td>
              <td>
                <span v-if="delay.trend" class="trend-badge" :class="'trend-' + delay.trend">{{ delay.trend }}</span>
                <span v-else>-</span>
              </td>
              <td>
                <span class="status-dot" :class="delay.isDelayed ? 'status-delayed' : 'status-ok'"></span>
                {{ delay.isDelayed ? 'Delayed' : 'Normal' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else-if="!loading" class="card">
      <p><span aria-hidden="true">✅</span> {{ $t('delay.noDelays') }}</p>
      <p>{{ $t('delay.autoFetch') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'

const store = useWeatherStore()
const toast = useToast()
const { delays, delaysLoading: loading, delaysError: error } = storeToRefs(store)

const refreshing = ref(false)

const delayedCount = computed(() => delays.value.filter(d => d.isDelayed).length)
const avgDelay = computed(() => {
  const delayed = delays.value.filter(d => d.avgDelayMinutes != null && d.avgDelayMinutes > 0)
  if (delayed.length === 0) return null
  const total = delayed.reduce((sum, d) => sum + (d.avgDelayMinutes || 0), 0)
  return Math.round(total / delayed.length)
})

async function refreshData() {
  refreshing.value = true
  try {
    await store.refreshDelays()
    toast.success('Airport delay data refreshed')
  } catch {
    toast.error('Failed to refresh delay data')
  } finally {
    refreshing.value = false
  }
}

onMounted(() => {
  store.fetchDelays()
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

.stats-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
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

.stat-icon { font-size: 24px; flex-shrink: 0; }

.stat-body { display: flex; flex-direction: column; }

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

.td-truncate {
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.row-delayed {
  background: #fff3e0;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 4px;
}

.status-delayed { background: #f44336; }
.status-ok { background: #4caf50; }

.trend-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.trend-increasing { background: #ffebee; color: #c62828; }
.trend-decreasing { background: #e8f5e9; color: #2e7d32; }
.trend-stable { background: #e3f2fd; color: #1565c0; }
</style>
