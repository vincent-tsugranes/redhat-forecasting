import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import FreshnessBadge from '../../components/FreshnessBadge.vue'

describe('FreshnessBadge', () => {
  it('renders nothing when fetchedAt is not provided', () => {
    const wrapper = mount(FreshnessBadge, {
      props: { fetchedAt: '', dataType: 'forecast' },
    })
    expect(wrapper.find('.freshness-badge').exists()).toBe(false)
  })

  it('renders with freshness-fresh class for recent data', () => {
    const wrapper = mount(FreshnessBadge, {
      props: {
        fetchedAt: new Date().toISOString(),
        dataType: 'forecast',
      },
    })
    expect(wrapper.find('.freshness-fresh').exists()).toBe(true)
  })

  it('renders with freshness-stale class for old data', () => {
    const old = new Date(Date.now() - 120 * 60 * 1000).toISOString()
    const wrapper = mount(FreshnessBadge, {
      props: {
        fetchedAt: old,
        dataType: 'forecast',
      },
    })
    expect(wrapper.find('.freshness-stale').exists()).toBe(true)
  })

  it('has role="status" for a11y', () => {
    const wrapper = mount(FreshnessBadge, {
      props: {
        fetchedAt: new Date().toISOString(),
        dataType: 'forecast',
      },
    })
    expect(wrapper.find('[role="status"]').exists()).toBe(true)
  })

  it('has aria-label describing freshness status', () => {
    const wrapper = mount(FreshnessBadge, {
      props: {
        fetchedAt: new Date().toISOString(),
        dataType: 'forecast',
      },
    })
    const badge = wrapper.find('.freshness-badge')
    expect(badge.attributes('aria-label')).toContain('Data is current')
  })

  it('displays relative time text', () => {
    const wrapper = mount(FreshnessBadge, {
      props: {
        fetchedAt: new Date().toISOString(),
        dataType: 'airport',
      },
    })
    expect(wrapper.text()).toContain('just now')
  })
})
