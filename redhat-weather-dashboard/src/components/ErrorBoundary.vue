<template>
  <div v-if="error" class="error-boundary">
    <div class="error-content">
      <span class="error-icon" aria-hidden="true">&#x26A0;</span>
      <p class="error-message">{{ $t('error.sectionFailed') }}</p>
      <button class="error-retry" @click="retry">{{ $t('error.retry') }}</button>
    </div>
  </div>
  <slot v-else />
</template>

<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue'
import { logger } from '../utils/logger'

const error = ref<Error | null>(null)

onErrorCaptured((err: Error) => {
  error.value = err
  logger.error('ErrorBoundary caught:', err)
  return false
})

function retry() {
  error.value = null
}
</script>

<style scoped>
.error-boundary {
  padding: 24px;
  text-align: center;
  border: 1px dashed var(--border-color, #ddd);
  border-radius: 8px;
  background: var(--bg-card, #fafafa);
}

.error-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.error-icon {
  font-size: 32px;
  opacity: 0.6;
}

.error-message {
  color: var(--text-secondary, #666);
  font-size: 14px;
  margin: 0;
}

.error-retry {
  padding: 6px 16px;
  border: 1px solid var(--border-color, #ddd);
  border-radius: 6px;
  background: var(--bg-card, white);
  color: var(--text-primary, #333);
  font-size: 13px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.error-retry:hover {
  background: var(--bg-code, #f0f0f0);
}
</style>
