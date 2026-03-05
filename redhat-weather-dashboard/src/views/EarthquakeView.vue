<template>
  <div class="container">
    <h1>{{ $t('earthquake.title') }}</h1>

    <div class="card">
      <div style="display: flex; justify-content: space-between; align-items: center">
        <h2>{{ $t('earthquake.recentActivity') }}</h2>
        <button :disabled="refreshing" @click="refreshData">
          {{ refreshing ? $t('airport.refreshing') : $t('earthquake.refreshData') }}
        </button>
      </div>
    </div>

    <EarthquakeMap
      v-if="earthquakes.length > 0"
      :earthquakes="earthquakes"
      @quake-selected="onQuakeSelected"
    />

    <EarthquakeSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="earthquakes.length > 0">
      <div
        v-for="quake in earthquakes"
        :key="quake.id"
        class="card"
        :class="{ 'card-selected': selectedQuakeId === quake.usgsId }"
      >
        <div class="quake-header">
          <h3>
            <span
              class="magnitude-badge"
              :class="getMagnitudeClass(quake.magnitude)"
            >M{{ quake.magnitude }}</span>
            {{ quake.place }}
          </h3>
        </div>

        <div class="quake-info">
          <div class="info-item">
            <strong>{{ $t('earthquake.depth') }}</strong>
            {{ quake.depthKm }} km
          </div>
          <div class="info-item">
            <strong>{{ $t('earthquake.time') }}</strong>
            {{ formatDate(quake.eventTime) }}
          </div>
          <div v-if="quake.felt" class="info-item">
            <strong>{{ $t('earthquake.felt') }}</strong>
            {{ quake.felt }} {{ $t('earthquake.reports') }}
          </div>
          <div v-if="quake.tsunami" class="info-item tsunami-warning">
            <span aria-hidden="true">🌊</span> {{ $t('earthquake.tsunamiWarning') }}
          </div>
          <div v-if="quake.alert" class="info-item">
            <strong>{{ $t('earthquake.alert') }}</strong>
            <span class="alert-badge" :class="'alert-' + quake.alert">{{ quake.alert }}</span>
          </div>
        </div>

        <div class="quake-location">
          <strong>{{ $t('earthquake.position') }}</strong>
          {{ quake.latitude.toFixed(3) }}°{{ quake.latitude >= 0 ? 'N' : 'S' }},
          {{ Math.abs(quake.longitude).toFixed(3) }}°{{ quake.longitude >= 0 ? 'E' : 'W' }}
        </div>

        <div v-if="quake.fetchedAt" class="quake-time">
          <FreshnessBadge :fetched-at="quake.fetchedAt" data-type="earthquake" />
        </div>
      </div>
    </div>

    <div v-else-if="!loading" class="card">
      <p><span aria-hidden="true">✅</span> {{ $t('earthquake.noRecent') }}</p>
      <p>{{ $t('earthquake.autoFetch') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import { formatDate } from '../utils/dateUtils'
import FreshnessBadge from '../components/FreshnessBadge.vue'
import EarthquakeMap from '../components/EarthquakeMap.vue'
import EarthquakeSkeleton from '../components/skeletons/EarthquakeSkeleton.vue'

const store = useWeatherStore()
const toast = useToast()
const { earthquakes, earthquakesLoading: loading, earthquakesError: error } = storeToRefs(store)

const refreshing = ref(false)
const selectedQuakeId = ref<string | null>(null)

function onQuakeSelected(usgsId: string) {
  selectedQuakeId.value = usgsId
}

async function refreshData() {
  refreshing.value = true
  try {
    await store.refreshEarthquakes()
    toast.success('Earthquake data refreshed')
  } catch {
    toast.error('Failed to refresh earthquake data')
  } finally {
    refreshing.value = false
  }
}

function getMagnitudeClass(magnitude: number) {
  if (magnitude >= 7) return 'mag-major'
  if (magnitude >= 5) return 'mag-strong'
  if (magnitude >= 4) return 'mag-moderate'
  return 'mag-light'
}

onMounted(() => {
  store.fetchEarthquakes()
})
</script>

<style scoped>
.quake-header {
  margin-bottom: 10px;
}

.quake-header h3 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.magnitude-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  font-weight: bold;
  color: white;
  font-size: 14px;
  white-space: nowrap;
}

.mag-light {
  background: #4caf50;
}

.mag-moderate {
  background: #ff9800;
}

.mag-strong {
  background: #f44336;
}

.mag-major {
  background: #9c27b0;
}

.quake-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin: 12px 0;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tsunami-warning {
  color: #f44336;
  font-weight: bold;
}

.alert-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
  color: white;
  text-transform: uppercase;
  font-size: 12px;
}

.alert-green {
  background: #4caf50;
}

.alert-yellow {
  background: #ffc107;
  color: #333;
}

.alert-orange {
  background: #ff9800;
}

.alert-red {
  background: #f44336;
}

.quake-location,
.quake-time {
  margin: 8px 0;
  color: var(--text-secondary, #666);
  font-size: 13px;
}

.card-selected {
  border: 2px solid #ee0000;
  box-shadow: 0 0 12px rgba(238, 0, 0, 0.2);
}
</style>
