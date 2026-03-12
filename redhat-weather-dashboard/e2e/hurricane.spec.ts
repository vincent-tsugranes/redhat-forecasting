import { test, expect } from '@playwright/test'

test.describe('Hurricane View', () => {
  test('loads and shows hurricane heading', async ({ page }) => {
    await page.goto('/hurricanes')
    await page.waitForLoadState('networkidle')
    await expect(page.locator('h1').first()).toBeVisible()
  })

  test('shows hurricane tracking content', async ({ page }) => {
    await page.goto('/hurricanes')
    await page.waitForLoadState('networkidle')
    // Should show either storm data or empty state
    const content = page.locator('.container')
    await expect(content).toBeVisible()
  })
})
