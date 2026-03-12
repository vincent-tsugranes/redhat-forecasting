import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, h } from 'vue'
import i18n from '../../i18n'
import VolcanicAshView from '../../views/VolcanicAshView.vue'
import { useWeatherStore } from '../../stores/weatherStore'

vi.mock('../../services/weatherService', () => ({
  default: {
    getActiveVolcanicAsh: vi.fn().mockResolvedValue([]),
    getAirports: vi.fn().mockResolvedValue([]),
    getRecentEarthquakes: vi.fn().mockResolvedValue([]),
    getActiveStorms: vi.fn().mockResolvedValue([]),
    getActiveAlerts: vi.fn().mockResolvedValue([]),
    getActiveGroundStops: vi.fn().mockResolvedValue([]),
    getRecentLightning: vi.fn().mockResolvedValue([]),
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

const mockVolcanicAsh = [
  { id: 1, advisoryId: 'VA1', firId: 'KZAK', firName: 'Oakland', volcanoName: 'Mount St Helens', hazard: 'Volcanic Ash', severity: 'MOD', validTimeFrom: '2024-01-15T10:00:00', validTimeTo: '2024-01-15T22:00:00', altitudeLowFt: 10000, altitudeHighFt: 35000, rawText: 'VA ADVISORY...', fetchedAt: new Date().toISOString() },
]

const FreshnessBadgeStub = defineComponent({
  name: 'FreshnessBadge',
  props: ['fetchedAt', 'dataType'],
  render() { return h('span', 'fresh') },
})

describe('VolcanicAshView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    const store = useWeatherStore()
    store.volcanicAsh = mockVolcanicAsh as never
    store.volcanicAshLoading = false
  })

  it('renders the title', async () => {
    const wrapper = mount(VolcanicAshView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('Volcanic Ash Advisories')
  })

  it('renders advisory cards with volcano name', async () => {
    const wrapper = mount(VolcanicAshView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('Mount St Helens')
    expect(wrapper.find('.va-badge').exists()).toBe(true)
  })

  it('renders stat chips', async () => {
    const wrapper = mount(VolcanicAshView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    const chips = wrapper.findAll('.stat-chip')
    expect(chips.length).toBeGreaterThanOrEqual(1)
  })

  it('filters advisories by volcano name', async () => {
    const wrapper = mount(VolcanicAshView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    const input = wrapper.find('input[type="text"]')
    await input.setValue('nonexistent')
    await flushPromises()
    expect(wrapper.text()).not.toContain('Mount St Helens')
  })

  it('renders refresh button', async () => {
    const wrapper = mount(VolcanicAshView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    const btn = wrapper.find('button.btn-sm')
    expect(btn.exists()).toBe(true)
  })

  it('shows raw text in advisory card', async () => {
    const wrapper = mount(VolcanicAshView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('VA ADVISORY...')
  })
})
