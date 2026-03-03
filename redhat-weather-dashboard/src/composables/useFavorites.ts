import { ref, watch } from 'vue'

export interface FavoriteLocation {
  id: number
  name: string
  state?: string
}

const STORAGE_KEY = 'weather-favorites'

function loadFavorites(): FavoriteLocation[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : []
  } catch {
    return []
  }
}

const favorites = ref<FavoriteLocation[]>(loadFavorites())

watch(
  favorites,
  (val) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(val))
  },
  { deep: true },
)

export function useFavorites() {
  function addFavorite(location: FavoriteLocation) {
    if (!favorites.value.some((f) => f.id === location.id)) {
      favorites.value = [...favorites.value, location]
    }
  }

  function removeFavorite(id: number) {
    favorites.value = favorites.value.filter((f) => f.id !== id)
  }

  function isFavorite(id: number): boolean {
    return favorites.value.some((f) => f.id === id)
  }

  return {
    favorites,
    addFavorite,
    removeFavorite,
    isFavorite,
  }
}
