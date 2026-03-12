<template>
  <div id="app">
    <header class="app-header">
      <div class="container header-content">
        <div class="header-top">
          <h1>{{ $t('app.title') }}</h1>
          <button
            class="hamburger"
            :class="{ 'hamburger-open': menuOpen }"
            aria-label="Toggle navigation menu"
            :aria-expanded="menuOpen"
            @click="toggleMenu"
          >
            <span class="hamburger-line"></span>
            <span class="hamburger-line"></span>
            <span class="hamburger-line"></span>
          </button>
        </div>
        <div class="header-controls" :class="{ 'nav-open': menuOpen }">
          <nav>
            <router-link to="/">{{ $t('nav.dashboard') }}</router-link>
            <router-link to="/forecasts">{{ $t('nav.forecasts') }}</router-link>
            <router-link to="/airports">{{ $t('nav.airports') }}</router-link>
            <router-link to="/hurricanes">{{ $t('nav.hurricanes') }}</router-link>
            <router-link to="/earthquakes">{{ $t('nav.earthquakes') }}</router-link>
            <router-link to="/pireps">{{ $t('nav.pireps') }}</router-link>
            <router-link to="/sigmets">{{ $t('nav.sigmets') }}</router-link>
            <router-link to="/delays">{{ $t('nav.delays') }}</router-link>
            <router-link to="/cwas">{{ $t('nav.cwas') }}</router-link>
            <router-link to="/winds-aloft">{{ $t('nav.windsAloft') }}</router-link>
            <router-link to="/space-weather">{{ $t('nav.spaceWeather') }}</router-link>
            <router-link to="/map">{{ $t('nav.map') }}</router-link>
          </nav>
          <div class="header-actions">
            <GlobalSearch />
            <button
              class="header-icon-btn"
              :title="notificationsEnabled ? $t('notifications.disable') : $t('notifications.enable')"
              :aria-label="notificationsEnabled ? $t('notifications.disable') : $t('notifications.enable')"
              @click="toggleNotifications"
            >
              <span aria-hidden="true">{{ notificationsEnabled ? '🔔' : '🔕' }}</span>
            </button>
            <button
              class="header-icon-btn"
              title="Settings"
              aria-label="Settings"
              @click="showSettings = !showSettings"
            >
              <span aria-hidden="true">⚙</span>
            </button>
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
      </div>
    </header>

    <SettingsPanel v-if="showSettings" @close="showSettings = false" />

    <a href="#main-content" class="skip-link">Skip to main content</a>

    <main id="main-content">
      <router-view v-slot="{ Component }">
        <Transition name="fade" mode="out-in">
          <ErrorBoundary>
            <component :is="Component" />
          </ErrorBoundary>
        </Transition>
      </router-view>
    </main>

    <footer class="app-footer">
      <div class="container">
        <p>
          {{ $t('app.footer') }}
        </p>
      </div>
    </footer>

    <ToastContainer />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAlertNotifications } from './composables/useAlertNotifications'
import ToastContainer from './components/ToastContainer.vue'
import SettingsPanel from './components/SettingsPanel.vue'
import GlobalSearch from './components/GlobalSearch.vue'
import ErrorBoundary from './components/ErrorBoundary.vue'

const { notificationsEnabled, toggleNotifications } = useAlertNotifications()

const route = useRoute()
const menuOpen = ref(false)
const showSettings = ref(false)

function toggleMenu() {
  menuOpen.value = !menuOpen.value
}

// Close menu on route change
watch(() => route.path, () => {
  menuOpen.value = false
})

function loadTheme(): string {
  try {
    return localStorage.getItem('theme') ||
      (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
  } catch {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }
}

const theme = ref(loadTheme())

function applyTheme(t: string) {
  document.documentElement.dataset.theme = t
}

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  try { localStorage.setItem('theme', theme.value) } catch { /* private browsing */ }
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

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hamburger {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  width: 40px;
  height: 40px;
}

.hamburger-line {
  display: block;
  width: 24px;
  height: 2px;
  background: white;
  border-radius: 1px;
  transition: transform 0.3s, opacity 0.3s;
}

.hamburger-open .hamburger-line:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
}

.hamburger-open .hamburger-line:nth-child(2) {
  opacity: 0;
}

.hamburger-open .hamburger-line:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
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

.header-icon-btn {
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

.header-icon-btn:hover {
  background: rgba(255, 255, 255, 0.35);
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

.skip-link {
  position: absolute;
  top: -40px;
  left: 0;
  background: #ee0000;
  color: white;
  padding: 8px 16px;
  z-index: 10000;
  font-size: 14px;
  text-decoration: none;
  border-radius: 0 0 4px 0;
  transition: top 0.2s;
}

.skip-link:focus {
  top: 0;
}

/* Page transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .app-header {
    padding: 12px 0;
  }

  .app-header h1 {
    font-size: 1.2rem;
    margin: 0;
  }

  .hamburger {
    display: flex;
  }

  .header-controls {
    display: none;
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    margin-top: 12px;
    max-height: 0;
    overflow: hidden;
    transition: max-height 0.3s ease;
  }

  .header-controls.nav-open {
    display: flex;
    max-height: 500px;
  }

  nav {
    flex-direction: column;
    gap: 4px;
  }

  nav a {
    padding: 10px 16px;
    font-size: 14px;
    border-radius: 6px;
  }

  .header-actions {
    justify-content: center;
    padding-top: 8px;
    border-top: 1px solid rgba(255, 255, 255, 0.2);
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
