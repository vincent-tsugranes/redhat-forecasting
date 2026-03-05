<template>
  <div class="settings-overlay" @click.self="$emit('close')">
    <div
      ref="panelRef"
      class="settings-panel"
      role="dialog"
      aria-modal="true"
      aria-label="Settings"
      @keydown.esc="$emit('close')"
    >
      <div class="settings-header">
        <h3>{{ $t('settings.title') }}</h3>
        <button class="close-btn" aria-label="Close settings" @click="$emit('close')">&times;</button>
      </div>
      <div class="settings-body">
        <div class="setting-group">
          <label for="temp-unit">{{ $t('settings.temperature') }}</label>
          <select id="temp-unit" v-model="tempUnit">
            <option value="F">{{ $t('settings.fahrenheit') }}</option>
            <option value="C">{{ $t('settings.celsius') }}</option>
          </select>
        </div>
        <div class="setting-group">
          <label for="speed-unit">{{ $t('settings.windSpeed') }}</label>
          <select id="speed-unit" v-model="speedUnit">
            <option value="mph">{{ $t('settings.mph') }}</option>
            <option value="kmh">{{ $t('settings.kmh') }}</option>
          </select>
        </div>
        <div class="setting-group">
          <label for="time-format">{{ $t('settings.timeFormat') }}</label>
          <select id="time-format" v-model="timeFormat">
            <option value="12h">{{ $t('settings.time12h') }}</option>
            <option value="24h">{{ $t('settings.time24h') }}</option>
          </select>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useUnitPreferences } from '../composables/useUnitPreferences'

defineEmits<{
  close: []
}>()

const { tempUnit, speedUnit, timeFormat } = useUnitPreferences()

const panelRef = ref<HTMLElement | null>(null)

function trapFocus(e: KeyboardEvent) {
  if (e.key !== 'Tab' || !panelRef.value) return
  const focusable = panelRef.value.querySelectorAll<HTMLElement>(
    'button, select, input, [tabindex]:not([tabindex="-1"])',
  )
  if (focusable.length === 0) return
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (e.shiftKey && document.activeElement === first) {
    e.preventDefault()
    last.focus()
  } else if (!e.shiftKey && document.activeElement === last) {
    e.preventDefault()
    first.focus()
  }
}

onMounted(() => {
  panelRef.value?.querySelector<HTMLElement>('button, select, input')?.focus()
  document.addEventListener('keydown', trapFocus)
})

onUnmounted(() => {
  document.removeEventListener('keydown', trapFocus)
})
</script>

<style scoped>
.settings-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(2px);
  z-index: 5000;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 100px;
}

.settings-panel {
  background: var(--bg-card, white);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  width: 340px;
  max-width: 90vw;
  overflow: hidden;
}

.settings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light, #eee);
}

.settings-header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: var(--text-primary, #333);
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: var(--text-secondary, #666);
  padding: 0;
  line-height: 1;
}

.close-btn:hover {
  color: var(--text-primary, #333);
}

.settings-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.setting-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.setting-group label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary, #666);
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.setting-group select {
  padding: 8px 12px;
  border: 2px solid var(--border-color, #ddd);
  border-radius: 6px;
  font-size: 14px;
  background: var(--bg-input, #fff);
  color: var(--text-primary, #333);
  cursor: pointer;
}

.setting-group select:focus {
  border-color: #ee0000;
  outline: none;
}
</style>
