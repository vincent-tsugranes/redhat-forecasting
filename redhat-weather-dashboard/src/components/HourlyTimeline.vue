<template>
  <div v-if="hourlySlots.length > 0" class="hourly-timeline-container">
    <h3>{{ $t('forecast.hourlyTimeline') }}</h3>
    <div class="hourly-timeline-scroll">
      <div v-for="slot in hourlySlots" :key="slot.time" class="hourly-slot">
        <div class="slot-time">{{ slot.timeLabel }}</div>
        <div class="slot-day">{{ slot.dayLabel }}</div>
        <div class="slot-icon" aria-hidden="true">{{ slot.icon }}</div>
        <div class="slot-temp">{{ formatTemp(slot.tempF) }}</div>
        <div class="slot-details">
          <div v-if="slot.precipChance != null">
            <span aria-hidden="true">☔</span> {{ slot.precipChance }}%
          </div>
          <div v-if="slot.windMph != null">
            <span aria-hidden="true">💨</span> {{ formatSpeed(slot.windMph) }}
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

const { formatTemp, formatSpeed, formatTime } = useUnitPreferences()

const props = defineProps<{
  forecasts: WeatherForecast[]
}>()

interface HourlySlot {
  time: string
  timeLabel: string
  dayLabel: string
  icon: string
  tempF: number
  precipChance: number | null
  windMph: number | null
}

const hourlySlots = computed<HourlySlot[]>(() => {
  const now = Date.now()
  const cutoff = now + 48 * 60 * 60 * 1000

  return [...props.forecasts]
    .filter((f) => {
      const t = new Date(f.validFrom).getTime()
      return t >= now - 3 * 60 * 60 * 1000 && t <= cutoff
    })
    .sort((a, b) => new Date(a.validFrom).getTime() - new Date(b.validFrom).getTime())
    .map((f) => {
      const dt = new Date(f.validFrom)
      return {
        time: f.validFrom,
        timeLabel: formatTime(dt),
        dayLabel: dt.toLocaleDateString('en-US', { weekday: 'short' }),
        icon: getWeatherIcon(f.weatherShortDescription || f.weatherDescription || ''),
        tempF: f.temperatureFahrenheit,
        precipChance: f.precipitationProbability ?? null,
        windMph: f.windSpeedMph,
      }
    })
})
</script>

<style scoped>
.hourly-timeline-container {
  margin-bottom: 20px;
}

.hourly-timeline-container h3 {
  margin-bottom: 10px;
}

.hourly-timeline-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding: 4px 0 12px;
  -webkit-overflow-scrolling: touch;
}

.hourly-timeline-scroll::-webkit-scrollbar {
  height: 6px;
}

.hourly-timeline-scroll::-webkit-scrollbar-thumb {
  background: var(--border-color, #ddd);
  border-radius: 3px;
}

.hourly-slot {
  flex: 0 0 80px;
  scroll-snap-align: start;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-color, #ddd);
  border-radius: 10px;
  padding: 10px 6px;
  text-align: center;
  transition: box-shadow 0.2s;
}

.hourly-slot:hover {
  box-shadow: 0 2px 8px var(--shadow-md, rgba(0, 0, 0, 0.15));
}

.slot-time {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary, #333);
}

.slot-day {
  font-size: 11px;
  color: var(--text-secondary, #666);
  margin-bottom: 4px;
}

.slot-icon {
  font-size: 1.4rem;
  margin: 2px 0;
}

.slot-temp {
  font-weight: 600;
  font-size: 15px;
  color: var(--accent);
  margin-bottom: 4px;
}

.slot-details {
  font-size: 11px;
  color: var(--text-secondary, #666);
}

.slot-details div {
  margin-top: 2px;
}
</style>
