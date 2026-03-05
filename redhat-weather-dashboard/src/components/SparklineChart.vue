<template>
  <div class="sparkline-container">
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
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement)

const props = withDefaults(defineProps<{
  data: number[]
  color?: string
}>(), {
  color: '#ee0000',
})

const chartData = computed(() => ({
  labels: props.data.map((_, i) => String(i)),
  datasets: [
    {
      data: props.data,
      borderColor: props.color,
      borderWidth: 1.5,
      pointRadius: 0,
      tension: 0.4,
      fill: false,
    },
  ],
}))

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: { enabled: false },
  },
  scales: {
    x: { display: false },
    y: { display: false },
  },
  elements: {
    line: { borderWidth: 1.5 },
  },
  animation: {
    duration: 500,
  },
} as const
</script>

<style scoped>
.sparkline-container {
  height: 40px;
  width: 100%;
}
</style>
