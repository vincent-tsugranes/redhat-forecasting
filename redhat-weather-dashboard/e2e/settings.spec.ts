import { test, expect } from '@playwright/test'

test.describe('Settings Panel', () => {
  test('opens settings panel on button click', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    const settingsBtn = page.locator('button[aria-label="Settings"]')
    await settingsBtn.click()
    // Settings panel should appear
    await expect(page.locator('.settings-panel, .settings-overlay')).toBeVisible({ timeout: 5000 })
  })
})
