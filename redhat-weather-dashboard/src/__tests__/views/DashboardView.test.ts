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
    getAirports: vi.fn().mockResolvedValue([]),
    getActiveAlerts: vi.fn().mockResolvedValue([]),
  },
}))

vi.mock('../../services/api', () => ({
  default: { get: vi.fn().mockResolvedValue({ data: {} }) },
  weatherApi: { get: vi.fn().mockResolvedValue({ data: [] }) },
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
          AirportMap: true,
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
          AirportMap: true,
          RouterLink,
        },
      },
    })
    await flushPromises()

    const refreshBtn = wrapper.find('button[aria-label="Refresh weather data"]')
    expect(refreshBtn.exists()).toBe(true)
  })

  it('renders location list', async () => {
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [i18n],
        stubs: {
          DataStatusCard: true,
          AirportMap: true,
          RouterLink,
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('New York')
    expect(wrapper.text()).toContain('Los Angeles')
  })

  it('renders earthquake card with link', async () => {
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [i18n],
        stubs: {
          DataStatusCard: true,
          AirportMap: true,
          RouterLink,
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Earthquake Monitor')
    expect(wrapper.text()).toContain('View Earthquakes')
  })

  it('renders space weather card with link', async () => {
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [i18n],
        stubs: {
          DataStatusCard: true,
          AirportMap: true,
          RouterLink,
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Space Weather')
    expect(wrapper.text()).toContain('View Space Weather')
  })

  it('renders six dashboard cards', async () => {
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [i18n],
        stubs: {
          DataStatusCard: true,
          AirportMap: true,
          RouterLink,
        },
      },
    })
    await flushPromises()

    const gridCards = wrapper.findAll('.grid > .card')
    expect(gridCards).toHaveLength(6)
  })
})
