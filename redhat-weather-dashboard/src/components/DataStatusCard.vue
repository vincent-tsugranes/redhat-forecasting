<template>
  <div class="status-card">
    <h3><span aria-hidden="true">📊</span> {{ $t('status.title') }}</h3>
    <div v-if="loading" class="loading">{{ $t('status.loadingStatus') }}</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else class="status-content">
      <div class="status-grid">
        <div class="status-item">
          <span class="label">{{ $t('status.airportsLoaded') }}</span>
          <span class="value" :class="{ complete: status.loadingComplete }">
            {{ status.airports?.toLocaleString() }} / {{ status.expectedAirports?.toLocaleString() }}
          </span>
        </div>
        <div class="status-item">
          <span class="label">{{ $t('status.cities') }}</span>
          <span class="value">{{ status.cities }}</span>
        </div>
        <div class="status-item">
          <span class="label">{{ $t('status.totalLocations') }}</span>
          <span class="value">{{ status.totalLocations?.toLocaleString() }}</span>
        </div>
      </div>
      <div
        v-if="status.percentLoaded !== undefined"
        class="progress-bar"
        role="progressbar"
        :aria-valuenow="status.percentLoaded"
        aria-valuemin="0"
        aria-valuemax="100"
        :aria-label="$t('status.loadingProgress')"
      >
        <div class="progress-fill" :style="{ width: status.percentLoaded + '%' }"></div>
        <span class="progress-text">{{ status.percentLoaded }}%</span>
      </div>
      <div
        class="status-badge"
        :class="status.loadingComplete ? 'success' : 'warning'"
        role="status"
      >
        <span aria-hidden="true">{{ status.loadingComplete ? '✓' : '⚠' }}</span>
        {{ status.loadingComplete ? $t('status.dataReady') : $t('status.loading') }}
      </div>

      <div class="data-counts-section">
        <h4>Active Data</h4>
        <div class="data-counts-grid">
          <div class="data-count-item">
            <span class="count-icon" aria-hidden="true">🌤️</span>
            <div class="count-info">
              <span class="count-value">{{ status.activeForecasts?.toLocaleString() ?? 0 }}</span>
              <span class="count-label">{{ $t('status.activeForecasts') }}</span>
            </div>
          </div>
          <div class="data-count-item">
            <span class="count-icon" aria-hidden="true">🌍</span>
            <div class="count-info">
              <span class="count-value">{{ status.activeEarthquakes?.toLocaleString() ?? 0 }}</span>
              <span class="count-label">{{ $t('status.activeEarthquakes') }}</span>
            </div>
          </div>
          <div class="data-count-item">
            <span class="count-icon" aria-hidden="true">🌀</span>
            <div class="count-info">
              <span class="count-value">{{ status.activeHurricanes?.toLocaleString() ?? 0 }}</span>
              <span class="count-label">{{ $t('status.activeHurricanes') }}</span>
            </div>
          </div>
          <div class="data-count-item">
            <span class="count-icon" aria-hidden="true">✈️</span>
            <div class="count-info">
              <span class="count-value">{{ status.metarReports?.toLocaleString() ?? 0 }}</span>
              <span class="count-label">{{ $t('status.metarReports') }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="freshnessSources.length > 0" class="freshness-section">
        <h4>{{ $t('status.dataFreshness') }}</h4>
        <div v-for="source in freshnessSources" :key="source.name" class="freshness-row">
          <span class="label">{{ source.label }}</span>
          <span class="freshness-age" :class="source.freshnessClass">
            {{ source.ageText }}
          </span>
        </div>
      </div>

      <div v-if="schedulerRows.length > 0" class="scheduler-section">
        <h4>{{ $t('status.schedulerTiming') }}</h4>
        <div v-for="row in schedulerRows" :key="row.name" class="scheduler-row">
          <div class="scheduler-header">
            <span class="scheduler-name">
              <span aria-hidden="true">{{ row.icon }}</span> {{ row.name }}
            </span>
            <span class="scheduler-timing">
              <span class="scheduler-last">{{ row.lastRunText }}</span>
              <span class="scheduler-next" :class="row.statusClass">{{ row.nextRunText }}</span>
            </span>
          </div>
          <div
            class="scheduler-progress"
            role="progressbar"
            :aria-valuenow="row.progress"
            aria-valuemin="0"
            aria-valuemax="100"
            :aria-label="row.name + ' refresh cycle progress'"
          >
            <div
              class="scheduler-progress-fill"
              :class="row.progressClass"
              :style="{ width: row.progress + '%' }"
            ></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '../services/api'
import { logger } from '../utils/logger'

interface SchedulerInfo {
  name: string
  source: string
  intervalMinutes: number
  enabled: boolean
  lastRun: string | null
  ageMinutes: number | null
  nextRunMinutes: number | null
}

interface DataStatus {
  totalLocations: number
  airports: number
  cities: number
  airportsLoaded: boolean
  expectedAirports: number
  loadingComplete: boolean
  percentLoaded: number
  activeForecasts: number
  activeEarthquakes: number
  activeHurricanes: number
  metarReports: number
  dataFreshness: Record<string, string | number>
  schedulers: SchedulerInfo[]
}

const status = ref<DataStatus>({
  totalLocations: 0,
  airports: 0,
  cities: 0,
  airportsLoaded: false,
  expectedAirports: 9313,
  loadingComplete: false,
  percentLoaded: 0,
  activeForecasts: 0,
  activeEarthquakes: 0,
  activeHurricanes: 0,
  metarReports: 0,
  dataFreshness: {},
  schedulers: [],
})
const loading = ref(true)
const error = ref<string | null>(null)

const SOURCE_LABELS: Record<string, string> = {
  forecasts: 'Forecasts',
  metar: 'Airport METAR',
  hurricanes: 'Hurricanes',
  earthquakes: 'Earthquakes',
  'space-weather': 'Space Weather',
  alerts: 'Weather Alerts',
}

const freshnessSources = computed(() => {
  const df = status.value.dataFreshness
  if (!df) return []

  const sources: { name: string; label: string; ageMinutes: number; ageText: string; freshnessClass: string }[] = []
  const seen = new Set<string>()

  for (const key of Object.keys(df)) {
    const match = key.match(/^(.+)\.ageMinutes$/)
    if (!match) continue
    const name = match[1]
    if (seen.has(name)) continue
    seen.add(name)

    const ageMinutes = Number(df[`${name}.ageMinutes`] ?? 0)
    const label = SOURCE_LABELS[name] || name.charAt(0).toUpperCase() + name.slice(1)

    let ageText: string
    if (ageMinutes < 1) {
      ageText = 'Just now'
    } else if (ageMinutes < 60) {
      ageText = `${ageMinutes} min ago`
    } else {
      const hours = Math.floor(ageMinutes / 60)
      ageText = `${hours}h ${ageMinutes % 60}m ago`
    }

    let freshnessClass: string
    if (ageMinutes <= 15) {
      freshnessClass = 'freshness-fresh'
    } else if (ageMinutes <= 60) {
      freshnessClass = 'freshness-aging'
    } else {
      freshnessClass = 'freshness-stale'
    }

    sources.push({ name, label, ageMinutes, ageText, freshnessClass })
  }

  return sources.sort((a, b) => a.ageMinutes - b.ageMinutes)
})

const SCHEDULER_ICONS: Record<string, string> = {
  'noaa-forecast': '🌤️',
  'aviation-metar': '✈️',
  'usgs-earthquake': '🌍',
  'nhc-hurricane': '🌀',
  'noaa-alerts': '⚠️',
}

const schedulerRows = computed(() => {
  const schedulers = status.value.schedulers
  if (!schedulers || schedulers.length === 0) return []

  return schedulers.filter(s => s.enabled).map(s => {
    const icon = SCHEDULER_ICONS[s.source] || '📡'
    const age = s.ageMinutes
    const interval = s.intervalMinutes
    const nextRun = s.nextRunMinutes

    let lastRunText: string
    if (age == null) {
      lastRunText = 'Never'
    } else if (age < 1) {
      lastRunText = 'Just now'
    } else if (age < 60) {
      lastRunText = `${age}m ago`
    } else {
      lastRunText = `${Math.floor(age / 60)}h ${age % 60}m ago`
    }

    let nextRunText: string
    let statusClass: string
    if (nextRun == null) {
      nextRunText = 'Pending'
      statusClass = 'scheduler-pending'
    } else if (nextRun === 0) {
      nextRunText = 'Due now'
      statusClass = 'scheduler-due'
    } else {
      nextRunText = `in ${nextRun}m`
      statusClass = 'scheduler-ok'
    }

    // Progress: percentage of interval elapsed
    const progress = age != null ? Math.min(100, Math.round((age / interval) * 100)) : 0
    const progressClass = progress >= 100 ? 'progress-overdue' : progress >= 75 ? 'progress-soon' : 'progress-ok'

    return { name: s.name, icon, lastRunText, nextRunText, statusClass, progress, progressClass, interval }
  })
})

const fetchStatus = async () => {
  try {
    loading.value = true
    error.value = null
    const response = await api.get('/api/status/data')
    status.value = response.data
  } catch (err) {
    error.value = 'Failed to load data status'
    logger.error('Error fetching data status:', err)
  } finally {
    loading.value = false
  }
}

let refreshInterval: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchStatus()
  refreshInterval = setInterval(() => {
    fetchStatus()
  }, 30000)
})

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
    refreshInterval = null
  }
})
</script>

<style scoped>
.status-card {
  background: var(--bg-card, white);
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px var(--shadow, rgba(0, 0, 0, 0.1));
  margin-bottom: 20px;
}

h3 {
  margin: 0 0 15px 0;
  color: var(--text-primary, #333);
  font-size: 1.2rem;
}

.loading,
.error {
  padding: 10px;
  text-align: center;
  color: var(--text-secondary, #666);
}

.error {
  color: #d32f2f;
}

.status-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-grid {
  display: flex;
  flex-direction: column;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-light, #eee);
}

.label {
  font-weight: 500;
  color: var(--text-secondary, #666);
}

.value {
  font-weight: 600;
  color: var(--text-primary, #333);
}

.value.complete {
  color: #4caf50;
}

.progress-bar {
  position: relative;
  height: 30px;
  background: var(--bg-code, #f0f0f0);
  border-radius: 15px;
  overflow: hidden;
  margin: 10px 0;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4caf50, #8bc34a);
  transition: width 0.3s ease;
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-weight: 600;
  color: var(--text-primary, #333);
  font-size: 0.9rem;
}

.status-badge {
  padding: 10px 15px;
  border-radius: 6px;
  text-align: center;
  font-weight: 600;
  margin-top: 10px;
}

.status-badge.success {
  background: #e8f5e9;
  color: #2e7d32;
}

.status-badge.warning {
  background: #fff3e0;
  color: #e65100;
}

.data-counts-section {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid var(--border-light, #eee);
}

.data-counts-section h4 {
  margin: 0 0 10px 0;
  font-size: 0.95rem;
  color: var(--text-primary, #333);
}

.data-counts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.data-count-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: var(--bg-code, #f9f9f9);
  border-radius: 6px;
}

.count-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.count-info {
  display: flex;
  flex-direction: column;
}

.count-value {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--text-primary, #333);
  line-height: 1.2;
}

.count-label {
  font-size: 11px;
  color: var(--text-secondary, #666);
  white-space: nowrap;
}

.freshness-section {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid var(--border-light, #eee);
}

.freshness-section h4 {
  margin: 0 0 10px 0;
  font-size: 0.95rem;
  color: var(--text-primary, #333);
}

.freshness-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 0;
}

.freshness-age {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.freshness-fresh {
  background: #e8f5e9;
  color: #2e7d32;
}

.freshness-aging {
  background: #fff3e0;
  color: #e65100;
}

.freshness-stale {
  background: #ffebee;
  color: #c62828;
}

.scheduler-section {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid var(--border-light, #eee);
}

.scheduler-section h4 {
  margin: 0 0 10px 0;
  font-size: 0.95rem;
  color: var(--text-primary, #333);
}

.scheduler-row {
  margin-bottom: 10px;
}

.scheduler-row:last-child {
  margin-bottom: 0;
}

.scheduler-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.scheduler-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary, #333);
}

.scheduler-timing {
  display: flex;
  gap: 10px;
  font-size: 12px;
}

.scheduler-last {
  color: var(--text-secondary, #999);
}

.scheduler-next {
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 8px;
  font-size: 11px;
}

.scheduler-ok {
  background: #e8f5e9;
  color: #2e7d32;
}

.scheduler-due {
  background: #fff3e0;
  color: #e65100;
}

.scheduler-pending {
  background: var(--bg-code, #f5f5f5);
  color: var(--text-secondary, #999);
}

.scheduler-progress {
  height: 4px;
  background: var(--bg-code, #eee);
  border-radius: 2px;
  overflow: hidden;
}

.scheduler-progress-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.5s ease;
}

.progress-ok {
  background: #4caf50;
}

.progress-soon {
  background: #ff9800;
}

.progress-overdue {
  background: #f44336;
}

@media (max-width: 480px) {
  .data-counts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
