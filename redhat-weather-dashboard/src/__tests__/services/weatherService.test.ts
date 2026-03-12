import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock the api module at the HTTP level
const mockGet = vi.fn()
const mockPost = vi.fn()
vi.mock('../../services/api', () => ({
  weatherApi: {
    get: (...args: unknown[]) => mockGet(...args),
    post: (...args: unknown[]) => mockPost(...args),
  },
  default: {
    get: vi.fn(),
  },
}))

// Import AFTER mocking
import weatherService from '../../services/weatherService'

describe('weatherService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getAirports', () => {
    it('unwraps a paginated response into a plain array', async () => {
      const airports = [
        { id: 1, name: 'JFK', latitude: 40.6, longitude: -73.7, locationType: 'airport', airportCode: 'KJFK' },
        { id: 2, name: 'LAX', latitude: 33.9, longitude: -118.4, locationType: 'airport', airportCode: 'KLAX' },
      ]
      mockGet.mockResolvedValue({
        data: { data: airports, page: 0, size: 10000, totalElements: 2, totalPages: 1 },
      })

      const result = await weatherService.getAirports()
      expect(Array.isArray(result)).toBe(true)
      expect(result).toHaveLength(2)
      expect(result[0].airportCode).toBe('KJFK')
    })

    it('handles a raw array response (backwards compat)', async () => {
      const airports = [
        { id: 1, name: 'JFK', latitude: 40.6, longitude: -73.7, locationType: 'airport', airportCode: 'KJFK' },
      ]
      mockGet.mockResolvedValue({ data: airports })

      const result = await weatherService.getAirports()
      expect(Array.isArray(result)).toBe(true)
      expect(result).toHaveLength(1)
    })

    it('returns [] when response has empty data array', async () => {
      mockGet.mockResolvedValue({
        data: { data: [], page: 0, size: 10000, totalElements: 0, totalPages: 0 },
      })

      const result = await weatherService.getAirports()
      expect(Array.isArray(result)).toBe(true)
      expect(result).toHaveLength(0)
    })
  })

  describe('getForecastsByLocation', () => {
    it('unwraps paginated forecast response', async () => {
      const forecasts = [
        {
          id: 10,
          source: 'noaa',
          forecastTime: '2024-01-01T00:00:00',
          validFrom: '2024-01-01T00:00:00',
          validTo: '2024-01-01T06:00:00',
          latitude: 40.7,
          longitude: -74.0,
          temperatureFahrenheit: 32,
          temperatureCelsius: 0,
          windSpeedMph: 5,
          weatherDescription: 'Clear',
        },
      ]
      mockGet.mockResolvedValue({
        data: { data: forecasts, page: 0, size: 500, totalElements: 1, totalPages: 1 },
      })

      const result = await weatherService.getForecastsByLocation(1)
      expect(Array.isArray(result)).toBe(true)
      expect(result).toHaveLength(1)
    })

    it('handles raw array response', async () => {
      mockGet.mockResolvedValue({ data: [] })
      const result = await weatherService.getForecastsByLocation(1)
      expect(result).toEqual([])
    })
  })

  describe('getForecastsByCoordinates', () => {
    it('unwraps paginated coordinate forecast response', async () => {
      mockGet.mockResolvedValue({
        data: { data: [{ id: 1 }], page: 0, size: 500, totalElements: 1, totalPages: 1 },
      })

      const result = await weatherService.getForecastsByCoordinates(40.7, -74.0)
      expect(Array.isArray(result)).toBe(true)
      expect(result).toHaveLength(1)
    })

    it('handles raw array response', async () => {
      mockGet.mockResolvedValue({ data: [{ id: 1 }] })
      const result = await weatherService.getForecastsByCoordinates(40.7, -74.0)
      expect(Array.isArray(result)).toBe(true)
    })
  })

  describe('non-paginated methods', () => {
    it('getHistoricalForecasts passes through raw response', async () => {
      const rawData = [{ id: 1, source: 'noaa' }]
      mockGet.mockResolvedValue({ data: rawData })

      const result = await weatherService.getHistoricalForecasts(1, 7)
      expect(result).toEqual(rawData)
    })

    it('getCurrentForecast passes through raw response', async () => {
      const rawData = [{ id: 1, source: 'noaa' }]
      mockGet.mockResolvedValue({ data: rawData })

      const result = await weatherService.getCurrentForecast(40.7, -74.0)
      expect(result).toEqual(rawData)
    })

  })
})
