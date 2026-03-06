<template>
  <div class="container">
    <h1>{{ $t('earthquake.title') }}</h1>

    <div class="card">
      <div class="card-header-row">
        <h2>{{ $t('earthquake.recentActivity') }}</h2>
        <div class="header-actions">
          <button class="btn-sm btn-icon" :class="viewMode === 'table' ? '' : 'btn-secondary'" @click="viewMode = 'table'">
            <span aria-hidden="true">📋</span> Table
          </button>
          <button class="btn-sm btn-icon" :class="viewMode === 'cards' ? '' : 'btn-secondary'" @click="viewMode = 'cards'">
            <span aria-hidden="true">🃏</span> Cards
          </button>
          <button class="btn-sm" :disabled="refreshing" @click="refreshData">
            {{ refreshing ? $t('airport.refreshing') : $t('earthquake.refreshData') }}
          </button>
        </div>
      </div>
    </div>

    <EarthquakeMap
      v-if="earthquakes.length > 0"
      :earthquakes="earthquakes"
      @quake-selected="onQuakeSelected"
    />

    <EarthquakeSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <!-- TABLE VIEW -->
    <div v-if="earthquakes.length > 0 && viewMode === 'table'" class="card">
      <div class="table-controls">
        <input
          v-model="filterText"
          type="text"
          class="table-filter"
          placeholder="Filter by location..."
        />
        <span class="table-meta">{{ filteredEarthquakes.length }} of {{ earthquakes.length }} earthquakes</span>
      </div>
      <div class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th @click="toggleSort('magnitude')">
                Mag
                <span class="sort-indicator" :class="{ active: sortKey === 'magnitude' }">{{ sortIcon('magnitude') }}</span>
              </th>
              <th @click="toggleSort('place')">
                Location
                <span class="sort-indicator" :class="{ active: sortKey === 'place' }">{{ sortIcon('place') }}</span>
              </th>
              <th @click="toggleSort('depthKm')">
                Depth
                <span class="sort-indicator" :class="{ active: sortKey === 'depthKm' }">{{ sortIcon('depthKm') }}</span>
              </th>
              <th @click="toggleSort('eventTime')">
                Time
                <span class="sort-indicator" :class="{ active: sortKey === 'eventTime' }">{{ sortIcon('eventTime') }}</span>
              </th>
              <th>Felt</th>
              <th>Alert</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="quake in sortedEarthquakes"
              :key="quake.id"
              :class="{ 'row-selected': selectedQuakeId === quake.usgsId }"
              @click="onQuakeSelected(quake.usgsId)"
            >
              <td>
                <span class="magnitude-badge" :class="getMagnitudeClass(quake.magnitude)">M{{ quake.magnitude }}</span>
              </td>
              <td class="td-truncate">
                {{ quake.place }}
                <span v-if="quake.tsunami" class="tsunami-tag" aria-hidden="true">🌊</span>
              </td>
              <td>{{ quake.depthKm }} km</td>
              <td class="td-nowrap">{{ formatDate(quake.eventTime) }}</td>
              <td>{{ quake.felt || '-' }}</td>
              <td>
                <span v-if="quake.alert" class="alert-badge" :class="'alert-' + quake.alert">{{ quake.alert }}</span>
                <span v-else>-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- CARD VIEW -->
    <div v-if="earthquakes.length > 0 && viewMode === 'cards'">
      <div
        v-for="quake in earthquakes"
        :key="quake.id"
        class="card"
        :class="{ 'card-selected': selectedQuakeId === quake.usgsId }"
      >
        <div class="quake-header">
          <h3>
            <span
              class="magnitude-badge"
              :class="getMagnitudeClass(quake.magnitude)"
            >M{{ quake.magnitude }}</span>
            {{ quake.place }}
          </h3>
        </div>

        <div class="quake-info">
          <div class="info-item">
            <strong>{{ $t('earthquake.depth') }}</strong>
            {{ quake.depthKm }} km
          </div>
          <div class="info-item">
            <strong>{{ $t('earthquake.time') }}</strong>
            {{ formatDate(quake.eventTime) }}
          </div>
          <div v-if="quake.felt" class="info-item">
            <strong>{{ $t('earthquake.felt') }}</strong>
            {{ quake.felt }} {{ $t('earthquake.reports') }}
          </div>
          <div v-if="quake.tsunami" class="info-item tsunami-warning">
            <span aria-hidden="true">🌊</span> {{ $t('earthquake.tsunamiWarning') }}
          </div>
          <div v-if="quake.alert" class="info-item">
            <strong>{{ $t('earthquake.alert') }}</strong>
            <span class="alert-badge" :class="'alert-' + quake.alert">{{ quake.alert }}</span>
          </div>
        </div>

        <div class="quake-location">
          <strong>{{ $t('earthquake.position') }}</strong>
          {{ quake.latitude.toFixed(3) }}{{ quake.latitude >= 0 ? 'N' : 'S' }},
          {{ Math.abs(quake.longitude).toFixed(3) }}{{ quake.longitude >= 0 ? 'E' : 'W' }}
        </div>

        <div v-if="quake.fetchedAt" class="quake-time">
          <FreshnessBadge :fetched-at="quake.fetchedAt" data-type="earthquake" />
        </div>
      </div>
    </div>

    <div v-else-if="!loading && earthquakes.length === 0" class="card">
      <p><span aria-hidden="true">✅</span> {{ $t('earthquake.noRecent') }}</p>
      <p>{{ $t('earthquake.autoFetch') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import { formatDate } from '../utils/dateUtils'
import FreshnessBadge from '../components/FreshnessBadge.vue'
import EarthquakeMap from '../components/EarthquakeMap.vue'
import EarthquakeSkeleton from '../components/skeletons/EarthquakeSkeleton.vue'

const store = useWeatherStore()
const toast = useToast()
const { earthquakes, earthquakesLoading: loading, earthquakesError: error } = storeToRefs(store)

const refreshing = ref(false)
const selectedQuakeId = ref<string | null>(null)
const viewMode = ref<'table' | 'cards'>('table')
const filterText = ref('')
const sortKey = ref<string>('eventTime')
const sortDir = ref<'asc' | 'desc'>('desc')

function onQuakeSelected(usgsId: string) {
  selectedQuakeId.value = usgsId
}

async function refreshData() {
  refreshing.value = true
  try {
    await store.refreshEarthquakes()
    toast.success('Earthquake data refreshed')
  } catch {
    toast.error('Failed to refresh earthquake data')
  } finally {
    refreshing.value = false
  }
}

function getMagnitudeClass(magnitude: number) {
  if (magnitude >= 7) return 'mag-major'
  if (magnitude >= 5) return 'mag-strong'
  if (magnitude >= 4) return 'mag-moderate'
  return 'mag-light'
}

function toggleSort(key: string) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = key === 'eventTime' || key === 'magnitude' ? 'desc' : 'asc'
  }
}

function sortIcon(key: string) {
  if (sortKey.value !== key) return '⇅'
  return sortDir.value === 'asc' ? '↑' : '↓'
}

const filteredEarthquakes = computed(() => {
  if (!filterText.value) return earthquakes.value
  const q = filterText.value.toLowerCase()
  return earthquakes.value.filter(eq => eq.place?.toLowerCase().includes(q))
})

const sortedEarthquakes = computed(() => {
  const data = [...filteredEarthquakes.value]
  const dir = sortDir.value === 'asc' ? 1 : -1
  const key = sortKey.value as keyof (typeof data)[0]

  data.sort((a, b) => {
    const av = a[key]
    const bv = b[key]
    if (av == null && bv == null) return 0
    if (av == null) return 1
    if (bv == null) return -1
    if (typeof av === 'string' && typeof bv === 'string') return av.localeCompare(bv) * dir
    return ((av as number) - (bv as number)) * dir
  })

  return data
})

onMounted(() => {
  store.fetchEarthquakes()
})
</script>

<style scoped>
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.card-header-row h2 {
  margin-bottom: 0;
}

.header-actions {
  display: flex;
  gap: 6px;
  align-items: center;
}

.quake-header {
  margin-bottom: 10px;
}

.quake-header h3 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.magnitude-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
  color: white;
  font-size: 12px;
  white-space: nowrap;
}

.mag-light { background: #4caf50; }
.mag-moderate { background: #ff9800; }
.mag-strong { background: #f44336; }
.mag-major { background: #9c27b0; }

.quake-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin: 12px 0;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tsunami-warning {
  color: #f44336;
  font-weight: bold;
}

.tsunami-tag {
  margin-left: 4px;
}

.alert-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
  color: white;
  text-transform: uppercase;
  font-size: 12px;
}

.alert-green { background: #4caf50; }
.alert-yellow { background: #ffc107; color: #333; }
.alert-orange { background: #ff9800; }
.alert-red { background: #f44336; }

.quake-location,
.quake-time {
  margin: 8px 0;
  color: var(--text-secondary, #666);
  font-size: 13px;
}

.card-selected {
  border: 2px solid #ee0000;
  box-shadow: 0 0 12px rgba(238, 0, 0, 0.2);
}

.td-truncate {
  max-width: 280px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.td-nowrap {
  white-space: nowrap;
}
</style>
