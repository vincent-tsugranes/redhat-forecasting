<template>
  <div class="earthquake-map-container">
    <div
      ref="mapContainer"
      class="map-container"
      role="application"
      aria-label="Interactive earthquake map"
    ></div>
    <div class="magnitude-legend">
      <div class="legend-title">Magnitude</div>
      <div class="legend-item"><span class="legend-dot dot-light"></span> 2.5 - 4.0</div>
      <div class="legend-item"><span class="legend-dot dot-moderate"></span> 4.0 - 5.0</div>
      <div class="legend-item"><span class="legend-dot dot-strong"></span> 5.0 - 7.0</div>
      <div class="legend-item"><span class="legend-dot dot-major"></span> 7.0+</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { type Earthquake } from '../services/weatherService'
import { formatDate } from '../utils/dateUtils'

const props = defineProps<{
  earthquakes: Earthquake[]
}>()

const emit = defineEmits<{
  quakeSelected: [usgsId: string]
}>()

const mapContainer = ref<HTMLElement | null>(null)
const map = ref<L.Map | null>(null)
const markersLayer = ref<L.LayerGroup | null>(null)

function getMagnitudeColor(magnitude: number): string {
  if (magnitude >= 7) return '#9c27b0'
  if (magnitude >= 5) return '#f44336'
  if (magnitude >= 4) return '#ff9800'
  return '#4caf50'
}

function initializeMap() {
  if (!mapContainer.value) return

  map.value = L.map(mapContainer.value).setView([20, -120], 3)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors',
    maxZoom: 18,
  }).addTo(map.value as L.Map)

  markersLayer.value = L.layerGroup().addTo(map.value as L.Map)
  placeMarkers()
}

function placeMarkers() {
  if (!markersLayer.value || !map.value) return
  markersLayer.value.clearLayers()

  props.earthquakes.forEach((quake) => {
    const color = getMagnitudeColor(quake.magnitude)
    const radius = Math.max(quake.magnitude * 3, 5)

    const marker = L.circleMarker([quake.latitude, quake.longitude], {
      radius,
      fillColor: color,
      color: '#fff',
      weight: 1,
      opacity: 1,
      fillOpacity: 0.7,
    })

    const popupContent = `
      <div class="quake-popup">
        <div class="popup-mag" style="color: ${color}; font-size: 18px; font-weight: bold;">M${quake.magnitude}</div>
        <div class="popup-place">${quake.place || 'Unknown location'}</div>
        <div class="popup-detail">Depth: ${quake.depthKm} km</div>
        <div class="popup-detail">Time: ${formatDate(quake.eventTime)}</div>
        ${quake.felt ? `<div class="popup-detail">Felt: ${quake.felt} reports</div>` : ''}
        ${quake.tsunami ? '<div class="popup-tsunami">🌊 Tsunami warning</div>' : ''}
      </div>
    `
    marker.bindPopup(popupContent)

    marker.on('click', () => {
      emit('quakeSelected', quake.usgsId)
    })

    markersLayer.value!.addLayer(marker)
  })
}

watch(() => props.earthquakes, placeMarkers, { deep: true })

onMounted(initializeMap)

onBeforeUnmount(() => {
  if (map.value) {
    map.value.remove()
    map.value = null
  }
  markersLayer.value = null
})
</script>

<style scoped>
.earthquake-map-container {
  position: relative;
  width: 100%;
  height: clamp(300px, 40vh, 500px);
  margin-bottom: 12px;
}

.map-container {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.magnitude-legend {
  position: absolute;
  bottom: 20px;
  right: 10px;
  z-index: 1000;
  background: var(--bg-card, white);
  padding: 8px 12px;
  border-radius: 6px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  font-size: 12px;
  line-height: 1.6;
}

.legend-title {
  font-weight: 600;
  margin-bottom: 4px;
  font-size: 11px;
  color: var(--text-secondary, #666);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-primary, #333);
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.2);
}

.dot-light { background: var(--severity-light); }
.dot-moderate { background: var(--severity-moderate); }
.dot-strong { background: var(--severity-strong); }
.dot-major { background: var(--severity-major); }

:deep(.quake-popup) {
  padding: 4px;
}

:deep(.popup-place) {
  font-size: 14px;
  font-weight: 500;
  margin: 4px 0;
}

:deep(.popup-detail) {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin: 2px 0;
}

:deep(.popup-tsunami) {
  color: #f44336;
  font-weight: bold;
  font-size: 12px;
  margin-top: 4px;
}
</style>
