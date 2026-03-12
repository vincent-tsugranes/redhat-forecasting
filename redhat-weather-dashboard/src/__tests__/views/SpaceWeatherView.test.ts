import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import i18n from '../../i18n'
import SpaceWeatherView from '../../views/SpaceWeatherView.vue'

// Mock the weather service
vi.mock('../../services/weatherService', () => ({
  default: {
    getSpaceWeather: vi.fn().mockResolvedValue({
      kpIndex: 3,
      kpLevel: 'Unsettled',
      solarWindSpeed: 400,
      geomagneticStormLevel: 'G0',
      auroraChance: '15%',
      alerts: [],
      fetchedAt: new Date().toISOString(),
    }),
    getAirports: vi.fn().mockResolvedValue([]),
    getRecentEarthquakes: vi.fn().mockResolvedValue([]),
    getActiveStorms: vi.fn().mockResolvedValue([]),
    getActiveAlerts: vi.fn().mockResolvedValue([]),
  },
}))

vi.mock('../../services/api', () => ({
  default: { get: vi.fn().mockResolvedValue({ data: {} }) },
  weatherApi: { get: vi.fn().mockResolvedValue({ data: {} }) },
}))

// Mock vue-router
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ path: '/space-weather', query: {} }),
}))

// Stub FreshnessBadge to avoid DataType "space" not being defined
const FreshnessBadgeStub = defineComponent({
  name: 'FreshnessBadge',
  props: ['fetchedAt', 'dataType'],
  render() { return h('span', 'fresh') },
})

describe('SpaceWeatherView', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders space weather title', async () => {
    const wrapper = mount(SpaceWeatherView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, KpIndexGauge: true, SpaceWeatherSkeleton: true },
      },
    })
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('Space Weather')
  })

  it('renders refresh button', async () => {
    const wrapper = mount(SpaceWeatherView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, KpIndexGauge: true, SpaceWeatherSkeleton: true },
      },
    })
    await flushPromises()
    const btn = wrapper.find('button')
    expect(btn.exists()).toBe(true)
  })

  it('displays space weather data when loaded', async () => {
    const wrapper = mount(SpaceWeatherView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, KpIndexGauge: true, SpaceWeatherSkeleton: true },
      },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('Kp Index')
  })
})
