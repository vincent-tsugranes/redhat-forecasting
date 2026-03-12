import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import i18n from '../../i18n'
import EarthquakeView from '../../views/EarthquakeView.vue'

// Mock the weather service
vi.mock('../../services/weatherService', () => ({
  default: {
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

  it('renders refresh button and view mode toggles', async () => {
    const wrapper = mount(EarthquakeView, {
      global: { plugins: [i18n] },
    })
    await flushPromises()
    const buttons = wrapper.findAll('button')
    expect(buttons.length).toBeGreaterThanOrEqual(3)
    const texts = buttons.map(b => b.text())
    expect(texts.some(t => t.includes('Refresh Data'))).toBe(true)
    expect(texts.some(t => t.includes('Table'))).toBe(true)
    expect(texts.some(t => t.includes('Cards'))).toBe(true)
  })

  it('shows empty state when no earthquakes', async () => {
    const wrapper = mount(EarthquakeView, {
      global: { plugins: [i18n] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('No significant earthquakes')
  })
})
