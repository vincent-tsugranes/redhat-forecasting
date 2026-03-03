<template>
  <div class="skeleton-forecast" aria-hidden="true">
    <div class="skeleton-card">
      <SkeletonLoader width="100%" height="44px" border-radius="8px" />
    </div>
    <div class="skeleton-card">
      <SkeletonLoader width="30%" height="24px" border-radius="4px" />
      <SkeletonLoader width="100%" height="200px" border-radius="8px" />
    </div>
    <div class="skeleton-card">
      <SkeletonLoader width="25%" height="24px" border-radius="4px" />
      <div v-for="i in 3" :key="i" class="skeleton-forecast-item">
        <SkeletonLoader width="40%" height="18px" border-radius="4px" />
        <div class="skeleton-row">
          <SkeletonLoader width="20%" height="16px" border-radius="4px" />
          <SkeletonLoader width="20%" height="16px" border-radius="4px" />
          <SkeletonLoader width="20%" height="16px" border-radius="4px" />
        </div>
        <SkeletonLoader width="70%" height="14px" border-radius="4px" />
      </div>
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

.skeleton-forecast-item {
  border: 1px solid var(--border-light, #eee);
  border-radius: 8px;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skeleton-row {
  display: flex;
  gap: 20px;
}
</style>
