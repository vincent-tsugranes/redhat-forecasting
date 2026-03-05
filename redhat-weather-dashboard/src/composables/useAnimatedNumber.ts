import { ref, watch, type Ref } from 'vue'

export function useAnimatedNumber(target: Ref<number>, duration = 800): Ref<number> {
  const displayValue = ref(0)

  function easeOutCubic(t: number): number {
    return 1 - Math.pow(1 - t, 3)
  }

  watch(
    target,
    (newVal, oldVal) => {
      const start = oldVal ?? 0
      const diff = newVal - start
      if (diff === 0) {
        displayValue.value = newVal
        return
      }

      const startTime = performance.now()

      function animate(currentTime: number) {
        const elapsed = currentTime - startTime
        const progress = Math.min(elapsed / duration, 1)
        displayValue.value = Math.round(start + diff * easeOutCubic(progress))

        if (progress < 1) {
          requestAnimationFrame(animate)
        }
      }

      requestAnimationFrame(animate)
    },
    { immediate: true },
  )

  return displayValue
}
