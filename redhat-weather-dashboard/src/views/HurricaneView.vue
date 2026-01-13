<template>
  <div class="container">
    <h1>Hurricane Tracking</h1>

    <div class="card">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <h2>Active Tropical Systems</h2>
        <button @click="refreshHurricanes" :disabled="refreshing">
          {{ refreshing ? 'Refreshing...' : 'Refresh Data' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading">Loading hurricane data...</div>
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="hurricanes.length > 0">
      <div v-for="storm in hurricanes" :key="storm.id" class="card">
        <div class="storm-header">
          <h2>{{ storm.stormName || 'Unnamed Storm' }}</h2>
          <div class="storm-id">{{ storm.stormId }}</div>
        </div>

        <div class="storm-info">
          <div class="info-item">
            <strong>Category:</strong>
            <span class="category" :class="'cat-' + (storm.category || 0)">
              {{ getCategoryLabel(storm.category) }}
            </span>
          </div>
          <div class="info-item">
            <strong>Max Winds:</strong>
            {{ storm.maxSustainedWindsMph }} mph
          </div>
          <div class="info-item">
            <strong>Pressure:</strong>
            {{ storm.minCentralPressureMb }} mb
          </div>
          <div class="info-item">
            <strong>Status:</strong>
            {{ storm.status }}
          </div>
        </div>

        <div class="storm-location">
          <strong>Position:</strong>
          {{ storm.latitude }}°{{ storm.latitude >= 0 ? 'N' : 'S' }},
          {{ Math.abs(storm.longitude) }}°{{ storm.longitude >= 0 ? 'E' : 'W' }}
        </div>

        <div class="storm-time">
          <strong>Advisory Time:</strong> {{ formatDate(storm.advisoryTime) }}
        </div>
      </div>
    </div>

    <div v-else-if="!loading" class="card">
      <p>✅ No active tropical systems at this time.</p>
      <p>Hurricane season in the Atlantic runs from June 1 to November 30.</p>
      <p>Data is automatically fetched from the National Hurricane Center every hour during hurricane season.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import weatherService, { type Hurricane } from '../services/weatherService'

const hurricanes = ref<Hurricane[]>([])
const loading = ref(false)
const refreshing = ref(false)
const error = ref<string | null>(null)

async function loadHurricanes() {
  loading.value = true
  error.value = null

  try {
    hurricanes.value = await weatherService.getActiveStorms()
  } catch (err: any) {
    error.value = err.message || 'Failed to load hurricane data'
    console.error('Error loading hurricanes:', err)
  } finally {
    loading.value = false
  }
}

async function refreshHurricanes() {
  refreshing.value = true
  error.value = null

  try {
    await weatherService.refreshHurricaneData()
    // Wait a moment for the data to be fetched
    setTimeout(() => {
      loadHurricanes()
      refreshing.value = false
    }, 2000)
  } catch (err: any) {
    error.value = err.message || 'Failed to refresh hurricane data'
    refreshing.value = false
  }
}

function getCategoryLabel(category: number | undefined): string {
  if (category === undefined || category === null) return 'N/A'
  if (category === 0) return 'Tropical Storm'
  return `Category ${category}`
}

function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadHurricanes()
})
</script>

<style scoped>
.storm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  border-bottom: 2px solid #ee0000;
  padding-bottom: 10px;
}

.storm-header h2 {
  margin: 0;
  color: #ee0000;
}

.storm-id {
  background: #333;
  color: white;
  padding: 5px 10px;
  border-radius: 4px;
  font-family: monospace;
}

.storm-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
  margin: 15px 0;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.category {
  padding: 5px 10px;
  border-radius: 4px;
  font-weight: bold;
  color: white;
  display: inline-block;
}

.cat-0 {
  background: #007bff;
}

.cat-1 {
  background: #ffc107;
  color: #333;
}

.cat-2 {
  background: #ff9800;
}

.cat-3 {
  background: #ff5722;
}

.cat-4 {
  background: #f44336;
}

.cat-5 {
  background: #9c27b0;
}

.storm-location,
.storm-time {
  margin: 10px 0;
  color: #666;
}
</style>
