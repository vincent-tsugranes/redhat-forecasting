import { vi } from 'vitest'

// Mock leaflet
vi.mock('leaflet', () => {
  const mockMap = {
    setView: vi.fn().mockReturnThis(),
    addLayer: vi.fn().mockReturnThis(),
    removeLayer: vi.fn().mockReturnThis(),
    fitBounds: vi.fn().mockReturnThis(),
    remove: vi.fn(),
  }
  const mockTileLayer = {
    addTo: vi.fn().mockReturnThis(),
  }
  const mockMarker = {
    bindPopup: vi.fn().mockReturnThis(),
    bindTooltip: vi.fn().mockReturnThis(),
    on: vi.fn().mockReturnThis(),
    addTo: vi.fn().mockReturnThis(),
    getLatLng: vi.fn(() => ({ lat: 0, lng: 0 })),
    openPopup: vi.fn(),
  }
  return {
    default: {
      map: vi.fn(() => mockMap),
      tileLayer: vi.fn(() => mockTileLayer),
      marker: vi.fn(() => mockMarker),
      divIcon: vi.fn(() => ({})),
      popup: vi.fn(() => ({ setContent: vi.fn().mockReturnThis(), getElement: vi.fn() })),
      latLngBounds: vi.fn(() => ({ pad: vi.fn().mockReturnThis() })),
      markerClusterGroup: vi.fn(() => ({
        clearLayers: vi.fn(),
        addLayers: vi.fn(),
        addLayer: vi.fn(),
        eachLayer: vi.fn(),
      })),
      layerGroup: vi.fn(() => ({
        addTo: vi.fn().mockReturnThis(),
        clearLayers: vi.fn(),
        addLayer: vi.fn(),
      })),
      polyline: vi.fn(() => ({ addTo: vi.fn().mockReturnThis() })),
      circleMarker: vi.fn(() => ({
        bindPopup: vi.fn().mockReturnThis(),
        addTo: vi.fn().mockReturnThis(),
        on: vi.fn().mockReturnThis(),
        setStyle: vi.fn().mockReturnThis(),
      })),
    },
  }
})

vi.mock('leaflet/dist/leaflet.css', () => ({}))
vi.mock('leaflet.markercluster/dist/MarkerCluster.css', () => ({}))
vi.mock('leaflet.markercluster/dist/MarkerCluster.Default.css', () => ({}))
vi.mock('leaflet.markercluster', () => ({}))

// Mock chart.js
vi.mock('chart.js', () => ({
  Chart: { register: vi.fn() },
  CategoryScale: vi.fn(),
  LinearScale: vi.fn(),
  PointElement: vi.fn(),
  LineElement: vi.fn(),
  Title: vi.fn(),
  Tooltip: vi.fn(),
  Legend: vi.fn(),
  Filler: vi.fn(),
}))

vi.mock('vue-chartjs', () => ({
  Line: {
    name: 'Line',
    template: '<canvas></canvas>',
    props: ['data', 'options'],
  },
}))
