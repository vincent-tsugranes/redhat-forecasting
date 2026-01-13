<template>
  <div id="app">
    <header class="app-header">
      <div class="container">
        <h1>Red Hat Weather Dashboard</h1>
        <nav>
          <router-link to="/">Dashboard</router-link>
          <router-link to="/forecasts">Forecasts</router-link>
          <router-link to="/airports">Airports</router-link>
          <router-link to="/hurricanes">Hurricanes</router-link>
        </nav>
      </div>
    </header>

    <main>
      <router-view />
    </main>

    <footer class="app-footer">
      <div class="container">
        <p>&copy; 2024 Red Hat Weather Service. Data from NOAA, Aviation Weather Center, NHC, and OpenWeatherMap.</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
// App component
</script>

<style scoped>
.app-header {
  background-color: #ee0000;
  color: white;
  padding: 20px 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.app-header h1 {
  color: white;
  margin: 0 0 10px 0;
}

nav {
  display: flex;
  gap: 20px;
}

nav a {
  color: white;
  text-decoration: none;
  padding: 8px 16px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

nav a:hover,
nav a.router-link-active {
  background-color: rgba(255, 255, 255, 0.2);
}

main {
  min-height: calc(100vh - 200px);
}

.app-footer {
  background-color: #333;
  color: white;
  padding: 20px 0;
  margin-top: 40px;
  text-align: center;
}

.app-footer p {
  margin: 0;
  font-size: 14px;
  color: #ccc;
}
</style>
