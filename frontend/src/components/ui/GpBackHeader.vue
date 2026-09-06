<script setup>
/*
 * 그린포켓 · 하위 화면 헤더 (기능명세서 COM-02)
 * "하위 화면(고지서 등록·상세, 목표 정하기, 리포트, 출금 등)은 뒤로가기 헤더를 쓴다."
 *
 * 스크롤해도 붙어 있도록 sticky 다. 탭바가 없는 화면에서 쓴다.
 *
 * `title` 을 비우면 뒤로가기만 남는다. 제목을 헤더 옆이 아니라 **본문 위 큰 제목**으로
 * 두는 화면(WF-07 · WF-10 시안)이 그렇게 쓴다 — `AppSubLayout` 의 `subtitle` 참고.
 *
 * `dismiss` 는 왼쪽 화살표 대신 **오른쪽 닫기(X)** 를 그린다. 되돌아가는 화면이 아니라
 * 한 번 보고 닫는 결과 화면(WF-11 마일리지 적립)이 그렇다 — 내는 이벤트는 `back` 그대로다.
 */
import IconChevronLeft from './icons/IconChevronLeft.vue'
import IconClose from './icons/IconClose.vue'

defineProps({
  title: { type: String, default: '' },
  centerTitle: { type: Boolean, default: false },
  // 뒤로가기(←) 대신 닫기(X). 위 주석 참고
  dismiss: { type: Boolean, default: false },
})
defineEmits(['back'])
</script>

<template>
  <header class="bg-canvas sticky top-0 z-10 flex h-14 items-center gap-0.5 px-1">
    <button
      v-if="!dismiss"
      type="button"
      aria-label="뒤로 가기"
      class="text-ink flex size-(--gp-min-touch) shrink-0 cursor-pointer items-center justify-center rounded-full border-0 bg-transparent"
      @click="$emit('back')"
    >
      <IconChevronLeft :size="22" />
    </button>
    <!-- 빈 제목이면 h1 자체를 그리지 않는다. 빈 헤딩은 화면 낭독기에서 잡음이 된다 -->
    <h1
      v-if="title"
      class="text-list-title text-ink m-0 truncate"
      :class="centerTitle ? 'pointer-events-none absolute inset-x-14 text-center' : ''"
    >
      {{ title }}
    </h1>
    <div class="ml-auto flex items-center">
      <!-- 오른쪽 보조 액션(저장 등) -->
      <slot name="action" />
      <button
        v-if="dismiss"
        type="button"
        aria-label="닫기"
        class="text-ink flex size-(--gp-min-touch) shrink-0 cursor-pointer items-center justify-center rounded-full border-0 bg-transparent"
        @click="$emit('back')"
      >
        <IconClose :size="22" />
      </button>
    </div>
  </header>
</template>
