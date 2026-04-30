<template>
  <div class="container">
    <h1>{{ $t('aqi.title') }}</h1>

    <div class="card">
      <div class="card-header-row">
        <h2>{{ $t('aqi.currentConditions') }}</h2>
        <div class="header-actions">
          <select v-model="selectedLocationId" class="location-select">
            <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
          </select>
          <button class="btn-sm" :disabled="refreshing" @click="refreshData">
            {{ refreshing ? $t('airport.refreshing') : $t('aqi.refreshData') }}
          </button>
        </div>
      </div>
    </div>

    <TableSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="!loading && airQuality" class="aqi-content">
      <div class="aqi-gauge-card card">
        <div class="aqi-gauge" :class="aqiClass">
          <div class="aqi-value">{{ airQuality.aqi }}</div>
          <div class="aqi-label">{{ aqiLabel }}</div>
        </div>
        <div class="aqi-description">{{ aqiDescription }}</div>
        <FreshnessBadge v-if="airQuality.fetchedAt" :fetched-at="airQuality.fetchedAt" data-type="airQuality" />
      </div>

      <div class="pollutant-grid">
        <div v-for="p in pollutants" :key="p.key" class="card pollutant-card">
          <div class="pollutant-name">{{ p.name }}</div>
          <div class="pollutant-value">{{ p.value != null ? p.value.toFixed(1) : '-' }}</div>
          <div class="pollutant-unit">&micro;g/m&sup3;</div>
        </div>
      </div>
    </div>

    <div v-if="!loading && !airQuality && !error" class="card">
      <p><Wind :size="16" aria-hidden="true" /> {{ $t('aqi.noData') }}</p>
      <p>{{ $t('aqi.selectLocation') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import TableSkeleton from '../components/skeletons/TableSkeleton.vue'
import FreshnessBadge from '../components/FreshnessBadge.vue'
import { Wind } from 'lucide-vue-next'

const store = useWeatherStore()
const toast = useToast()
const { airports: locations, airQuality, airQualityLoading: loading, airQualityError: error } = storeToRefs(store)

const refreshing = ref(false)
const selectedLocationId = ref<number | null>(null)

const AQI_LEVELS = [
  { max: 1, label: 'Good', class: 'aqi-good', description: 'Air quality is satisfactory with little or no risk.' },
  { max: 2, label: 'Fair', class: 'aqi-fair', description: 'Air quality is acceptable. Some pollutants may pose a moderate health concern for sensitive individuals.' },
  { max: 3, label: 'Moderate', class: 'aqi-moderate', description: 'Members of sensitive groups may experience health effects. The general public is less likely to be affected.' },
  { max: 4, label: 'Poor', class: 'aqi-poor', description: 'Health effects may be experienced by everyone. Sensitive groups may experience more serious effects.' },
  { max: 5, label: 'Very Poor', class: 'aqi-very-poor', description: 'Health alert: everyone may experience serious health effects. Avoid outdoor activities.' },
]

const aqiLevel = computed(() => {
  if (!airQuality.value) return AQI_LEVELS[0]
  return AQI_LEVELS.find(l => airQuality.value!.aqi <= l.max) || AQI_LEVELS[4]
})

const aqiClass = computed(() => aqiLevel.value.class)
const aqiLabel = computed(() => aqiLevel.value.label)
const aqiDescription = computed(() => aqiLevel.value.description)

const pollutants = computed(() => {
  const aq = airQuality.value
  if (!aq) return []
  return [
    { key: 'pm2_5', name: 'PM2.5', value: aq.pm2_5 },
    { key: 'pm10', name: 'PM10', value: aq.pm10 },
    { key: 'o3', name: 'Ozone (O₃)', value: aq.o3 },
    { key: 'no2', name: 'NO₂', value: aq.no2 },
    { key: 'so2', name: 'SO₂', value: aq.so2 },
    { key: 'co', name: 'CO', value: aq.co },
    { key: 'nh3', name: 'NH₃', value: aq.nh3 },
    { key: 'no', name: 'NO', value: aq.no },
  ]
})

watch(selectedLocationId, (id) => {
  if (id) store.fetchAirQuality(id)
})

async function refreshData() {
  refreshing.value = true
  try {
    await store.refreshAirQuality()
    if (selectedLocationId.value) {
      await store.fetchAirQuality(selectedLocationId.value)
    }
    toast.success('Air quality data refreshed')
  } catch {
    toast.error('Failed to refresh air quality data')
  } finally {
    refreshing.value = false
  }
}

onMounted(() => {
  store.fetchAirports()
  if (locations.value.length > 0 && !selectedLocationId.value) {
    selectedLocationId.value = locations.value[0].id
  }
})

watch(() => locations.value.length, () => {
  if (locations.value.length > 0 && !selectedLocationId.value) {
    selectedLocationId.value = locations.value[0].id
  }
})
</script>

<style scoped>
.aqi-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.aqi-gauge-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px;
}

.aqi-gauge {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
}

.aqi-value {
  font-size: 36px;
  line-height: 1;
}

.aqi-label {
  font-size: 14px;
  margin-top: 4px;
}

.aqi-description {
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary, #666);
  max-width: 500px;
}

.aqi-good { background: #4caf50; }
.aqi-fair { background: #8bc34a; }
.aqi-moderate { background: #ff9800; }
.aqi-poor { background: #f44336; }
.aqi-very-poor { background: #9c27b0; }

.pollutant-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.pollutant-card {
  text-align: center;
  padding: 16px 12px;
}

.pollutant-name {
  font-size: 12px;
  color: var(--text-secondary, #666);
  font-weight: 600;
  margin-bottom: 8px;
}

.pollutant-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--accent);
}

.pollutant-unit {
  font-size: 11px;
  color: var(--text-muted, #999);
  margin-top: 2px;
}

.location-select {
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid var(--border-color, #ddd);
  background: var(--bg-card, white);
  color: var(--text-primary, #333);
  font-size: 13px;
}

@media (max-width: 480px) {
  .pollutant-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .aqi-gauge {
    width: 100px;
    height: 100px;
  }

  .aqi-value {
    font-size: 28px;
  }
}
</style>
