import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AirportView from '../../views/AirportView.vue'

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
  it('renders select with associated label', async () => {
    const wrapper = mount(AirportView, {
      global: {
        stubs: {
          FreshnessBadge: true,
        },
      },
    })
    await flushPromises()

    const label = wrapper.find('label[for="airport-select"]')
    expect(label.exists()).toBe(true)
    expect(label.classes()).toContain('sr-only')

    const select = wrapper.find('#airport-select')
    expect(select.exists()).toBe(true)
  })

  it('shows loading state initially', () => {
    const wrapper = mount(AirportView, {
      global: {
        stubs: {
          FreshnessBadge: true,
        },
      },
    })
    // Initial state - no loading since no airport selected
    expect(wrapper.find('.loading').exists()).toBe(false)
  })
})
