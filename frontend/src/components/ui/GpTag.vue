<script setup>
/*
 * 그린포켓 · 배지
 * 수치·상태를 다는 작은 라벨. 알약이 아니라 rounded-xs 사각형이다.
 *   tone : sub(회색) | primary | positive | confirmed | estimated | negative
 *
 * 기능명세서 COM-06 — 예상/확인/입금/지급 완료는 **색만으로 구분하지 않고**
 * 반드시 텍스트 라벨을 넣는다. 이 컴포넌트가 그 텍스트를 담는 자리다.
 */
const props = defineProps({
  tone: { type: String, default: 'sub' },
  small: { type: Boolean, default: false },
})

const TONE = {
  sub: 'bg-surface-sub text-muted',
  primary: 'bg-primary-bg text-primary-on-soft',
  positive: 'bg-positive-bg text-on-positive',
  confirmed: 'bg-confirmed-bg text-on-confirmed',
  negative: 'bg-negative-bg text-negative',
  estimated:
    'bg-transparent text-estimated shadow-[inset_0_0_0_1.5px_var(--color-estimated-border)]',
}
const toneClass = () => TONE[props.tone] ?? TONE.sub
</script>

<template>
  <span
    class="inline-flex flex-none items-center rounded-xs font-semibold"
    :class="[
      toneClass(),
      small ? 'h-(--gp-tag-sm-h) px-1.5 text-caption-sm' : 'h-(--gp-tag-h) px-2 text-label',
    ]"
  >
    <slot />
  </span>
</template>
