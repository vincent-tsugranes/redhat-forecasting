import { createRouter, createWebHistory } from 'vue-router'
import { useWeatherStore } from '../stores/weatherStore'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: () => import('../views/DashboardView.vue'),
    },
    {
      path: '/forecasts',
      name: 'forecasts',
      component: () => import('../views/ForecastView.vue'),
    },
    {
      path: '/airports',
      name: 'airports',
      component: () => import('../views/AirportView.vue'),
    },
    {
      path: '/hurricanes',
      name: 'hurricanes',
      component: () => import('../views/HurricaneView.vue'),
    },
    {
      path: '/earthquakes',
      name: 'earthquakes',
      component: () => import('../views/EarthquakeView.vue'),
    },
    {
      path: '/space-weather',
      name: 'space-weather',
      component: () => import('../views/SpaceWeatherView.vue'),
    },
    {
      path: '/pireps',
      name: 'pireps',
      component: () => import('../views/PirepView.vue'),
    },
    {
      path: '/sigmets',
      name: 'sigmets',
      component: () => import('../views/SigmetView.vue'),
    },
    {
      path: '/delays',
      name: 'delays',
      component: () => import('../views/AirportDelayView.vue'),
    },
    {
      path: '/tfrs',
      name: 'tfrs',
      component: () => import('../views/TfrView.vue'),
    },
    {
      path: '/cwas',
      name: 'cwas',
      component: () => import('../views/CwaView.vue'),
    },
    {
      path: '/winds-aloft',
      name: 'winds-aloft',
      component: () => import('../views/WindsAloftView.vue'),
    },
    {
      path: '/ground-stops',
      name: 'ground-stops',
      component: () => import('../views/GroundStopView.vue'),
    },
    {
      path: '/volcanic-ash',
      name: 'volcanic-ash',
      component: () => import('../views/VolcanicAshView.vue'),
    },
    {
      path: '/lightning',
      name: 'lightning',
      component: () => import('../views/LightningView.vue'),
    },
    {
      path: '/map',
      name: 'map',
      component: () => import('../views/MapView.vue'),
    },
  ],
})

router.beforeEach((to) => {
  const store = useWeatherStore()
  switch (to.name) {
    case 'airports':
      store.fetchAirports()
      break
    case 'map':
      store.fetchAirports()
      store.fetchEarthquakes()
      store.fetchHurricanes()
      break
    case 'hurricanes':
      store.fetchHurricanes()
      break
    case 'earthquakes':
      store.fetchEarthquakes()
      break
    case 'pireps':
      store.fetchPireps()
      break
    case 'sigmets':
      store.fetchSigmets()
      break
    case 'delays':
      store.fetchDelays()
      break
    case 'tfrs':
      store.fetchTfrs()
      break
    case 'cwas':
      store.fetchCwas()
      break
    case 'winds-aloft':
      store.fetchWindsAloft()
      break
    case 'ground-stops':
      store.fetchGroundStops()
      break
    case 'volcanic-ash':
      store.fetchVolcanicAsh()
      break
    case 'lightning':
      store.fetchLightning()
      break
    case 'forecasts':
      store.fetchAirports()
      break
  }
})

export default router
