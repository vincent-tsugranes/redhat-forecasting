<template>
  <div class="container">
    <h1>{{ $t('space.title') }}</h1>

    <div class="card">
      <div style="display: flex; justify-content: space-between; align-items: center">
        <h2>{{ $t('space.currentConditions') }}</h2>
        <button :disabled="refreshing" @click="refreshData">
          {{ refreshing ? $t('airport.refreshing') : $t('space.refreshData') }}
        </button>
      </div>
    </div>

    <SpaceWeatherSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="spaceWeather" class="grid">
      <div class="card kp-card">
        <h3>{{ $t('space.kpIndex') }}</h3>
        <KpIndexGauge :value="spaceWeather.kpIndex" />
        <div class="kp-level" :class="'level-' + spaceWeather.kpLevel.replace(/\s+/g, '-')">
          {{ spaceWeather.kpLevel }}
        </div>
      </div>

      <div class="card">
        <h3><span aria-hidden="true">☀️</span> {{ $t('space.geomagneticStorm') }}</h3>
        <div class="metric-value" :class="getStormClass(spaceWeather.geomagneticStormLevel)">
          {{ spaceWeather.geomagneticStormLevel }}
        </div>
        <div class="metric-label">{{ getStormDescription(spaceWeather.geomagneticStormLevel) }}</div>
      </div>

      <div class="card">
        <h3><span aria-hidden="true">🌌</span> {{ $t('space.auroraForecast') }}</h3>
        <div class="metric-value">{{ spaceWeather.auroraChance }}</div>
        <div class="metric-label">{{ $t('space.auroraChance') }}</div>
      </div>

      <div v-if="spaceWeather.solarWindSpeed" class="card">
        <h3><span aria-hidden="true">💨</span> {{ $t('space.solarWind') }}</h3>
        <div class="metric-value">{{ spaceWeather.solarWindSpeed?.toFixed(1) }}</div>
        <div class="metric-label">nT ({{ $t('space.magneticField') }})</div>
      </div>
    </div>

    <div v-if="spaceWeather && spaceWeather.alerts.length > 0" class="card">
      <h2>{{ $t('space.activeAlerts') }}</h2>
      <div v-for="(alert, index) in spaceWeather.alerts" :key="index" class="space-alert">
        <div v-if="alert.issueTime" class="alert-time">{{ alert.issueTime }}</div>
        <div class="alert-message">{{ alert.message }}</div>
      </div>
    </div>

    <div v-if="spaceWeather && spaceWeather.fetchedAt" class="card">
      <FreshnessBadge :fetched-at="spaceWeather.fetchedAt" data-type="space" />
    </div>

    <div v-if="!loading && !spaceWeather && !error" class="card">
      <p>{{ $t('space.noData') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import weatherService, { type SpaceWeather } from '../services/weatherService'
import FreshnessBadge from '../components/FreshnessBadge.vue'
import KpIndexGauge from '../components/KpIndexGauge.vue'
import SpaceWeatherSkeleton from '../components/skeletons/SpaceWeatherSkeleton.vue'

const spaceWeather = ref<SpaceWeather | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const refreshing = ref(false)
let refreshInterval: ReturnType<typeof setInterval> | null = null

async function fetchData() {
  loading.value = true
  error.value = null
  try {
    spaceWeather.value = await weatherService.getSpaceWeather()
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Failed to load space weather data'
  } finally {
    loading.value = false
  }
}

async function refreshData() {
  refreshing.value = true
  try {
    spaceWeather.value = await weatherService.getSpaceWeather()
  } catch {
    // keep existing data
  } finally {
    refreshing.value = false
  }
}

function getStormClass(level?: string): string {
  if (!level) return ''
  const num = parseInt(level.replace('G', ''))
  if (num >= 4) return 'storm-extreme'
  if (num >= 2) return 'storm-moderate'
  return 'storm-quiet'
}

function getStormDescription(level?: string): string {
  if (!level) return ''
  switch (level) {
    case 'G0': return 'No storm'
    case 'G1': return 'Minor'
    case 'G2': return 'Moderate'
    case 'G3': return 'Strong'
    case 'G4': return 'Severe'
    case 'G5': return 'Extreme'
    default: return ''
  }
}

onMounted(() => {
  fetchData()
  refreshInterval = setInterval(refreshData, 5 * 60 * 1000)
})

onUnmounted(() => {
  if (refreshInterval) clearInterval(refreshInterval)
})
</script>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.kp-card {
  text-align: center;
}

.kp-level {
  margin-top: 8px;
  font-weight: 600;
  text-transform: uppercase;
  font-size: 14px;
  letter-spacing: 0.5px;
}

.level-quiet { color: #4caf50; }
.level-unsettled { color: #ff9800; }
.level-storm { color: #f44336; }
.level-strong-storm { color: #9c27b0; }
.level-extreme { color: #880e4f; }

.metric-value {
  font-size: 28px;
  font-weight: 700;
  margin: 12px 0 4px;
  color: var(--text-primary, #333);
}

.metric-label {
  font-size: 13px;
  color: var(--text-secondary, #666);
}

.storm-quiet { color: #4caf50; }
.storm-moderate { color: #ff9800; }
.storm-extreme { color: #f44336; }

.space-alert {
  border: 1px solid var(--border-light, #eee);
  border-radius: 8px;
  padding: 12px;
  margin: 8px 0;
  background: var(--bg-code, #f9f9f9);
}

.alert-time {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-bottom: 6px;
}

.alert-message {
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
}
</style>
