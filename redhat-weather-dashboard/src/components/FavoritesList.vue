<template>
  <div v-if="favorites.length > 0" class="favorites-section">
    <h2><span aria-hidden="true">★</span> {{ $t('favorites.title') }}</h2>
    <div class="favorites-chips">
      <div v-for="fav in favorites" :key="fav.id" class="favorite-chip">
        <router-link :to="{ name: 'forecasts', query: { locationId: fav.id } }" class="chip-link">
          {{ fav.name }}<span v-if="fav.state" class="chip-state">, {{ fav.state }}</span>
        </router-link>
        <button
          class="chip-remove"
          :aria-label="$t('favorites.removeNamed', { name: fav.name })"
          @click="removeFavorite(fav.id)"
        >
          &times;
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useFavorites } from '../composables/useFavorites'

const { favorites, removeFavorite } = useFavorites()
</script>

<style scoped>
.favorites-section {
  margin-bottom: 20px;
}

.favorites-section h2 {
  font-size: 1.3rem;
  margin-bottom: 12px;
}

.favorites-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.favorite-chip {
  display: inline-flex;
  align-items: center;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-color, #ddd);
  border-radius: 20px;
  padding: 6px 12px;
  font-size: 14px;
  transition: box-shadow 0.2s;
}

.favorite-chip:hover {
  box-shadow: 0 2px 8px var(--shadow, rgba(0, 0, 0, 0.1));
}

.chip-link {
  color: var(--text-primary, #333);
  text-decoration: none;
}

.chip-link:hover {
  color: var(--accent);
}

.chip-state {
  color: var(--text-secondary, #666);
}

.chip-remove {
  background: none;
  border: none;
  font-size: 1.1rem;
  cursor: pointer;
  padding: 0 0 0 8px;
  line-height: 1;
  color: var(--text-muted, #999);
}

.chip-remove:hover {
  color: var(--error-text, #c00);
  background: none;
}
</style>
