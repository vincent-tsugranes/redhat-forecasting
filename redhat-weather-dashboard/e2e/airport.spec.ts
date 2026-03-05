import { test, expect } from '@playwright/test'

test.describe('Airport Weather', () => {
  test('airport search input is visible', async ({ page }) => {
    await page.goto('/airports')
    const searchInput = page.locator('#airport-search')
    await expect(searchInput).toBeVisible()
    await expect(searchInput).toHaveAttribute('role', 'combobox')
  })

  test('typing in search shows filtered airport results', async ({ page }) => {
    await page.goto('/airports')
    // Wait for all airport data to load (multiple paginated requests)
    await page.waitForLoadState('networkidle')
    const searchInput = page.locator('#airport-search')
    await expect(searchInput).toBeVisible()
    // Type a search query
    await searchInput.fill('JFK')
    // Wait for search results dropdown
    await expect(page.locator('#airport-listbox')).toBeVisible({ timeout: 15000 })
    // Should have at least one result
    await expect(page.locator('.search-result-item').first()).toBeVisible()
  })

  test('selecting an airport shows weather data or no-data hint', async ({ page }) => {
    await page.goto('/airports')
    // Wait for all airport data to load (multiple paginated requests)
    await page.waitForLoadState('networkidle')
    const searchInput = page.locator('#airport-search')
    await expect(searchInput).toBeVisible()
    // Type a common airport code
    await searchInput.fill('JFK')
    // Wait for results and click the first one
    await expect(page.locator('.search-result-item').first()).toBeVisible({ timeout: 15000 })
    await page.locator('.search-result-item').first().click()
    // Verify weather data or "no data" message renders
    await expect(
      page
        .locator('.weather-report')
        .first()
        .or(page.getByText('No weather data available for this airport yet.')),
    ).toBeVisible({ timeout: 10000 })
  })
})
