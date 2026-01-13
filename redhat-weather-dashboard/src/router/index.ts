import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import ForecastView from '../views/ForecastView.vue'
import AirportView from '../views/AirportView.vue'
import HurricaneView from '../views/HurricaneView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: DashboardView
    },
    {
      path: '/forecasts',
      name: 'forecasts',
      component: ForecastView
    },
    {
      path: '/airports',
      name: 'airports',
      component: AirportView
    },
    {
      path: '/hurricanes',
      name: 'hurricanes',
      component: HurricaneView
    }
  ]
})

export default router
