<template>
  <div class="skeleton-hurricane" aria-hidden="true">
    <div class="skeleton-card">
      <SkeletonLoader width="100%" height="300px" border-radius="8px" />
    </div>
    <div v-for="i in 2" :key="i" class="skeleton-card">
      <div class="skeleton-header">
        <SkeletonLoader width="40%" height="24px" border-radius="4px" />
        <SkeletonLoader width="80px" height="24px" border-radius="4px" />
      </div>
      <div class="skeleton-info-grid">
        <div v-for="j in 4" :key="j" class="skeleton-info-item">
          <SkeletonLoader width="60%" height="14px" border-radius="4px" />
          <SkeletonLoader width="40%" height="18px" border-radius="4px" />
        </div>
      </div>
      <SkeletonLoader width="50%" height="14px" border-radius="4px" />
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

.skeleton-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 2px solid var(--border-light, #eee);
}

.skeleton-info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.skeleton-info-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
</style>
