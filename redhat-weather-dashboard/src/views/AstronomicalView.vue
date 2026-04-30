<template>
  <div class="container">
    <h1>{{ $t('astronomical.title') }}</h1>

    <div class="card">
      <div class="card-header-row">
        <h2>{{ $t('astronomical.todayData') }}</h2>
        <div class="header-actions">
          <select v-model="selectedLocationId" class="location-select">
            <option v-for="loc in locations" :key="loc.id" :value="loc.id">{{ loc.name }}</option>
          </select>
        </div>
      </div>
    </div>

    <TableSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="!loading && astronomical" class="astro-content">
      <div class="astro-grid">
        <!-- Sun Card -->
        <div class="card sun-card">
          <h3><Sunrise :size="18" aria-hidden="true" /> {{ $t('astronomical.sunData') }}</h3>
          <div class="sun-times">
            <div class="sun-event">
              <Sunrise :size="20" aria-hidden="true" class="sun-icon rise" />
              <div>
                <div class="event-label">{{ $t('astronomical.sunrise') }}</div>
                <div class="event-time">{{ formatUtcTime(astronomical.sunrise) }}</div>
              </div>
            </div>
            <div class="sun-event">
              <Sunset :size="20" aria-hidden="true" class="sun-icon set" />
              <div>
                <div class="event-label">{{ $t('astronomical.sunset') }}</div>
                <div class="event-time">{{ formatUtcTime(astronomical.sunset) }}</div>
              </div>
            </div>
          </div>
          <div class="sun-details">
            <div v-if="astronomical.solarNoon" class="detail-row">
              <span class="detail-label">{{ $t('astronomical.solarNoon') }}</span>
              <span class="detail-value">{{ formatUtcTime(astronomical.solarNoon) }}</span>
            </div>
            <div v-if="astronomical.dayLength" class="detail-row">
              <span class="detail-label">{{ $t('astronomical.dayLength') }}</span>
              <span class="detail-value">{{ formatDayLength(astronomical.dayLength) }}</span>
            </div>
            <div v-if="astronomical.civilTwilightBegin" class="detail-row">
              <span class="detail-label">{{ $t('astronomical.civilTwilight') }}</span>
              <span class="detail-value">{{ formatUtcTime(astronomical.civilTwilightBegin) }} – {{ formatUtcTime(astronomical.civilTwilightEnd) }}</span>
            </div>
          </div>
        </div>

        <!-- Moon Card -->
        <div class="card moon-card">
          <h3><Moon :size="18" aria-hidden="true" /> {{ $t('astronomical.moonData') }}</h3>
          <div v-if="astronomical.moonPhaseName" class="moon-phase-display">
            <div class="moon-icon-large">{{ moonEmoji }}</div>
            <div class="moon-phase-name">{{ astronomical.moonPhaseName }}</div>
            <div v-if="astronomical.moonIllumination != null" class="moon-illumination">
              {{ Math.round(astronomical.moonIllumination * 100) }}% illuminated
            </div>
          </div>
          <div v-if="astronomical.nextMoonPhases && astronomical.nextMoonPhases.length > 0" class="upcoming-phases">
            <div class="detail-label">{{ $t('astronomical.upcomingPhases') }}</div>
            <div v-for="phase in astronomical.nextMoonPhases" :key="phase.date" class="phase-row">
              <span class="phase-emoji">{{ phaseEmoji(phase.phase) }}</span>
              <span class="phase-name">{{ phase.phase }}</span>
              <span class="phase-date">{{ phase.date }}</span>
            </div>
          </div>
        </div>

        <!-- UV Card -->
        <div class="card uv-card">
          <h3><Sun :size="18" aria-hidden="true" /> {{ $t('astronomical.uvIndex') }}</h3>
          <div v-if="astronomical.uvIndex != null" class="uv-display">
            <div class="uv-gauge" :class="uvClass">
              <div class="uv-value">{{ Math.round(astronomical.uvIndex) }}</div>
              <div class="uv-label">{{ uvLabel }}</div>
            </div>
            <div class="uv-advice">{{ uvAdvice }}</div>
          </div>
          <div v-else class="no-data">{{ $t('astronomical.noUvData') }}</div>
        </div>
      </div>
    </div>

    <div v-if="!loading && !astronomical && !error" class="card">
      <p><Sun :size="16" aria-hidden="true" /> {{ $t('astronomical.noData') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import TableSkeleton from '../components/skeletons/TableSkeleton.vue'
import { Sunrise, Sunset, Moon, Sun } from 'lucide-vue-next'

const store = useWeatherStore()
const { airports: locations, astronomical, astronomicalLoading: loading, astronomicalError: error } = storeToRefs(store)

const selectedLocationId = ref<number | null>(null)

const selectedLocation = computed(() => {
  if (!selectedLocationId.value) return null
  return locations.value.find(l => l.id === selectedLocationId.value) || null
})

watch(selectedLocation, (loc) => {
  if (loc) store.fetchAstronomical(loc.latitude, loc.longitude)
})

function formatUtcTime(timeStr: string | null): string {
  if (!timeStr) return '-'
  try {
    const d = new Date(timeStr)
    return d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' })
  } catch {
    return timeStr
  }
}

function formatDayLength(seconds: string | null): string {
  if (!seconds) return '-'
  const s = parseInt(seconds)
  if (isNaN(s)) return seconds
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  return `${h}h ${m}m`
}

const MOON_PHASES: Record<string, string> = {
  'New Moon': '🌑',
  'Waxing Crescent': '🌒',
  'First Quarter': '🌓',
  'Waxing Gibbous': '🌔',
  'Full Moon': '🌕',
  'Waning Gibbous': '🌖',
  'Last Quarter': '🌗',
  'Third Quarter': '🌗',
  'Waning Crescent': '🌘',
}

const moonEmoji = computed(() => {
  if (!astronomical.value?.moonPhaseName) return '🌙'
  return MOON_PHASES[astronomical.value.moonPhaseName] || '🌙'
})

function phaseEmoji(phase: string): string {
  return MOON_PHASES[phase] || '🌙'
}

const UV_LEVELS = [
  { max: 2, label: 'Low', class: 'uv-low', advice: 'No protection needed. You can safely stay outside.' },
  { max: 5, label: 'Moderate', class: 'uv-moderate', advice: 'Wear sunscreen. Seek shade during midday hours.' },
  { max: 7, label: 'High', class: 'uv-high', advice: 'Reduce sun exposure between 10am–4pm. Wear sunscreen, a hat, and sunglasses.' },
  { max: 10, label: 'Very High', class: 'uv-very-high', advice: 'Take extra precautions. Unprotected skin will be damaged quickly.' },
  { max: Infinity, label: 'Extreme', class: 'uv-extreme', advice: 'Avoid sun exposure. Unprotected skin can burn in minutes.' },
]

const uvLevel = computed(() => {
  if (astronomical.value?.uvIndex == null) return UV_LEVELS[0]
  return UV_LEVELS.find(l => astronomical.value!.uvIndex! <= l.max) || UV_LEVELS[4]
})

const uvClass = computed(() => uvLevel.value.class)
const uvLabel = computed(() => uvLevel.value.label)
const uvAdvice = computed(() => uvLevel.value.advice)

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
.astro-content {
  margin-top: 16px;
}

.astro-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

.sun-card h3, .moon-card h3, .uv-card h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.sun-times {
  display: flex;
  justify-content: space-around;
  margin-bottom: 16px;
}

.sun-event {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sun-icon.rise { color: #ff9800; }
.sun-icon.set { color: #e91e63; }

.event-label {
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.event-time {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary, #333);
}

.sun-details {
  border-top: 1px solid var(--border-color, #eee);
  padding-top: 12px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
}

.detail-label {
  color: var(--text-secondary, #666);
  font-weight: 500;
}

.detail-value {
  color: var(--text-primary, #333);
  font-weight: 600;
}

.moon-phase-display {
  text-align: center;
  margin-bottom: 16px;
}

.moon-icon-large {
  font-size: 48px;
  margin-bottom: 8px;
}

.moon-phase-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary, #333);
}

.moon-illumination {
  font-size: 13px;
  color: var(--text-secondary, #666);
  margin-top: 4px;
}

.upcoming-phases {
  border-top: 1px solid var(--border-color, #eee);
  padding-top: 12px;
}

.phase-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
}

.phase-emoji { font-size: 16px; }

.phase-name {
  flex: 1;
  color: var(--text-primary, #333);
}

.phase-date {
  color: var(--text-secondary, #666);
  font-size: 12px;
}

.uv-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.uv-gauge {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
}

.uv-value {
  font-size: 32px;
  line-height: 1;
}

.uv-label {
  font-size: 13px;
  margin-top: 4px;
}

.uv-low { background: #4caf50; }
.uv-moderate { background: #ff9800; }
.uv-high { background: #f44336; }
.uv-very-high { background: #9c27b0; }
.uv-extreme { background: #d50000; }

.uv-advice {
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary, #666);
  max-width: 300px;
}

.no-data {
  text-align: center;
  color: var(--text-muted, #999);
  padding: 24px;
  font-style: italic;
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
  .astro-grid {
    grid-template-columns: 1fr;
  }

  .sun-times {
    flex-direction: column;
    gap: 12px;
    align-items: center;
  }
}
</style>
