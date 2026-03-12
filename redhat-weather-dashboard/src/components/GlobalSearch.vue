<template>
  <div class="global-search-wrapper">
    <button ref="triggerRef" class="search-trigger" aria-label="Open search" @click="openSearch">
      <span aria-hidden="true">🔍</span>
    </button>

    <Teleport to="body">
      <Transition name="search-overlay">
        <div v-if="isOpen" class="search-overlay" @click.self="closeSearch">
          <div ref="modalRef" class="search-modal" role="dialog" aria-modal="true" aria-label="Search">
            <div class="search-input-wrapper">
              <span class="search-icon" aria-hidden="true">🔍</span>
              <input
                ref="searchInputRef"
                v-model="query"
                type="text"
                :placeholder="$t('search.placeholder')"
                :aria-label="$t('search.ariaLabel')"
                class="search-input"
                @input="onInput"
                @keydown="onKeydown"
              />
              <kbd class="search-shortcut">ESC</kbd>
            </div>

            <div v-if="query.length >= 2" class="search-results" role="listbox" :aria-label="$t('search.ariaLabel')">
              <div v-if="results.length === 0" class="no-results">
                {{ $t('search.noResults') }}
              </div>

              <template v-for="group in groupedResults" :key="group.category">
                <div class="result-category" role="presentation">{{ $t('search.' + group.category) }}</div>
                <div
                  v-for="(item, idx) in group.items"
                  :key="item.id"
                  class="result-item"
                  :class="{ 'result-active': flatIndex(group, idx) === highlightedIndex }"
                  role="option"
                  :aria-selected="flatIndex(group, idx) === highlightedIndex"
                  tabindex="-1"
                  @click="navigateTo(item)"
                  @keydown.enter.prevent="navigateTo(item)"
                  @keydown.space.prevent="navigateTo(item)"
                  @mouseenter="highlightedIndex = flatIndex(group, idx)"
                >
                  <span class="result-icon" aria-hidden="true">{{ item.icon }}</span>
                  <div class="result-text">
                    <div class="result-name">{{ item.name }}</div>
                    <div v-if="item.detail" class="result-detail">{{ item.detail }}</div>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useWeatherStore } from '../stores/weatherStore'
import { storeToRefs } from 'pinia'

interface SearchResult {
  id: string
  name: string
  detail?: string
  icon: string
  category: 'airports' | 'earthquakes' | 'hurricanes' | 'groundStops' | 'volcanicAsh' | 'lightning'
  route: { name: string; query?: Record<string, string> }
}

const router = useRouter()
const store = useWeatherStore()
const { airports, earthquakes, hurricanes, groundStops, volcanicAsh } = storeToRefs(store)

const isOpen = ref(false)
const query = ref('')
const highlightedIndex = ref(-1)
const searchInputRef = ref<HTMLInputElement | null>(null)
const modalRef = ref<HTMLElement | null>(null)
const triggerRef = ref<HTMLElement | null>(null)

const results = computed<SearchResult[]>(() => {
  if (query.value.length < 2) return []
  const q = query.value.toLowerCase()
  const items: SearchResult[] = []

  for (const apt of airports.value) {
    if (items.length >= 20) break
    const code = apt.airportCode || ''
    if (code.toLowerCase().includes(q) || apt.name?.toLowerCase().includes(q)) {
      items.push({
        id: `apt-${apt.id}`,
        name: `${code} - ${apt.name}`,
        detail: apt.state || apt.country,
        icon: '✈️',
        category: 'airports',
        route: { name: 'forecasts', query: { locationId: String(apt.id) } },
      })
    }
  }

  for (const eq of earthquakes.value) {
    if (items.length >= 40) break
    if (eq.place?.toLowerCase().includes(q)) {
      items.push({
        id: `eq-${eq.id}`,
        name: `M${eq.magnitude} - ${eq.place}`,
        detail: eq.alert ? `Alert: ${eq.alert}` : undefined,
        icon: '🌍',
        category: 'earthquakes',
        route: { name: 'earthquakes' },
      })
    }
  }

  for (const h of hurricanes.value) {
    if (items.length >= 45) break
    if (h.stormName?.toLowerCase().includes(q) || h.stormId?.toLowerCase().includes(q)) {
      items.push({
        id: `hur-${h.id}`,
        name: h.stormName || h.stormId,
        detail: h.category != null ? `Category ${h.category}` : undefined,
        icon: '🌀',
        category: 'hurricanes',
        route: { name: 'hurricanes' },
      })
    }
  }

  for (const gs of groundStops.value) {
    if (items.length >= 50) break
    if (gs.airportCode?.toLowerCase().includes(q) || gs.airportName?.toLowerCase().includes(q)) {
      items.push({
        id: `gs-${gs.id}`,
        name: `${gs.airportCode} - ${gs.programType}`,
        detail: gs.reason || undefined,
        icon: '🛑',
        category: 'groundStops',
        route: { name: 'ground-stops' },
      })
    }
  }

  for (const va of volcanicAsh.value) {
    if (items.length >= 55) break
    const name = va.volcanoName || va.firName || ''
    if (name.toLowerCase().includes(q) || va.hazard?.toLowerCase().includes(q)) {
      items.push({
        id: `va-${va.id}`,
        name: name || 'Volcanic Ash Advisory',
        detail: va.firName || undefined,
        icon: '🌋',
        category: 'volcanicAsh',
        route: { name: 'volcanic-ash' },
      })
    }
  }

  return items
})

const groupedResults = computed(() => {
  const groups: { category: string; items: SearchResult[] }[] = []
  const categories = ['airports', 'earthquakes', 'hurricanes', 'groundStops', 'volcanicAsh', 'lightning'] as const

  for (const cat of categories) {
    const items = results.value.filter((r) => r.category === cat)
    if (items.length > 0) {
      groups.push({ category: cat, items })
    }
  }

  return groups
})

function flatIndex(group: { category: string; items: SearchResult[] }, idx: number): number {
  let offset = 0
  for (const g of groupedResults.value) {
    if (g.category === group.category) return offset + idx
    offset += g.items.length
  }
  return offset + idx
}

function openSearch() {
  isOpen.value = true
  query.value = ''
  highlightedIndex.value = -1
  nextTick(() => searchInputRef.value?.focus())
}

function closeSearch() {
  isOpen.value = false
  query.value = ''
  nextTick(() => triggerRef.value?.focus())
}

function onInput() {
  highlightedIndex.value = -1
}

function onKeydown(e: KeyboardEvent) {
  const total = results.value.length
  switch (e.key) {
    case 'ArrowDown':
      e.preventDefault()
      highlightedIndex.value = Math.min(highlightedIndex.value + 1, total - 1)
      break
    case 'ArrowUp':
      e.preventDefault()
      highlightedIndex.value = Math.max(highlightedIndex.value - 1, 0)
      break
    case 'Enter':
      e.preventDefault()
      if (highlightedIndex.value >= 0 && highlightedIndex.value < total) {
        navigateTo(results.value[highlightedIndex.value])
      }
      break
    case 'Escape':
      e.preventDefault()
      closeSearch()
      break
  }
}

function navigateTo(item: SearchResult) {
  closeSearch()
  router.push(item.route)
}

function onGlobalKeydown(e: KeyboardEvent) {
  if (e.key === '/' && !isOpen.value && !(e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement || e.target instanceof HTMLSelectElement)) {
    e.preventDefault()
    openSearch()
  }
}

function trapFocus(e: KeyboardEvent) {
  if (e.key !== 'Tab' || !modalRef.value) return
  const focusable = modalRef.value.querySelectorAll<HTMLElement>(
    'input, button, [tabindex]:not([tabindex="-1"])',
  )
  if (focusable.length === 0) return
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (e.shiftKey && document.activeElement === first) {
    e.preventDefault()
    last.focus()
  } else if (!e.shiftKey && document.activeElement === last) {
    e.preventDefault()
    first.focus()
  }
}

onMounted(() => {
  document.addEventListener('keydown', onGlobalKeydown)
  document.addEventListener('keydown', trapFocus)
  // Pre-load data for search
  store.fetchAirports()
})

onUnmounted(() => {
  document.removeEventListener('keydown', onGlobalKeydown)
  document.removeEventListener('keydown', trapFocus)
})
</script>

<style scoped>
.search-trigger {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  padding: 0;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
  flex-shrink: 0;
}

.search-trigger:hover {
  background: rgba(255, 255, 255, 0.35);
}

.search-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  z-index: 8000;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 80px;
}

.search-modal {
  background: var(--bg-card, white);
  border-radius: 12px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.3);
  width: 560px;
  max-width: 90vw;
  max-height: 70vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-light, #eee);
}

.search-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 16px;
  background: transparent;
  color: var(--text-primary, #333);
}

.search-shortcut {
  padding: 2px 8px;
  border: 1px solid var(--border-color, #ddd);
  border-radius: 4px;
  font-size: 11px;
  color: var(--text-secondary, #999);
  font-family: monospace;
}

.search-results {
  overflow-y: auto;
  padding: 8px 0;
}

.no-results {
  padding: 20px;
  text-align: center;
  color: var(--text-secondary, #999);
  font-size: 14px;
}

.result-category {
  padding: 8px 20px 4px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-secondary, #999);
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 20px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.result-item:hover,
.result-active {
  background: var(--bg-code, #f5f5f5);
}

.result-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.result-name {
  font-weight: 500;
  font-size: 14px;
  color: var(--text-primary, #333);
}

.result-detail {
  font-size: 12px;
  color: var(--text-secondary, #666);
  margin-top: 1px;
}

.search-overlay-enter-active,
.search-overlay-leave-active {
  transition: opacity 0.2s ease;
}

.search-overlay-enter-from,
.search-overlay-leave-to {
  opacity: 0;
}
</style>
