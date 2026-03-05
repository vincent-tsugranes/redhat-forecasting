import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useFavorites } from '../../composables/useFavorites'

// Provide a working localStorage mock for this test
const store: Record<string, string> = {}
vi.stubGlobal('localStorage', {
  getItem: (key: string) => store[key] ?? null,
  setItem: (key: string, value: string) => { store[key] = value },
  removeItem: (key: string) => { delete store[key] },
})

describe('useFavorites', () => {
  beforeEach(() => {
    Object.keys(store).forEach((k) => delete store[k])
    const { favorites } = useFavorites()
    favorites.value = []
  })

  it('starts with empty favorites', () => {
    const { favorites } = useFavorites()
    expect(favorites.value).toEqual([])
  })

  it('adds a favorite location', () => {
    const { addFavorite, favorites } = useFavorites()
    addFavorite({ id: 1, name: 'Test City', state: 'TX' })
    expect(favorites.value).toHaveLength(1)
    expect(favorites.value[0].name).toBe('Test City')
  })

  it('prevents duplicate favorites', () => {
    const { addFavorite, favorites } = useFavorites()
    addFavorite({ id: 1, name: 'Test City', state: 'TX' })
    addFavorite({ id: 1, name: 'Test City', state: 'TX' })
    expect(favorites.value).toHaveLength(1)
  })

  it('removes a favorite by id', () => {
    const { addFavorite, removeFavorite, favorites } = useFavorites()
    addFavorite({ id: 1, name: 'City A' })
    addFavorite({ id: 2, name: 'City B' })
    removeFavorite(1)
    expect(favorites.value).toHaveLength(1)
    expect(favorites.value[0].id).toBe(2)
  })

  it('checks if a location is a favorite', () => {
    const { addFavorite, isFavorite } = useFavorites()
    addFavorite({ id: 1, name: 'City A' })
    expect(isFavorite(1)).toBe(true)
    expect(isFavorite(99)).toBe(false)
  })

  it('isFavorite returns false after removal', () => {
    const { addFavorite, removeFavorite, isFavorite } = useFavorites()
    addFavorite({ id: 1, name: 'City A' })
    removeFavorite(1)
    expect(isFavorite(1)).toBe(false)
  })
})
