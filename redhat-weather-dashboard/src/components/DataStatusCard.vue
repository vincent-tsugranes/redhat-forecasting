<template>
  <div class="status-card">
    <h3>📊 Data Status</h3>
    <div v-if="loading" class="loading">Loading status...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else class="status-content">
      <div class="status-item">
        <span class="label">Airports Loaded:</span>
        <span class="value" :class="{ complete: status.loadingComplete }">
          {{ status.airports?.toLocaleString() }} / {{ status.expectedAirports?.toLocaleString() }}
        </span>
      </div>
      <div class="status-item">
        <span class="label">Cities:</span>
        <span class="value">{{ status.cities }}</span>
      </div>
      <div class="status-item">
        <span class="label">Total Locations:</span>
        <span class="value">{{ status.totalLocations?.toLocaleString() }}</span>
      </div>
      <div class="progress-bar" v-if="status.percentLoaded !== undefined">
        <div class="progress-fill" :style="{ width: status.percentLoaded + '%' }"></div>
        <span class="progress-text">{{ status.percentLoaded }}%</span>
      </div>
      <div class="status-badge" :class="status.loadingComplete ? 'success' : 'warning'">
        {{ status.loadingComplete ? '✓ Data Ready' : '⚠ Loading...' }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../services/api'

interface DataStatus {
  totalLocations: number
  airports: number
  cities: number
  airportsLoaded: boolean
  expectedAirports: number
  loadingComplete: boolean
  percentLoaded: number
}

const status = ref<DataStatus>({
  totalLocations: 0,
  airports: 0,
  cities: 0,
  airportsLoaded: false,
  expectedAirports: 9313,
  loadingComplete: false,
  percentLoaded: 0
})
const loading = ref(true)
const error = ref<string | null>(null)

const fetchStatus = async () => {
  try {
    loading.value = true
    error.value = null
    const response = await api.get('/api/status/data')
    status.value = response.data
  } catch (err) {
    error.value = 'Failed to load data status'
    console.error('Error fetching data status:', err)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchStatus()
  // Refresh every 30 seconds if not complete
  const interval = setInterval(() => {
    if (!status.value.loadingComplete) {
      fetchStatus()
    } else {
      clearInterval(interval)
    }
  }, 30000)
})
</script>

<style scoped>
.status-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

h3 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 1.2rem;
}

.loading, .error {
  padding: 10px;
  text-align: center;
  color: #666;
}

.error {
  color: #d32f2f;
}

.status-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}

.label {
  font-weight: 500;
  color: #666;
}

.value {
  font-weight: 600;
  color: #333;
}

.value.complete {
  color: #4caf50;
}

.progress-bar {
  position: relative;
  height: 30px;
  background: #f0f0f0;
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
  color: #333;
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
</style>
