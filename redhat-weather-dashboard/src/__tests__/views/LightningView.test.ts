import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, h } from 'vue'
import i18n from '../../i18n'
import LightningView from '../../views/LightningView.vue'

vi.mock('../../services/weatherService', () => ({
  default: {
    getRecentLightning: vi.fn().mockResolvedValue([
      { id: 1, strikeId: 'LS1', latitude: 34.05, longitude: -118.25, strikeTime: '2024-01-15T10:00:00', amplitudeKa: 25.3, strikeType: 'CG', fetchedAt: new Date().toISOString() },
      { id: 2, strikeId: 'LS2', latitude: 40.71, longitude: -74.01, strikeTime: '2024-01-15T10:05:00', amplitudeKa: 42.1, strikeType: 'IC', fetchedAt: new Date().toISOString() },
    ]),
    getAirports: vi.fn().mockResolvedValue([]),
    getRecentEarthquakes: vi.fn().mockResolvedValue([]),
    getActiveStorms: vi.fn().mockResolvedValue([]),
    getActiveAlerts: vi.fn().mockResolvedValue([]),
    getActiveGroundStops: vi.fn().mockResolvedValue([]),
    getActiveVolcanicAsh: vi.fn().mockResolvedValue([]),
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

const FreshnessBadgeStub = defineComponent({
  name: 'FreshnessBadge',
  props: ['fetchedAt', 'dataType'],
  render() { return h('span', 'fresh') },
})

const LightningMapStub = defineComponent({
  name: 'LightningMap',
  props: ['strikes'],
  render() { return h('div', { class: 'lightning-map-stub' }, 'Map') },
})

describe('LightningView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders the title', async () => {
    const wrapper = mount(LightningView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, LightningMap: LightningMapStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('Lightning Activity')
  })

  it('renders strike table with data', async () => {
    const wrapper = mount(LightningView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, LightningMap: LightningMapStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    expect(wrapper.find('.data-table').exists()).toBe(true)
    expect(wrapper.text()).toContain('25.3 kA')
    expect(wrapper.text()).toContain('CG')
  })

  it('renders stat chip with strike count', async () => {
    const wrapper = mount(LightningView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, LightningMap: LightningMapStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    const chips = wrapper.findAll('.stat-chip')
    expect(chips.length).toBeGreaterThanOrEqual(1)
    expect(wrapper.text()).toContain('2')
  })

  it('has sortable column headers', async () => {
    const wrapper = mount(LightningView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, LightningMap: LightningMapStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    const sortableHeaders = wrapper.findAll('th[role="columnheader"]')
    expect(sortableHeaders.length).toBe(2)
  })

  it('renders lightning map component', async () => {
    const wrapper = mount(LightningView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, LightningMap: LightningMapStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    expect(wrapper.find('.lightning-map-stub').exists()).toBe(true)
  })

  it('renders refresh button', async () => {
    const wrapper = mount(LightningView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, LightningMap: LightningMapStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    const btn = wrapper.find('button.btn-sm')
    expect(btn.exists()).toBe(true)
  })
})
