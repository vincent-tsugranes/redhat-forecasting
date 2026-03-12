import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, h } from 'vue'
import i18n from '../../i18n'
import GroundStopView from '../../views/GroundStopView.vue'

vi.mock('../../services/weatherService', () => ({
  default: {
    getActiveGroundStops: vi.fn().mockResolvedValue([
      { id: 1, groundStopId: 'GS1', airportCode: 'KJFK', airportName: 'John F Kennedy', programType: 'Ground Stop', reason: 'Weather', avgDelayMinutes: 45, maxDelayMinutes: 90, fetchedAt: new Date().toISOString() },
      { id: 2, groundStopId: 'GDP1', airportCode: 'KEWR', airportName: 'Newark', programType: 'GDP', reason: 'Volume', avgDelayMinutes: 20, maxDelayMinutes: 40, fetchedAt: new Date().toISOString() },
    ]),
    getAirports: vi.fn().mockResolvedValue([]),
    getRecentEarthquakes: vi.fn().mockResolvedValue([]),
    getActiveStorms: vi.fn().mockResolvedValue([]),
    getActiveAlerts: vi.fn().mockResolvedValue([]),
    getActiveVolcanicAsh: vi.fn().mockResolvedValue([]),
    getRecentLightning: vi.fn().mockResolvedValue([]),
  },
}))

vi.mock('../../services/api', () => ({
  default: { get: vi.fn().mockResolvedValue({ data: {} }) },
  weatherApi: { get: vi.fn().mockResolvedValue({ data: [] }) },
}))

const FreshnessBadgeStub = defineComponent({
  name: 'FreshnessBadge',
  props: ['fetchedAt', 'dataType'],
  render() { return h('span', 'fresh') },
})

describe('GroundStopView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders the title', async () => {
    const wrapper = mount(GroundStopView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    expect(wrapper.find('h1').text()).toBe('Ground Stops & GDPs')
  })

  it('renders ground stop table with data', async () => {
    const wrapper = mount(GroundStopView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('KJFK')
    expect(wrapper.text()).toContain('KEWR')
    expect(wrapper.find('.data-table').exists()).toBe(true)
  })

  it('renders stat chips with counts', async () => {
    const wrapper = mount(GroundStopView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    const chips = wrapper.findAll('.stat-chip')
    expect(chips.length).toBeGreaterThanOrEqual(1)
  })

  it('has sortable column headers', async () => {
    const wrapper = mount(GroundStopView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    const sortableHeaders = wrapper.findAll('th[role="columnheader"]')
    expect(sortableHeaders.length).toBe(3)
  })

  it('filters by airport code', async () => {
    const wrapper = mount(GroundStopView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    const input = wrapper.find('input[type="text"]')
    await input.setValue('JFK')
    await flushPromises()
    expect(wrapper.text()).toContain('KJFK')
    expect(wrapper.text()).not.toContain('KEWR')
  })

  it('renders refresh button', async () => {
    const wrapper = mount(GroundStopView, {
      global: {
        plugins: [i18n],
        stubs: { FreshnessBadge: FreshnessBadgeStub, TableSkeleton: true, ErrorBoundary: { template: '<div><slot /></div>' } },
      },
    })
    await flushPromises()
    const btn = wrapper.find('button.btn-sm')
    expect(btn.exists()).toBe(true)
  })
})
