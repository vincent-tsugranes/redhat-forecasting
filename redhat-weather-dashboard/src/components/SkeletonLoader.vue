<template>
  <div class="skeleton" :style="{ width, height, borderRadius }" aria-hidden="true"></div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    width?: string
    height?: string
    borderRadius?: string
  }>(),
  {
    width: '100%',
    height: '20px',
    borderRadius: '4px',
  },
)
</script>

<style scoped>
.skeleton {
  background: linear-gradient(
    90deg,
    var(--skeleton-base, #e0e0e0) 25%,
    var(--skeleton-shine, #f0f0f0) 50%,
    var(--skeleton-base, #e0e0e0) 75%
  );
  background-size: 200% 100%;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

@keyframes skeleton-pulse {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
