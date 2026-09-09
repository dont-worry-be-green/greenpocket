<script setup>
/*
 * 그린포켓 · 카드
 * 화면의 기본 담는 그릇. 모서리 24(rounded-card) + 2겹 그림자(shadow-card).
 * v2 는 그림자 금지였는데, 배경(#F2F7F4)과 카드(#FFF)의 면 대비 1.083 만으로는 카드가 안 보인다는
 * 팀 피드백이 있어 v3 에서 면 대비 위에 그림자를 얹었다. 둘 중 하나만 쓰면 안 된다.
 *   tone : default(흰) | sub(회색) | estimated(예상값) | confirmed(확정된 돈)
 * estimated 와 confirmed 는 기능명세서 COM-06 의 예상/확인 구분이다.
 * **색만으로 구분하지 않는다.** 라벨(GpTag)을 반드시 함께 넣는다.
 */
import GpTag from './GpTag.vue'

const props = defineProps({
  title: { type: String, default: '' },
  caption: { type: String, default: '' }, // 제목 밑 한 줄 설명
  badge: { type: String, default: '' }, // 제목 옆 회색 배지 (기간 등)
  tone: { type: String, default: 'default' },
})

const TONE = {
  default: 'bg-surface',
  sub: 'bg-surface-sub',
  estimated: 'bg-estimated-bg',
  confirmed: 'bg-confirmed-bg',
}
const toneClass = () => TONE[props.tone] ?? TONE.default
</script>

<template>
  <section class="rounded-card shadow-card p-(--gp-card-pad)" :class="toneClass()">
    <header v-if="title || $slots.action" class="mb-3 flex items-center justify-between">
      <div class="flex items-center gap-2">
        <h2 v-if="title" class="text-section tracking-display m-0">{{ title }}</h2>
        <GpTag v-if="badge">{{ badge }}</GpTag>
      </div>
      <slot name="action" />
    </header>
    <p v-if="caption" class="text-caption text-muted mt-0 mb-3">{{ caption }}</p>
    <slot />
  </section>
</template>
