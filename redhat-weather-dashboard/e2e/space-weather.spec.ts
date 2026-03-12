import { test, expect } from '@playwright/test'

test.describe('Space Weather View', () => {
  test('loads and shows space weather heading', async ({ page }) => {
    await page.goto('/space-weather')
    await page.waitForLoadState('networkidle')
    await expect(page.locator('h1').first()).toBeVisible()
  })

  test('shows space weather content', async ({ page }) => {
    await page.goto('/space-weather')
    await page.waitForLoadState('networkidle')
    const content = page.locator('.container')
    await expect(content).toBeVisible()
  })
})
