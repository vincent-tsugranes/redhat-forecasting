<template>
  <div class="hurricane-map-container">
    <div ref="mapContainer" class="map-container" role="application" aria-label="Hurricane tracking map"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, shallowRef, onMounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import weatherService, { type Hurricane } from '../services/weatherService'
import { formatDate, formatRelativeTime, getFreshnessLevel } from '../utils/dateUtils'

const props = defineProps<{
  storms: Hurricane[]
}>()

const emit = defineEmits<{
  'storm-selected': [stormId: string]
}>()

const CATEGORY_COLORS: Record<number, string> = {
  0: '#007bff',
  1: '#ffc107',
  2: '#ff9800',
  3: '#ff5722',
  4: '#f44336',
  5: '#9c27b0',
}

function getCategoryColor(category?: number): string {
  return CATEGORY_COLORS[category ?? 0] ?? CATEGORY_COLORS[0]
}

function getCategoryLabel(category?: number): string {
  if (category === undefined || category === null) return 'N/A'
  if (category === 0) return 'Tropical Storm'
  return `Category ${category}`
}

const mapContainer = ref<HTMLElement | null>(null)
const map = shallowRef<L.Map | null>(null)
const stormMarkersLayer = shallowRef<L.LayerGroup | null>(null)
const trackLayer = shallowRef<L.LayerGroup | null>(null)
const selectedStormId = ref<string | null>(null)

function initializeMap() {
  if (!mapContainer.value) return

  map.value = L.map(mapContainer.value).setView([25, -70], 4)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
    maxZoom: 18,
  }).addTo(map.value)

  stormMarkersLayer.value = L.layerGroup().addTo(map.value)
  trackLayer.value = L.layerGroup().addTo(map.value)
}

function placeStormMarkers() {
  if (!stormMarkersLayer.value || !map.value) return
  stormMarkersLayer.value.clearLayers()

  props.storms.forEach((storm) => {
    const color = getCategoryColor(storm.category)
    const marker = L.marker([storm.latitude, storm.longitude], {
      icon: L.divIcon({
        className: 'storm-marker',
        html: `<div class="storm-marker-icon" style="background:${color};border-color:${color}">🌀</div>`,
        iconSize: [28, 28],
        iconAnchor: [14, 14],
      }),
    })

    marker.bindTooltip(storm.stormName || storm.stormId, {
      permanent: true,
      direction: 'top',
      offset: [0, -16],
      className: 'storm-label',
    })

    marker.on('click', () => {
      selectedStormId.value = storm.stormId
      emit('storm-selected', storm.stormId)
      loadTrack(storm.stormId)
    })

    marker.addTo(stormMarkersLayer.value!)
  })

  // Auto-fit bounds if there are storms
  if (props.storms.length > 0) {
    const bounds = L.latLngBounds(
      props.storms.map((s) => [s.latitude, s.longitude] as [number, number])
    )
    map.value.fitBounds(bounds.pad(0.5), { maxZoom: 6 })
  }
}

async function loadTrack(stormId: string) {
  if (!trackLayer.value || !map.value) return
  trackLayer.value.clearLayers()

  try {
    const trackPoints = await weatherService.getStormTrack(stormId)
    if (trackPoints.length === 0) return

    // Draw polyline segments colored by category
    for (let i = 1; i < trackPoints.length; i++) {
      const prev = trackPoints[i - 1]
      const curr = trackPoints[i]
      const color = getCategoryColor(curr.category)

      L.polyline(
        [
          [prev.latitude, prev.longitude],
          [curr.latitude, curr.longitude],
        ],
        { color, weight: 3, opacity: 0.8 }
      ).addTo(trackLayer.value!)
    }

    // Draw circle markers at each advisory point
    trackPoints.forEach((pt) => {
      const color = getCategoryColor(pt.category)
      const circle = L.circleMarker([pt.latitude, pt.longitude], {
        radius: 5,
        fillColor: color,
        color: '#fff',
        weight: 2,
        fillOpacity: 0.9,
      })

      const freshnessLevel = pt.fetchedAt ? getFreshnessLevel(pt.fetchedAt, 'hurricane') : 'fresh'
      const relativeTime = pt.fetchedAt ? formatRelativeTime(pt.fetchedAt) : ''

      circle.bindPopup(`
        <div class="track-popup">
          <div class="popup-header">
            <strong>${pt.stormName || pt.stormId}</strong>
          </div>
          <div class="popup-category" style="background:${color};color:${pt.category === 1 ? '#333' : '#fff'}">
            ${getCategoryLabel(pt.category)}
          </div>
          <div class="popup-details">
            ${pt.maxSustainedWindsMph != null ? `<div class="popup-item"><span class="popup-label">Winds:</span> <span class="popup-value">${pt.maxSustainedWindsMph} mph</span></div>` : ''}
            ${pt.minCentralPressureMb != null ? `<div class="popup-item"><span class="popup-label">Pressure:</span> <span class="popup-value">${pt.minCentralPressureMb} mb</span></div>` : ''}
            <div class="popup-item"><span class="popup-label">Position:</span> <span class="popup-value">${Math.abs(pt.latitude).toFixed(1)}°${pt.latitude >= 0 ? 'N' : 'S'}, ${Math.abs(pt.longitude).toFixed(1)}°${pt.longitude >= 0 ? 'E' : 'W'}</span></div>
            <div class="popup-time">${formatDate(pt.advisoryTime)}</div>
            ${relativeTime ? `<span class="freshness-indicator freshness-${freshnessLevel}">${relativeTime}</span>` : ''}
          </div>
        </div>
      `)

      circle.addTo(trackLayer.value!)
    })

    // Fit map to track bounds
    const trackBounds = L.latLngBounds(
      trackPoints.map((pt) => [pt.latitude, pt.longitude] as [number, number])
    )
    map.value.fitBounds(trackBounds.pad(0.3), { maxZoom: 8 })
  } catch (error) {
    console.error('Error loading storm track:', error)
  }
}

watch(
  () => props.storms,
  () => {
    placeStormMarkers()
  }
)

onMounted(() => {
  initializeMap()
  if (props.storms.length > 0) {
    placeStormMarkers()
  }
})
</script>

<style scoped>
.hurricane-map-container {
  width: 100%;
  height: clamp(300px, 50vh, 500px);
  margin-bottom: 20px;
}

.map-container {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.storm-marker) {
  background: none;
  border: none;
}

:deep(.storm-marker-icon) {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

:deep(.storm-label) {
  font-weight: bold;
  font-size: 12px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border: none;
  border-radius: 4px;
  padding: 2px 6px;
}

:deep(.storm-label::before) {
  border-top-color: rgba(0, 0, 0, 0.7);
}

:deep(.track-popup) {
  padding: 4px;
  min-width: 180px;
}

:deep(.track-popup .popup-header) {
  font-size: 15px;
  color: #333;
  margin-bottom: 6px;
}

:deep(.track-popup .popup-category) {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 8px;
}

:deep(.track-popup .popup-details) {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

:deep(.track-popup .popup-item) {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  gap: 8px;
}

:deep(.track-popup .popup-label) {
  color: #666;
  font-weight: 500;
}

:deep(.track-popup .popup-value) {
  color: #333;
  font-weight: 600;
  text-align: right;
}

:deep(.track-popup .popup-time) {
  font-size: 11px;
  color: #999;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid #eee;
  text-align: center;
}

:deep(.freshness-indicator) {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 600;
  margin-top: 4px;
}

:deep(.freshness-fresh) {
  background: #e8f5e9;
  color: #2e7d32;
}

:deep(.freshness-aging) {
  background: #fff3e0;
  color: #e65100;
}

:deep(.freshness-stale) {
  background: #ffebee;
  color: #c62828;
}

:deep(.leaflet-popup-content-wrapper) {
  border-radius: 8px;
}
</style>
