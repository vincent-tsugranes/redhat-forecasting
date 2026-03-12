<template>
  <div class="container">
    <h1>{{ $t('volcanicAsh.title') }}</h1>

    <div class="card">
      <div class="card-header-row">
        <h2>{{ $t('volcanicAsh.activeAdvisories') }}</h2>
        <div class="header-actions">
          <input
            v-model="filterText"
            type="text"
            class="filter-input"
            :placeholder="$t('volcanicAsh.filterPlaceholder')"
            aria-label="Filter by volcano or FIR name"
          />
          <button class="btn-sm" :disabled="refreshing" @click="refreshData">
            {{ refreshing ? $t('airport.refreshing') : $t('volcanicAsh.refreshData') }}
          </button>
        </div>
      </div>
    </div>

    <TableSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="!loading && volcanicAsh.length > 0" class="stats-bar">
      <span class="stat-chip stat-alert">
        <span aria-hidden="true">🌋</span> {{ filteredAdvisories.length }} {{ $t('volcanicAsh.active') }}
      </span>
    </div>

    <ErrorBoundary>
      <div v-if="filteredAdvisories.length > 0">
        <div v-for="advisory in filteredAdvisories" :key="advisory.id" class="card">
          <div class="advisory-header">
            <h3>
              <span class="va-badge">VA</span>
              {{ advisory.volcanoName || advisory.firName || 'Volcanic Ash Advisory' }}
            </h3>
            <FreshnessBadge v-if="advisory.fetchedAt" :fetched-at="advisory.fetchedAt" data-type="volcanicAsh" />
          </div>
          <div class="advisory-info">
            <div v-if="advisory.firName" class="info-item">
              <strong>{{ $t('volcanicAsh.fir') }}</strong> {{ advisory.firName }} ({{ advisory.firId }})
            </div>
            <div v-if="advisory.hazard" class="info-item">
              <strong>{{ $t('volcanicAsh.hazard') }}</strong> {{ advisory.hazard }}
            </div>
            <div v-if="advisory.severity" class="info-item">
              <strong>{{ $t('volcanicAsh.severity') }}</strong> {{ advisory.severity }}
            </div>
            <div v-if="advisory.altitudeLowFt || advisory.altitudeHighFt" class="info-item">
              <strong>{{ $t('volcanicAsh.altitude') }}</strong>
              {{ advisory.altitudeLowFt ? advisory.altitudeLowFt.toLocaleString() + ' ft' : 'SFC' }}
              –
              {{ advisory.altitudeHighFt ? advisory.altitudeHighFt.toLocaleString() + ' ft' : 'UNL' }}
            </div>
            <div v-if="advisory.validTimeFrom" class="info-item">
              <strong>{{ $t('volcanicAsh.valid') }}</strong> {{ formatDate(advisory.validTimeFrom) }} – {{ formatDate(advisory.validTimeTo) }}
            </div>
          </div>
          <div v-if="advisory.rawText" class="raw-text">{{ advisory.rawText }}</div>
        </div>
      </div>
    </ErrorBoundary>

    <div v-if="!loading && volcanicAsh.length === 0" class="card">
      <p><span aria-hidden="true">✅</span> {{ $t('volcanicAsh.noActive') }}</p>
      <p>{{ $t('volcanicAsh.autoFetch') }}</p>
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
import FreshnessBadge from '../components/FreshnessBadge.vue'
import ErrorBoundary from '../components/ErrorBoundary.vue'

const store = useWeatherStore()
const toast = useToast()
const { volcanicAsh, volcanicAshLoading: loading, volcanicAshError: error } = storeToRefs(store)

const refreshing = ref(false)
const filterText = ref('')

const filteredAdvisories = computed(() => {
  if (!filterText.value) return volcanicAsh.value
  const q = filterText.value.toLowerCase()
  return volcanicAsh.value.filter(va => {
    const name = va.volcanoName || ''
    const fir = va.firName || ''
    return name.toLowerCase().includes(q) || fir.toLowerCase().includes(q)
  })
})

async function refreshData() {
  refreshing.value = true
  try {
    await store.refreshVolcanicAsh()
    toast.success('Volcanic ash data refreshed')
  } catch {
    toast.error('Failed to refresh volcanic ash data')
  } finally {
    refreshing.value = false
  }
}

onMounted(() => {
  store.fetchVolcanicAsh()
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

.filter-input {
  padding: 4px 8px;
  font-size: 13px;
  border: 1px solid var(--border-color, #ddd);
  border-radius: 4px;
  background: var(--bg-card, white);
  color: var(--text-primary, #333);
  width: 180px;
}

.stats-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.advisory-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.advisory-header h3 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.va-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
  color: white;
  background: var(--volcanic-ash);
  font-size: 12px;
}

.advisory-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin: 12px 0;
}

.info-item {
  font-size: 14px;
}

.raw-text {
  font-family: monospace;
  font-size: 12px;
  background: var(--bg-code, #f5f5f5);
  padding: 10px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
  margin-top: 8px;
  color: var(--text-secondary, #666);
}
</style>
