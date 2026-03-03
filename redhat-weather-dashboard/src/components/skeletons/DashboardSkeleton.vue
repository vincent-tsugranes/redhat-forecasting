<template>
  <div class="skeleton-dashboard" aria-hidden="true">
    <div class="skeleton-grid">
      <div v-for="i in 4" :key="i" class="skeleton-card">
        <SkeletonLoader width="60%" height="24px" border-radius="4px" />
        <SkeletonLoader width="80%" height="16px" border-radius="4px" />
        <SkeletonLoader width="40%" height="36px" border-radius="6px" />
      </div>
    </div>
    <div class="skeleton-card skeleton-map">
      <SkeletonLoader width="40%" height="24px" border-radius="4px" />
      <SkeletonLoader width="100%" height="300px" border-radius="8px" />
    </div>
  </div>
</template>

<script setup lang="ts">
import SkeletonLoader from '../SkeletonLoader.vue'
</script>

<style scoped>
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.skeleton-card {
  background: var(--bg-card, white);
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px var(--shadow, rgba(0, 0, 0, 0.1));
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-map {
  margin-top: 20px;
}
</style>
