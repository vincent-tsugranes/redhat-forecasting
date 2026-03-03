<template>
  <div class="skeleton-airport" aria-hidden="true">
    <div class="skeleton-card">
      <SkeletonLoader width="30%" height="24px" border-radius="4px" />
      <SkeletonLoader width="100%" height="44px" border-radius="8px" />
    </div>
    <div class="skeleton-card">
      <SkeletonLoader width="50%" height="24px" border-radius="4px" />
      <SkeletonLoader width="100%" height="60px" border-radius="4px" />
      <div class="skeleton-row">
        <SkeletonLoader width="60px" height="30px" border-radius="4px" />
        <SkeletonLoader width="15%" height="16px" border-radius="4px" />
        <SkeletonLoader width="15%" height="16px" border-radius="4px" />
        <SkeletonLoader width="15%" height="16px" border-radius="4px" />
      </div>
    </div>
    <div class="skeleton-card">
      <SkeletonLoader width="60%" height="24px" border-radius="4px" />
      <SkeletonLoader width="100%" height="60px" border-radius="4px" />
    </div>
  </div>
</template>

<script setup lang="ts">
import SkeletonLoader from '../SkeletonLoader.vue'
</script>

<style scoped>
.skeleton-card {
  background: var(--bg-card, white);
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px var(--shadow, rgba(0, 0, 0, 0.1));
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.skeleton-row {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}
</style>
