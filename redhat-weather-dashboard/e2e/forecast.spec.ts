import { test, expect } from '@playwright/test'

test.describe('Forecast View', () => {
  test('search input is visible and functional', async ({ page }) => {
    await page.goto('/forecasts')
    const searchInput = page.locator('#location-search')
    await expect(searchInput).toBeVisible()
    // Type a search query
    await searchInput.fill('New')
    // Wait for search results dropdown
    await expect(page.locator('#search-listbox')).toBeVisible({ timeout: 10000 })
  })

  test('selecting a location shows forecast or no-data message', async ({ page }) => {
    await page.goto('/forecasts')
    const searchInput = page.locator('#location-search')
    await expect(searchInput).toBeVisible()
    await searchInput.fill('New')
    // Wait for results and click the first one
    await expect(page.locator('.search-result-item').first()).toBeVisible({ timeout: 10000 })
    await page.locator('.search-result-item').first().click()
    // Verify forecast data or "no data" message renders
    await expect(
      page.locator('.forecast-item').first().or(page.getByText('No forecast data')),
    ).toBeVisible({ timeout: 15000 })
  })
})
