import { Sun, CloudSun, Cloud, CloudFog, Wind, CloudDrizzle, CloudRain, CloudSnow, CloudLightning } from 'lucide-vue-next'
import type { Component } from 'vue'

export function getWeatherIcon(description: string): Component {
  const desc = (description || '').toLowerCase()
  if (desc.includes('thunder') || desc.includes('storm')) return CloudLightning
  if (desc.includes('snow') || desc.includes('blizzard')) return CloudSnow
  if (desc.includes('rain') || desc.includes('shower')) return CloudRain
  if (desc.includes('drizzle')) return CloudDrizzle
  if (desc.includes('cloud') || desc.includes('overcast')) return Cloud
  if (desc.includes('fog') || desc.includes('mist') || desc.includes('haze')) return CloudFog
  if (desc.includes('wind')) return Wind
  if (desc.includes('partly') || desc.includes('mostly sunny')) return CloudSun
  return Sun
}
