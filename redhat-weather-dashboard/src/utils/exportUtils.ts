import { type WeatherForecast } from '../services/weatherService'

function escapeCSV(value: string | number | null | undefined): string {
  if (value == null) return ''
  const str = String(value)
  if (str.includes(',') || str.includes('"') || str.includes('\n')) {
    return `"${str.replace(/"/g, '""')}"`
  }
  return str
}

export function exportForecastsToCSV(forecasts: WeatherForecast[], locationName: string): void {
  const headers = [
    'Date',
    'Time',
    'Temp (°F)',
    'Temp (°C)',
    'Wind (mph)',
    'Wind Direction',
    'Precip (%)',
    'Humidity (%)',
    'Description',
  ]

  const rows = forecasts.map((f) => {
    const dt = new Date(f.validFrom)
    return [
      dt.toLocaleDateString('en-US'),
      dt.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
      f.temperatureFahrenheit,
      f.temperatureCelsius,
      f.windSpeedMph,
      f.windDirection ?? '',
      f.precipitationProbability ?? '',
      f.humidity ?? '',
      escapeCSV(f.weatherDescription || f.weatherShortDescription || ''),
    ]
  })

  const csvContent = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n')
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `forecast-${locationName.replace(/\s+/g, '-').toLowerCase()}-${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
