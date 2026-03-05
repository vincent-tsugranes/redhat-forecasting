<template>
  <div class="skeleton-grid">
    <div v-for="i in 4" :key="i" class="card skeleton-card">
      <div class="skeleton-line skeleton-title"></div>
      <div class="skeleton-block"></div>
      <div class="skeleton-line skeleton-subtitle"></div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.skeleton-card {
  animation: pulse 1.5s ease-in-out infinite;
  text-align: center;
}

.skeleton-line {
  height: 16px;
  background: var(--bg-code, #e0e0e0);
  border-radius: 4px;
  margin: 0 auto;
}

.skeleton-title {
  width: 60%;
  margin-bottom: 16px;
}

.skeleton-block {
  width: 80px;
  height: 40px;
  background: var(--bg-code, #e0e0e0);
  border-radius: 6px;
  margin: 0 auto 12px;
}

.skeleton-subtitle {
  width: 40%;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
