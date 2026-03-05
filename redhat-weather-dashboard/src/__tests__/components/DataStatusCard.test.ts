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
        activeForecasts: 1250,
        activeEarthquakes: 3,
        activeHurricanes: 1,
        metarReports: 425,
        dataFreshness: {
          'forecasts.ageMinutes': 5,
          'forecasts.lastFetch': '2024-01-15T10:00:00',
          'metar.ageMinutes': 45,
          'metar.lastFetch': '2024-01-15T09:20:00',
          'earthquakes.ageMinutes': 120,
          'earthquakes.lastFetch': '2024-01-15T08:00:00',
        },
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

  // --- Data rendering tests ---

  it('renders active data count values', async () => {
    const wrapper = mount(DataStatusCard, { global: { plugins: [i18n] } })
    await flushPromises()

    const countValues = wrapper.findAll('.count-value')
    expect(countValues.length).toBe(4)
    const texts = countValues.map((el) => el.text())
    expect(texts).toContain('1,250')
    expect(texts).toContain('3')
    expect(texts).toContain('1')
    expect(texts).toContain('425')
  })

  it('renders freshness badges with correct classes', async () => {
    const wrapper = mount(DataStatusCard, { global: { plugins: [i18n] } })
    await flushPromises()

    // forecasts.ageMinutes=5 -> freshness-fresh
    // metar.ageMinutes=45 -> freshness-aging
    // earthquakes.ageMinutes=120 -> freshness-stale
    expect(wrapper.find('.freshness-fresh').exists()).toBe(true)
    expect(wrapper.find('.freshness-aging').exists()).toBe(true)
    expect(wrapper.find('.freshness-stale').exists()).toBe(true)
  })

  it('renders Active Data section heading', async () => {
    const wrapper = mount(DataStatusCard, { global: { plugins: [i18n] } })
    await flushPromises()

    const h4s = wrapper.findAll('h4')
    const activeDataH4 = h4s.filter((h4) => h4.text() === 'Active Data')
    expect(activeDataH4.length).toBe(1)
  })

  it('renders Data Freshness section with freshness rows', async () => {
    const wrapper = mount(DataStatusCard, { global: { plugins: [i18n] } })
    await flushPromises()

    expect(wrapper.find('.freshness-section').exists()).toBe(true)
    const freshnessRows = wrapper.findAll('.freshness-row')
    expect(freshnessRows.length).toBe(3)
  })
})
