import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import i18n from '../../i18n'
import UnifiedMap from '../../components/UnifiedMap.vue'

// Mock the weather service (store depends on it)
vi.mock('../../services/weatherService', () => ({
  default: {
    getAirports: vi.fn().mockResolvedValue([
      { id: 1, name: 'JFK', latitude: 40.6, longitude: -73.7, locationType: 'airport', airportCode: 'KJFK' },
      { id: 2, name: 'LAX', latitude: 33.9, longitude: -118.4, locationType: 'airport', airportCode: 'KLAX' },
    ]),
    getRecentEarthquakes: vi.fn().mockResolvedValue([
      { id: 10, usgsId: 'us1234', magnitude: 4.5, place: 'California', eventTime: '2024-01-15T10:00:00', latitude: 34.0, longitude: -118.0, depthKm: 10 },
    ]),
    getActiveStorms: vi.fn().mockResolvedValue([
      { id: 20, stormId: 'AL012024', stormName: 'Alberto', latitude: 25.0, longitude: -80.0, category: 1, maxSustainedWindsMph: 80, minCentralPressureMb: 985, advisoryTime: '2024-01-15T10:00:00' },
    ]),
  },
}))

// Mock dateUtils (used in popup content)
vi.mock('../../utils/dateUtils', () => ({
  formatDate: vi.fn((d: string) => d),
}))

describe('UnifiedMap', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders layer toggle checkboxes', async () => {
    const wrapper = mount(UnifiedMap, { global: { plugins: [i18n] } })
    await flushPromises()

    const checkboxes = wrapper.findAll('.layer-toggle input[type="checkbox"]')
    expect(checkboxes).toHaveLength(4)
  })

  it('renders legend sections based on active layers', async () => {
    const wrapper = mount(UnifiedMap, { global: { plugins: [i18n] } })
    await flushPromises()

    // By default: showAirports=true, showEarthquakes=true, showHurricanes=true, showRadar=false
    const legendSections = wrapper.findAll('.legend-section')
    expect(legendSections).toHaveLength(3)
  })

  it('renders map container with correct aria attributes', async () => {
    const wrapper = mount(UnifiedMap, { global: { plugins: [i18n] } })
    await flushPromises()

    const mapContainer = wrapper.find('[role="application"]')
    expect(mapContainer.exists()).toBe(true)
    expect(mapContainer.attributes('aria-label')).toBe('Unified weather map')
  })

  it('hides airport legend when airport layer is toggled off', async () => {
    const wrapper = mount(UnifiedMap, { global: { plugins: [i18n] } })
    await flushPromises()

    // Find the airports checkbox and uncheck it
    const airportCheckbox = wrapper.findAll('.layer-toggle input[type="checkbox"]')[0]
    await airportCheckbox.setValue(false)
    await flushPromises()

    // Airport legend section should be gone
    const legendSections = wrapper.findAll('.legend-section')
    expect(legendSections).toHaveLength(2)
  })
})
