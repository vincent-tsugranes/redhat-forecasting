import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import i18n from '../../i18n'
import AirportView from '../../views/AirportView.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
  useRoute: () => ({ path: '/airports', query: {} }),
}))

// Mock the weather service
vi.mock('../../services/weatherService', () => ({
  default: {
    getAirports: vi.fn().mockResolvedValue([]),
    getLatestMetar: vi.fn().mockResolvedValue(null),
    getLatestTaf: vi.fn().mockResolvedValue(null),
    refreshAirportWeather: vi.fn().mockResolvedValue(undefined),
  },
}))

describe('AirportView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders search input with associated label', async () => {
    const wrapper = mount(AirportView, {
      global: {
        plugins: [i18n],
        stubs: {
          FreshnessBadge: true,
          AirportSkeleton: true,
        },
      },
    })
    await flushPromises()

    const label = wrapper.find('label[for="airport-search"]')
    expect(label.exists()).toBe(true)
    expect(label.classes()).toContain('sr-only')

    const input = wrapper.find('#airport-search')
    expect(input.exists()).toBe(true)
    expect(input.attributes('role')).toBe('combobox')
  })

  it('shows no loading state initially', () => {
    const wrapper = mount(AirportView, {
      global: {
        plugins: [i18n],
        stubs: {
          FreshnessBadge: true,
          AirportSkeleton: true,
        },
      },
    })
    // Initial state - no loading since no airport selected
    expect(wrapper.find('.loading').exists()).toBe(false)
  })
})
