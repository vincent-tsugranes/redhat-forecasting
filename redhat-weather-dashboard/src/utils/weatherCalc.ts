/**
 * Compute "feels like" temperature (wind chill or heat index).
 * Returns null if inputs are insufficient.
 */
export function computeFeelsLike(
  tempF: number,
  windMph: number | null,
  humidity: number | null,
): number | null {
  // Wind chill (below 50F and wind > 3 mph)
  if (tempF <= 50 && windMph != null && windMph > 3) {
    const wc =
      35.74 +
      0.6215 * tempF -
      35.75 * Math.pow(windMph, 0.16) +
      0.4275 * tempF * Math.pow(windMph, 0.16)
    return Math.round(wc)
  }

  // Heat index (above 80F and humidity available)
  if (tempF >= 80 && humidity != null) {
    const t = tempF
    const rh = humidity
    let hi =
      -42.379 +
      2.04901523 * t +
      10.14333127 * rh -
      0.22475541 * t * rh -
      0.00683783 * t * t -
      0.05481717 * rh * rh +
      0.00122874 * t * t * rh +
      0.00085282 * t * rh * rh -
      0.00000199 * t * t * rh * rh

    // Low humidity adjustment
    if (rh < 13 && t >= 80 && t <= 112) {
      hi -= ((13 - rh) / 4) * Math.sqrt((17 - Math.abs(t - 95)) / 17)
    }
    // High humidity adjustment
    if (rh > 85 && t >= 80 && t <= 87) {
      hi += ((rh - 85) / 10) * ((87 - t) / 5)
    }

    return Math.round(hi)
  }

  return null
}

/**
 * Compute dew point from temperature (F) and relative humidity (%).
 * Uses Magnus approximation. Returns value in Fahrenheit.
 */
export function computeDewPointF(tempF: number, humidity: number): number {
  const tempC = (tempF - 32) * (5 / 9)
  const a = 17.27
  const b = 237.7
  const alpha = (a * tempC) / (b + tempC) + Math.log(humidity / 100)
  const dewC = (b * alpha) / (a - alpha)
  return Math.round(dewC * (9 / 5) + 32)
}

/**
 * Get compass direction label from degrees.
 */
export function degreesToCompass(deg: number): string {
  const dirs = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE', 'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW']
  return dirs[Math.round(deg / 22.5) % 16]
}
