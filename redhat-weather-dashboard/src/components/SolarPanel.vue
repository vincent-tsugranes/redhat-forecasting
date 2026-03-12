<template>
  <div v-if="solarData || loading" class="card solar-panel">
    <h3>{{ $t('solar.title') }}</h3>
    <div v-if="loading" class="solar-skeleton">
      <div class="skeleton-block"></div>
      <div class="skeleton-block"></div>
      <div class="skeleton-block"></div>
    </div>
    <div v-else-if="solarData" class="solar-grid">
      <div class="solar-item">
        <div class="solar-icon" aria-hidden="true">🌅</div>
        <div class="solar-label">{{ $t('solar.sunrise') }}</div>
        <div class="solar-value">{{ solarData.sunrise }}</div>
      </div>
      <div class="solar-item">
        <div class="solar-icon" aria-hidden="true">🌇</div>
        <div class="solar-label">{{ $t('solar.sunset') }}</div>
        <div class="solar-value">{{ solarData.sunset }}</div>
      </div>
      <div class="solar-item">
        <div class="solar-icon" aria-hidden="true">⏱️</div>
        <div class="solar-label">{{ $t('solar.dayLength') }}</div>
        <div class="solar-value">{{ solarData.dayLengthFormatted }}</div>
      </div>
    </div>
    <div v-if="solarData" class="daylight-bar-container">
      <div class="daylight-labels">
        <span>{{ $t('solar.daylight') }}</span>
        <span>{{ daylightPercent }}%</span>
      </div>
      <div
        class="daylight-bar"
        role="progressbar"
        :aria-valuenow="daylightPercent"
        aria-valuemin="0"
        aria-valuemax="100"
        :aria-label="`${daylightPercent}% daylight`"
      >
        <div class="daylight-fill" :style="{ width: daylightPercent + '%' }"></div>
      </div>
      <div class="daylight-labels darkness-labels">
        <span>{{ $t('solar.darkness') }}</span>
        <span>{{ 100 - daylightPercent }}%</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import weatherService, { type SolarData } from '../services/weatherService'

const props = defineProps<{
  locationId: number
}>()

const solarData = ref<SolarData | null>(null)
const loading = ref(false)

const daylightPercent = computed(() => {
  if (!solarData.value) return 0
  const totalSeconds = 24 * 60 * 60
  return Math.round((solarData.value.dayLengthSeconds / totalSeconds) * 100)
})

async function fetchSolarData(locationId: number) {
  loading.value = true
  try {
    solarData.value = await weatherService.getSolarData(locationId)
  } catch {
    solarData.value = null
  } finally {
    loading.value = false
  }
}

watch(
  () => props.locationId,
  (newId) => {
    if (newId) fetchSolarData(newId)
  },
  { immediate: true },
)
</script>

<style scoped>
.solar-panel h3 {
  margin-bottom: 10px;
  font-size: 1.1rem;
}

.solar-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.solar-item {
  text-align: center;
  padding: 10px 6px;
  background: var(--bg-code, #f9f9f9);
  border-radius: 8px;
}

.solar-icon {
  font-size: 20px;
  margin-bottom: 4px;
}

.solar-label {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-bottom: 4px;
}

.solar-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary, #333);
}

.daylight-bar-container {
  margin-top: 8px;
}

.daylight-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-bottom: 4px;
}

.darkness-labels {
  margin-top: 4px;
  margin-bottom: 0;
}

.daylight-bar {
  height: 10px;
  background: var(--bg-code, #e0e0e0);
  border-radius: 5px;
  overflow: hidden;
}

.daylight-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--aging-color), var(--storm-cat1));
  border-radius: 5px;
  transition: width 0.5s ease;
}

.solar-skeleton {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.skeleton-block {
  height: 80px;
  background: var(--bg-code, #f0f0f0);
  border-radius: 8px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

@media (max-width: 480px) {
  .solar-grid {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .solar-item {
    display: flex;
    align-items: center;
    gap: 12px;
    text-align: left;
    padding: 10px 12px;
  }

  .solar-icon {
    margin-bottom: 0;
    font-size: 20px;
  }

  .solar-skeleton {
    grid-template-columns: 1fr;
  }
}
</style>
