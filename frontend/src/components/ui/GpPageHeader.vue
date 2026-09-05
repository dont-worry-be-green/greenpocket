<script setup>
/*
 * 그린포켓 · 탭 루트 화면 헤더 (기능명세서 COM-02)
 * 제목 한 줄 + 설명 한 줄. 시안(연동전.png)의 "Green What-if" 자리다.
 *
 * 하위 화면(고지서 상세·목표 정하기·출금 등)은 이게 아니라 GpBackHeader 를 쓴다.
 */
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  eyebrow: { type: String, default: '' },
})
</script>

<template>
  <header class="flex items-start justify-between gap-3 px-(--gp-gutter) pt-5 pb-4">
    <div class="min-w-0">
      <p v-if="eyebrow" class="text-body-sm text-muted mt-0 mb-1">{{ eyebrow }}</p>
      <h1 class="text-title tracking-title text-ink m-0">{{ title }}</h1>
      <p v-if="subtitle" class="text-caption text-muted mt-1 mb-0">{{ subtitle }}</p>
    </div>
    <!-- 제목 오른쪽 보조 액션(새로고침 등). 없으면 자리를 차지하지 않는다 -->
    <slot name="action" />
  </header>
</template>
