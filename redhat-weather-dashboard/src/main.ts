import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import { logger } from './utils/logger'
import './style.css'

const app = createApp(App)

app.config.errorHandler = (err, instance, info) => {
  logger.error('Vue Error:', err)
  logger.error('Component:', instance?.$options?.name || 'unknown')
  logger.error('Info:', info)
}

window.addEventListener('unhandledrejection', (event) => {
  logger.error('Unhandled promise rejection:', event.reason)
})

app.use(createPinia())
app.use(router)
app.use(i18n)

app.mount('#app')
