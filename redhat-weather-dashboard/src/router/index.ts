import { createRouter, createWebHistory } from 'vue-router'

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
  ],
})

export default router
