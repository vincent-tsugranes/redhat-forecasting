import { vi } from 'vitest'

// Mock EventSource (not available in jsdom)
class MockEventSource {
  static readonly CONNECTING = 0
  static readonly OPEN = 1
  static readonly CLOSED = 2
  readonly CONNECTING = 0
  readonly OPEN = 1
  readonly CLOSED = 2
  readyState = MockEventSource.OPEN
  url: string
  onopen: ((ev: Event) => void) | null = null
  onmessage: ((ev: MessageEvent) => void) | null = null
  onerror: ((ev: Event) => void) | null = null
  private listeners: Record<string, Array<(ev: MessageEvent) => void>> = {}
  constructor(url: string) { this.url = url }
  addEventListener(type: string, listener: (ev: MessageEvent) => void) { (this.listeners[type] ??= []).push(listener) }
  removeEventListener(type: string, listener: (ev: MessageEvent) => void) { this.listeners[type] = (this.listeners[type] ?? []).filter(l => l !== listener) }
  close() { this.readyState = MockEventSource.CLOSED }
  dispatchEvent(_event: Event) { return true }
}
Object.defineProperty(globalThis, 'EventSource', { value: MockEventSource, writable: true })

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
      tileLayer: Object.assign(vi.fn(() => mockTileLayer), {
        wms: vi.fn(() => ({ ...mockTileLayer, setOpacity: vi.fn() })),
      }),
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
