<script setup>
/*
 * 그린포켓 · 목표 구간 칩 (WF-04)
 * 에코마일리지 인센티브는 계단 함수라 연속 슬라이더를 쓰지 않는다.
 *
 * 구간은 서버가 준다 — `GET /eco/rounds/{roundId}/goal-form` 의 `tiers`.
 *   [{ tier: 'TIER_10', label: '10~15%', targetRate: 10.000, mileage: 30000 }]
 * **기본값을 두지 않는다.** 마일리지 금액은 제도 데이터라 프론트가 굳혀두면
 * 서버 값이 바뀌었을 때 화면만 틀린 숫자를 보여주게 된다 (frontend/AGENTS.md 9절).
 *
 * v-model 은 `tier` 코드 문자열이다. 서버의 `selectedTier` 와 같은 값이라 그대로 왕복한다.
 */
import { formatMileage } from '@/utils/format'

defineProps({
  modelValue: { type: String, default: null }, // 'TIER_10'
  tiers: { type: Array, required: true },
  label: { type: String, default: '감축 목표 구간' },
})
defineEmits(['update:modelValue'])
</script>

<template>
  <div class="mt-3 flex gap-2" role="radiogroup" :aria-label="label">
    <button
      v-for="t in tiers"
      :key="t.tier"
      type="button"
      role="radio"
      :aria-checked="t.tier === modelValue"
      class="ease-standard flex-1 cursor-pointer rounded-md border-0 px-1.5 pt-[11px] pb-3 text-center transition duration-140"
      :class="
        t.tier === modelValue
          ? 'bg-primary-bg shadow-[inset_0_0_0_2px_var(--color-primary)]'
          : 'bg-surface-sub'
      "
      @click="$emit('update:modelValue', t.tier)"
    >
      <span
        class="text-caption block font-semibold"
        :class="t.tier === modelValue ? 'text-primary-on-soft' : 'text-muted'"
      >
        {{ t.label }}
      </span>
      <span
        class="text-body-strong mt-[3px] block font-bold tabular-nums"
        :class="t.tier === modelValue ? 'text-primary-on-soft' : 'text-icon-off'"
      >
        {{ formatMileage(t.mileage) }}
      </span>
    </button>
  </div>
</template>
