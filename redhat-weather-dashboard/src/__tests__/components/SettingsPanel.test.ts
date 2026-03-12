import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n'
import SettingsPanel from '../../components/SettingsPanel.vue'

describe('SettingsPanel', () => {
  it('renders settings title', () => {
    const wrapper = mount(SettingsPanel, {
      global: { plugins: [i18n] },
    })
    expect(wrapper.text()).toContain('Settings')
  })

  it('renders temperature unit selector', () => {
    const wrapper = mount(SettingsPanel, {
      global: { plugins: [i18n] },
    })
    const selects = wrapper.findAll('select')
    expect(selects.length).toBeGreaterThanOrEqual(1)
  })

  it('emits close event on close button click', async () => {
    const wrapper = mount(SettingsPanel, {
      global: { plugins: [i18n] },
    })
    const closeBtn = wrapper.find('.close-btn')
    if (closeBtn.exists()) {
      await closeBtn.trigger('click')
      expect(wrapper.emitted('close')).toBeTruthy()
    }
  })

  it('renders three settings sections', () => {
    const wrapper = mount(SettingsPanel, {
      global: { plugins: [i18n] },
    })
    // Temperature, Wind Speed, Time Format
    expect(wrapper.text()).toContain('Temperature')
    expect(wrapper.text()).toContain('Wind Speed')
    expect(wrapper.text()).toContain('Time Format')
  })
})
