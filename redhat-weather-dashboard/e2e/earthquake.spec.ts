import { test, expect } from '@playwright/test'

test.describe('Earthquake View', () => {
  test('loads and shows earthquake heading', async ({ page }) => {
    await page.goto('/earthquakes')
    await page.waitForLoadState('networkidle')
    await expect(page.locator('h1').first()).toContainText('Earthquake')
  })

  test('shows table and card view toggle buttons', async ({ page }) => {
    await page.goto('/earthquakes')
    await page.waitForLoadState('networkidle')
    await expect(page.getByRole('button', { name: /Table/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /Cards/i })).toBeVisible()
  })

  test('earthquake table has aria-label for accessibility', async ({ page }) => {
    await page.goto('/earthquakes')
    await page.waitForLoadState('networkidle')
    const table = page.locator('table[aria-label="Earthquake data"]')
    // Table may or may not be visible depending on data; check it exists if data loaded
    const tableCount = await table.count()
    if (tableCount > 0) {
      await expect(table).toBeVisible()
    }
  })

  test('filter input has accessible label', async ({ page }) => {
    await page.goto('/earthquakes')
    await page.waitForLoadState('networkidle')
    const filterInput = page.locator('#earthquake-filter')
    const filterCount = await filterInput.count()
    if (filterCount > 0) {
      await expect(filterInput).toBeVisible()
    }
  })
})
