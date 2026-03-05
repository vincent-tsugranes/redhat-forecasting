<template>
  <div class="map-view">
    <div class="map-header">
      <h1>{{ $t('map.title') }}</h1>
    </div>
    <div class="map-fullpage">
      <ErrorBoundary>
        <UnifiedMap />
      </ErrorBoundary>
    </div>
  </div>
</template>

<script setup lang="ts">
import UnifiedMap from '../components/UnifiedMap.vue'
import ErrorBoundary from '../components/ErrorBoundary.vue'
</script>

<style scoped>
.map-view {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 160px);
}

.map-header {
  padding: 16px 20px 8px;
}

.map-header h1 {
  margin: 0;
  color: #ee0000;
  font-size: 1.5rem;
}

.map-fullpage {
  flex: 1;
  padding: 0 20px 20px;
  min-height: 0;
}
</style>
