import { test, expect } from '@playwright/test'

test.describe('Dashboard', () => {
  test('loads and shows dashboard heading', async ({ page }) => {
    await page.goto('/')
    await expect(page.locator('h1')).toContainText('Weather Dashboard')
  })

  test('data status card loads and shows content', async ({ page }) => {
    await page.goto('/')
    await expect(page.locator('.status-card')).toBeVisible()
    // Wait for status content to load (loading spinner disappears)
    await expect(page.locator('.status-card .status-content')).toBeVisible({ timeout: 10000 })
  })

  test('data status card shows active data counts', async ({ page }) => {
    await page.goto('/')
    await expect(page.locator('.data-counts-section')).toBeVisible({ timeout: 10000 })
    await expect(page.locator('.data-counts-section h4')).toContainText('Active Data')
    // Should have 4 count items
    await expect(page.locator('.data-count-item')).toHaveCount(4)
  })
})
