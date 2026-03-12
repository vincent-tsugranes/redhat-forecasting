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
        <span aria-hidden="true">⚡</span> {{ lightning.length }} {{ $t('lightning.strikes') }}
      </span>
      <FreshnessBadge v-if="lightning.length > 0 && lightning[0].fetchedAt" :fetched-at="lightning[0].fetchedAt" data-type="lightning" />
    </div>

    <ErrorBoundary>
      <LightningMap v-if="lightning.length > 0" :strikes="lightning" />
    </ErrorBoundary>

    <ErrorBoundary>
      <div v-if="sortedStrikes.length > 0" class="card">
        <div class="table-controls">
          <span class="table-meta">{{ $t('lightning.page') }} {{ currentPage }}/{{ totalPages }} ({{ lightning.length }} total)</span>
          <div class="pagination">
            <button class="btn-sm btn-secondary" :disabled="currentPage <= 1" @click="currentPage--">Prev</button>
            <button class="btn-sm btn-secondary" :disabled="currentPage >= totalPages" @click="currentPage++">Next</button>
          </div>
        </div>
        <div class="table-wrapper">
          <table class="data-table" aria-label="Recent lightning strikes">
            <thead>
              <tr>
                <th role="columnheader" tabindex="0" @click="toggleSort('strikeTime')" @keydown.enter.prevent="toggleSort('strikeTime')" @keydown.space.prevent="toggleSort('strikeTime')">
                  {{ $t('lightning.time') }}
                  <span class="sort-indicator" :class="{ active: sortKey === 'strikeTime' }">{{ sortIcon('strikeTime') }}</span>
                </th>
                <th>{{ $t('lightning.location') }}</th>
                <th role="columnheader" tabindex="0" @click="toggleSort('amplitudeKa')" @keydown.enter.prevent="toggleSort('amplitudeKa')" @keydown.space.prevent="toggleSort('amplitudeKa')">
                  {{ $t('lightning.amplitude') }}
                  <span class="sort-indicator" :class="{ active: sortKey === 'amplitudeKa' }">{{ sortIcon('amplitudeKa') }}</span>
                </th>
                <th>{{ $t('lightning.type') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="strike in paginatedStrikes" :key="strike.id">
                <td class="td-nowrap">{{ formatDate(strike.strikeTime) }}</td>
                <td>{{ strike.latitude.toFixed(2) }}, {{ strike.longitude.toFixed(2) }}</td>
                <td>{{ strike.amplitudeKa ? strike.amplitudeKa.toFixed(1) + ' kA' : '-' }}</td>
                <td>{{ strike.strikeType || 'CG' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </ErrorBoundary>

    <div v-if="!loading && lightning.length === 0" class="card">
      <p><span aria-hidden="true">✅</span> {{ $t('lightning.noRecent') }}</p>
      <p>{{ $t('lightning.autoFetch') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import { formatDate } from '../utils/dateUtils'
import type { LightningStrike } from '../services/weatherService'
import TableSkeleton from '../components/skeletons/TableSkeleton.vue'
import FreshnessBadge from '../components/FreshnessBadge.vue'
import ErrorBoundary from '../components/ErrorBoundary.vue'
import LightningMap from '../components/LightningMap.vue'

const store = useWeatherStore()
const toast = useToast()
const { lightning, lightningLoading: loading, lightningError: error } = storeToRefs(store)

const refreshing = ref(false)
const sortKey = ref<string>('strikeTime')
const sortDir = ref<'asc' | 'desc'>('desc')
const currentPage = ref(1)
const pageSize = 50

const sortedStrikes = computed(() => {
  const data = [...lightning.value]
  const dir = sortDir.value === 'asc' ? 1 : -1
  const key = sortKey.value as keyof LightningStrike
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

const totalPages = computed(() => Math.max(1, Math.ceil(sortedStrikes.value.length / pageSize)))

const paginatedStrikes = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return sortedStrikes.value.slice(start, start + pageSize)
})

function toggleSort(key: string) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = key === 'strikeTime' || key === 'amplitudeKa' ? 'desc' : 'asc'
  }
  currentPage.value = 1
}

function sortIcon(key: string) {
  if (sortKey.value !== key) return '⇅'
  return sortDir.value === 'asc' ? '↑' : '↓'
}

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
  align-items: center;
  margin-bottom: 16px;
}

.table-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.pagination {
  display: flex;
  gap: 4px;
}

.td-nowrap { white-space: nowrap; }

.sort-indicator {
  font-size: 11px;
  opacity: 0.4;
  margin-left: 2px;
}
.sort-indicator.active { opacity: 1; }
</style>
