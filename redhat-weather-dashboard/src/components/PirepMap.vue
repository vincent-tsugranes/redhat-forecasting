<template>
  <div class="card">
    <h2><span aria-hidden="true">✈️</span> PIREP Locations</h2>
    <div ref="mapContainer" class="pirep-map" role="application" aria-label="Pilot report locations map"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, shallowRef, onMounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import type { Pirep } from '../services/weatherService'
import { formatDate } from '../utils/dateUtils'

const props = defineProps<{ pireps: Pirep[] }>()

const mapContainer = ref<HTMLElement | null>(null)
const map = shallowRef<L.Map | null>(null)
const markerLayer = shallowRef<L.LayerGroup | null>(null)

const TURB_COLORS: Record<string, string> = {
  NEG: '#4caf50', LGT: '#8bc34a', MOD: '#ff9800', SEV: '#f44336', EXTRM: '#9c27b0',
}
const ICE_COLORS: Record<string, string> = {
  NEG: '#4caf50', TRC: '#8bc34a', LGT: '#8bc34a', MOD: '#ff9800', SEV: '#f44336',
}

function getColor(pirep: Pirep): string {
  if (pirep.turbulenceIntensity && pirep.turbulenceIntensity !== 'NEG') {
    return TURB_COLORS[pirep.turbulenceIntensity] || '#2196f3'
  }
  if (pirep.icingIntensity && pirep.icingIntensity !== 'NEG') {
    return ICE_COLORS[pirep.icingIntensity] || '#2196f3'
  }
  return '#2196f3'
}

function placeMarkers() {
  if (!markerLayer.value) return
  markerLayer.value.clearLayers()

  for (const pirep of props.pireps) {
    const color = getColor(pirep)
    const marker = L.circleMarker([pirep.latitude, pirep.longitude], {
      radius: 5,
      fillColor: color,
      color: '#fff',
      weight: 1,
      fillOpacity: 0.8,
    })

    let details = ''
    if (pirep.altitudeFt) details += `Altitude: ${pirep.altitudeFt.toLocaleString()} ft<br/>`
    if (pirep.turbulenceIntensity) details += `Turbulence: ${pirep.turbulenceIntensity}<br/>`
    if (pirep.icingIntensity) details += `Icing: ${pirep.icingIntensity}<br/>`
    if (pirep.aircraftType) details += `Aircraft: ${pirep.aircraftType}<br/>`
    details += formatDate(pirep.observationTime)

    marker.bindPopup(`<strong>PIREP</strong><br/>${details}`)
    markerLayer.value.addLayer(marker)
  }
}

onMounted(() => {
  if (!mapContainer.value) return
  map.value = L.map(mapContainer.value).setView([39, -98], 4)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
    maxZoom: 18,
  }).addTo(map.value)
  markerLayer.value = L.layerGroup().addTo(map.value)
  placeMarkers()
})

watch(() => props.pireps, placeMarkers, { deep: true })
</script>

<style scoped>
.pirep-map {
  height: 400px;
  border-radius: 8px;
  overflow: hidden;
}
</style>
