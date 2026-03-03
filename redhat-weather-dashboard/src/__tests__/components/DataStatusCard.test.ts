import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import i18n from '../../i18n'
import DataStatusCard from '../../components/DataStatusCard.vue'

// Mock the API modules
vi.mock('../../services/api', () => ({
  default: {
    get: vi.fn().mockResolvedValue({
      data: {
        totalLocations: 100,
        airports: 50,
        cities: 8,
        airportsLoaded: true,
        expectedAirports: 9313,
        loadingComplete: false,
        percentLoaded: 50,
      },
    }),
  },
  weatherApi: {
    get: vi.fn().mockResolvedValue({ data: [] }),
  },
}))

describe('DataStatusCard', () => {
  it('renders heading with aria-hidden emoji', async () => {
    const wrapper = mount(DataStatusCard, { global: { plugins: [i18n] } })
    await flushPromises()

    const heading = wrapper.find('h3')
    expect(heading.exists()).toBe(true)
    const ariaHidden = wrapper.find('h3 [aria-hidden="true"]')
    expect(ariaHidden.exists()).toBe(true)
  })

  it('progress bar has role="progressbar"', async () => {
    const wrapper = mount(DataStatusCard, { global: { plugins: [i18n] } })
    await flushPromises()

    const progressBar = wrapper.find('[role="progressbar"]')
    expect(progressBar.exists()).toBe(true)
  })

  it('progress bar has aria-valuenow, aria-valuemin, aria-valuemax', async () => {
    const wrapper = mount(DataStatusCard, { global: { plugins: [i18n] } })
    await flushPromises()

    const progressBar = wrapper.find('[role="progressbar"]')
    expect(progressBar.attributes('aria-valuenow')).toBe('50')
    expect(progressBar.attributes('aria-valuemin')).toBe('0')
    expect(progressBar.attributes('aria-valuemax')).toBe('100')
  })

  it('status badge has role="status"', async () => {
    const wrapper = mount(DataStatusCard, { global: { plugins: [i18n] } })
    await flushPromises()

    const badge = wrapper.find('[role="status"]')
    expect(badge.exists()).toBe(true)
  })

  it('status badge emoji has aria-hidden', async () => {
    const wrapper = mount(DataStatusCard, { global: { plugins: [i18n] } })
    await flushPromises()

    const badge = wrapper.find('.status-badge')
    const ariaHidden = badge.find('[aria-hidden="true"]')
    expect(ariaHidden.exists()).toBe(true)
  })
})
