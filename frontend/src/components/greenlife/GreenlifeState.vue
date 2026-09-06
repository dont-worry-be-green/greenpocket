<script setup>
/*
 * 로딩·에러·빈 상태 래퍼 (COM-08). 무반응 화면이나 빈 화면을 남기지 않는다.
 *
 * `pocket/PocketState.vue` 와 거의 같지만 **일부러 복제했다.** 도메인 폴더를 넘어 import
 * 하지 않는 것이 규칙이고(frontend/AGENTS.md 4절), `ui/` 로 올리면 넷이 함께 쓰는 공용
 * 파일이 되어 스켈레톤 모양을 바꿀 때마다 다른 화면이 같이 흔들린다.
 */
import GpButton from '@/components/ui/GpButton.vue'

defineProps({
  loading: { type: Boolean, default: false },
  error: { type: Object, default: null },
  empty: { type: Boolean, default: false },
  emptyMessage: { type: String, default: '표시할 내용이 없어요.' },
})
defineEmits(['retry'])
</script>

<template>
  <div v-if="loading" class="space-y-3 py-2" aria-label="불러오는 중">
    <div class="bg-skeleton h-40 animate-pulse rounded-xl" />
    <div class="bg-skeleton h-56 animate-pulse rounded-lg" />
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
