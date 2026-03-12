<template>
  <div class="container">
    <h1>{{ $t('hurricane.title') }}</h1>

    <div class="card">
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
        <h2>{{ $t('hurricane.activeSystems') }}</h2>
        <div style="display: flex; gap: 8px; align-items: center">
          <select v-model="basinFilter" :aria-label="$t('hurricane.basinFilter')" class="basin-filter">
            <option value="">{{ $t('hurricane.allBasins') }}</option>
            <option v-for="b in availableBasins" :key="b" :value="b">
              {{ basinName(b) }}
            </option>
          </select>
          <button :disabled="refreshing" @click="refreshHurricanes">
            {{ refreshing ? $t('airport.refreshing') : $t('hurricane.refreshData') }}
          </button>
        </div>
      </div>
    </div>

    <ErrorBoundary>
      <HurricaneMap
        v-if="filteredStorms.length > 0"
        :storms="filteredStorms"
        @storm-selected="onStormSelected"
      />
    </ErrorBoundary>

    <HurricaneSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="filteredStorms.length > 0">
      <div
        v-for="storm in filteredStorms"
        :key="storm.id"
        class="card"
        :class="{ 'card-selected': selectedStormId === storm.stormId }"
      >
        <div class="storm-header">
          <div class="storm-header-left">
            <h2>{{ storm.stormName || 'Unnamed Storm' }}</h2>
            <span v-if="storm.basin" class="basin-badge" :class="'basin-' + (storm.basin || '').toLowerCase()">
              {{ basinName(storm.basin) }}
            </span>
          </div>
          <div class="storm-meta">
            <span class="storm-type">{{ getStormType(storm) }}</span>
            <span class="storm-id">{{ storm.stormId }}</span>
          </div>
        </div>

        <div class="storm-info">
          <div class="info-item">
            <strong>{{ $t('hurricane.category') }}</strong>
            <span
              class="category"
              :class="'cat-' + (storm.category || 0)"
              :aria-label="'Category: ' + getCategoryLabel(storm)"
            >
              {{ getCategoryLabel(storm) }}
            </span>
          </div>
          <div class="info-item">
            <strong>{{ $t('hurricane.maxWinds') }}</strong>
            {{ storm.maxSustainedWindsMph }} mph
          </div>
          <div class="info-item">
            <strong>{{ $t('hurricane.pressure') }}</strong>
            {{ storm.minCentralPressureMb }} mb
          </div>
          <div class="info-item">
            <strong>{{ $t('hurricane.statusLabel') }}</strong>
            {{ storm.status }}
          </div>
          <div v-if="storm.movementSpeedMph != null" class="info-item">
            <strong>{{ $t('hurricane.movement') }}</strong>
            {{ storm.movementSpeedMph }} mph{{
              storm.movementDirection != null ? ` at ${storm.movementDirection}\u00b0` : ''
            }}
          </div>
        </div>

        <div class="storm-location">
          <strong>{{ $t('hurricane.position') }}</strong>
          {{ storm.latitude }}\u00b0{{ storm.latitude >= 0 ? 'N' : 'S' }},
          {{ Math.abs(storm.longitude) }}\u00b0{{ storm.longitude >= 0 ? 'E' : 'W' }}
        </div>

        <div class="storm-time">
          <strong>{{ $t('hurricane.advisoryTime') }}</strong> {{ formatDate(storm.advisoryTime) }}
          <FreshnessBadge
            v-if="storm.fetchedAt"
            :fetched-at="storm.fetchedAt"
            data-type="hurricane"
          />
        </div>
      </div>
    </div>

    <div v-else-if="!loading" class="card">
      <p><span aria-hidden="true">✅</span> {{ $t('hurricane.noActive') }}</p>
      <p>{{ $t('hurricane.seasonInfo') }}</p>
      <p>{{ $t('hurricane.autoFetch') }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import { formatDate } from '../utils/dateUtils'
import type { Hurricane } from '../services/weatherService'
import FreshnessBadge from '../components/FreshnessBadge.vue'
import HurricaneMap from '../components/HurricaneMap.vue'
import HurricaneSkeleton from '../components/skeletons/HurricaneSkeleton.vue'
import ErrorBoundary from '../components/ErrorBoundary.vue'

const { t } = useI18n()
const toast = useToast()

const store = useWeatherStore()
const { hurricanes, hurricanesLoading: loading, hurricanesError: error } = storeToRefs(store)

const refreshing = ref(false)
const selectedStormId = ref<string | null>(null)
const basinFilter = ref('')

const BASIN_NAMES: Record<string, string> = {
  AT: 'Atlantic',
  EP: 'Eastern Pacific',
  CP: 'Central Pacific',
  WP: 'Western Pacific',
  IO: 'Indian Ocean',
  SH: 'Southern Hemisphere',
}

// Basin determines the proper name for major storms
const BASIN_STORM_TYPES: Record<string, string> = {
  AT: 'Hurricane',
  EP: 'Hurricane',
  CP: 'Hurricane',
  WP: 'Typhoon',
  IO: 'Cyclone',
  SH: 'Cyclone',
}

function basinName(basin: string): string {
  return BASIN_NAMES[basin] || basin
}

function getStormType(storm: Hurricane): string {
  if (storm.category != null && storm.category >= 1 && storm.basin) {
    return BASIN_STORM_TYPES[storm.basin] || 'Hurricane'
  }
  if (storm.category === 0) return t('hurricane.tropicalStorm')
  return BASIN_STORM_TYPES[storm.basin || 'AT'] || 'Hurricane'
}

const availableBasins = computed(() => {
  const basins = new Set<string>()
  for (const s of hurricanes.value) {
    if (s.basin) basins.add(s.basin)
  }
  return [...basins].sort()
})

const filteredStorms = computed(() => {
  if (!basinFilter.value) return hurricanes.value
  return hurricanes.value.filter((s) => s.basin === basinFilter.value)
})

function onStormSelected(stormId: string) {
  selectedStormId.value = stormId
}

async function refreshHurricanes() {
  refreshing.value = true
  try {
    await store.refreshHurricanes()
    toast.success('Tropical system data refreshed')
  } catch {
    toast.error('Failed to refresh tropical system data')
  } finally {
    refreshing.value = false
  }
}

function getCategoryLabel(storm: Hurricane): string {
  if (storm.category === undefined || storm.category === null) return t('hurricane.na')
  if (storm.category === 0) return t('hurricane.tropicalStorm')
  const type = BASIN_STORM_TYPES[storm.basin || 'AT'] || 'Hurricane'
  return `${type} Cat ${storm.category}`
}

onMounted(() => {
  store.fetchHurricanes()
})
</script>

<style scoped>
.storm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  border-bottom: 2px solid var(--accent);
  padding-bottom: 8px;
  flex-wrap: wrap;
  gap: 6px;
}

.storm-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.storm-header-left h2 {
  margin: 0;
  color: var(--accent);
}

.storm-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.storm-type {
  background: var(--accent);
  color: white;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.storm-id {
  background: #333;
  color: white;
  padding: 3px 8px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
}

.basin-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.basin-at { background: #e3f2fd; color: #1565c0; }
.basin-ep { background: #e8f5e9; color: #2e7d32; }
.basin-cp { background: #fff3e0; color: #e65100; }
.basin-wp { background: #fce4ec; color: #c62828; }
.basin-io { background: #f3e5f5; color: #7b1fa2; }
.basin-sh { background: #e0f7fa; color: #00695c; }

.basin-filter {
  padding: 6px 10px;
  border: 1px solid var(--border, #ccc);
  border-radius: 6px;
  background: var(--card-bg, #fff);
  color: var(--text-primary, #333);
  font-size: 13px;
}

.storm-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
  margin: 10px 0;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.category {
  padding: 3px 8px;
  border-radius: 4px;
  font-weight: bold;
  color: white;
  display: inline-block;
}

.cat-0 { background: var(--storm-ts); }
.cat-1 { background: var(--storm-cat1); color: var(--storm-cat1-text); }
.cat-2 { background: var(--storm-cat2); }
.cat-3 { background: var(--storm-cat3); }
.cat-4 { background: var(--storm-cat4); }
.cat-5 { background: var(--storm-cat5); }

.storm-location,
.storm-time {
  margin: 6px 0;
  color: var(--text-secondary, #666);
  font-size: 13px;
}

.card-selected {
  border: 2px solid var(--accent);
  box-shadow: 0 0 12px rgba(238, 0, 0, 0.2);
}
</style>
