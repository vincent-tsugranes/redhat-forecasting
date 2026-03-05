<template>
  <div v-if="normals || loading" class="card climate-card">
    <h3>{{ $t('climate.title') }}</h3>
    <div v-if="loading" class="climate-skeleton">
      <div class="skeleton-block"></div>
      <div class="skeleton-block"></div>
    </div>
    <div v-else-if="normals" class="climate-grid">
      <div class="climate-item">
        <div class="climate-label">{{ $t('climate.normalHigh') }}</div>
        <div class="climate-value">{{ normals.avgHighF != null ? formatTemp(normals.avgHighF) : '' }}</div>
        <div v-if="currentTemp != null" class="departure" :class="getDepartureClass(tempDeparture)">
          {{ formatDeparture(tempDeparture) }}
        </div>
      </div>
      <div class="climate-item">
        <div class="climate-label">{{ $t('climate.normalLow') }}</div>
        <div class="climate-value">{{ normals.avgLowF != null ? formatTemp(normals.avgLowF) : '' }}</div>
      </div>
      <div v-if="normals.avgPrecipProbability != null" class="climate-item">
        <div class="climate-label">{{ $t('climate.avgPrecip') }}</div>
        <div class="climate-value">{{ normals.avgPrecipProbability?.toFixed(0) }}%</div>
      </div>
      <div v-if="normals.avgWindSpeedMph != null" class="climate-item">
        <div class="climate-label">{{ $t('climate.avgWind') }}</div>
        <div class="climate-value">{{ normals.avgWindSpeedMph != null ? formatSpeed(normals.avgWindSpeedMph) : '' }}</div>
      </div>
    </div>
    <div v-if="normals" class="sample-info">
      {{ $t('climate.basedOn', { count: normals.sampleCount }) }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import weatherService, { type ClimateNormals } from '../services/weatherService'
import { useUnitPreferences } from '../composables/useUnitPreferences'

const { formatTemp, formatSpeed } = useUnitPreferences()

const props = defineProps<{
  locationId: number
  currentTemp?: number | null
}>()

const normals = ref<ClimateNormals | null>(null)
const loading = ref(false)

const tempDeparture = computed(() => {
  if (props.currentTemp == null || !normals.value?.avgHighF) return null
  return props.currentTemp - normals.value.avgHighF.valueOf()
})

function getDepartureClass(departure: number | null): string {
  if (departure == null) return ''
  if (departure > 5) return 'departure-hot'
  if (departure < -5) return 'departure-cold'
  return 'departure-normal'
}

function formatDeparture(departure: number | null): string {
  if (departure == null) return ''
  const sign = departure >= 0 ? '+' : ''
  return `${sign}${departure.toFixed(0)}°F vs normal`
}

async function fetchNormals(locationId: number) {
  loading.value = true
  try {
    normals.value = await weatherService.getClimateNormals(locationId)
  } catch {
    normals.value = null
  } finally {
    loading.value = false
  }
}

watch(
  () => props.locationId,
  (newId) => {
    if (newId) fetchNormals(newId)
  },
  { immediate: true },
)
</script>

<style scoped>
.climate-card h3 {
  margin-bottom: 12px;
  font-size: 1.1rem;
}

.climate-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  gap: 12px;
  margin-bottom: 8px;
}

.climate-item {
  text-align: center;
  padding: 8px;
  background: var(--bg-code, #f9f9f9);
  border-radius: 8px;
}

.climate-label {
  font-size: 11px;
  color: var(--text-secondary, #666);
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.climate-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary, #333);
}

.departure {
  font-size: 12px;
  font-weight: 600;
  margin-top: 4px;
}

.departure-hot {
  color: #f44336;
}

.departure-cold {
  color: #2196f3;
}

.departure-normal {
  color: #4caf50;
}

.sample-info {
  font-size: 11px;
  color: var(--text-secondary, #999);
  text-align: center;
  margin-top: 8px;
}

.climate-skeleton {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.skeleton-block {
  height: 60px;
  background: var(--bg-code, #f0f0f0);
  border-radius: 8px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
