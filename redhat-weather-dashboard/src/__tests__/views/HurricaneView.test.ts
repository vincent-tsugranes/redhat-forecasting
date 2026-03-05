import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import i18n from '../../i18n'
import HurricaneView from '../../views/HurricaneView.vue'

// Mock the weather service
vi.mock('../../services/weatherService', () => ({
  default: {
    getLocations: vi.fn().mockResolvedValue([]),
    getAirports: vi.fn().mockResolvedValue([]),
    getRecentEarthquakes: vi.fn().mockResolvedValue([]),
    getActiveStorms: vi.fn().mockResolvedValue([]),
    getActiveAlerts: vi.fn().mockResolvedValue([]),
    getForecastsByLocation: vi.fn().mockResolvedValue([]),
    getSignificantEarthquakes: vi.fn().mockResolvedValue([]),
    refreshEarthquakes: vi.fn().mockResolvedValue(undefined),
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
  useRoute: () => ({ path: '/hurricanes', query: {} }),
}))

describe('HurricaneView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders hurricane title', async () => {
    const wrapper = mount(HurricaneView, {
      global: { plugins: [i18n] },
    })
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('Hurricane Tracking')
  })

  it('renders refresh button', async () => {
    const wrapper = mount(HurricaneView, {
      global: { plugins: [i18n] },
    })
    await flushPromises()
    const btn = wrapper.find('button')
    expect(btn.exists()).toBe(true)
  })

  it('shows empty state when no storms', async () => {
    const wrapper = mount(HurricaneView, {
      global: { plugins: [i18n] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('No active tropical systems')
  })
})
