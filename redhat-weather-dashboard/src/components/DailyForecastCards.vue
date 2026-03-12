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
        <div v-if="day.feelsLikeHighF != null && Math.abs(day.feelsLikeHighF - day.highF) >= 3" class="day-feels">
          Feels {{ formatTemp(day.feelsLikeHighF) }}
        </div>
        <div class="day-details">
          <div v-if="day.windMph != null" class="wind-row">
            <span v-if="day.windDir != null" class="wind-arrow" :style="{ transform: 'rotate(' + (day.windDir + 180) + 'deg)' }" aria-hidden="true">&#x2191;</span>
            <span v-else aria-hidden="true">&#x1F4A8;</span>
            {{ formatSpeed(day.windMph) }}
            <span v-if="day.windDir != null" class="wind-dir-label">{{ compassDir(day.windDir) }}</span>
          </div>
          <div v-if="day.precipChance != null" class="precip-row">
            <span aria-hidden="true">&#x2614;</span> {{ day.precipChance }}%
            <div class="precip-bar-bg"><div class="precip-bar-fill" :style="{ width: day.precipChance + '%' }"></div></div>
          </div>
          <div v-if="day.humidity != null" class="humidity-row">
            <span aria-hidden="true">&#x1F4A7;</span> {{ day.humidity }}%
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
import { computeFeelsLike, degreesToCompass } from '../utils/weatherCalc'

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
  feelsLikeHighF: number | null
  windMph: number | null
  windDir: number | null
  precipChance: number | null
  humidity: number | null
}

const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

function compassDir(deg: number): string {
  return degreesToCompass(deg)
}

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
    let windDirSum = 0
    let windDirCount = 0
    let maxPrecip: number | null = null
    let totalHumidity = 0
    let humidityCount = 0
    let bestCondition = ''
    let highWindMph: number | null = null
    let highHumidity: number | null = null

    for (const item of items) {
      if (item.temperatureFahrenheit > highF) {
        highF = item.temperatureFahrenheit
        highWindMph = item.windSpeedMph
        highHumidity = item.humidity ?? null
      }
      lowF = Math.min(lowF, item.temperatureFahrenheit)
      if (item.windSpeedMph != null) {
        totalWind += item.windSpeedMph
        windCount++
      }
      if (item.windDirection != null) {
        windDirSum += item.windDirection
        windDirCount++
      }
      if (item.precipitationProbability != null) {
        maxPrecip = Math.max(maxPrecip ?? 0, item.precipitationProbability)
      }
      if (item.humidity != null) {
        totalHumidity += item.humidity
        humidityCount++
      }
      if (!bestCondition) {
        bestCondition = item.weatherShortDescription || item.weatherDescription || ''
      }
    }

    const avgWind = windCount > 0 ? totalWind / windCount : null
    const avgWindDir = windDirCount > 0 ? Math.round(windDirSum / windDirCount) : null
    const avgHumidity = humidityCount > 0 ? Math.round(totalHumidity / humidityCount) : null
    const hf = highF === -Infinity ? 0 : highF
    const feelsLike = computeFeelsLike(hf, highWindMph, highHumidity)

    result.push({
      date: dateKey,
      dayName,
      dateLabel,
      icon: getWeatherIcon(bestCondition),
      condition: bestCondition,
      highF: hf,
      lowF: lowF === Infinity ? 0 : lowF,
      feelsLikeHighF: feelsLike,
      windMph: avgWind,
      windDir: avgWindDir,
      precipChance: maxPrecip,
      humidity: avgHumidity,
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
  margin-bottom: 4px;
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

.day-feels {
  font-size: 10px;
  color: var(--text-muted, #999);
  margin-bottom: 6px;
}

.day-details {
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.day-details > div {
  margin-top: 3px;
}

.wind-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
}

.wind-arrow {
  display: inline-block;
  font-size: 12px;
  line-height: 1;
}

.wind-dir-label {
  font-size: 10px;
  color: var(--text-muted, #999);
}

.precip-row {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.precip-bar-bg {
  width: 80%;
  height: 3px;
  background: var(--border-color, #ddd);
  border-radius: 2px;
  overflow: hidden;
}

.precip-bar-fill {
  height: 100%;
  background: #42a5f5;
  border-radius: 2px;
}

.humidity-row {
  font-size: 10px;
  color: var(--text-muted, #999);
}

/* Responsive: stack cards on small screens */
@media (max-width: 480px) {
  .daily-cards-scroll {
    flex-direction: column;
    overflow-x: visible;
  }

  .daily-card {
    flex: none;
    display: grid;
    grid-template-columns: auto 1fr auto;
    grid-template-rows: auto auto;
    gap: 0 12px;
    text-align: left;
    padding: 10px 14px;
    align-items: center;
  }

  .day-icon {
    grid-row: 1 / 3;
    font-size: 1.6rem;
    margin: 0;
  }

  .day-name {
    grid-column: 2;
  }

  .day-date {
    grid-column: 2;
    margin-bottom: 0;
  }

  .day-condition {
    display: none;
  }

  .day-temps {
    grid-row: 1 / 3;
    grid-column: 3;
    flex-direction: column;
    gap: 2px;
    margin-bottom: 0;
  }

  .day-feels {
    grid-column: 2 / 4;
  }

  .day-details {
    grid-column: 1 / 4;
    display: flex;
    gap: 12px;
    margin-top: 4px;
    padding-top: 4px;
    border-top: 1px solid var(--border-light, #eee);
  }
}
</style>
