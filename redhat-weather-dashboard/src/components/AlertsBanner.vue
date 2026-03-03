<template>
  <div v-if="alertError" class="alerts-error">{{ $t('alerts.loadError') }}</div>
  <div v-if="alerts.length > 0" class="alerts-banner" :class="'severity-' + highestSeverity">
    <button
      class="alerts-header"
      :aria-expanded="expanded"
      :aria-label="$t('alerts.toggleAriaLabel')"
      @click="expanded = !expanded"
    >
      <div class="alerts-title">
        <span class="alert-icon" aria-hidden="true">{{ severityIcon }}</span>
        <span class="sr-only">Severity: {{ highestSeverity }}</span>
        <strong>{{ $t('alerts.activeAlerts', { count: alerts.length }, alerts.length) }}</strong>
      </div>
      <span class="expand-btn">{{ expanded ? $t('alerts.hide') : $t('alerts.show') }}</span>
    </button>

    <div v-if="expanded" class="alerts-list">
      <div
        v-for="alert in alerts"
        :key="alert.id"
        class="alert-item"
        :class="'severity-' + (alert.severity || 'Unknown').toLowerCase()"
      >
        <div class="alert-event">
          <span class="severity-badge">{{ alert.severity }}</span>
          {{ alert.event }}
        </div>
        <div v-if="alert.headline" class="alert-headline">{{ alert.headline }}</div>
        <div class="alert-meta">
          <span v-if="alert.areaDesc" class="alert-area">{{ alert.areaDesc }}</span>
          <span v-if="alert.expires" class="alert-expires"
            >{{ $t('alerts.expires') }} {{ formatDate(alert.expires) }}</span
          >
        </div>
        <details v-if="alert.description" class="alert-description">
          <summary>{{ $t('alerts.details') }}</summary>
          <p>{{ alert.description }}</p>
        </details>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { formatDate } from '../utils/dateUtils'

const store = useWeatherStore()
const { alerts, alertsError: alertError } = storeToRefs(store)

const expanded = ref(false)
let refreshInterval: ReturnType<typeof setInterval> | null = null

const severityRank: Record<string, number> = {
  Extreme: 4,
  Severe: 3,
  Moderate: 2,
  Minor: 1,
  Unknown: 0,
}

const highestSeverity = computed(() => {
  if (alerts.value.length === 0) return 'unknown'
  const highest = alerts.value.reduce(
    (max, alert) => {
      const rank = severityRank[alert.severity || 'Unknown'] || 0
      return rank > max.rank ? { severity: alert.severity || 'Unknown', rank } : max
    },
    { severity: 'Unknown', rank: 0 },
  )
  return highest.severity.toLowerCase()
})

const severityIcon = computed(() => {
  switch (highestSeverity.value) {
    case 'extreme':
      return '🔴'
    case 'severe':
      return '🟠'
    case 'moderate':
      return '🟡'
    case 'minor':
      return '🔵'
    default:
      return '⚠️'
  }
})

onMounted(() => {
  store.fetchAlerts()
  refreshInterval = setInterval(() => store.refreshAlerts(), 5 * 60 * 1000)
})

onUnmounted(() => {
  if (refreshInterval) clearInterval(refreshInterval)
})
</script>

<style scoped>
.alerts-banner {
  border-radius: 8px;
  margin-bottom: 20px;
  overflow: hidden;
}

.severity-extreme {
  background: #b71c1c;
  color: white;
}

.severity-severe {
  background: #e65100;
  color: white;
}

.severity-moderate {
  background: #f57f17;
  color: #333;
}

.severity-minor {
  background: #0d47a1;
  color: white;
}

.severity-unknown {
  background: #616161;
  color: white;
}

.alerts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  width: 100%;
  background: none;
  border: none;
  color: inherit;
  font: inherit;
  text-align: left;
}

.alerts-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.alert-icon {
  font-size: 16px;
}

.expand-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: inherit;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.expand-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.alerts-list {
  padding: 0 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  padding: 12px;
}

.alert-event {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.severity-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  background: rgba(255, 255, 255, 0.25);
}

.alert-headline {
  font-size: 13px;
  margin-bottom: 6px;
  opacity: 0.9;
}

.alert-meta {
  display: flex;
  gap: 16px;
  font-size: 11px;
  opacity: 0.8;
  flex-wrap: wrap;
}

.alert-description {
  margin-top: 8px;
  font-size: 12px;
  opacity: 0.9;
}

.alert-description summary {
  cursor: pointer;
  font-weight: 500;
  margin-bottom: 6px;
}

.alert-description p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.5;
  max-height: 200px;
  overflow-y: auto;
}

.alerts-error {
  background: var(--error-bg, #fee);
  color: var(--error-text, #c00);
  padding: 10px 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 13px;
  text-align: center;
}
</style>
