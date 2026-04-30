import { ref } from 'vue'
import { useWeatherStore } from '../stores/weatherStore'
import { logger } from '../utils/logger'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

export function useDashboardSSE() {
  const connected = ref(false)
  let eventSource: EventSource | null = null

  const EVENT_TYPES = [
    'earthquakes',
    'hurricanes',
    'alerts',
    'pireps',
    'sigmets',
    'cwas',
    'tfrs',
    'delays',
    'groundStops',
    'volcanicAsh',
    'lightning',
    'spaceWeather',
  ] as const

  function connect() {
    if (eventSource) return

    const url = `${API_BASE_URL}/api/weather/dashboard/stream`
    eventSource = new EventSource(url)
    const store = useWeatherStore()

    eventSource.addEventListener('connected', () => {
      connected.value = true
      logger.info('Dashboard SSE connected')
    })

    for (const eventType of EVENT_TYPES) {
      eventSource.addEventListener(eventType, (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data)
          store.applySSEUpdate(eventType, data)
        } catch (e) {
          logger.error(`Failed to parse SSE event: ${eventType}`, e)
        }
      })
    }

    eventSource.onerror = () => {
      connected.value = false
      // EventSource auto-reconnects on transient errors (readyState stays CONNECTING).
      // Only when it moves to CLOSED do we need manual reconnection.
      if (eventSource?.readyState === EventSource.CLOSED) {
        eventSource = null
        setTimeout(() => connect(), 5000)
      }
    }
  }

  function disconnect() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
      connected.value = false
    }
  }

  return { connected, connect, disconnect }
}
