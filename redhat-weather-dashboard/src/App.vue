<template>
  <div id="app">
    <header class="app-header">
      <div class="container header-content">
        <h1>{{ $t('app.title') }}</h1>
        <div class="header-controls">
          <nav>
            <router-link to="/">{{ $t('nav.dashboard') }}</router-link>
            <router-link to="/forecasts">{{ $t('nav.forecasts') }}</router-link>
            <router-link to="/airports">{{ $t('nav.airports') }}</router-link>
            <router-link to="/hurricanes">{{ $t('nav.hurricanes') }}</router-link>
          </nav>
          <button
            class="theme-toggle"
            :title="theme === 'dark' ? $t('app.themeLight') : $t('app.themeDark')"
            :aria-label="theme === 'dark' ? $t('app.themeLight') : $t('app.themeDark')"
            @click="toggleTheme"
          >
            <span aria-hidden="true">{{ theme === 'dark' ? '☀️' : '🌙' }}</span>
          </button>
        </div>
      </div>
    </header>

    <main>
      <router-view />
    </main>

    <footer class="app-footer">
      <div class="container">
        <p>
          {{ $t('app.footer') }}
        </p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const theme = ref(
  localStorage.getItem('theme') ||
    (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'),
)

function applyTheme(t: string) {
  document.documentElement.dataset.theme = t
}

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem('theme', theme.value)
  applyTheme(theme.value)
}

onMounted(() => {
  applyTheme(theme.value)
})
</script>

<style scoped>
.app-header {
  background-color: var(--header-bg, #ee0000);
  color: white;
  padding: 20px 0;
  box-shadow: 0 2px 4px var(--shadow, rgba(0, 0, 0, 0.1));
}

.app-header h1 {
  color: white;
  margin: 0 0 10px 0;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

nav {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
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

.theme-toggle {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  padding: 0;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
  flex-shrink: 0;
}

.theme-toggle:hover {
  background: rgba(255, 255, 255, 0.35);
}

main {
  min-height: calc(100vh - 200px);
}

.app-footer {
  background-color: var(--footer-bg, #333);
  color: white;
  padding: 20px 0;
  margin-top: 40px;
  text-align: center;
}

.app-footer p {
  margin: 0;
  font-size: 14px;
  color: var(--footer-text, #ccc);
}

@media (max-width: 768px) {
  .app-header {
    padding: 12px 0;
  }

  .app-header h1 {
    font-size: 1.2rem;
    margin: 0 0 8px 0;
  }

  .header-controls {
    flex-wrap: wrap;
  }

  nav {
    gap: 8px;
  }

  nav a {
    padding: 6px 10px;
    font-size: 13px;
  }

  .app-footer {
    padding: 12px 0;
    margin-top: 20px;
  }

  .app-footer p {
    font-size: 12px;
  }
}
</style>
