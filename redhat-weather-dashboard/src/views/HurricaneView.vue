<template>
  <div class="container">
    <h1>{{ $t('hurricane.title') }}</h1>

    <div class="card">
      <div style="display: flex; justify-content: space-between; align-items: center">
        <h2>{{ $t('hurricane.activeSystems') }}</h2>
        <button :disabled="refreshing" @click="refreshHurricanes">
          {{ refreshing ? $t('airport.refreshing') : $t('hurricane.refreshData') }}
        </button>
      </div>
    </div>

    <ErrorBoundary>
      <HurricaneMap
        v-if="hurricanes.length > 0"
        :storms="hurricanes"
        @storm-selected="onStormSelected"
      />
    </ErrorBoundary>

    <HurricaneSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="hurricanes.length > 0">
      <div
        v-for="storm in hurricanes"
        :key="storm.id"
        class="card"
        :class="{ 'card-selected': selectedStormId === storm.stormId }"
      >
        <div class="storm-header">
          <h2>{{ storm.stormName || 'Unnamed Storm' }}</h2>
          <div class="storm-id">{{ storm.stormId }}</div>
        </div>

        <div class="storm-info">
          <div class="info-item">
            <strong>{{ $t('hurricane.category') }}</strong>
            <span
              class="category"
              :class="'cat-' + (storm.category || 0)"
              :aria-label="'Hurricane category: ' + getCategoryLabel(storm.category)"
            >
              {{ getCategoryLabel(storm.category) }}
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
              storm.movementDirection != null ? ` at ${storm.movementDirection}°` : ''
            }}
          </div>
        </div>

        <div class="storm-location">
          <strong>{{ $t('hurricane.position') }}</strong>
          {{ storm.latitude }}°{{ storm.latitude >= 0 ? 'N' : 'S' }},
          {{ Math.abs(storm.longitude) }}°{{ storm.longitude >= 0 ? 'E' : 'W' }}
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
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import { useToast } from '../composables/useToast'
import { formatDate } from '../utils/dateUtils'
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

function onStormSelected(stormId: string) {
  selectedStormId.value = stormId
}

async function refreshHurricanes() {
  refreshing.value = true
  try {
    await store.refreshHurricanes()
    toast.success('Hurricane data refreshed')
  } catch {
    toast.error('Failed to refresh hurricane data')
  } finally {
    refreshing.value = false
  }
}

function getCategoryLabel(category: number | undefined): string {
  if (category === undefined || category === null) return t('hurricane.na')
  if (category === 0) return t('hurricane.tropicalStorm')
  return t('hurricane.categoryN', { n: category })
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
}

.storm-header h2 {
  margin: 0;
  color: var(--accent);
}

.storm-id {
  background: #333;
  color: white;
  padding: 3px 8px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
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

.cat-0 {
  background: var(--storm-ts);
}

.cat-1 {
  background: var(--storm-cat1);
  color: var(--storm-cat1-text);
}

.cat-2 {
  background: var(--storm-cat2);
}

.cat-3 {
  background: var(--storm-cat3);
}

.cat-4 {
  background: var(--storm-cat4);
}

.cat-5 {
  background: var(--storm-cat5);
}

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
