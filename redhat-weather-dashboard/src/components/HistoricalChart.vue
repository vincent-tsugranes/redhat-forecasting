<template>
  <div class="historical-chart-section card">
    <div class="historical-header">
      <h2>{{ $t('forecast.history.title') }}</h2>
      <div class="period-toggles">
        <button
          v-for="period in periods"
          :key="period.days"
          :class="{ active: selectedDays === period.days }"
          :aria-pressed="selectedDays === period.days"
          @click="changePeriod(period.days)"
        >
          {{ period.label }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="historical-loading">
      <div class="skeleton-chart"></div>
    </div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="historicalForecasts.length === 0" class="historical-empty">
      {{ $t('forecast.history.noData') }}
    </div>
    <div
      v-else
      class="historical-chart-container"
      role="img"
      :aria-label="$t('forecast.history.chartAriaLabel')"
    >
      <Line :data="chartData" :options="chartOptions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js'
import { formatDate } from '../utils/dateUtils'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler)

const props = defineProps<{
  locationId: number
}>()

const store = useWeatherStore()
const {
  historicalForecasts,
  historicalLoading: loading,
  historicalError: error,
} = storeToRefs(store)

const selectedDays = ref(7)

const periods = [
  { days: 7, label: '7d' },
  { days: 14, label: '14d' },
  { days: 30, label: '30d' },
]

function changePeriod(days: number) {
  selectedDays.value = days
  store.fetchHistoricalForecasts(props.locationId, days)
}

watch(
  () => props.locationId,
  (newId) => {
    if (newId) {
      store.fetchHistoricalForecasts(newId, selectedDays.value)
    }
  },
  { immediate: true },
)

interface DailyAggregate {
  date: string
  highF: number
  lowF: number
  avgPrecip: number | null
}

const dailyData = computed<DailyAggregate[]>(() => {
  const grouped = new Map<string, { temps: number[]; precips: number[] }>()

  for (const f of historicalForecasts.value) {
    const dateKey = f.validFrom.substring(0, 10)
    if (!grouped.has(dateKey)) {
      grouped.set(dateKey, { temps: [], precips: [] })
    }
    const g = grouped.get(dateKey)!
    g.temps.push(f.temperatureFahrenheit)
    if (f.precipitationProbability != null) {
      g.precips.push(f.precipitationProbability)
    }
  }

  const result: DailyAggregate[] = []
  for (const [date, group] of grouped) {
    result.push({
      date,
      highF: Math.max(...group.temps),
      lowF: Math.min(...group.temps),
      avgPrecip:
        group.precips.length > 0
          ? Math.round(group.precips.reduce((a, b) => a + b, 0) / group.precips.length)
          : null,
    })
  }

  return result.sort((a, b) => a.date.localeCompare(b.date))
})

const chartData = computed(() => {
  const labels = dailyData.value.map((d) => formatDate(d.date + 'T12:00:00'))

  const datasets = [
    {
      label: 'High (°F)',
      data: dailyData.value.map((d) => d.highF),
      borderColor: '#ee0000',
      backgroundColor: 'rgba(238, 0, 0, 0.1)',
      fill: '+1',
      tension: 0.3,
      pointRadius: 3,
    },
    {
      label: 'Low (°F)',
      data: dailyData.value.map((d) => d.lowF),
      borderColor: '#2196f3',
      backgroundColor: 'rgba(33, 150, 243, 0.1)',
      fill: false,
      tension: 0.3,
      pointRadius: 3,
    },
  ]

  const hasPrecip = dailyData.value.some((d) => d.avgPrecip != null)
  if (hasPrecip) {
    datasets.push({
      label: 'Avg Precip (%)',
      data: dailyData.value.map((d) => d.avgPrecip ?? 0),
      borderColor: '#009688',
      backgroundColor: 'rgba(0, 150, 136, 0.15)',
      fill: false,
      tension: 0.3,
      pointRadius: 2,
    })
  }

  return { labels, datasets }
})

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  interaction: {
    mode: 'index' as const,
    intersect: false,
  },
  plugins: {
    legend: { position: 'top' as const },
    tooltip: { enabled: true },
    filler: { propagate: true },
  },
  scales: {
    x: {
      ticks: { maxRotation: 45, maxTicksLimit: 15 },
    },
    y: {
      type: 'linear' as const,
      position: 'left' as const,
      title: { display: true, text: 'Temperature (°F)' },
    },
  },
}))
</script>

<style scoped>
.historical-chart-section {
  margin-top: 20px;
}

.historical-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.historical-header h2 {
  margin-bottom: 0;
}

.period-toggles {
  display: flex;
  gap: 4px;
}

.period-toggles button {
  padding: 6px 14px;
  font-size: 13px;
  border-radius: 16px;
  background: var(--bg-code, #f5f5f5);
  color: var(--text-secondary, #666);
  border: 1px solid var(--border-color, #ddd);
  cursor: pointer;
  transition: all 0.2s;
}

.period-toggles button:hover {
  background: var(--border-color, #ddd);
}

.period-toggles button.active {
  background: #ee0000;
  color: white;
  border-color: #ee0000;
}

.historical-chart-container {
  position: relative;
  height: clamp(250px, 35vh, 350px);
}

.historical-loading {
  padding: 20px 0;
}

.skeleton-chart {
  height: 250px;
  background: linear-gradient(
    90deg,
    var(--skeleton-base, #e0e0e0) 25%,
    var(--skeleton-shine, #f5f5f5) 50%,
    var(--skeleton-base, #e0e0e0) 75%
  );
  background-size: 200% 100%;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
  border-radius: 8px;
}

@keyframes skeleton-pulse {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.historical-empty {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-secondary, #666);
  font-style: italic;
}
</style>
