<template>
  <div class="status-card">
    <div class="card-header">
      <h3><span aria-hidden="true">📊</span> {{ $t('status.title') }}</h3>
      <span
        class="status-badge"
        :class="status.loadingComplete ? 'success' : 'warning'"
        role="status"
      >
        <span aria-hidden="true">{{ status.loadingComplete ? '✓' : '⚠' }}</span>
        {{ status.loadingComplete ? $t('status.dataReady') : $t('status.loading') }}
      </span>
    </div>
    <div v-if="loading" class="loading">{{ $t('status.loadingStatus') }}</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else class="status-content">
      <div class="location-row">
        <span class="loc-stat">
          <span class="label">Airports:</span>
          <span class="value" :class="{ complete: status.loadingComplete }">{{ status.airports?.toLocaleString() }}/{{ status.expectedAirports?.toLocaleString() }}</span>
        </span>
        <div
          v-if="status.percentLoaded !== undefined && !status.loadingComplete"
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
      </div>

      <div class="data-counts-grid">
        <div class="data-count-item">
          <span class="count-icon" aria-hidden="true">🌤️</span>
          <span class="count-value">{{ status.activeForecasts?.toLocaleString() ?? 0 }}</span>
          <span class="count-label">{{ $t('status.activeForecasts') }}</span>
        </div>
        <div class="data-count-item">
          <span class="count-icon" aria-hidden="true">🌍</span>
          <span class="count-value">{{ status.activeEarthquakes?.toLocaleString() ?? 0 }}</span>
          <span class="count-label">{{ $t('status.activeEarthquakes') }}</span>
        </div>
        <div class="data-count-item">
          <span class="count-icon" aria-hidden="true">🌀</span>
          <span class="count-value">{{ status.activeHurricanes?.toLocaleString() ?? 0 }}</span>
          <span class="count-label">{{ $t('status.activeHurricanes') }}</span>
        </div>
        <div class="data-count-item">
          <span class="count-icon" aria-hidden="true">✈️</span>
          <span class="count-value">{{ status.metarReports?.toLocaleString() ?? 0 }}</span>
          <span class="count-label">{{ $t('status.metarReports') }}</span>
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
              <span v-if="row.freshnessClass" class="freshness-age" :class="row.freshnessClass">{{ row.freshnessText }}</span>
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

      <div v-if="standaloneFreshness.length > 0" class="freshness-section">
        <h4>{{ $t('status.dataFreshness') }}</h4>
        <div v-for="source in standaloneFreshness" :key="source.name" class="freshness-row">
          <span class="label">{{ source.label }}</span>
          <span class="freshness-age" :class="source.freshnessClass">
            {{ source.ageText }}
          </span>
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
  airports: number
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
  airports: 0,
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
  'swpc-space-weather': 'Space Weather',
  alerts: 'Weather Alerts',
}

// Map freshness source names to scheduler source keys
const FRESHNESS_TO_SCHEDULER: Record<string, string> = {
  forecasts: 'noaa-forecast',
  metar: 'aviation-metar',
  hurricanes: 'nhc-hurricane',
  earthquakes: 'usgs-earthquake',
  'swpc-space-weather': 'swpc-space-weather',
  alerts: 'noaa-alerts',
}

function parseFreshness(df: Record<string, string | number>) {
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
      ageText = `${ageMinutes}m ago`
    } else {
      const hours = Math.floor(ageMinutes / 60)
      ageText = `${hours}h${ageMinutes % 60}m`
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
}

const freshnessSources = computed(() => {
  const df = status.value.dataFreshness
  if (!df) return []
  return parseFreshness(df)
})

// Freshness sources not covered by any scheduler row
const standaloneFreshness = computed(() => {
  const schedulerSourceKeys = new Set(
    (status.value.schedulers || []).filter(s => s.enabled).map(s => s.source)
  )
  return freshnessSources.value.filter(f => {
    const schedulerKey = FRESHNESS_TO_SCHEDULER[f.name]
    return !schedulerKey || !schedulerSourceKeys.has(schedulerKey)
  })
})

const SCHEDULER_ICONS: Record<string, string> = {
  'noaa-forecast': '🌤️',
  'aviation-metar': '✈️',
  'usgs-earthquake': '🌍',
  'nhc-hurricane': '🌀',
  'noaa-alerts': '⚠️',
  'swpc-space-weather': '☀️',
}

// Reverse map: scheduler source key -> freshness source name
const SCHEDULER_TO_FRESHNESS: Record<string, string> = Object.fromEntries(
  Object.entries(FRESHNESS_TO_SCHEDULER).map(([k, v]) => [v, k])
)

const schedulerRows = computed(() => {
  const schedulers = status.value.schedulers
  if (!schedulers || schedulers.length === 0) return []

  const freshnessMap = new Map(freshnessSources.value.map(f => [f.name, f]))

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
      lastRunText = `${Math.floor(age / 60)}h${age % 60}m`
    }

    let nextRunText: string
    let statusClass: string
    if (nextRun == null) {
      nextRunText = 'Pending'
      statusClass = 'scheduler-pending'
    } else if (nextRun === 0) {
      nextRunText = 'Due'
      statusClass = 'scheduler-due'
    } else {
      nextRunText = `in ${nextRun}m`
      statusClass = 'scheduler-ok'
    }

    // Progress: percentage of interval elapsed
    const progress = age != null ? Math.min(100, Math.round((age / interval) * 100)) : 0
    const progressClass = progress >= 100 ? 'progress-overdue' : progress >= 75 ? 'progress-soon' : 'progress-ok'

    // Merge freshness data if available
    const freshnessKey = SCHEDULER_TO_FRESHNESS[s.source]
    const freshness = freshnessKey ? freshnessMap.get(freshnessKey) : undefined
    const freshnessClass = freshness?.freshnessClass || ''
    const freshnessText = freshness?.ageText || ''

    return { name: s.name, icon, lastRunText, nextRunText, statusClass, progress, progressClass, interval, freshnessClass, freshnessText }
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
  padding: 14px;
  box-shadow: 0 2px 4px var(--shadow, rgba(0, 0, 0, 0.1));
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

h3 {
  margin: 0;
  color: var(--text-primary, #333);
  font-size: 1.1rem;
}

.loading,
.error {
  padding: 6px;
  text-align: center;
  color: var(--text-secondary, #666);
  font-size: 13px;
}

.error {
  color: #d32f2f;
}

.status-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.location-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.loc-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.label {
  font-weight: 500;
  color: var(--text-secondary, #666);
  font-size: 12px;
}

.value {
  font-weight: 600;
  color: var(--text-primary, #333);
  font-size: 13px;
}

.value.complete {
  color: #4caf50;
}

.progress-bar {
  position: relative;
  height: 16px;
  background: var(--bg-code, #f0f0f0);
  border-radius: 8px;
  overflow: hidden;
  flex: 1;
  min-width: 60px;
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
  font-size: 11px;
}

.status-badge {
  padding: 3px 10px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 12px;
  white-space: nowrap;
}

.status-badge.success {
  background: #e8f5e9;
  color: #2e7d32;
}

.status-badge.warning {
  background: #fff3e0;
  color: #e65100;
}

.data-counts-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.data-count-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 6px;
  background: var(--bg-code, #f9f9f9);
  border-radius: 4px;
}

.count-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.count-value {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--text-primary, #333);
  line-height: 1;
}

.count-label {
  font-size: 10px;
  color: var(--text-secondary, #666);
  white-space: nowrap;
  display: none;
}

.data-count-item:hover .count-label {
  display: block;
}

.freshness-section {
  padding-top: 6px;
  border-top: 1px solid var(--border-light, #eee);
}

.freshness-section h4 {
  margin: 0 0 4px 0;
  font-size: 0.85rem;
  color: var(--text-primary, #333);
}

.freshness-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 2px 0;
}

.freshness-age {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 8px;
  font-size: 11px;
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
  padding-top: 6px;
  border-top: 1px solid var(--border-light, #eee);
}

.scheduler-section h4 {
  margin: 0 0 4px 0;
  font-size: 0.85rem;
  color: var(--text-primary, #333);
}

.scheduler-row {
  margin-bottom: 5px;
}

.scheduler-row:last-child {
  margin-bottom: 0;
}

.scheduler-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
}

.scheduler-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-primary, #333);
}

.scheduler-timing {
  display: flex;
  gap: 6px;
  align-items: center;
  font-size: 11px;
}

.scheduler-last {
  color: var(--text-secondary, #999);
}

.scheduler-next {
  font-weight: 600;
  padding: 1px 5px;
  border-radius: 8px;
  font-size: 10px;
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
  height: 3px;
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
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
