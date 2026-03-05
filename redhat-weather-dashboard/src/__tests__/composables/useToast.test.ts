import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useToast } from '../../composables/useToast'

describe('useToast', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    const { toasts } = useToast()
    toasts.value = []
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('adds a success toast', () => {
    const { success, toasts } = useToast()
    success('Test success')
    expect(toasts.value).toHaveLength(1)
    expect(toasts.value[0].type).toBe('success')
    expect(toasts.value[0].message).toBe('Test success')
  })

  it('adds an error toast', () => {
    const { error, toasts } = useToast()
    error('Test error')
    expect(toasts.value).toHaveLength(1)
    expect(toasts.value[0].type).toBe('error')
    expect(toasts.value[0].message).toBe('Test error')
  })

  it('adds an info toast', () => {
    const { info, toasts } = useToast()
    info('Test info')
    expect(toasts.value).toHaveLength(1)
    expect(toasts.value[0].type).toBe('info')
  })

  it('auto-removes success toast after 3000ms', () => {
    const { success, toasts } = useToast()
    success('Will disappear')
    expect(toasts.value).toHaveLength(1)
    vi.advanceTimersByTime(3000)
    expect(toasts.value).toHaveLength(0)
  })

  it('auto-removes error toast after 5000ms', () => {
    const { error, toasts } = useToast()
    error('Will disappear slowly')
    expect(toasts.value).toHaveLength(1)
    vi.advanceTimersByTime(3000)
    expect(toasts.value).toHaveLength(1) // still there
    vi.advanceTimersByTime(2000)
    expect(toasts.value).toHaveLength(0) // gone after 5s
  })

  it('can add multiple toasts', () => {
    const { success, error, toasts } = useToast()
    success('First')
    error('Second')
    expect(toasts.value).toHaveLength(2)
  })

  it('assigns unique ids', () => {
    const { success, info, toasts } = useToast()
    success('A')
    info('B')
    expect(toasts.value[0].id).not.toBe(toasts.value[1].id)
  })
})
