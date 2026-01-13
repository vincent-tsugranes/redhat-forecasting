import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8090'

export const weatherApi = axios.create({
  baseURL: `${API_BASE_URL}/api/weather`,
  headers: {
    'Content-Type': 'application/json'
  },
  timeout: 15000
})

// Request interceptor
weatherApi.interceptors.request.use(
  (config) => {
    console.log('API Request:', config.method?.toUpperCase(), config.url)
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
weatherApi.interceptors.response.use(
  (response) => {
    console.log('API Response:', response.status, response.config.url)
    return response
  },
  (error) => {
    console.error('API Error:', error.response?.status, error.config?.url, error.message)
    return Promise.reject(error)
  }
)

export default weatherApi
