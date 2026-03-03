import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import i18n from '../../i18n'
import ForecastView from '../../views/ForecastView.vue'

// Mock the weather service
vi.mock('../../services/weatherService', () => ({
  default: {
    getLocations: vi.fn().mockResolvedValue([]),
    getActiveAlerts: vi.fn().mockResolvedValue([]),
    getForecastsByLocation: vi.fn().mockResolvedValue([]),
  },
}))

// Mock the router
vi.mock('vue-router', () => ({
  useRouter: vi.fn(() => ({ push: vi.fn() })),
  useRoute: vi.fn(() => ({ params: {}, query: {} })),
}))

describe('ForecastView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders search input with associated label', async () => {
    const wrapper = mount(ForecastView, {
      global: {
        plugins: [i18n],
        stubs: {
          AlertsBanner: true,
          FreshnessBadge: true,
          ForecastChart: true,
        },
      },
    })
    await flushPromises()

    const label = wrapper.find('label[for="location-search"]')
    expect(label.exists()).toBe(true)
    expect(label.classes()).toContain('sr-only')

    const input = wrapper.find('#location-search')
    expect(input.exists()).toBe(true)
  })

  it('search input has combobox role', async () => {
    const wrapper = mount(ForecastView, {
      global: {
        plugins: [i18n],
        stubs: {
          AlertsBanner: true,
          FreshnessBadge: true,
          ForecastChart: true,
        },
      },
    })
    await flushPromises()

    const input = wrapper.find('#location-search')
    expect(input.attributes('role')).toBe('combobox')
    expect(input.attributes('aria-autocomplete')).toBe('list')
    expect(input.attributes('aria-controls')).toBe('search-listbox')
  })

  it('search input has aria-expanded false when no results', async () => {
    const wrapper = mount(ForecastView, {
      global: {
        plugins: [i18n],
        stubs: {
          AlertsBanner: true,
          FreshnessBadge: true,
          ForecastChart: true,
        },
      },
    })
    await flushPromises()

    const input = wrapper.find('#location-search')
    expect(input.attributes('aria-expanded')).toBe('false')
  })
})
