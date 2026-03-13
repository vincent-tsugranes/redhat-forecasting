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

export interface Pirep {
  id: number
  pirepId: string
  reportType: string
  rawText: string
  observationTime: string
  latitude: number
  longitude: number
  altitudeFt?: number
  aircraftType?: string
  skyCondition?: string
  turbulenceIntensity?: string
  turbulenceType?: string
  icingIntensity?: string
  icingType?: string
  weatherConditions?: string
  temperatureCelsius?: number
  windSpeedKnots?: number
  windDirection?: number
  visibilityMiles?: number
  fetchedAt?: string
}

export interface Sigmet {
  id: number
  sigmetId: string
  sigmetType: string
  scope?: string
  firId?: string
  firName?: string
  hazard?: string
  severity?: string
  validTimeFrom: string
  validTimeTo: string
  altitudeLowFt?: number
  altitudeHighFt?: number
  rawText?: string
  geojson?: object
  fetchedAt?: string
}

export interface Cwa {
  id: number
  cwaId: string
  artcc: string
  hazard?: string
  severity?: string
  validTimeFrom: string
  validTimeTo: string
  altitudeLowFt?: number
  altitudeHighFt?: number
  rawText?: string
  geojson?: object
  fetchedAt?: string
}

export interface WindsAloft {
  id: number
  forecastId: string
  stationId: string
  latitude?: number
  longitude?: number
  elevationFt?: number
  validTime: string
  forecastHour?: number
  altitudeFt: number
  windDirection?: number
  windSpeedKnots?: number
  temperatureCelsius?: number
  fetchedAt?: string
}

export interface Tfr {
  id: number
  notamId: string
  notamKey?: string
  facility: string
  state?: string
  tfrType: string
  description?: string
  effectiveDate?: string
  expireDate?: string
  latitude?: number
  longitude?: number
  geojson?: object
  isNew: boolean
  fetchedAt?: string
}

export interface GroundStop {
  id: number
  groundStopId: string
  airportCode: string
  airportName?: string
  programType: string
  reason?: string
  endTime?: string
  avgDelayMinutes?: number
  maxDelayMinutes?: number
  fetchedAt?: string
}

export interface VolcanicAshAdvisory {
  id: number
  advisoryId: string
  firId?: string
  firName?: string
  volcanoName?: string
  hazard?: string
  severity?: string
  validTimeFrom: string
  validTimeTo: string
  altitudeLowFt?: number
  altitudeHighFt?: number
  rawText?: string
  geojson?: object
  fetchedAt?: string
}

export interface LightningStrike {
  id: number
  strikeId: string
  latitude: number
  longitude: number
  strikeTime: string
  amplitudeKa?: number
  strikeType?: string
  fetchedAt?: string
}

export interface AirportDelay {
  id: number
  delayId: string
  airportCode: string
  airportName?: string
  delayType: string
  reason?: string
  avgDelayMinutes?: number
  minDelayMinutes?: number
  maxDelayMinutes?: number
  trend?: string
  isDelayed: boolean
  fetchedAt?: string
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
  async getAirports(): Promise<Location[]> {
    const pageSize = 200
    // Fetch first page to get total page count
    const firstResponse = await weatherApi.get('/locations/airports', { params: { page: 0, size: pageSize } })
    const firstBody = firstResponse.data
    const firstData: Location[] = Array.isArray(firstBody) ? firstBody : firstBody.data ?? []
    const totalPages: number = firstBody.totalPages ?? 1

    if (totalPages <= 1) return firstData

    // Fetch remaining pages in parallel
    const promises = []
    for (let page = 1; page < totalPages; page++) {
      promises.push(weatherApi.get('/locations/airports', { params: { page, size: pageSize } }))
    }
    const responses = await Promise.all(promises)
    const allAirports = [...firstData]
    for (const resp of responses) {
      const body = resp.data
      const data: Location[] = Array.isArray(body) ? body : body.data ?? []
      allAirports.push(...data)
    }
    return allAirports
  },

  // Weather Forecasts
  async getForecastsByLocation(locationId: number): Promise<WeatherForecast[]> {
    const pageSize = 200
    const firstResponse = await weatherApi.get(`/forecasts/location/${locationId}`, { params: { page: 0, size: pageSize } })
    const firstBody = firstResponse.data
    const firstData: WeatherForecast[] = Array.isArray(firstBody) ? firstBody : firstBody.data ?? []
    const totalPages: number = firstBody.totalPages ?? 1

    if (totalPages <= 1) return firstData

    const promises = []
    for (let page = 1; page < totalPages; page++) {
      promises.push(weatherApi.get(`/forecasts/location/${locationId}`, { params: { page, size: pageSize } }))
    }
    const responses = await Promise.all(promises)
    const allForecasts = [...firstData]
    for (const resp of responses) {
      const body = resp.data
      const data: WeatherForecast[] = Array.isArray(body) ? body : body.data ?? []
      allForecasts.push(...data)
    }
    return allForecasts
  },

  async getHistoricalForecasts(locationId: number, days: number = 7): Promise<WeatherForecast[]> {
    const pageSize = 200
    const firstResponse = await weatherApi.get(`/forecasts/location/${locationId}/history`, {
      params: { days, page: 0, size: pageSize },
    })
    const firstBody = firstResponse.data
    const firstData: WeatherForecast[] = Array.isArray(firstBody) ? firstBody : firstBody.data ?? []
    const totalPages: number = firstBody.totalPages ?? 1

    if (totalPages <= 1) return firstData

    const promises = []
    for (let page = 1; page < totalPages; page++) {
      promises.push(weatherApi.get(`/forecasts/location/${locationId}/history`, { params: { days, page, size: pageSize } }))
    }
    const responses = await Promise.all(promises)
    const allForecasts = [...firstData]
    for (const resp of responses) {
      const body = resp.data
      const data: WeatherForecast[] = Array.isArray(body) ? body : body.data ?? []
      allForecasts.push(...data)
    }
    return allForecasts
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
    const pageSize = 200
    const firstResponse = await weatherApi.get('/forecasts/coordinates', {
      params: { lat, lon, from, to, page: 0, size: pageSize },
    })
    const firstBody = firstResponse.data
    const firstData: WeatherForecast[] = Array.isArray(firstBody) ? firstBody : firstBody.data ?? []
    const totalPages: number = firstBody.totalPages ?? 1

    if (totalPages <= 1) return firstData

    const promises = []
    for (let page = 1; page < totalPages; page++) {
      promises.push(weatherApi.get('/forecasts/coordinates', { params: { lat, lon, from, to, page, size: pageSize } }))
    }
    const responses = await Promise.all(promises)
    const allForecasts = [...firstData]
    for (const resp of responses) {
      const body = resp.data
      const data: WeatherForecast[] = Array.isArray(body) ? body : body.data ?? []
      allForecasts.push(...data)
    }
    return allForecasts
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

  // PIREPs
  async getRecentPireps(): Promise<Pirep[]> {
    const response = await weatherApi.get('/pireps/recent')
    return response.data
  },

  async refreshPireps(): Promise<void> {
    await weatherApi.post('/pireps/refresh')
  },

  // SIGMETs
  async getActiveSigmets(): Promise<Sigmet[]> {
    const response = await weatherApi.get('/sigmets/active')
    return response.data
  },

  async refreshSigmets(): Promise<void> {
    await weatherApi.post('/sigmets/refresh')
  },

  // CWAs
  async getActiveCwas(): Promise<Cwa[]> {
    const response = await weatherApi.get('/cwas/active')
    return response.data
  },

  async refreshCwas(): Promise<void> {
    await weatherApi.post('/cwas/refresh')
  },

  // Winds Aloft
  async getLatestWindsAloft(): Promise<WindsAloft[]> {
    const response = await weatherApi.get('/winds-aloft/latest')
    return response.data
  },

  async refreshWindsAloft(): Promise<void> {
    await weatherApi.post('/winds-aloft/refresh')
  },

  // TFRs
  async getActiveTfrs(): Promise<Tfr[]> {
    const response = await weatherApi.get('/tfrs/active')
    return response.data
  },

  async refreshTfrs(): Promise<void> {
    await weatherApi.post('/tfrs/refresh')
  },

  // Ground Stops
  async getActiveGroundStops(): Promise<GroundStop[]> {
    const response = await weatherApi.get('/ground-stops/active')
    return response.data
  },

  async refreshGroundStops(): Promise<void> {
    await weatherApi.post('/ground-stops/refresh')
  },

  // Volcanic Ash Advisories
  async getActiveVolcanicAsh(): Promise<VolcanicAshAdvisory[]> {
    const response = await weatherApi.get('/volcanic-ash/active')
    return response.data
  },

  async refreshVolcanicAsh(): Promise<void> {
    await weatherApi.post('/volcanic-ash/refresh')
  },

  // Lightning
  async getRecentLightning(): Promise<LightningStrike[]> {
    const response = await weatherApi.get('/lightning/recent')
    return response.data
  },

  async refreshLightning(): Promise<void> {
    await weatherApi.post('/lightning/refresh')
  },

  // Airport Delays
  async getActiveDelays(): Promise<AirportDelay[]> {
    const response = await weatherApi.get('/delays/active')
    return response.data
  },

  async refreshDelays(): Promise<void> {
    await weatherApi.post('/delays/refresh')
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
