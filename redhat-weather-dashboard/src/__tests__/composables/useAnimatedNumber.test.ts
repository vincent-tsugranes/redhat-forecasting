import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { ref, nextTick } from 'vue'
import { useAnimatedNumber } from '../../composables/useAnimatedNumber'

describe('useAnimatedNumber', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    // Mock requestAnimationFrame
    let frameId = 0
    vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
      frameId++
      setTimeout(() => cb(performance.now()), 16)
      return frameId
    })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('starts at 0', () => {
    const target = ref(0)
    const display = useAnimatedNumber(target)
    expect(display.value).toBe(0)
  })

  it('immediately sets value when diff is 0', () => {
    const target = ref(0)
    const display = useAnimatedNumber(target)
    expect(display.value).toBe(0)
  })

  it('animates towards target value', async () => {
    const target = ref(100)
    const display = useAnimatedNumber(target, 100)

    // Advance past animation duration
    vi.advanceTimersByTime(200)

    expect(display.value).toBe(100)
  })

  it('returns a ref', () => {
    const target = ref(50)
    const display = useAnimatedNumber(target)
    expect(typeof display.value).toBe('number')
  })
})
