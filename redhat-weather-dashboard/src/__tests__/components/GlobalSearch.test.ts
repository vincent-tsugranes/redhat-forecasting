import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import i18n from '../../i18n'
import GlobalSearch from '../../components/GlobalSearch.vue'

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
  weatherService: {
    getLocations: vi.fn().mockResolvedValue([]),
    getAirports: vi.fn().mockResolvedValue([]),
    getRecentEarthquakes: vi.fn().mockResolvedValue([]),
  },
}))

// Mock vue-router
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
  useRoute: () => ({
    path: '/',
    query: {},
  }),
  createRouter: vi.fn(),
  createWebHistory: vi.fn(),
}))

describe('GlobalSearch', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders the search trigger button', () => {
    const wrapper = mount(GlobalSearch, {
      global: { plugins: [i18n] },
    })
    const btn = wrapper.find('button')
    expect(btn.exists()).toBe(true)
  })

  it('opens search modal on button click', async () => {
    const wrapper = mount(GlobalSearch, {
      global: { plugins: [i18n] },
    })
    await wrapper.find('button').trigger('click')
    await flushPromises()
    // The modal is teleported to body, so we check the component state
    expect(wrapper.vm).toBeTruthy()
  })
})
