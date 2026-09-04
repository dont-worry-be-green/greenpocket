<script setup>
/*
 * 그린포켓 · 버튼
 *   variant : primary(채움) | pill(연한 배경) | wide(카드 안 전체폭) | ghost
 *   size    : cta(48) | wide(40) | pill(28)
 * 화면 하단 CTA 는 variant="primary" size="cta" 하나만 쓴다.
 */
const props = defineProps({
  variant: { type: String, default: 'primary' },
  size: { type: String, default: 'cta' },
  disabled: { type: Boolean, default: false },
})
defineEmits(['click'])

const VARIANT = {
  primary: 'bg-primary text-on-primary enabled:active:bg-primary-pressed',
  pill: 'bg-primary-bg text-primary-on-soft',
  wide: 'w-full bg-primary-bg text-primary-on-soft',
  ghost: 'bg-transparent text-primary-on-soft',
}
const SIZE = {
  cta: 'h-(--gp-cta-h) w-full rounded-md text-button',
  wide: 'h-(--gp-wbtn-h) rounded-sm text-body-strong',
  pill: 'h-(--gp-pill-h) rounded-full px-3.5 text-label',
}

const variantClass = () => VARIANT[props.variant] ?? VARIANT.primary
const sizeClass = () => SIZE[props.size] ?? SIZE.cta
</script>

<template>
  <button
    type="button"
    :disabled="disabled"
    class="ease-standard inline-flex cursor-pointer items-center justify-center border-0 font-semibold transition-colors duration-140 active:scale-[0.985] disabled:cursor-not-allowed disabled:bg-disabled-bg disabled:text-disabled-text"
    :class="[variantClass(), sizeClass()]"
    @click="$emit('click', $event)"
  >
    <slot />
  </button>
</template>
