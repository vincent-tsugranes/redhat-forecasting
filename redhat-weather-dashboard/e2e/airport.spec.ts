import { test, expect } from '@playwright/test'

test.describe('Airport Weather', () => {
  test('airport select loads with options', async ({ page }) => {
    await page.goto('/airports')
    const select = page.locator('#airport-select')
    await expect(select).toBeVisible()
    // Wait for airport options to load from the store
    await expect(select.locator('option')).not.toHaveCount(1, { timeout: 15000 })
  })

  test('selecting an airport shows weather data or no-data hint', async ({ page }) => {
    await page.goto('/airports')
    const select = page.locator('#airport-select')
    await expect(select).toBeVisible()
    // Wait for options to populate
    await page.waitForTimeout(2000)
    const options = select.locator('option:not([value=""])')
    const count = await options.count()
    if (count > 0) {
      const firstValue = await options.first().getAttribute('value')
      if (firstValue) {
        await select.selectOption(firstValue)
        // Wait for weather data or no-data hint
        await expect(
          page.locator('.metar-section').or(page.getByText('No weather data available for this airport yet.')),
        ).toBeVisible({ timeout: 10000 })
      }
    }
  })
})
