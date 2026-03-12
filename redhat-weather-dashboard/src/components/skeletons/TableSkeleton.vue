<template>
  <div class="card skeleton-card">
    <div class="skeleton-table">
      <div class="skeleton-thead">
        <div v-for="i in 5" :key="'h'+i" class="skeleton-th"></div>
      </div>
      <div v-for="r in 4" :key="r" class="skeleton-tr">
        <div v-for="c in 5" :key="c" class="skeleton-td"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-card {
  animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-thead {
  display: flex;
  gap: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--bg-code, #e0e0e0);
}

.skeleton-th {
  flex: 1;
  height: 14px;
  background: var(--bg-code, #e0e0e0);
  border-radius: 4px;
}

.skeleton-tr {
  display: flex;
  gap: 12px;
  padding: 6px 0;
}

.skeleton-td {
  flex: 1;
  height: 16px;
  background: var(--bg-code, #e0e0e0);
  border-radius: 4px;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
