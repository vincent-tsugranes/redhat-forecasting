<template>
  <button
    class="favorite-btn"
    :class="{ 'favorite-btn--active': active }"
    :aria-label="active ? $t('favorites.remove') : $t('favorites.add')"
    @click="$emit('toggle')"
  >
    <span aria-hidden="true">{{ active ? '★' : '☆' }}</span>
  </button>
</template>

<script setup lang="ts">
defineProps<{
  active: boolean
}>()

defineEmits<{
  toggle: []
}>()
</script>

<style scoped>
.favorite-btn {
  background: none;
  border: none;
  font-size: 1.4rem;
  cursor: pointer;
  padding: 4px 8px;
  line-height: 1;
  color: var(--text-muted, #999);
  transition: color 0.2s;
}

.favorite-btn:hover {
  color: #f5a623;
  background: none;
}

.favorite-btn--active {
  color: #f5a623;
}
</style>
