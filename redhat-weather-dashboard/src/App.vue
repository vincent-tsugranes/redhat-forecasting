<template>
  <div id="app">
    <header class="app-header">
      <div class="container header-content">
        <div class="header-top">
          <router-link to="/" class="header-brand">{{ $t('app.title') }}</router-link>
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
            <router-link to="/" exact>{{ $t('nav.dashboard') }}</router-link>
            <router-link to="/forecasts">{{ $t('nav.forecasts') }}</router-link>

            <div class="nav-group" @mouseenter="showDropdown('aviation')" @mouseleave="scheduleCloseDropdown">
              <button
                class="nav-group-trigger"
                :class="{ active: aviationRoutes.includes($route.path) }"
                :aria-expanded="openDropdown === 'aviation'"
                @click="openDropdown = openDropdown === 'aviation' ? null : 'aviation'"
              >
                {{ $t('nav.aviation') }} <span class="caret" aria-hidden="true">▾</span>
              </button>
              <div v-show="openDropdown === 'aviation'" class="nav-dropdown" @mouseenter="cancelCloseDropdown" @mouseleave="scheduleCloseDropdown">
                <router-link to="/airports" class="nav-dropdown-item" @click="openDropdown = null">
                  <span class="nav-icon" aria-hidden="true">✈️</span> {{ $t('nav.airports') }}
                  <span v-if="airports.length" class="nav-count">{{ airports.length.toLocaleString() }}</span>
                </router-link>
                <router-link to="/pireps" class="nav-dropdown-item" @click="openDropdown = null">
                  <span class="nav-icon" aria-hidden="true">📋</span> {{ $t('nav.pireps') }}
                  <span v-if="pireps.length" class="nav-count">{{ pireps.length }}</span>
                </router-link>
                <router-link to="/sigmets" class="nav-dropdown-item" @click="openDropdown = null">
                  <span class="nav-icon" aria-hidden="true">🚨</span> {{ $t('nav.sigmets') }}
                  <span v-if="sigmets.length" class="nav-count">{{ sigmets.length }}</span>
                </router-link>
                <router-link to="/cwas" class="nav-dropdown-item" @click="openDropdown = null">
                  <span class="nav-icon" aria-hidden="true">📡</span> {{ $t('nav.cwas') }}
                  <span v-if="cwas.length" class="nav-count">{{ cwas.length }}</span>
                </router-link>
                <router-link to="/tfrs" class="nav-dropdown-item" @click="openDropdown = null">
                  <span class="nav-icon" aria-hidden="true">🚫</span> {{ $t('nav.tfrs') }}
                  <span v-if="tfrs.length" class="nav-count">{{ tfrs.length }}</span>
                </router-link>
                <router-link to="/winds-aloft" class="nav-dropdown-item" @click="openDropdown = null">
                  <span class="nav-icon" aria-hidden="true">💨</span> {{ $t('nav.windsAloft') }}
                  <span v-if="windsAloft.length" class="nav-count">{{ windsAloft.length }}</span>
                </router-link>
                <router-link to="/delays" class="nav-dropdown-item" @click="openDropdown = null">
                  <span class="nav-icon" aria-hidden="true">⏱️</span> {{ $t('nav.delays') }}
                  <span v-if="delayedCount" class="nav-count nav-count-alert">{{ delayedCount }}</span>
                </router-link>
                <router-link to="/ground-stops" class="nav-dropdown-item" @click="openDropdown = null">
                  <span class="nav-icon" aria-hidden="true">🛑</span> {{ $t('nav.groundStops') }}
                  <span v-if="groundStops.length" class="nav-count nav-count-alert">{{ groundStops.length }}</span>
                </router-link>
              </div>
            </div>

            <div class="nav-group" @mouseenter="showDropdown('hazards')" @mouseleave="scheduleCloseDropdown">
              <button
                class="nav-group-trigger"
                :class="{ active: hazardRoutes.includes($route.path) }"
                :aria-expanded="openDropdown === 'hazards'"
                @click="openDropdown = openDropdown === 'hazards' ? null : 'hazards'"
              >
                {{ $t('nav.hazards') }} <span class="caret" aria-hidden="true">▾</span>
              </button>
              <div v-show="openDropdown === 'hazards'" class="nav-dropdown" @mouseenter="cancelCloseDropdown" @mouseleave="scheduleCloseDropdown">
                <router-link to="/hurricanes" class="nav-dropdown-item" @click="openDropdown = null">
                  <span class="nav-icon" aria-hidden="true">🌀</span> {{ $t('nav.hurricanes') }}
                  <span v-if="hurricanes.length" class="nav-count nav-count-alert">{{ hurricanes.length }}</span>
                </router-link>
                <router-link to="/earthquakes" class="nav-dropdown-item" @click="openDropdown = null">
                  <span class="nav-icon" aria-hidden="true">🌍</span> {{ $t('nav.earthquakes') }}
                  <span v-if="earthquakes.length" class="nav-count">{{ earthquakes.length }}</span>
                </router-link>
                <router-link to="/volcanic-ash" class="nav-dropdown-item" @click="openDropdown = null">
                  <span class="nav-icon" aria-hidden="true">🌋</span> {{ $t('nav.volcanicAsh') }}
                  <span v-if="volcanicAsh.length" class="nav-count nav-count-alert">{{ volcanicAsh.length }}</span>
                </router-link>
                <router-link to="/lightning" class="nav-dropdown-item" @click="openDropdown = null">
                  <span class="nav-icon" aria-hidden="true">⚡</span> {{ $t('nav.lightning') }}
                  <span v-if="lightning.length" class="nav-count">{{ lightning.length }}</span>
                </router-link>
              </div>
            </div>

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
      <router-view />
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAlertNotifications } from './composables/useAlertNotifications'
import { useWeatherStore } from './stores/weatherStore'
import ToastContainer from './components/ToastContainer.vue'
import SettingsPanel from './components/SettingsPanel.vue'
import GlobalSearch from './components/GlobalSearch.vue'

const { notificationsEnabled, toggleNotifications } = useAlertNotifications()

const store = useWeatherStore()
const {
  airports, pireps, sigmets, cwas, tfrs, windsAloft, delays, groundStops,
  hurricanes, earthquakes, volcanicAsh, lightning,
} = storeToRefs(store)

const delayedCount = computed(() => delays.value.filter(d => d.isDelayed).length)

const route = useRoute()
const menuOpen = ref(false)
const showSettings = ref(false)
const openDropdown = ref<string | null>(null)

const aviationRoutes = ['/airports', '/pireps', '/sigmets', '/cwas', '/tfrs', '/winds-aloft', '/delays', '/ground-stops']
const hazardRoutes = ['/hurricanes', '/earthquakes', '/volcanic-ash', '/lightning']

let dropdownCloseTimer: ReturnType<typeof setTimeout> | null = null

function showDropdown(name: string) {
  if (dropdownCloseTimer) {
    clearTimeout(dropdownCloseTimer)
    dropdownCloseTimer = null
  }
  openDropdown.value = name
}

function scheduleCloseDropdown() {
  dropdownCloseTimer = setTimeout(() => {
    openDropdown.value = null
    dropdownCloseTimer = null
  }, 150)
}

function cancelCloseDropdown() {
  if (dropdownCloseTimer) {
    clearTimeout(dropdownCloseTimer)
    dropdownCloseTimer = null
  }
}

function toggleMenu() {
  menuOpen.value = !menuOpen.value
}

// Close menu and dropdowns on route change
watch(() => route.path, () => {
  menuOpen.value = false
  openDropdown.value = null
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
  padding: 8px 0;
  box-shadow: 0 2px 4px var(--shadow, rgba(0, 0, 0, 0.1));
}

.header-brand {
  color: white;
  text-decoration: none;
  font-size: 1.3rem;
  font-weight: 700;
  white-space: nowrap;
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
  gap: 8px;
  margin-top: 6px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
}

nav {
  display: flex;
  gap: 4px;
  align-items: center;
  flex-wrap: wrap;
}

nav > a {
  color: white;
  text-decoration: none;
  padding: 4px 10px;
  border-radius: 4px;
  transition: background-color 0.2s;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

nav > a:hover,
nav > a.router-link-active {
  background-color: rgba(255, 255, 255, 0.2);
}

/* Dropdown groups */
.nav-group {
  position: relative;
}

.nav-group-trigger {
  color: white;
  background: none;
  border: none;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color 0.2s;
}

.nav-group-trigger:hover,
.nav-group-trigger.active {
  background-color: rgba(255, 255, 255, 0.2);
}

.caret {
  font-size: 10px;
  margin-left: 2px;
}

.nav-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  background: var(--bg-card, white);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  min-width: 180px;
  z-index: 2000;
  padding: 6px 0;
  display: flex;
  flex-direction: column;
}

/* Invisible bridge so mouse can travel from trigger to dropdown */
.nav-dropdown::before {
  content: '';
  position: absolute;
  top: -8px;
  left: 0;
  right: 0;
  height: 8px;
}

.nav-dropdown a {
  color: var(--text-primary, #333);
  text-decoration: none;
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 500;
  transition: background-color 0.15s;
}

.nav-dropdown-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-icon {
  font-size: 14px;
  width: 18px;
  text-align: center;
  flex-shrink: 0;
}

.nav-count {
  margin-left: auto;
  font-size: 11px;
  font-weight: 600;
  background: var(--bg-code, #f0f0f0);
  color: var(--text-secondary, #666);
  padding: 1px 6px;
  border-radius: 8px;
  min-width: 20px;
  text-align: center;
}

.nav-count-alert {
  background: var(--accent);
  color: white;
}

.nav-dropdown a:hover {
  background: var(--bg-code, #f5f5f5);
}

.nav-dropdown a.router-link-active {
  color: var(--accent);
  font-weight: 600;
}

.header-icon-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  padding: 0;
  font-size: 16px;
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
  width: 32px;
  height: 32px;
  padding: 0;
  font-size: 16px;
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
  padding: 12px 0;
  margin-top: 24px;
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
  background: var(--accent);
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
    padding: 10px 0;
  }

  .header-brand {
    font-size: 1.1rem;
  }

  .hamburger {
    display: flex;
  }

  .header-controls {
    display: none;
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
    margin-top: 10px;
    max-height: 0;
    overflow: visible;
    transition: max-height 0.3s ease;
  }

  .header-controls.nav-open {
    display: flex;
    max-height: 600px;
  }

  nav {
    flex-direction: column;
    gap: 2px;
  }

  nav > a {
    padding: 10px 16px;
    font-size: 14px;
    border-radius: 6px;
  }

  .nav-group {
    display: flex;
    flex-direction: column;
  }

  .nav-group-trigger {
    padding: 10px 16px;
    font-size: 14px;
    text-align: left;
    border-radius: 6px;
  }

  .nav-dropdown {
    position: static;
    margin-top: 0;
    box-shadow: none;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 6px;
    padding: 2px 0;
  }

  .nav-dropdown a {
    color: white;
    padding: 8px 16px 8px 24px;
    font-size: 14px;
  }

  .nav-dropdown a:hover {
    background: rgba(255, 255, 255, 0.15);
  }

  .nav-dropdown a.router-link-active {
    color: white;
    background: rgba(255, 255, 255, 0.2);
  }

  .nav-count {
    background: rgba(255, 255, 255, 0.25);
    color: white;
  }

  .nav-count-alert {
    background: rgba(255, 255, 255, 0.4);
  }

  .header-actions {
    justify-content: center;
    padding-top: 8px;
    border-top: 1px solid rgba(255, 255, 255, 0.2);
    margin-left: 0;
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
