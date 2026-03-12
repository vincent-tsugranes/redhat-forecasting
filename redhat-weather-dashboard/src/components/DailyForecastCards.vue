<template>
  <div v-if="dailyForecasts.length > 0" class="daily-cards-container">
    <div class="daily-cards-scroll">
      <div v-for="day in dailyForecasts" :key="day.date" class="daily-card">
        <div class="day-name">{{ day.dayName }}</div>
        <div class="day-date">{{ day.dateLabel }}</div>
        <div class="day-icon" aria-hidden="true">{{ day.icon }}</div>
        <div class="day-condition">{{ day.condition }}</div>
        <div class="day-temps">
          <span class="temp-high">{{ formatTemp(day.highF) }}</span>
          <span class="temp-low">{{ formatTemp(day.lowF) }}</span>
        </div>
        <div class="day-details">
          <div v-if="day.windMph != null">
            <span aria-hidden="true">💨</span> {{ formatSpeed(day.windMph) }}
          </div>
          <div v-if="day.precipChance != null">
            <span aria-hidden="true">☔</span> {{ day.precipChance }}%
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { type WeatherForecast } from '../services/weatherService'
import { getWeatherIcon } from '../utils/weatherIcons'
import { useUnitPreferences } from '../composables/useUnitPreferences'

const { formatTemp, formatSpeed } = useUnitPreferences()

const props = defineProps<{
  forecasts: WeatherForecast[]
}>()

interface DailyForecast {
  date: string
  dayName: string
  dateLabel: string
  icon: string
  condition: string
  highF: number
  lowF: number
  windMph: number | null
  precipChance: number | null
}

const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

const dailyForecasts = computed<DailyForecast[]>(() => {
  const grouped = new Map<string, WeatherForecast[]>()

  for (const f of props.forecasts) {
    const dateKey = f.validFrom.substring(0, 10)
    if (!grouped.has(dateKey)) {
      grouped.set(dateKey, [])
    }
    grouped.get(dateKey)!.push(f)
  }

  const result: DailyForecast[] = []

  for (const [dateKey, items] of grouped) {
    const d = new Date(dateKey + 'T12:00:00')
    const dayName = DAYS[d.getDay()]
    const dateLabel = `${MONTHS[d.getMonth()]} ${d.getDate()}`

    let highF = -Infinity
    let lowF = Infinity
    let totalWind = 0
    let windCount = 0
    let maxPrecip: number | null = null
    let bestCondition = ''

    for (const item of items) {
      highF = Math.max(highF, item.temperatureFahrenheit)
      lowF = Math.min(lowF, item.temperatureFahrenheit)
      if (item.windSpeedMph != null) {
        totalWind += item.windSpeedMph
        windCount++
      }
      if (item.precipitationProbability != null) {
        maxPrecip = Math.max(maxPrecip ?? 0, item.precipitationProbability)
      }
      if (!bestCondition) {
        bestCondition =
          item.weatherShortDescription || item.weatherDescription || ''
      }
    }

    result.push({
      date: dateKey,
      dayName,
      dateLabel,
      icon: getWeatherIcon(bestCondition),
      condition: bestCondition,
      highF: highF === -Infinity ? 0 : highF,
      lowF: lowF === Infinity ? 0 : lowF,
      windMph: windCount > 0 ? totalWind / windCount : null,
      precipChance: maxPrecip,
    })
  }

  return result.slice(0, 7)
})
</script>

<style scoped>
.daily-cards-container {
  margin-bottom: 20px;
}

.daily-cards-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding: 4px 0 12px;
  -webkit-overflow-scrolling: touch;
}

.daily-cards-scroll::-webkit-scrollbar {
  height: 6px;
}

.daily-cards-scroll::-webkit-scrollbar-thumb {
  background: var(--border-color, #ddd);
  border-radius: 3px;
}

.daily-card {
  flex: 0 0 130px;
  scroll-snap-align: start;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-color, #ddd);
  border-radius: 12px;
  padding: 14px 10px;
  text-align: center;
  transition: box-shadow 0.2s;
}

.daily-card:hover {
  box-shadow: 0 4px 12px var(--shadow-md, rgba(0, 0, 0, 0.15));
}

.day-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary, #333);
}

.day-date {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-bottom: 8px;
}

.day-icon {
  font-size: 2rem;
  margin: 4px 0;
}

.day-condition {
  font-size: 11px;
  color: var(--text-secondary, #666);
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.day-temps {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 6px;
}

.temp-high {
  font-weight: 600;
  color: var(--accent);
  font-size: 16px;
}

.temp-low {
  color: var(--text-muted, #999);
  font-size: 16px;
}

.day-details {
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.day-details div {
  margin-top: 2px;
}
</style>
