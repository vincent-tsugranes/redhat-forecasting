import { ref, watch } from 'vue'

export type TempUnit = 'F' | 'C'
export type SpeedUnit = 'mph' | 'kmh'
export type TimeFormat = '12h' | '24h'

function getStorageItem(key: string): string | null {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function setStorageItem(key: string, value: string) {
  try {
    localStorage.setItem(key, value)
  } catch {
    // ignore in environments without localStorage
  }
}

const tempUnit = ref<TempUnit>((getStorageItem('tempUnit') as TempUnit) || 'F')
const speedUnit = ref<SpeedUnit>((getStorageItem('speedUnit') as SpeedUnit) || 'mph')
const timeFormat = ref<TimeFormat>((getStorageItem('timeFormat') as TimeFormat) || '12h')

watch(tempUnit, (v) => setStorageItem('tempUnit', v))
watch(speedUnit, (v) => setStorageItem('speedUnit', v))
watch(timeFormat, (v) => setStorageItem('timeFormat', v))

export function useUnitPreferences() {
  function convertTemp(fahrenheit: number): number {
    if (tempUnit.value === 'C') {
      return Math.round(((fahrenheit - 32) * 5) / 9)
    }
    return Math.round(fahrenheit)
  }

  function formatTemp(fahrenheit: number): string {
    return `${convertTemp(fahrenheit)}°${tempUnit.value}`
  }

  function convertSpeed(mph: number): number {
    if (speedUnit.value === 'kmh') {
      return Math.round(mph * 1.60934)
    }
    return Math.round(mph)
  }

  function formatSpeed(mph: number): string {
    const unit = speedUnit.value === 'kmh' ? 'km/h' : 'mph'
    return `${convertSpeed(mph)} ${unit}`
  }

  function formatTime(date: Date): string {
    if (timeFormat.value === '24h') {
      return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false })
    }
    return date.toLocaleTimeString('en-US', { hour: 'numeric', hour12: true })
  }

  return {
    tempUnit,
    speedUnit,
    timeFormat,
    convertTemp,
    formatTemp,
    convertSpeed,
    formatSpeed,
    formatTime,
  }
}
