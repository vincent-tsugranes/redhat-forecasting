import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import i18n from '../../i18n'
import ForecastView from '../../views/ForecastView.vue'
import { useWeatherStore } from '../../stores/weatherStore'

// Mock the weather service
vi.mock('../../services/weatherService', () => ({
  default: {
    getAirports: vi.fn().mockResolvedValue([
      { id: 1, name: 'New York', state: 'NY', country: 'US', latitude: 40.7, longitude: -74.0, locationType: 'airport', airportCode: 'KJFK' },
    ]),
    getActiveAlerts: vi.fn().mockResolvedValue([]),
    getForecastsByLocation: vi.fn().mockResolvedValue([
      {
        id: 100,
        source: 'noaa',
        forecastTime: '2024-01-15T10:00:00',
        validFrom: '2024-01-15T10:00:00',
        validTo: '2024-01-15T16:00:00',
        latitude: 40.7,
        longitude: -74.0,
        temperatureFahrenheit: 35,
        temperatureCelsius: 1.7,
        windSpeedMph: 10,
        weatherDescription: 'Partly Cloudy',
      },
      {
        id: 101,
        source: 'noaa',
        forecastTime: '2024-01-15T16:00:00',
        validFrom: '2024-01-15T16:00:00',
        validTo: '2024-01-15T22:00:00',
        latitude: 40.7,
        longitude: -74.0,
        temperatureFahrenheit: 28,
        temperatureCelsius: -2.2,
        windSpeedMph: 15,
        weatherDescription: 'Snow',
      },
    ]),
    getHistoricalForecasts: vi.fn().mockResolvedValue([]),
    getSolarData: vi.fn().mockResolvedValue(null),
    getClimateNormals: vi.fn().mockResolvedValue(null),
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

  // --- Forecast data rendering tests ---

  const fullStubs = {
    AlertsBanner: true,
    FreshnessBadge: true,
    ForecastChart: true,
    ForecastSkeleton: true,
    FavoriteButton: true,
    DailyForecastCards: true,
    HourlyTimeline: true,
    HistoricalChart: true,
    SolarPanel: true,
    ClimateNormalsCard: true,
    WindRoseChart: true,
    ErrorBoundary: true,
  }

  it('renders forecast data when location is selected', async () => {
    const wrapper = mount(ForecastView, {
      global: {
        plugins: [i18n],
        stubs: fullStubs,
      },
    })
    await flushPromises()

    // Fetch locations then select one
    const store = useWeatherStore()
    await store.fetchAirports()
    await store.fetchForecasts(1)
    await flushPromises()

    // The component should show the forecast period count
    expect(wrapper.text()).toContain('2')
  })

  it('renders forecast period count text', async () => {
    const wrapper = mount(ForecastView, {
      global: {
        plugins: [i18n],
        stubs: fullStubs,
      },
    })
    await flushPromises()

    const store = useWeatherStore()
    await store.fetchAirports()
    await store.fetchForecasts(1)
    await flushPromises()

    // Verify the period count is shown in the table header
    const text = wrapper.text()
    expect(text).toContain('periods')
  })
})
