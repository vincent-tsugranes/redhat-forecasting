import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import AlertsBanner from '../../components/AlertsBanner.vue'

// Mock the weather service
vi.mock('../../services/weatherService', () => ({
  default: {
    getActiveAlerts: vi.fn().mockResolvedValue([]),
  },
}))

describe('AlertsBanner', () => {
  it('renders nothing when no alerts', async () => {
    const wrapper = mount(AlertsBanner)
    await flushPromises()
    expect(wrapper.find('.alerts-banner').exists()).toBe(false)
  })

  it('uses button element for clickable header (a11y)', async () => {
    const { default: weatherService } = await import('../../services/weatherService')
    vi.mocked(weatherService.getActiveAlerts).mockResolvedValueOnce([
      {
        id: 1,
        alertId: 'test-1',
        event: 'Flood Warning',
        severity: 'Severe',
      },
    ])

    const wrapper = mount(AlertsBanner)
    await flushPromises()

    const header = wrapper.find('.alerts-header')
    expect(header.element.tagName).toBe('BUTTON')
  })

  it('has aria-expanded attribute on header', async () => {
    const { default: weatherService } = await import('../../services/weatherService')
    vi.mocked(weatherService.getActiveAlerts).mockResolvedValueOnce([
      {
        id: 1,
        alertId: 'test-1',
        event: 'Flood Warning',
        severity: 'Severe',
      },
    ])

    const wrapper = mount(AlertsBanner)
    await flushPromises()

    const header = wrapper.find('.alerts-header')
    expect(header.attributes('aria-expanded')).toBe('false')
  })

  it('severity icon has aria-hidden', async () => {
    const { default: weatherService } = await import('../../services/weatherService')
    vi.mocked(weatherService.getActiveAlerts).mockResolvedValueOnce([
      {
        id: 1,
        alertId: 'test-1',
        event: 'Flood Warning',
        severity: 'Severe',
      },
    ])

    const wrapper = mount(AlertsBanner)
    await flushPromises()

    const icon = wrapper.find('.alert-icon')
    expect(icon.attributes('aria-hidden')).toBe('true')
  })
})
