<script setup>
/*
 * 그린포켓 · 하위 화면 헤더 (기능명세서 COM-02)
 * "하위 화면(고지서 등록·상세, 목표 정하기, 리포트, 출금 등)은 뒤로가기 헤더를 쓴다."
 *
 * 스크롤해도 붙어 있도록 sticky 다. 탭바가 없는 화면에서 쓴다.
 */
import IconChevronLeft from './icons/IconChevronLeft.vue'

defineProps({
  title: { type: String, default: '' },
})
defineEmits(['back'])
</script>

<template>
  <header class="bg-canvas sticky top-0 z-10 flex h-14 items-center gap-0.5 px-1">
    <button
      type="button"
      aria-label="뒤로 가기"
      class="text-ink flex size-(--gp-min-touch) shrink-0 cursor-pointer items-center justify-center rounded-full border-0 bg-transparent"
      @click="$emit('back')"
    >
      <IconChevronLeft :size="22" />
    </button>
    <h1 class="text-list-title text-ink m-0 truncate">{{ title }}</h1>
    <div class="ml-auto flex items-center">
      <!-- 오른쪽 보조 액션(닫기·저장 등) -->
      <slot name="action" />
    </div>
  </header>
</template>
