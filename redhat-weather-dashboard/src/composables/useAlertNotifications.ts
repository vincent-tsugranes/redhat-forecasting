import { ref } from 'vue'
import { type WeatherAlert } from '../services/weatherService'

const STORAGE_KEY = 'weather-notifications-enabled'
const NOTIFIED_KEY = 'weather-notified-alerts'

function loadEnabled(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === 'true'
  } catch {
    return false
  }
}

function loadNotifiedIds(): Set<string> {
  try {
    const stored = localStorage.getItem(NOTIFIED_KEY)
    return stored ? new Set(JSON.parse(stored)) : new Set()
  } catch {
    return new Set()
  }
}

function saveNotifiedIds(ids: Set<string>) {
  localStorage.setItem(NOTIFIED_KEY, JSON.stringify([...ids]))
}

const notificationsEnabled = ref(loadEnabled())

export function useAlertNotifications() {
  async function requestPermission(): Promise<boolean> {
    if (!('Notification' in window)) return false
    if (Notification.permission === 'granted') return true
    if (Notification.permission === 'denied') return false
    const result = await Notification.requestPermission()
    return result === 'granted'
  }

  async function toggleNotifications() {
    if (notificationsEnabled.value) {
      notificationsEnabled.value = false
      localStorage.setItem(STORAGE_KEY, 'false')
      return
    }

    const granted = await requestPermission()
    if (granted) {
      notificationsEnabled.value = true
      localStorage.setItem(STORAGE_KEY, 'true')
    }
  }

  function checkAndNotify(alerts: WeatherAlert[]) {
    if (!notificationsEnabled.value) return
    if (!('Notification' in window) || Notification.permission !== 'granted') return

    const severeAlerts = alerts.filter(
      (a) => a.severity === 'Severe' || a.severity === 'Extreme',
    )

    if (severeAlerts.length === 0) return

    const notifiedIds = loadNotifiedIds()
    const newAlerts = severeAlerts.filter((a) => !notifiedIds.has(a.alertId))

    for (const alert of newAlerts) {
      new Notification(alert.event, {
        body: alert.headline || `${alert.severity} weather alert`,
        tag: alert.alertId,
        icon: '⚠️',
      })
      notifiedIds.add(alert.alertId)
    }

    if (newAlerts.length > 0) {
      saveNotifiedIds(notifiedIds)
    }
  }

  return {
    notificationsEnabled,
    toggleNotifications,
    checkAndNotify,
  }
}
