import { weatherApi } from './api'

export interface Location {
  id: number
  name: string
  latitude: number
  longitude: number
  locationType: string
  airportCode?: string
  state?: string
  country?: string
}

export interface WeatherForecast {
  id: number
  source: string
  forecastTime: string
  validFrom: string
  validTo: string
  latitude: number
  longitude: number
  temperatureFahrenheit: number
  temperatureCelsius: number
  precipitationProbability?: number
  windSpeedMph: number
  windDirection?: number
  humidity?: number
  weatherDescription: string
  weatherShortDescription?: string
}

export interface AirportWeather {
  id: number
  airportCode: string
  reportType: string
  rawText: string
  observationTime: string
  latitude: number
  longitude: number
  visibility Miles?: number
  ceilingFeet?: number
  windSpeedKnots?: number
  windDirection?: number
  temperatureCelsius?: number
  dewpointCelsius?: number
  flightCategory?: string
}

export interface Hurricane {
  id: number
  stormId: string
  stormName?: string
  stormNumber?: number
  basin?: string
  advisoryTime: string
  latitude: number
  longitude: number
  category?: number
  maxSustainedWindsMph?: number
  minCentralPressureMb?: number
  status?: string
  classification?: string
}

export const weatherService = {
  // Locations
  async getLocations(): Promise<Location[]> {
    const response = await weatherApi.get('/locations')
    return response.data
  },

  async getLocationById(id: number): Promise<Location> {
    const response = await weatherApi.get(`/locations/${id}`)
    return response.data
  },

  async getAirports(): Promise<Location[]> {
    const response = await weatherApi.get('/locations/airports')
    return response.data
  },

  // Weather Forecasts
  async getForecastsByLocation(locationId: number): Promise<WeatherForecast[]> {
    const response = await weatherApi.get(`/forecasts/location/${locationId}`)
    return response.data
  },

  async getCurrentForecast(lat: number, lon: number): Promise<WeatherForecast[]> {
    const response = await weatherApi.get('/forecasts/current', {
      params: { lat, lon }
    })
    return response.data
  },

  async getForecastsByCoordinates(
    lat: number,
    lon: number,
    from?: string,
    to?: string
  ): Promise<WeatherForecast[]> {
    const response = await weatherApi.get('/forecasts/coordinates', {
      params: { lat, lon, from, to }
    })
    return response.data
  },

  // Airport Weather
  async getAirportWeather(code: string): Promise<AirportWeather[]> {
    const response = await weatherApi.get(`/airports/${code}`)
    return response.data
  },

  async getLatestMetar(code: string): Promise<AirportWeather> {
    const response = await weatherApi.get(`/airports/${code}/metar`)
    return response.data
  },

  async getLatestTaf(code: string): Promise<AirportWeather> {
    const response = await weatherApi.get(`/airports/${code}/taf`)
    return response.data
  },

  async refreshAirportWeather(code: string): Promise<void> {
    await weatherApi.post(`/airports/${code}/refresh`)
  },

  // Hurricanes
  async getActiveStorms(): Promise<Hurricane[]> {
    const response = await weatherApi.get('/hurricanes/active')
    return response.data
  },

  async getStormById(stormId: string): Promise<Hurricane[]> {
    const response = await weatherApi.get(`/hurricanes/${stormId}`)
    return response.data
  },

  async getStormTrack(stormId: string, from?: string, to?: string): Promise<Hurricane[]> {
    const response = await weatherApi.get(`/hurricanes/${stormId}/track`, {
      params: { from, to }
    })
    return response.data
  },

  async refreshHurricaneData(): Promise<void> {
    await weatherApi.post('/hurricanes/refresh')
  }
}

export default weatherService
