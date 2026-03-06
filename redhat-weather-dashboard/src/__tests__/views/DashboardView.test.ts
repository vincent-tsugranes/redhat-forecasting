import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import i18n from '../../i18n'
import DashboardView from '../../views/DashboardView.vue'

// Mock the weather service
vi.mock('../../services/weatherService', () => ({
  default: {
    getLocations: vi.fn().mockResolvedValue([
      {
        id: 1,
        name: 'New York',
        state: 'NY',
        country: 'US',
        latitude: 40.7,
        longitude: -74.0,
        locationType: 'city',
      },
      {
        id: 2,
        name: 'Los Angeles',
        state: 'CA',
        country: 'US',
        latitude: 34.0,
        longitude: -118.2,
        locationType: 'city',
      },
    ]),
    getAirports: vi.fn().mockResolvedValue([
      { id: 3, name: 'JFK', latitude: 40.6, longitude: -73.7, locationType: 'airport', airportCode: 'KJFK' },
    ]),
    getActiveAlerts: vi.fn().mockResolvedValue([]),
    getRecentEarthquakes: vi.fn().mockResolvedValue([
      { id: 10, usgsId: 'us1234', magnitude: 4.5, place: 'California', eventTime: '2024-01-15T10:00:00', latitude: 34.0, longitude: -118.0, depthKm: 10 },
    ]),
    getActiveStorms: vi.fn().mockResolvedValue([]),
  },
}))

vi.mock('../../services/api', () => ({
  default: { get: vi.fn().mockResolvedValue({ data: {} }) },
  weatherApi: { get: vi.fn().mockResolvedValue({ data: [] }) },
}))

vi.mock('../../utils/dateUtils', () => ({
  formatDate: vi.fn((d: string) => d),
  formatRelativeTime: vi.fn(() => '5m ago'),
}))

// Mock router-link
const RouterLink = {
  name: 'RouterLink',
  template: '<a><slot /></a>',
  props: ['to'],
}

describe('DashboardView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders section headings with aria-hidden emojis', async () => {
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [i18n],
        stubs: {
          DataStatusCard: true,
          UnifiedMap: true,
          RouterLink,
        },
      },
    })
    await flushPromises()

    const ariaHiddenSpans = wrapper.findAll('[aria-hidden="true"]')
    expect(ariaHiddenSpans.length).toBeGreaterThanOrEqual(4)
  })

  it('refresh button has aria-label', async () => {
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [i18n],
        stubs: {
          DataStatusCard: true,
          UnifiedMap: true,
          RouterLink,
        },
      },
    })
    await flushPromises()

    const refreshBtn = wrapper.find('button[aria-label="Refresh weather data"]')
    expect(refreshBtn.exists()).toBe(true)
  })

  it('renders stats row with live counts', async () => {
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [i18n],
        stubs: {
          DataStatusCard: true,
          UnifiedMap: true,
          RouterLink,
        },
      },
    })
    await flushPromises()

    const statTiles = wrapper.findAll('.stat-tile')
    expect(statTiles.length).toBe(5)
    // Should show location count, airport count, etc.
    expect(wrapper.text()).toContain('Locations')
    expect(wrapper.text()).toContain('Airports')
    expect(wrapper.text()).toContain('Active Storms')
    expect(wrapper.text()).toContain('Earthquakes')
    expect(wrapper.text()).toContain('Active Alerts')
  })

  it('renders earthquake table with data', async () => {
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [i18n],
        stubs: {
          DataStatusCard: true,
          UnifiedMap: true,
          RouterLink,
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Earthquake Monitor')
    expect(wrapper.text()).toContain('California')
    expect(wrapper.find('.magnitude-badge').exists()).toBe(true)
  })

  it('renders quick link cards for forecasts, airports, space weather', async () => {
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [i18n],
        stubs: {
          DataStatusCard: true,
          UnifiedMap: true,
          RouterLink,
        },
      },
    })
    await flushPromises()

    const quickLinks = wrapper.findAll('.quick-link')
    expect(quickLinks.length).toBe(3)
    expect(wrapper.text()).toContain('Weather Forecasts')
    expect(wrapper.text()).toContain('Airport Weather')
    expect(wrapper.text()).toContain('Space Weather')
  })

  it('renders unified map instead of airport map', async () => {
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [i18n],
        stubs: {
          DataStatusCard: true,
          UnifiedMap: true,
          RouterLink,
        },
      },
    })
    await flushPromises()

    expect(wrapper.findComponent({ name: 'UnifiedMap' }).exists()).toBe(true)
  })
})
