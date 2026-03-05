<template>
  <div class="kp-gauge" role="meter" :aria-valuenow="value" aria-valuemin="0" aria-valuemax="9" :aria-label="`Kp index: ${value}`">
    <div class="gauge-bar">
      <div
        v-for="i in 9"
        :key="i"
        class="gauge-segment"
        :class="{ 'segment-active': i <= Math.ceil(value), 'segment-current': i === Math.ceil(value) }"
        :style="{ background: i <= Math.ceil(value) ? getSegmentColor(i) : undefined }"
      >
        <span class="segment-label">{{ i }}</span>
      </div>
    </div>
    <div class="gauge-value">{{ value.toFixed(1) }}</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  value: number
}>()

function getSegmentColor(index: number): string {
  if (index <= 3) return '#4caf50'
  if (index === 4) return '#8bc34a'
  if (index === 5) return '#ff9800'
  if (index <= 7) return '#f44336'
  return '#9c27b0'
}
</script>

<style scoped>
.kp-gauge {
  padding: 8px 0;
}

.gauge-bar {
  display: flex;
  gap: 3px;
  margin-bottom: 8px;
}

.gauge-segment {
  flex: 1;
  height: 24px;
  background: var(--bg-code, #e0e0e0);
  border-radius: 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.segment-active {
  color: white;
  font-weight: 600;
}

.segment-current {
  transform: scaleY(1.2);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.segment-label {
  font-size: 10px;
}

.gauge-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary, #333);
}
</style>
