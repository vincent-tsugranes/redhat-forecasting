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
  fetchedAt?: string
}

export interface AirportWeather {
  id: number
  airportCode: string
  reportType: string
  rawText: string
  observationTime: string
  latitude: number
  longitude: number
  visibilityMiles?: number
  ceilingFeet?: number
  windSpeedKnots?: number
  windDirection?: number
  temperatureCelsius?: number
  dewpointCelsius?: number
  windGustKnots?: number
  altimeterInches?: number
  skyCondition?: string
  weatherConditions?: string
  flightCategory?: string
  fetchedAt?: string
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
  movementDirection?: number
  movementSpeedMph?: number
  maxSustainedWindsKnots?: number
  intensity?: string
  fetchedAt?: string
}

export interface SolarData {
  locationId: number
  locationName: string
  sunrise: string
  sunset: string
  dayLengthSeconds: number
  dayLengthFormatted: string
  fetchedAt: string
}

export interface Earthquake {
  id: number
  usgsId: string
  magnitude: number
  place: string
  eventTime: string
  latitude: number
  longitude: number
  depthKm: number
  magnitudeType?: string
  status?: string
  tsunami?: boolean
  felt?: number
  cdi?: number
  alert?: string
  significance?: number
  fetchedAt?: string
}

export interface SpaceWeather {
  kpIndex: number
  kpLevel: string
  solarWindSpeed?: number
  geomagneticStormLevel?: string
  auroraChance?: string
  alerts: SpaceWeatherAlert[]
  fetchedAt: string
}

export interface SpaceWeatherAlert {
  issueTime: string
  message: string
}

export interface ClimateNormals {
  locationId: number
  locationName: string
  month: number
  avgHighF: number
  avgLowF: number
  avgHighC: number
  avgLowC: number
  avgPrecipProbability: number
  avgHumidity: number
  avgWindSpeedMph: number
  sampleCount: number
  fetchedAt: string
}

export interface WeatherAlert {
  id: number
  alertId: string
  event: string
  headline?: string
  description?: string
  severity?: string
  certainty?: string
  urgency?: string
  areaDesc?: string
  effective?: string
  expires?: string
  senderName?: string
  fetchedAt?: string
}

export const weatherService = {
  // Locations
  async getLocations(): Promise<Location[]> {
    const response = await weatherApi.get('/locations', { params: { size: 500 } })
    const body = response.data
    return Array.isArray(body) ? body : body.data ?? []
  },

  async getLocationById(id: number): Promise<Location> {
    const response = await weatherApi.get(`/locations/${id}`)
    return response.data
  },

  async getAirports(): Promise<Location[]> {
    const response = await weatherApi.get('/locations/airports', { params: { size: 10000 } })
    const body = response.data
    return Array.isArray(body) ? body : body.data ?? []
  },

  // Weather Forecasts
  async getForecastsByLocation(locationId: number): Promise<WeatherForecast[]> {
    const response = await weatherApi.get(`/forecasts/location/${locationId}`, { params: { size: 500 } })
    const body = response.data
    return Array.isArray(body) ? body : body.data ?? []
  },

  async getHistoricalForecasts(locationId: number, days: number = 7): Promise<WeatherForecast[]> {
    const response = await weatherApi.get(`/forecasts/location/${locationId}/history`, {
      params: { days },
    })
    return response.data
  },

  async getCurrentForecast(lat: number, lon: number): Promise<WeatherForecast[]> {
    const response = await weatherApi.get('/forecasts/current', {
      params: { lat, lon },
    })
    return response.data
  },

  async getForecastsByCoordinates(
    lat: number,
    lon: number,
    from?: string,
    to?: string,
  ): Promise<WeatherForecast[]> {
    const response = await weatherApi.get('/forecasts/coordinates', {
      params: { lat, lon, from, to, size: 500 },
    })
    const body = response.data
    return Array.isArray(body) ? body : body.data ?? []
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
      params: { from, to },
    })
    return response.data
  },

  async refreshHurricaneData(): Promise<void> {
    await weatherApi.post('/hurricanes/refresh')
  },

  // Weather Alerts
  async getActiveAlerts(): Promise<WeatherAlert[]> {
    const response = await weatherApi.get('/alerts/active')
    return response.data
  },

  async getAlertsBySeverity(severity: string): Promise<WeatherAlert[]> {
    const response = await weatherApi.get(`/alerts/severity/${severity}`)
    return response.data
  },

  async refreshAlerts(): Promise<void> {
    await weatherApi.post('/alerts/refresh')
  },

  // Earthquakes
  async getRecentEarthquakes(): Promise<Earthquake[]> {
    const response = await weatherApi.get('/earthquakes/recent')
    return response.data
  },

  async getSignificantEarthquakes(): Promise<Earthquake[]> {
    const response = await weatherApi.get('/earthquakes/significant')
    return response.data
  },

  async refreshEarthquakes(): Promise<void> {
    await weatherApi.post('/earthquakes/refresh')
  },

  // Space Weather
  async getSpaceWeather(): Promise<SpaceWeather> {
    const response = await weatherApi.get('/space-weather')
    return response.data
  },

  // Climate Normals
  async getClimateNormals(locationId: number): Promise<ClimateNormals> {
    const response = await weatherApi.get(`/climate/${locationId}`)
    return response.data
  },

  // Solar Data
  async getSolarData(locationId: number): Promise<SolarData> {
    const response = await weatherApi.get(`/solar/${locationId}`)
    return response.data
  },
}

export default weatherService
