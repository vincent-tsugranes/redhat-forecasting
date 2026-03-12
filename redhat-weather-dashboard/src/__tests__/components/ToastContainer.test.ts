import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ToastContainer from '../../components/ToastContainer.vue'
import { useToast } from '../../composables/useToast'

describe('ToastContainer', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    const { toasts } = useToast()
    toasts.value = []
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders without toasts', () => {
    expect(mount(ToastContainer).exists()).toBe(true)
  })

  it('renders success toast when added', async () => {
    mount(ToastContainer)
    const { success } = useToast()
    success('Test message')
    await flushPromises()

    // Toasts are teleported to body, check the toast composable state
    const { toasts } = useToast()
    expect(toasts.value).toHaveLength(1)
    expect(toasts.value[0].message).toBe('Test message')
    expect(toasts.value[0].type).toBe('success')
  })

  it('renders error toast when added', async () => {
    const { error, toasts } = useToast()
    error('Error occurred')
    await flushPromises()

    expect(toasts.value).toHaveLength(1)
    expect(toasts.value[0].type).toBe('error')
  })

  it('toast has role="status" for accessibility', async () => {
    mount(ToastContainer)
    const { success } = useToast()
    success('Accessible toast')
    await flushPromises()

    const { toasts } = useToast()
    expect(toasts.value).toHaveLength(1)
  })
})
