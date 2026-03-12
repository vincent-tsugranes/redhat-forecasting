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
    expect(checkboxes).toHaveLength(8)
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

  it('does not show radar controls when radar is off', async () => {
    const wrapper = mount(UnifiedMap, { global: { plugins: [i18n] } })
    await flushPromises()

    // Radar is off by default
    expect(wrapper.find('.radar-controls').exists()).toBe(false)
  })

  it('shows radar controls when radar is toggled on', async () => {
    const wrapper = mount(UnifiedMap, { global: { plugins: [i18n] } })
    await flushPromises()

    // Toggle radar on (4th checkbox)
    const radarCheckbox = wrapper.findAll('.layer-toggle input[type="checkbox"]')[7]
    await radarCheckbox.setValue(true)
    await flushPromises()

    expect(wrapper.find('.radar-controls').exists()).toBe(true)
  })

  it('renders radar product selector with four options', async () => {
    const wrapper = mount(UnifiedMap, { global: { plugins: [i18n] } })
    await flushPromises()

    // Toggle radar on
    const radarCheckbox = wrapper.findAll('.layer-toggle input[type="checkbox"]')[7]
    await radarCheckbox.setValue(true)
    await flushPromises()

    const select = wrapper.find('.radar-select')
    expect(select.exists()).toBe(true)
    expect(select.attributes('aria-label')).toBe('Radar Product')

    const options = select.findAll('option')
    expect(options).toHaveLength(4)
  })

  it('renders radar opacity slider', async () => {
    const wrapper = mount(UnifiedMap, { global: { plugins: [i18n] } })
    await flushPromises()

    // Toggle radar on
    const radarCheckbox = wrapper.findAll('.layer-toggle input[type="checkbox"]')[7]
    await radarCheckbox.setValue(true)
    await flushPromises()

    const slider = wrapper.find('.opacity-slider')
    expect(slider.exists()).toBe(true)
    expect(slider.attributes('type')).toBe('range')
    expect(slider.attributes('min')).toBe('10')
    expect(slider.attributes('max')).toBe('90')
  })

  it('displays opacity percentage value', async () => {
    const wrapper = mount(UnifiedMap, { global: { plugins: [i18n] } })
    await flushPromises()

    // Toggle radar on
    const radarCheckbox = wrapper.findAll('.layer-toggle input[type="checkbox"]')[7]
    await radarCheckbox.setValue(true)
    await flushPromises()

    const opacityValue = wrapper.find('.opacity-value')
    expect(opacityValue.exists()).toBe(true)
    expect(opacityValue.text()).toBe('50%')
  })

  it('renders search bar with correct placeholder', async () => {
    const wrapper = mount(UnifiedMap, { global: { plugins: [i18n] } })
    await flushPromises()

    const searchInput = wrapper.find('#unified-map-search')
    expect(searchInput.exists()).toBe(true)
    expect(searchInput.attributes('placeholder')).toBe('Search airports, earthquakes, storms, TFRs...')
  })

  it('shows search results when typing a query', async () => {
    const wrapper = mount(UnifiedMap, { global: { plugins: [i18n] } })
    await flushPromises()

    const searchInput = wrapper.find('#unified-map-search')
    await searchInput.setValue('JFK')
    await searchInput.trigger('input')
    await flushPromises()

    const results = wrapper.findAll('.search-result-item')
    expect(results.length).toBeGreaterThan(0)
    expect(results[0].find('.result-title').text()).toContain('JFK')
  })

  it('shows no results for short queries', async () => {
    const wrapper = mount(UnifiedMap, { global: { plugins: [i18n] } })
    await flushPromises()

    const searchInput = wrapper.find('#unified-map-search')
    await searchInput.setValue('J')
    await searchInput.trigger('input')
    await flushPromises()

    const results = wrapper.findAll('.search-result-item')
    expect(results.length).toBe(0)
  })

  it('searches earthquakes by place name', async () => {
    const wrapper = mount(UnifiedMap, { global: { plugins: [i18n] } })
    await flushPromises()

    const searchInput = wrapper.find('#unified-map-search')
    await searchInput.setValue('California')
    await searchInput.trigger('input')
    await flushPromises()

    const results = wrapper.findAll('.search-result-item')
    expect(results.length).toBeGreaterThan(0)
    expect(results[0].find('.result-title').text()).toContain('California')
  })

  it('searches hurricanes by storm name', async () => {
    const wrapper = mount(UnifiedMap, { global: { plugins: [i18n] } })
    await flushPromises()

    const searchInput = wrapper.find('#unified-map-search')
    await searchInput.setValue('Alberto')
    await searchInput.trigger('input')
    await flushPromises()

    const results = wrapper.findAll('.search-result-item')
    expect(results.length).toBeGreaterThan(0)
    expect(results[0].find('.result-title').text()).toContain('Alberto')
  })
})
