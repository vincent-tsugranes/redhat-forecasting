// METAR/TAF raw text parser — converts aviation weather codes to human-readable text

const WEATHER_PHENOMENA: Record<string, string> = {
  // Intensity / Proximity
  '-': 'Light',
  '+': 'Heavy',
  VC: 'In the vicinity',

  // Descriptor
  MI: 'Shallow',
  BC: 'Patches of',
  PR: 'Partial',
  DR: 'Low drifting',
  BL: 'Blowing',
  SH: 'Showers',
  TS: 'Thunderstorm',
  FZ: 'Freezing',

  // Precipitation
  DZ: 'Drizzle',
  RA: 'Rain',
  SN: 'Snow',
  SG: 'Snow grains',
  IC: 'Ice crystals',
  PL: 'Ice pellets',
  GR: 'Hail',
  GS: 'Small hail',
  UP: 'Unknown precipitation',

  // Obscuration
  BR: 'Mist',
  FG: 'Fog',
  FU: 'Smoke',
  VA: 'Volcanic ash',
  DU: 'Widespread dust',
  SA: 'Sand',
  HZ: 'Haze',
  PY: 'Spray',

  // Other
  PO: 'Dust/sand whirls',
  SQ: 'Squall',
  FC: 'Funnel cloud/tornado',
  SS: 'Sandstorm',
  DS: 'Duststorm',
}

const SKY_COVER: Record<string, string> = {
  SKC: 'Clear',
  CLR: 'Clear',
  FEW: 'Few clouds',
  SCT: 'Scattered clouds',
  BKN: 'Broken clouds',
  OVC: 'Overcast',
  VV: 'Vertical visibility',
}

const CLOUD_TYPES: Record<string, string> = {
  CB: 'Cumulonimbus',
  TCU: 'Towering cumulus',
}

export interface DecodedSkyLayer {
  coverage: string
  coverageRaw: string
  altitude: number
  cloudType?: string
}

export interface DecodedWeather {
  station: string
  time: string
  isAuto: boolean
  wind: string | null
  visibility: string | null
  weather: string[]
  skyLayers: DecodedSkyLayer[]
  temperature: string | null
  dewpoint: string | null
  altimeter: string | null
  remarks: string | null
  flightCategory: string | null
}

export interface DecodedTafForecast {
  type: 'BASE' | 'FM' | 'TEMPO' | 'BECMG' | 'PROB'
  timeRange: string
  probability?: number
  wind: string | null
  visibility: string | null
  weather: string[]
  skyLayers: DecodedSkyLayer[]
}

export interface DecodedTaf {
  station: string
  issuedTime: string
  validRange: string
  forecasts: DecodedTafForecast[]
}

function parseWindDirection(dir: number): string {
  const dirs = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE', 'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW']
  return dirs[Math.round(dir / 22.5) % 16]
}

function decodeWind(token: string): string | null {
  const m = token.match(/^(\d{3}|VRB)(\d{2,3})(G(\d{2,3}))?KT$/)
  if (!m) return null
  const dir = m[1] === 'VRB' ? 'Variable' : `${m[1]}° (${parseWindDirection(parseInt(m[1]))})`
  const speed = parseInt(m[2])
  const gust = m[4] ? parseInt(m[4]) : null
  if (speed === 0) return 'Calm'
  let result = `${dir} at ${speed} knots`
  if (gust) result += `, gusting to ${gust} knots`
  return result
}

function decodeVisibility(token: string): string | null {
  if (token === 'P6SM') return 'Greater than 6 miles'
  if (token === 'M1/4SM') return 'Less than 1/4 mile'
  const m = token.match(/^(\d+)?(?:\s)?(\d\/\d)?SM$/)
  if (!m) return null
  const whole = m[1] ? parseInt(m[1]) : 0
  const frac = m[2] || ''
  const vis = whole ? `${whole}${frac ? ' ' + frac : ''}` : frac
  return `${vis} mile${whole !== 1 ? 's' : ''}`
}

function decodeWeatherPhenomena(token: string): string | null {
  // Match weather group: optional intensity, then pairs of 2-letter codes
  const m = token.match(/^([+-]|VC)?(.+)$/)
  if (!m) return null

  const intensity = m[1] || ''
  const codes = m[2]

  const parts: string[] = []
  if (intensity && WEATHER_PHENOMENA[intensity]) {
    parts.push(WEATHER_PHENOMENA[intensity])
  }

  // Break remaining into 2-char codes
  for (let i = 0; i < codes.length; i += 2) {
    const code = codes.substring(i, i + 2)
    if (WEATHER_PHENOMENA[code]) {
      parts.push(WEATHER_PHENOMENA[code].toLowerCase())
    } else {
      return null // not a weather token
    }
  }

  if (parts.length === 0) return null
  // Capitalize first word
  parts[0] = parts[0].charAt(0).toUpperCase() + parts[0].slice(1)
  return parts.join(' ')
}

function decodeSkyLayer(token: string): DecodedSkyLayer | null {
  const m = token.match(/^(SKC|CLR|FEW|SCT|BKN|OVC|VV)(\d{3})?(CB|TCU)?$/)
  if (!m) return null

  const coverageRaw = m[1]
  const coverage = SKY_COVER[coverageRaw] || coverageRaw
  const altitude = m[2] ? parseInt(m[2]) * 100 : 0
  const cloudType = m[3] ? CLOUD_TYPES[m[3]] : undefined

  return { coverage, coverageRaw, altitude, cloudType }
}

function decodeTempDew(token: string): { temp: string; dew: string } | null {
  const m = token.match(/^(M?\d{2})\/(M?\d{2})$/)
  if (!m) return null

  function parseTemp(s: string): string {
    const neg = s.startsWith('M')
    const val = parseInt(s.replace('M', ''))
    const c = neg ? -val : val
    const f = Math.round(c * 9 / 5 + 32)
    return `${c}°C (${f}°F)`
  }

  return { temp: parseTemp(m[1]), dew: parseTemp(m[2]) }
}

function decodeAltimeter(token: string): string | null {
  const m = token.match(/^A(\d{4})$/)
  if (!m) return null
  const inHg = parseInt(m[1]) / 100
  const hPa = Math.round(inHg * 33.8639)
  return `${inHg.toFixed(2)} inHg (${hPa} hPa)`
}

function formatTafTime(ddhhmmZ: string): string {
  const m = ddhhmmZ.match(/^(\d{2})(\d{2})(\d{2})Z?$/)
  if (!m) return ddhhmmZ
  return `Day ${m[1]} at ${m[2]}:${m[3]}Z`
}

function formatTafValidRange(token: string): string {
  const m = token.match(/^(\d{2})(\d{2})\/(\d{2})(\d{2})$/)
  if (!m) return token
  return `Day ${m[1]} ${m[2]}:00Z to Day ${m[3]} ${m[4]}:00Z`
}

function getFlightCategory(vis: string | null, layers: DecodedSkyLayer[]): string | null {
  if (!vis) return null
  // Parse visibility in miles
  let visMiles = 10
  const pMatch = vis.match(/Greater than/)
  if (pMatch) visMiles = 7
  else {
    const mMatch = vis.match(/Less than/)
    if (mMatch) visMiles = 0.2
    else {
      const nMatch = vis.match(/^([\d.]+)/)
      if (nMatch) visMiles = parseFloat(nMatch[1])
      // handle fractions like "1/2"
      const fracMatch = vis.match(/(\d+)\/(\d+)/)
      if (fracMatch && !vis.match(/^\d+ \d+\/\d+/)) {
        visMiles = parseInt(fracMatch[1]) / parseInt(fracMatch[2])
      } else if (fracMatch) {
        // "1 1/2" style
        const wholeMatch = vis.match(/^(\d+)\s/)
        if (wholeMatch) visMiles = parseInt(wholeMatch[1]) + parseInt(fracMatch[1]) / parseInt(fracMatch[2])
      }
    }
  }

  const ceiling = layers.find(l => l.coverageRaw === 'BKN' || l.coverageRaw === 'OVC' || l.coverageRaw === 'VV')
  const ceilingFt = ceiling ? ceiling.altitude : Infinity

  if (visMiles < 1 || ceilingFt < 500) return 'LIFR'
  if (visMiles < 3 || ceilingFt < 1000) return 'IFR'
  if (visMiles <= 5 || ceilingFt <= 3000) return 'MVFR'
  return 'VFR'
}

// Weather token regex: optional intensity prefix, then 2-char codes from WEATHER_PHENOMENA
const wxCodes = Object.keys(WEATHER_PHENOMENA).filter(k => k.length === 2).join('|')
const wxPattern = new RegExp(`^[+-]?(?:VC)?(?:${wxCodes})+$`)

function isWeatherToken(token: string): boolean {
  return wxPattern.test(token)
}

export function parseMetar(raw: string): DecodedWeather {
  const parts = raw.trim().split(/\s+/)
  let idx = 0

  // Skip "METAR" or "SPECI" prefix
  if (parts[idx] === 'METAR' || parts[idx] === 'SPECI') idx++

  const station = parts[idx++] || ''
  const time = parts[idx] && /^\d{6}Z$/.test(parts[idx]) ? formatTafTime(parts[idx++]) : ''
  const isAuto = parts[idx] === 'AUTO' ? (idx++, true) : false

  let wind: string | null = null
  let visibility: string | null = null
  const weather: string[] = []
  const skyLayers: DecodedSkyLayer[] = []
  let temperature: string | null = null
  let dewpoint: string | null = null
  let altimeter: string | null = null
  let remarks: string | null = null

  while (idx < parts.length) {
    const token = parts[idx]

    if (token === 'RMK') {
      remarks = parts.slice(idx + 1).join(' ')
      break
    }

    // Wind
    if (!wind && /^\d{3}\d{2,3}(G\d{2,3})?KT$/.test(token) || token.match(/^VRB\d{2,3}(G\d{2,3})?KT$/)) {
      wind = decodeWind(token)
      idx++
      continue
    }

    // Visibility — may be two tokens like "1 1/2SM"
    if (!visibility && /^\d+SM$/.test(token)) {
      visibility = decodeVisibility(token)
      idx++
      continue
    }
    if (!visibility && token === 'P6SM') {
      visibility = decodeVisibility(token)
      idx++
      continue
    }
    if (!visibility && token === 'M1/4SM') {
      visibility = decodeVisibility(token)
      idx++
      continue
    }
    if (!visibility && /^\d+$/.test(token) && idx + 1 < parts.length && /^\d\/\d+SM$/.test(parts[idx + 1])) {
      visibility = decodeVisibility(`${token} ${parts[idx + 1]}`)
      idx += 2
      continue
    }
    if (!visibility && /^\d\/\d+SM$/.test(token)) {
      visibility = decodeVisibility(token)
      idx++
      continue
    }

    // Sky condition
    const sky = decodeSkyLayer(token)
    if (sky) {
      skyLayers.push(sky)
      idx++
      continue
    }

    // Temperature/Dewpoint
    if (!temperature && /^M?\d{2}\/M?\d{2}$/.test(token)) {
      const td = decodeTempDew(token)
      if (td) {
        temperature = td.temp
        dewpoint = td.dew
      }
      idx++
      continue
    }

    // Altimeter
    if (!altimeter && /^A\d{4}$/.test(token)) {
      altimeter = decodeAltimeter(token)
      idx++
      continue
    }

    // Weather phenomena
    if (isWeatherToken(token)) {
      const decoded = decodeWeatherPhenomena(token)
      if (decoded) weather.push(decoded)
      idx++
      continue
    }

    idx++
  }

  return {
    station,
    time,
    isAuto,
    wind,
    visibility,
    weather,
    skyLayers,
    temperature,
    dewpoint,
    altimeter,
    remarks,
    flightCategory: getFlightCategory(visibility, skyLayers),
  }
}

function parseTafForecastGroup(tokens: string[]): Omit<DecodedTafForecast, 'type' | 'timeRange' | 'probability'> {
  let wind: string | null = null
  let visibility: string | null = null
  const weather: string[] = []
  const skyLayers: DecodedSkyLayer[] = []

  let idx = 0
  while (idx < tokens.length) {
    const token = tokens[idx]

    if (/^\d{3}\d{2,3}(G\d{2,3})?KT$/.test(token) || /^VRB\d{2,3}(G\d{2,3})?KT$/.test(token)) {
      wind = decodeWind(token)
      idx++
      continue
    }

    if (/^P?6?SM$/.test(token) || /^\d+SM$/.test(token) || token === 'P6SM' || token === 'M1/4SM' || /^\d\/\d+SM$/.test(token)) {
      visibility = decodeVisibility(token)
      idx++
      continue
    }

    if (/^\d+$/.test(token) && idx + 1 < tokens.length && /^\d\/\d+SM$/.test(tokens[idx + 1])) {
      visibility = decodeVisibility(`${token} ${tokens[idx + 1]}`)
      idx += 2
      continue
    }

    const sky = decodeSkyLayer(token)
    if (sky) {
      skyLayers.push(sky)
      idx++
      continue
    }

    if (isWeatherToken(token)) {
      const decoded = decodeWeatherPhenomena(token)
      if (decoded) weather.push(decoded)
      idx++
      continue
    }

    if (token === 'NSW') {
      weather.push('No significant weather')
      idx++
      continue
    }

    idx++
  }

  return { wind, visibility, weather, skyLayers }
}

export function parseTaf(raw: string): DecodedTaf {
  const parts = raw.trim().split(/\s+/)
  let idx = 0

  // Skip "TAF" and "AMD" prefix
  if (parts[idx] === 'TAF') idx++
  if (parts[idx] === 'AMD' || parts[idx] === 'COR') idx++

  const station = parts[idx++] || ''
  const issuedTime = parts[idx] && /^\d{6}Z$/.test(parts[idx]) ? formatTafTime(parts[idx++]) : ''
  const validRange = parts[idx] && /^\d{4}\/\d{4}$/.test(parts[idx]) ? formatTafValidRange(parts[idx++]) : ''

  const forecasts: DecodedTafForecast[] = []

  // Collect tokens for base forecast (everything before first FM/TEMPO/BECMG/PROB)
  const groupDelimiters = /^(FM\d{6}|TEMPO|BECMG|PROB\d{2})$/

  // Gather base group
  const baseTokens: string[] = []
  while (idx < parts.length && !groupDelimiters.test(parts[idx])) {
    baseTokens.push(parts[idx++])
  }

  if (baseTokens.length > 0) {
    forecasts.push({
      type: 'BASE',
      timeRange: validRange,
      ...parseTafForecastGroup(baseTokens),
    })
  }

  // Parse subsequent groups
  while (idx < parts.length) {
    const token = parts[idx]

    if (/^FM(\d{6})$/.test(token)) {
      const m = token.match(/^FM(\d{6})$/)!
      const time = formatTafTime(m[1])
      idx++
      const groupTokens: string[] = []
      while (idx < parts.length && !groupDelimiters.test(parts[idx])) {
        groupTokens.push(parts[idx++])
      }
      forecasts.push({
        type: 'FM',
        timeRange: `From ${time}`,
        ...parseTafForecastGroup(groupTokens),
      })
    } else if (token === 'TEMPO' || token === 'BECMG') {
      const type = token as 'TEMPO' | 'BECMG'
      idx++
      // Next token might be time range like 0618/0622
      let timeRange = ''
      if (idx < parts.length && /^\d{4}\/\d{4}$/.test(parts[idx])) {
        timeRange = formatTafValidRange(parts[idx++])
      }
      const groupTokens: string[] = []
      while (idx < parts.length && !groupDelimiters.test(parts[idx])) {
        groupTokens.push(parts[idx++])
      }
      forecasts.push({
        type,
        timeRange: `${type === 'TEMPO' ? 'Temporary' : 'Becoming'}: ${timeRange}`,
        ...parseTafForecastGroup(groupTokens),
      })
    } else if (/^PROB(\d{2})$/.test(token)) {
      const prob = parseInt(token.match(/^PROB(\d{2})$/)![1])
      idx++
      let timeRange = ''
      // May be followed by TEMPO
      if (idx < parts.length && parts[idx] === 'TEMPO') {
        idx++
      }
      if (idx < parts.length && /^\d{4}\/\d{4}$/.test(parts[idx])) {
        timeRange = formatTafValidRange(parts[idx++])
      }
      const groupTokens: string[] = []
      while (idx < parts.length && !groupDelimiters.test(parts[idx])) {
        groupTokens.push(parts[idx++])
      }
      forecasts.push({
        type: 'PROB',
        timeRange,
        probability: prob,
        ...parseTafForecastGroup(groupTokens),
      })
    } else {
      idx++
    }
  }

  return { station, issuedTime, validRange, forecasts }
}
