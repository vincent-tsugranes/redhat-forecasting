import { test, expect } from '@playwright/test'

test.describe('Map', () => {
  test('map renders with layer controls', async ({ page }) => {
    await page.goto('/map')
    // Map container should appear
    await expect(
      page.locator('[role="application"][aria-label="Unified weather map"]'),
    ).toBeVisible({ timeout: 15000 })
    // Layer controls with checkboxes
    await expect(page.locator('.layer-controls')).toBeVisible()
    await expect(page.locator('.layer-toggle')).toHaveCount(4)
  })

  test('map legend renders for active layers', async ({ page }) => {
    await page.goto('/map')
    await expect(page.locator('.map-legend')).toBeVisible({ timeout: 15000 })
    // By default airports, earthquakes, hurricanes are on (3 legend sections)
    await expect(page.locator('.legend-section')).toHaveCount(3)
  })
})
