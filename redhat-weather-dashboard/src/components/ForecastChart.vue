<template>
  <div class="forecast-chart-container">
    <Line :data="chartData" :options="chartOptions" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
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
import type { WeatherForecast } from '../services/weatherService'
import { formatDate } from '../utils/dateUtils'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler)

const props = defineProps<{
  forecasts: WeatherForecast[]
}>()

const chartData = computed(() => {
  const sorted = [...props.forecasts].sort(
    (a, b) => new Date(a.validFrom).getTime() - new Date(b.validFrom).getTime()
  )

  const labels = sorted.map((f) => formatDate(f.validFrom))
  const hasPrecip = sorted.some((f) => f.precipitationProbability != null)

  const datasets = [
    {
      label: 'Temperature (°F)',
      data: sorted.map((f) => f.temperatureFahrenheit),
      borderColor: '#ee0000',
      backgroundColor: 'rgba(238, 0, 0, 0.1)',
      yAxisID: 'y',
      tension: 0.3,
      pointRadius: 3,
    },
    {
      label: 'Wind (mph)',
      data: sorted.map((f) => f.windSpeedMph),
      borderColor: '#2196f3',
      backgroundColor: 'rgba(33, 150, 243, 0.1)',
      yAxisID: 'y1',
      tension: 0.3,
      pointRadius: 3,
    },
  ]

  if (hasPrecip) {
    datasets.push({
      label: 'Precip (%)',
      data: sorted.map((f) => f.precipitationProbability ?? null) as number[],
      borderColor: '#009688',
      backgroundColor: 'rgba(0, 150, 136, 0.15)',
      yAxisID: 'y1',
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
    legend: {
      position: 'top' as const,
    },
    tooltip: {
      enabled: true,
    },
  },
  scales: {
    x: {
      ticks: {
        maxRotation: 45,
        maxTicksLimit: 12,
      },
    },
    y: {
      type: 'linear' as const,
      position: 'left' as const,
      title: {
        display: true,
        text: 'Temperature (°F)',
      },
    },
    y1: {
      type: 'linear' as const,
      position: 'right' as const,
      title: {
        display: true,
        text: 'Wind (mph) / Precip (%)',
      },
      grid: {
        drawOnChartArea: false,
      },
    },
  },
}))
</script>

<style scoped>
.forecast-chart-container {
  position: relative;
  height: 350px;
  margin-bottom: 20px;
}
</style>
