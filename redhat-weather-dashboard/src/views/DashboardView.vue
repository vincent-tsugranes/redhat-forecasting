<template>
  <div class="container">
    <h1>{{ $t('dashboard.title') }}</h1>

    <DataStatusCard />

    <FavoritesList />

    <DashboardSkeleton v-if="loading" />
    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="!loading" class="grid">
      <div class="card">
        <h2><span aria-hidden="true">📍</span> {{ $t('dashboard.locations') }}</h2>
        <p>
          <strong>{{ locations.length }}</strong> {{ $t('dashboard.locationsMonitored', { count: locations.length }) }}
        </p>
        <ul v-if="locations.length > 0" style="list-style: none; padding: 0">
          <li v-for="loc in locations.slice(0, 5)" :key="loc.id">
            {{ loc.name }}, {{ loc.state }}
          </li>
        </ul>
      </div>

      <div class="card">
        <h2><span aria-hidden="true">🌤️</span> {{ $t('dashboard.weatherForecasts') }}</h2>
        <p>{{ $t('dashboard.forecastUpdates') }}</p>
        <button :aria-label="$t('dashboard.refreshAriaLabel')" @click="loadData">{{ $t('dashboard.refreshData') }}</button>
      </div>

      <div class="card">
        <h2><span aria-hidden="true">✈️</span> {{ $t('dashboard.airportWeather') }}</h2>
        <p>{{ $t('dashboard.metarUpdates') }}</p>
        <router-link to="/airports">
          <button>{{ $t('dashboard.viewAirports') }}</button>
        </router-link>
      </div>

      <div class="card">
        <h2><span aria-hidden="true">🌀</span> {{ $t('dashboard.hurricaneTracking') }}</h2>
        <p>{{ $t('dashboard.activeMonitoring') }}</p>
        <router-link to="/hurricanes">
          <button>{{ $t('dashboard.viewHurricanes') }}</button>
        </router-link>
      </div>
    </div>

    <div v-if="!loading" class="card" style="margin-top: 20px">
      <h2><span aria-hidden="true">🗺️</span> {{ $t('dashboard.globalMap') }}</h2>
      <p style="margin-bottom: 15px; color: #666">
        {{ $t('dashboard.mapDescription', { count: locations.length }) }}
      </p>
      <AirportMap />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWeatherStore } from '../stores/weatherStore'
import AirportMap from '../components/AirportMap.vue'
import DataStatusCard from '../components/DataStatusCard.vue'
import DashboardSkeleton from '../components/skeletons/DashboardSkeleton.vue'
import FavoritesList from '../components/FavoritesList.vue'

const store = useWeatherStore()
const { locations, locationsLoading: loading, locationsError: error } = storeToRefs(store)

function loadData() {
  store.refreshLocations()
}

onMounted(() => {
  store.fetchLocations()
})
</script>

<style scoped>
h1 {
  color: #ee0000;
  margin-bottom: 30px;
}

h2 {
  font-size: 1.5rem;
  margin-bottom: 10px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

button {
  margin-top: 10px;
}

ul {
  margin-top: 10px;
}

ul li {
  padding: 5px 0;
  color: var(--text-secondary, #666);
}
</style>
