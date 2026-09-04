<script setup>
import GpButton from '@/components/ui/GpButton.vue'

defineProps({
  loading: { type: Boolean, default: false },
  error: { type: Object, default: null },
  empty: { type: Boolean, default: false },
  emptyMessage: { type: String, default: '표시할 내역이 없어요.' },
})
defineEmits(['retry'])
</script>

<template>
  <div v-if="loading" class="space-y-3 py-2" aria-label="불러오는 중">
    <div class="bg-skeleton h-44 animate-pulse rounded-xl" />
    <div class="bg-skeleton h-28 animate-pulse rounded-lg" />
  </div>
  <div v-else-if="error" class="bg-surface rounded-lg p-5 text-center">
    <p class="text-body-strong text-ink mt-0 mb-1">정보를 불러오지 못했어요</p>
    <p class="text-caption text-muted mt-0 mb-4">{{ error.message }}</p>
    <GpButton variant="wide" size="wide" @click="$emit('retry')">다시 시도</GpButton>
  </div>
  <div v-else-if="empty" class="bg-surface rounded-lg px-5 py-10 text-center">
    <p class="text-body text-muted m-0">{{ emptyMessage }}</p>
  </div>
  <slot v-else />
</template>
