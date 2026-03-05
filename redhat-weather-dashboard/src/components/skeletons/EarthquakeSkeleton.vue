<template>
  <div class="skeleton-container">
    <div v-for="i in 4" :key="i" class="card skeleton-card">
      <div class="skeleton-header">
        <div class="skeleton-badge"></div>
        <div class="skeleton-line skeleton-title"></div>
      </div>
      <div class="skeleton-grid">
        <div class="skeleton-line"></div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-card {
  animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.skeleton-badge {
  width: 50px;
  height: 28px;
  background: var(--bg-code, #e0e0e0);
  border-radius: 6px;
}

.skeleton-line {
  height: 16px;
  background: var(--bg-code, #e0e0e0);
  border-radius: 4px;
}

.skeleton-title {
  flex: 1;
  height: 20px;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
