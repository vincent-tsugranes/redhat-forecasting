<template>
  <div v-if="favorites.length > 0" class="favorite-weather-section">
    <h2><span aria-hidden="true">⭐</span> {{ $t('favorites.weatherAtGlance') }}</h2>
    <div class="favorite-weather-scroll">
      <div
        v-for="card in weatherCards"
        :key="card.locationId"
        class="favorite-weather-card"
        role="button"
        tabindex="0"
        :aria-label="$t('favorites.viewForecast', { name: card.name })"
        @click="navigateToForecast(card.locationId)"
        @keydown.enter="navigateToForecast(card.locationId)"
      >
        <div class="fwc-name">{{ card.name }}</div>
        <div v-if="card.loading" class="fwc-loading">
          <SkeletonLoader height="40px" width="60px" border-radius="8px" />
        </div>
        <template v-else-if="card.forecast">
          <div class="fwc-icon" aria-hidden="true">{{ card.icon }}</div>
          <div class="fwc-temp">{{ formatTemp(card.forecast.temperatureFahrenheit) }}</div>
          <div class="fwc-details">
            <span v-if="card.forecast.windSpeedMph != null">
              <span aria-hidden="true">💨</span> {{ formatSpeed(card.forecast.windSpeedMph) }}
            </span>
            <span v-if="card.forecast.precipitationProbability != null">
              <span aria-hidden="true">☔</span> {{ card.forecast.precipitationProbability }}%
            </span>
          </div>
          <div class="fwc-condition">
            {{ card.forecast.weatherShortDescription || '' }}
          </div>
        </template>
        <div v-else class="fwc-no-data">{{ $t('favorites.noData') }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useFavorites } from '../composables/useFavorites'
import { useUnitPreferences } from '../composables/useUnitPreferences'
import weatherService, { type WeatherForecast } from '../services/weatherService'
import { getWeatherIcon } from '../utils/weatherIcons'
import SkeletonLoader from './SkeletonLoader.vue'

const { formatTemp, formatSpeed } = useUnitPreferences()

const { favorites } = useFavorites()
const router = useRouter()

interface WeatherCard {
  locationId: number
  name: string
  loading: boolean
  forecast: WeatherForecast | null
  icon: string
}

const weatherCards = ref<WeatherCard[]>([])

async function loadWeatherForFavorites() {
  weatherCards.value = favorites.value.map((fav) => ({
    locationId: fav.id,
    name: fav.name,
    loading: true,
    forecast: null,
    icon: '☀️',
  }))

  const promises = weatherCards.value.map(async (card, index) => {
    try {
      const forecasts = await weatherService.getForecastsByLocation(card.locationId)
      const now = Date.now()
      const sorted = [...forecasts].sort(
        (a, b) =>
          Math.abs(new Date(a.validFrom).getTime() - now) -
          Math.abs(new Date(b.validFrom).getTime() - now),
      )
      const current = sorted[0] || null
      weatherCards.value[index] = {
        ...card,
        loading: false,
        forecast: current,
        icon: current
          ? getWeatherIcon(current.weatherShortDescription || current.weatherDescription || '')
          : '☀️',
      }
    } catch {
      weatherCards.value[index] = { ...card, loading: false, forecast: null, icon: '☀️' }
    }
  })

  await Promise.allSettled(promises)
}

function navigateToForecast(locationId: number) {
  router.push({ name: 'forecasts', query: { locationId: String(locationId) } })
}

onMounted(() => {
  if (favorites.value.length > 0) {
    loadWeatherForFavorites()
  }
})

watch(
  () => favorites.value.length,
  () => {
    loadWeatherForFavorites()
  },
)
</script>

<style scoped>
.favorite-weather-section {
  margin-bottom: 12px;
}

.favorite-weather-section h2 {
  font-size: 1.1rem;
  margin-bottom: 8px;
}

.favorite-weather-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding: 4px 0 12px;
  -webkit-overflow-scrolling: touch;
}

.favorite-weather-scroll::-webkit-scrollbar {
  height: 6px;
}

.favorite-weather-scroll::-webkit-scrollbar-thumb {
  background: var(--border-color, #ddd);
  border-radius: 3px;
}

.favorite-weather-card {
  flex: 0 0 130px;
  scroll-snap-align: start;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-color, #ddd);
  border-radius: 10px;
  padding: 10px 8px;
  text-align: center;
  cursor: pointer;
  transition:
    box-shadow 0.2s,
    transform 0.2s;
}

.favorite-weather-card:hover,
.favorite-weather-card:focus-visible {
  box-shadow: 0 4px 12px var(--shadow-md, rgba(0, 0, 0, 0.15));
  transform: translateY(-2px);
  outline: none;
}

.fwc-name {
  font-weight: 600;
  font-size: 12px;
  color: var(--text-primary, #333);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.fwc-loading {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

.fwc-icon {
  font-size: 1.5rem;
  margin: 2px 0;
}

.fwc-temp {
  font-weight: 700;
  font-size: 18px;
  color: var(--accent);
  margin-bottom: 2px;
}

.fwc-details {
  display: flex;
  justify-content: center;
  gap: 6px;
  font-size: 11px;
  color: var(--text-secondary, #666);
  margin-bottom: 2px;
}

.fwc-condition {
  font-size: 11px;
  color: var(--text-muted, #999);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.fwc-no-data {
  padding: 10px 0;
  font-size: 11px;
  color: var(--text-muted, #999);
  font-style: italic;
}
</style>
