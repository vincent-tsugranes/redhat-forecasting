import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import i18n from '../../i18n'
import EarthquakeView from '../../views/EarthquakeView.vue'

// Mock the weather service
vi.mock('../../services/weatherService', () => ({
  default: {
    getLocations: vi.fn().mockResolvedValue([]),
    getAirports: vi.fn().mockResolvedValue([]),
    getRecentEarthquakes: vi.fn().mockResolvedValue([]),
    getSignificantEarthquakes: vi.fn().mockResolvedValue([]),
    refreshEarthquakes: vi.fn().mockResolvedValue(undefined),
    getActiveStorms: vi.fn().mockResolvedValue([]),
    getActiveAlerts: vi.fn().mockResolvedValue([]),
    getForecastsByLocation: vi.fn().mockResolvedValue([]),
    refreshHurricaneData: vi.fn().mockResolvedValue(undefined),
  },
}))

vi.mock('../../services/api', () => ({
  default: { get: vi.fn().mockResolvedValue({ data: {} }) },
  weatherApi: { get: vi.fn().mockResolvedValue({ data: [] }) },
}))

// Mock vue-router
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ path: '/earthquakes', query: {} }),
}))

describe('EarthquakeView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders earthquake title', async () => {
    const wrapper = mount(EarthquakeView, {
      global: { plugins: [i18n] },
    })
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('Earthquake Monitor')
  })

  it('renders refresh button', async () => {
    const wrapper = mount(EarthquakeView, {
      global: { plugins: [i18n] },
    })
    await flushPromises()
    const btn = wrapper.find('button')
    expect(btn.exists()).toBe(true)
    expect(btn.text()).toContain('Refresh Data')
  })

  it('shows empty state when no earthquakes', async () => {
    const wrapper = mount(EarthquakeView, {
      global: { plugins: [i18n] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('No significant earthquakes')
  })
})
