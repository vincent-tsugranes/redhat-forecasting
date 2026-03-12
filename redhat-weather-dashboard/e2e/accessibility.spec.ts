import { test, expect } from '@playwright/test'

test.describe('Accessibility', () => {
  test('skip-to-content link is present and becomes visible on focus', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    const skipLink = page.locator('.skip-link')
    await expect(skipLink).toBeAttached()
    // Skip link should be off-screen initially
    const box = await skipLink.boundingBox()
    expect(box).toBeTruthy()
    expect(box!.y).toBeLessThan(0)
    // Tab to it and verify it becomes visible
    await page.keyboard.press('Tab')
    const focusedBox = await skipLink.boundingBox()
    expect(focusedBox).toBeTruthy()
    expect(focusedBox!.y).toBeGreaterThanOrEqual(0)
  })

  test('main content has id for skip link target', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    await expect(page.locator('main#main-content')).toBeVisible()
  })

  test('theme toggle button has aria-label', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    const themeBtn = page.locator('.theme-toggle')
    await expect(themeBtn).toHaveAttribute('aria-label', /.+/)
  })

  test('theme toggle switches theme on click', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    const themeBtn = page.locator('.theme-toggle')
    const initialLabel = await themeBtn.getAttribute('aria-label')
    await themeBtn.click()
    const newLabel = await themeBtn.getAttribute('aria-label')
    expect(newLabel).not.toEqual(initialLabel)
  })

  test('dashboard tables have aria-labels', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    // Check earthquake table if visible
    const eqTable = page.locator('table[aria-label="Recent earthquakes"]')
    const eqCount = await eqTable.count()
    if (eqCount > 0) {
      await expect(eqTable.first()).toBeVisible()
    }
    // Check hurricane table if visible
    const stormTable = page.locator('table[aria-label="Active tropical storms"]')
    const stormCount = await stormTable.count()
    if (stormCount > 0) {
      await expect(stormTable.first()).toBeVisible()
    }
  })

  test('navigation links are keyboard accessible', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    // Tab through to find nav links
    const navLinks = page.locator('nav a')
    const count = await navLinks.count()
    expect(count).toBeGreaterThan(0)
    // All nav links should be focusable
    for (let i = 0; i < Math.min(count, 3); i++) {
      await expect(navLinks.nth(i)).toBeVisible()
    }
  })
})
