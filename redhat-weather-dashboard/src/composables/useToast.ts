import { ref } from 'vue'

export interface Toast {
  id: number
  message: string
  type: 'success' | 'error' | 'info'
}

const toasts = ref<Toast[]>([])
let nextId = 0

function addToast(message: string, type: Toast['type'], duration: number) {
  const id = nextId++
  toasts.value.push({ id, message, type })
  setTimeout(() => {
    toasts.value = toasts.value.filter((t) => t.id !== id)
  }, duration)
}

export function useToast() {
  return {
    toasts,
    success(message: string) {
      addToast(message, 'success', 3000)
    },
    error(message: string) {
      addToast(message, 'error', 5000)
    },
    info(message: string) {
      addToast(message, 'info', 3000)
    },
  }
}
