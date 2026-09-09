<script setup>
/*
 * 평가 결과 요약 — WF-10 (B-5-02)
 *
 * `result` 는 `GET /eco/rounds/{roundId}/result` 그대로다.
 *
 * ── 한 카드에 결과를 다 담는다 ────────────────────────────────────────────
 * 최종 감축률 · 목표 달성 여부 · 구간과 마일리지 · 요금 변화까지 한 장이다(시안 WF-10).
 * 예전에는 요금 변화를 `EcoAmountBreakdown` 카드로 따로 뒀는데, 결과 화면에서 가장 먼저
 * 알고 싶은 것이 「얼마 줄었고 얼마 덜 냈나」라 두 카드로 나누면 그 답이 스크롤 아래로 밀린다.
 * (그 컴포넌트는 WF-11 이 계속 쓴다 — 거기서는 계산 근거를 펼쳐 보여야 한다)
 *
 * ── ⚠️ 「덜 낸 요금」 옆에 전환 버튼을 두지 않는다 (핵심 규칙 3) ────────────
 * `savedAmount` 는 포켓 잔액이 아니다(`savedIsPocketEligible: false`). 전환으로 가는 길은
 * 이 카드가 아니라 아래 마일리지 카드에 있다.
 *
 * ── 미달을 벌주지 않는다 ───────────────────────────────────────────────────
 * `achieved: false` 에 빨간 X 를 쓰지 않는다(api-spec.md 11.1). 걸었던 목표를 배지로 적는다.
 *
 * 확정 근거(`confirmedSource` · `confirmedAt`)는 화면 헤더 부제로 올렸다 — 핵심 규칙 7·10 은
 * 그 사실을 밝히라는 것이지 카드 안에 두라는 것이 아니고, 헤더가 화면 전체의 출처 자리다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import { changeRateParts, formatPercent, formatTier, formatMileage, formatWon } from '@/utils/format'

const props = defineProps({
  result: { type: Object, required: true },
  reportMode: { type: Boolean, default: false },
})

/**
 * 최종 감축률. `GpDelta` 의 화살표 없이 숫자 + 말만 — 자릿수·방향은 `changeRateParts` 한 곳.
 * 늘었으면(음수) 빨강 「늘었어요」, 0 이면 회색 「지난달과 같아요」, 값이 없으면 「-」.
 */
const rate = computed(() => {
  const parts = changeRateParts(props.result.finalRate)
  if (parts.direction === 'down') return { value: `${parts.value}%`, word: parts.word, text: 'text-decrease' }
  if (parts.direction === 'up') return { value: `${parts.value}%`, word: parts.word, text: 'text-increase' }
  return { value: parts.value, word: parts.word, text: 'text-muted' }
})

/** 서버가 `tierLabel` 을 주면 그쪽이 우선이다. `formatTier` 는 없을 때의 대체재다 */
const tierBadge = computed(() => {
  const tier = props.result.tierLabel || formatTier(props.result.tier)
  return `${tier} · ${formatMileage(props.result.confirmedMileage)}`
})

const goalBadge = computed(() => {
  const target = formatPercent(props.result.targetRate)
  return props.result.achieved ? `목표 ${target} 달성` : `목표 ${target} 미달`
})

const tierLabel = computed(() => props.result.tierLabel || formatTier(props.result.tier))
</script>

<template>
  <GpCard>
    <!-- 「직전 2년 같은 기간 평균보다」 캡션과 화살표는 뺐다(2026-09-10 수현 · 결정 C-40) -->
    <div v-if="reportMode" class="flex flex-wrap items-center justify-between gap-3">
      <p class="m-0 flex items-baseline gap-1.5 tabular-nums">
        <b class="text-amount-hero tracking-display" :class="rate.text">{{ rate.value }}</b>
        <span v-if="rate.word" class="text-caption text-ink-soft font-semibold">{{ rate.word }}</span>
      </p>
      <GpTag :tone="result.achieved ? 'positive' : 'sub'">{{ goalBadge }}</GpTag>
    </div>
    <p v-else class="m-0 flex items-baseline gap-1.5 tabular-nums">
      <b class="text-amount-hero tracking-display" :class="rate.text">{{ rate.value }}</b>
      <span v-if="rate.word" class="text-caption text-ink-soft font-semibold">{{ rate.word }}</span>
    </p>

    <div v-if="!reportMode" class="mt-3 flex flex-wrap items-center gap-1.5">
      <GpTag :tone="result.achieved ? 'positive' : 'sub'">{{ goalBadge }}</GpTag>
      <GpTag tone="confirmed">{{ tierBadge }}</GpTag>
    </div>

    <div v-else class="mt-4 flex flex-wrap items-center gap-2">
      <span class="bg-confirmed-bg text-on-confirmed rounded-full px-3 py-2 text-label font-semibold">
        {{ tierLabel }}
      </span>
      <span class="text-body-strong text-ink">
        {{ formatMileage(result.confirmedMileage) }}가 적립되었어요
      </span>
    </div>

    <div class="border-divider mt-4 flex flex-wrap items-baseline justify-between gap-2 border-t pt-3">
      <span class="text-body text-muted tabular-nums">
        {{ formatWon(result.amount.baselineTotal) }} →
        <strong class="text-ink font-semibold">{{ formatWon(result.amount.actualTotal) }}</strong>
      </span>
      <strong
        class="text-body-strong text-decrease tabular-nums"
        :class="reportMode ? 'bg-positive-bg rounded-md px-2.5 py-2' : ''"
      >
        {{ formatWon(result.amount.savedAmount) }} {{ reportMode ? '절약' : '덜 냄' }}
      </strong>
    </div>
  </GpCard>
</template>
