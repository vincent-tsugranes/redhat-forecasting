<template>
  <div v-if="hasWindData" class="card">
    <h2>{{ $t('forecast.windRose') }}</h2>
    <div class="wind-rose-container" role="img" :aria-label="$t('forecast.windRose') + ': wind direction and speed distribution chart'">
      <PolarArea :data="chartData" :options="chartOptions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { PolarArea } from 'vue-chartjs'
import {
  Chart as ChartJS,
  RadialLinearScale,
  ArcElement,
  Tooltip,
  Legend,
} from 'chart.js'
import type { WeatherForecast } from '../services/weatherService'

ChartJS.register(RadialLinearScale, ArcElement, Tooltip, Legend)

const props = defineProps<{
  forecasts: WeatherForecast[]
}>()

const DIRECTIONS = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE', 'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW']

function degreesToIndex(deg: number): number {
  return Math.round(((deg % 360) / 22.5)) % 16
}

const hasWindData = computed(() =>
  props.forecasts.some((f) => f.windDirection != null),
)

const windStats = computed(() => {
  const counts = new Array(16).fill(0)
  const speedSums = new Array(16).fill(0)

  for (const f of props.forecasts) {
    if (f.windDirection == null) continue
    const idx = degreesToIndex(f.windDirection)
    counts[idx]++
    speedSums[idx] += f.windSpeedMph ?? 0
  }

  const total = counts.reduce((a: number, b: number) => a + b, 0)
  return DIRECTIONS.map((dir, i) => ({
    direction: dir,
    frequency: total > 0 ? (counts[i] / total) * 100 : 0,
    avgSpeed: counts[i] > 0 ? speedSums[i] / counts[i] : 0,
  }))
})

function getSpeedColor(speed: number): string {
  if (speed >= 25) return 'rgba(156, 39, 176, 0.7)' // purple
  if (speed >= 15) return 'rgba(244, 67, 54, 0.7)'  // red
  if (speed >= 10) return 'rgba(255, 152, 0, 0.7)'  // orange
  if (speed >= 5) return 'rgba(76, 175, 80, 0.7)'   // green
  return 'rgba(33, 150, 243, 0.7)'                    // blue
}

const chartData = computed(() => ({
  labels: windStats.value.map((s) => s.direction),
  datasets: [
    {
      data: windStats.value.map((s) => Number(s.frequency.toFixed(1))),
      backgroundColor: windStats.value.map((s) => getSpeedColor(s.avgSpeed)),
      borderColor: windStats.value.map((s) => getSpeedColor(s.avgSpeed).replace('0.7', '1')),
      borderWidth: 1,
    },
  ],
}))

const chartOptions = {
  responsive: true,
  maintainAspectRatio: true,
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: (ctx: { dataIndex: number; raw: unknown }) => {
          const stat = windStats.value[ctx.dataIndex]
          return `${stat.direction}: ${ctx.raw}% (avg ${stat.avgSpeed.toFixed(0)} mph)`
        },
      },
    },
  },
  scales: {
    r: {
      ticks: {
        display: false,
      },
      grid: {
        color: 'rgba(0, 0, 0, 0.1)',
      },
      pointLabels: {
        font: { size: 11 },
        color: '#666',
      },
    },
  },
}
</script>

<style scoped>
.wind-rose-container {
  max-width: 400px;
  margin: 0 auto;
}
</style>
