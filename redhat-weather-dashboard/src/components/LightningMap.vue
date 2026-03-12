<template>
  <div class="card">
    <h2><span aria-hidden="true">⚡</span> Lightning Strikes</h2>
    <div ref="mapContainer" class="lightning-map" role="application" aria-label="Lightning strike locations map"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, shallowRef, onMounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import type { LightningStrike } from '../services/weatherService'

const props = defineProps<{ strikes: LightningStrike[] }>()

const mapContainer = ref<HTMLElement | null>(null)
const map = shallowRef<L.Map | null>(null)
const markerLayer = shallowRef<L.LayerGroup | null>(null)

function placeMarkers() {
  if (!markerLayer.value) return
  markerLayer.value.clearLayers()

  for (const strike of props.strikes) {
    const marker = L.circleMarker([strike.latitude, strike.longitude], {
      radius: 3,
      fillColor: '#ffeb3b',
      color: '#f57f17',
      weight: 1,
      fillOpacity: 0.7,
    })
    marker.bindPopup(
      `<strong>Lightning Strike</strong><br/>` +
      `${strike.latitude.toFixed(3)}, ${strike.longitude.toFixed(3)}<br/>` +
      (strike.amplitudeKa ? `Amplitude: ${strike.amplitudeKa.toFixed(1)} kA<br/>` : '') +
      `Type: ${strike.strikeType || 'CG'}`
    )
    markerLayer.value.addLayer(marker)
  }
}

onMounted(() => {
  if (!mapContainer.value) return
  const m = L.map(mapContainer.value, { zoomControl: true }).setView([39, -98], 4)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap',
    maxZoom: 18,
  }).addTo(m)
  markerLayer.value = L.layerGroup().addTo(m)
  map.value = m
  placeMarkers()
})

watch(() => props.strikes, placeMarkers, { deep: true })
</script>

<style scoped>
.lightning-map {
  height: 350px;
  border-radius: 8px;
  overflow: hidden;
}

h2 {
  margin: 0 0 10px 0;
  font-size: 1.1rem;
}
</style>
