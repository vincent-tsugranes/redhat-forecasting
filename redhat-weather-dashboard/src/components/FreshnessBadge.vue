<template>
  <span v-if="fetchedAt" class="freshness-badge" :class="freshnessClass" :title="tooltipText">
    {{ relativeTime }}
  </span>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { formatRelativeTime, getFreshnessLevel, type DataType, type FreshnessLevel } from '../utils/dateUtils'

const props = defineProps<{
  fetchedAt: string
  dataType: DataType
}>()

const tick = ref(0)
let intervalId: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  intervalId = setInterval(() => { tick.value++ }, 30000)
})

onUnmounted(() => {
  if (intervalId) clearInterval(intervalId)
})

const relativeTime = computed(() => {
  tick.value
  return formatRelativeTime(props.fetchedAt)
})

const freshnessLevel = computed((): FreshnessLevel => {
  tick.value
  return getFreshnessLevel(props.fetchedAt, props.dataType)
})

const freshnessClass = computed(() => `freshness-${freshnessLevel.value}`)

const tooltipText = computed(() => {
  const date = new Date(props.fetchedAt)
  const levelLabel = freshnessLevel.value === 'fresh' ? 'Data is current'
    : freshnessLevel.value === 'aging' ? 'Data may be refreshing soon'
    : 'Data is overdue for refresh'
  return `Fetched: ${date.toLocaleString()}\n${levelLabel}`
})
</script>

<style scoped>
.freshness-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.freshness-fresh {
  background: #e8f5e9;
  color: #2e7d32;
}

.freshness-aging {
  background: #fff3e0;
  color: #e65100;
}

.freshness-stale {
  background: #ffebee;
  color: #c62828;
}
</style>
