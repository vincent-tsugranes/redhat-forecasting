<template>
  <div class="container">
    <h1>{{ $t('volcanicAsh.title') }}</h1>

    <div class="card">
      <div class="card-header-row">
        <h2>{{ $t('volcanicAsh.activeAdvisories') }}</h2>
        <div class="header-actions">
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
        <span aria-hidden="true">🌋</span> {{ volcanicAsh.length }} Active
      </span>
    </div>

    <div v-if="volcanicAsh.length > 0">
      <div v-for="advisory in volcanicAsh" :key="advisory.id" class="card">
        <div class="advisory-header">
          <h3>
            <span class="va-badge">VA</span>
            {{ advisory.volcanoName || advisory.firName || 'Volcanic Ash Advisory' }}
          </h3>
        </div>
        <div class="advisory-info">
          <div v-if="advisory.firName" class="info-item">
            <strong>FIR:</strong> {{ advisory.firName }} ({{ advisory.firId }})
          </div>
          <div v-if="advisory.hazard" class="info-item">
            <strong>Hazard:</strong> {{ advisory.hazard }}
          </div>
          <div v-if="advisory.severity" class="info-item">
            <strong>Severity:</strong> {{ advisory.severity }}
          </div>
          <div v-if="advisory.altitudeLowFt || advisory.altitudeHighFt" class="info-item">
            <strong>Altitude:</strong>
            {{ advisory.altitudeLowFt ? advisory.altitudeLowFt.toLocaleString() + ' ft' : 'SFC' }}
            –
            {{ advisory.altitudeHighFt ? advisory.altitudeHighFt.toLocaleString() + ' ft' : 'UNL' }}
          </div>
          <div v-if="advisory.validTimeFrom" class="info-item">
            <strong>Valid:</strong> {{ formatDate(advisory.validTimeFrom) }} – {{ formatDate(advisory.validTimeTo) }}
          </div>
        </div>
        <div v-if="advisory.rawText" class="raw-text">{{ advisory.rawText }}</div>
      </div>
    </div>

    <div v-else-if="!loading" class="card">
      <p><span aria-hidden="true">✅</span> {{ $t('volcanicAsh.noActive') }}</p>
      <p>{{ $t('volcanicAsh.autoFetch') }}</p>
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
const { volcanicAsh, volcanicAshLoading: loading, volcanicAshError: error } = storeToRefs(store)

const refreshing = ref(false)

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
  background: #795548;
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
