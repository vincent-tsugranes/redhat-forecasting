<template>
  <div class="decoded-weather">
    <button class="decode-toggle btn-sm btn-secondary" @click="showDecoded = !showDecoded">
      {{ showDecoded ? 'Show Raw' : 'Decode' }}
    </button>

    <template v-if="!showDecoded">
      <slot />
    </template>

    <!-- METAR decoded -->
    <template v-else-if="type === 'metar' && decodedMetar">
      <div class="decoded-grid">
        <div v-if="decodedMetar.isAuto" class="decoded-row decoded-note">
          Automated observation
        </div>
        <div v-if="decodedMetar.wind" class="decoded-row">
          <span class="decoded-label">Wind</span>
          <span class="decoded-value">{{ decodedMetar.wind }}</span>
        </div>
        <div v-if="decodedMetar.visibility" class="decoded-row">
          <span class="decoded-label">Visibility</span>
          <span class="decoded-value">{{ decodedMetar.visibility }}</span>
        </div>
        <div v-for="(wx, i) in decodedMetar.weather" :key="'wx' + i" class="decoded-row">
          <span class="decoded-label">Weather</span>
          <span class="decoded-value">{{ wx }}</span>
        </div>
        <div v-for="(layer, i) in decodedMetar.skyLayers" :key="'sky' + i" class="decoded-row">
          <span class="decoded-label">Sky</span>
          <span class="decoded-value">
            {{ layer.coverage }}{{ layer.altitude ? ` at ${layer.altitude.toLocaleString()} ft` : '' }}{{ layer.cloudType ? ` (${layer.cloudType})` : '' }}
          </span>
        </div>
        <div v-if="decodedMetar.temperature" class="decoded-row">
          <span class="decoded-label">Temperature</span>
          <span class="decoded-value">{{ decodedMetar.temperature }}</span>
        </div>
        <div v-if="decodedMetar.dewpoint" class="decoded-row">
          <span class="decoded-label">Dewpoint</span>
          <span class="decoded-value">{{ decodedMetar.dewpoint }}</span>
        </div>
        <div v-if="decodedMetar.altimeter" class="decoded-row">
          <span class="decoded-label">Altimeter</span>
          <span class="decoded-value">{{ decodedMetar.altimeter }}</span>
        </div>
        <div v-if="decodedMetar.flightCategory" class="decoded-row">
          <span class="decoded-label">Category</span>
          <span class="decoded-value">
            <span class="flight-cat-badge" :class="'cat-' + decodedMetar.flightCategory">{{ decodedMetar.flightCategory }}</span>
          </span>
        </div>
        <div v-if="decodedMetar.remarks" class="decoded-row decoded-remarks">
          <span class="decoded-label">Remarks</span>
          <span class="decoded-value">{{ decodedMetar.remarks }}</span>
        </div>
      </div>
    </template>

    <!-- TAF decoded -->
    <template v-else-if="type === 'taf' && decodedTaf">
      <div class="taf-header">
        <span class="decoded-label">Valid</span>
        <span class="decoded-value">{{ decodedTaf.validRange }}</span>
      </div>
      <div v-for="(fc, i) in decodedTaf.forecasts" :key="i" class="taf-group" :class="'taf-type-' + fc.type.toLowerCase()">
        <div class="taf-group-header">
          <span class="taf-type-badge" :class="'badge-' + fc.type.toLowerCase()">
            {{ fc.type === 'BASE' ? 'Initial' : fc.type === 'FM' ? 'From' : fc.type === 'TEMPO' ? 'Temporary' : fc.type === 'BECMG' ? 'Becoming' : `Prob ${fc.probability}%` }}
          </span>
          <span class="taf-time">{{ fc.type === 'BASE' ? '' : fc.timeRange }}</span>
        </div>
        <div class="decoded-grid taf-details">
          <div v-if="fc.wind" class="decoded-row">
            <span class="decoded-label">Wind</span>
            <span class="decoded-value">{{ fc.wind }}</span>
          </div>
          <div v-if="fc.visibility" class="decoded-row">
            <span class="decoded-label">Visibility</span>
            <span class="decoded-value">{{ fc.visibility }}</span>
          </div>
          <div v-for="(wx, j) in fc.weather" :key="'wx' + j" class="decoded-row">
            <span class="decoded-label">Weather</span>
            <span class="decoded-value">{{ wx }}</span>
          </div>
          <div v-for="(layer, j) in fc.skyLayers" :key="'sky' + j" class="decoded-row">
            <span class="decoded-label">Sky</span>
            <span class="decoded-value">
              {{ layer.coverage }}{{ layer.altitude ? ` at ${layer.altitude.toLocaleString()} ft` : '' }}{{ layer.cloudType ? ` (${layer.cloudType})` : '' }}
            </span>
          </div>
          <div v-if="!fc.wind && !fc.visibility && fc.weather.length === 0 && fc.skyLayers.length === 0" class="decoded-row decoded-note">
            No changes
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { parseMetar, parseTaf } from '../utils/metarTafParser'

const props = defineProps<{
  rawText: string
  type: 'metar' | 'taf'
}>()

const showDecoded = ref(false)

const decodedMetar = computed(() => {
  if (props.type !== 'metar') return null
  return parseMetar(props.rawText)
})

const decodedTaf = computed(() => {
  if (props.type !== 'taf') return null
  return parseTaf(props.rawText)
})
</script>

<style scoped>
.decoded-weather {
  position: relative;
}

.decode-toggle {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 1;
}

.decoded-grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.decoded-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 13px;
  padding: 3px 0;
}

.decoded-label {
  font-weight: 600;
  color: var(--text-secondary, #666);
  min-width: 80px;
  flex-shrink: 0;
  font-size: 12px;
  text-transform: uppercase;
}

.decoded-value {
  color: var(--text-primary, #333);
}

.decoded-note {
  color: var(--text-muted, #999);
  font-style: italic;
}

.decoded-remarks {
  border-top: 1px solid var(--border-light, #eee);
  padding-top: 6px;
  margin-top: 4px;
}

.decoded-remarks .decoded-value {
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.flight-cat-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 700;
  font-size: 12px;
  color: white;
}

.cat-VFR { background: var(--flight-vfr); }
.cat-MVFR { background: var(--flight-mvfr); }
.cat-IFR { background: var(--flight-ifr); }
.cat-LIFR { background: var(--flight-lifr); }

/* TAF styles */
.taf-header {
  display: flex;
  gap: 8px;
  align-items: baseline;
  font-size: 13px;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--border-light, #eee);
}

.taf-group {
  margin-bottom: 8px;
  padding: 8px;
  border-radius: 6px;
  background: var(--bg-card, white);
  border-left: 3px solid var(--border-color, #ddd);
}

.taf-group:last-child {
  margin-bottom: 0;
}

.taf-type-base { border-left-color: var(--color-info); }
.taf-type-fm { border-left-color: var(--color-success); }
.taf-type-tempo { border-left-color: var(--color-warning); }
.taf-type-becmg { border-left-color: var(--severity-moderate); }
.taf-type-prob { border-left-color: var(--text-muted, #999); }

.taf-group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.taf-type-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  color: white;
}

.badge-base { background: var(--color-info); }
.badge-fm { background: var(--color-success); }
.badge-tempo { background: var(--color-warning); color: #333; }
.badge-becmg { background: var(--severity-moderate); }
.badge-prob { background: var(--text-muted, #999); }

.taf-time {
  font-size: 12px;
  color: var(--text-secondary, #666);
}

.taf-details {
  padding-left: 4px;
}

.taf-details .decoded-label {
  min-width: 70px;
}
</style>
