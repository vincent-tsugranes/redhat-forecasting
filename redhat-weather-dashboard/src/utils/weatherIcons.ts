export function getWeatherIcon(description: string): string {
  const desc = (description || '').toLowerCase()
  if (desc.includes('thunder') || desc.includes('storm')) return '⛈️'
  if (desc.includes('snow') || desc.includes('blizzard')) return '🌨️'
  if (desc.includes('rain') || desc.includes('shower')) return '🌧️'
  if (desc.includes('drizzle')) return '🌦️'
  if (desc.includes('cloud') || desc.includes('overcast')) return '☁️'
  if (desc.includes('fog') || desc.includes('mist') || desc.includes('haze')) return '🌫️'
  if (desc.includes('wind')) return '💨'
  if (desc.includes('partly') || desc.includes('mostly sunny')) return '⛅'
  return '☀️'
}
