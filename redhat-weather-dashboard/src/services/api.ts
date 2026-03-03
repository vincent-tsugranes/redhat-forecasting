import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8090'

const MAX_RETRIES = 2
const RETRY_BASE_DELAY = 1000

interface RetryConfig extends InternalAxiosRequestConfig {
  __retryCount?: number
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function shouldRetry(error: any): boolean {
  // Retry on network errors (no response)
  if (!error.response) return true
  // Retry on 5xx server errors
  return error.response.status >= 500
}

function addRetryInterceptor(instance: AxiosInstance) {
  instance.interceptors.response.use(
    (response) => response,
    async (error) => {
      const config = error.config as RetryConfig
      if (!config) return Promise.reject(error)

      config.__retryCount = config.__retryCount || 0

      if (config.__retryCount >= MAX_RETRIES || !shouldRetry(error)) {
        console.error('API Error:', error.response?.status, config.url, error.message)
        return Promise.reject(error)
      }

      config.__retryCount++
      const delay = RETRY_BASE_DELAY * Math.pow(2, config.__retryCount - 1)
      console.warn(
        `API retry ${config.__retryCount}/${MAX_RETRIES} for ${config.url} in ${delay}ms`,
      )

      await new Promise((resolve) => setTimeout(resolve, delay))
      return instance(config)
    },
  )
}

// General API instance for all endpoints
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
})

// Weather-specific API instance
export const weatherApi = axios.create({
  baseURL: `${API_BASE_URL}/api/weather`,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
})

addRetryInterceptor(api)
addRetryInterceptor(weatherApi)

export default api
