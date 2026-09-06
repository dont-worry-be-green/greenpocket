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
import GpDelta from '@/components/ui/GpDelta.vue'
import GpTag from '@/components/ui/GpTag.vue'
import { formatPercent, formatTier, formatMileage, formatWon } from '@/utils/format'

const props = defineProps({
  result: { type: Object, required: true },
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
</script>

<template>
  <GpCard>
    <p class="text-caption text-muted mt-0 mb-1">기준 사용량보다</p>
    <GpDelta :value="result.finalRate" size="xl" />

    <div class="mt-3 flex flex-wrap items-center gap-1.5">
      <GpTag :tone="result.achieved ? 'positive' : 'sub'">{{ goalBadge }}</GpTag>
      <GpTag tone="confirmed">{{ tierBadge }}</GpTag>
    </div>

    <div class="border-divider mt-4 flex flex-wrap items-baseline justify-between gap-2 border-t pt-3">
      <span class="text-body text-muted tabular-nums">
        {{ formatWon(result.amount.baselineTotal) }} →
        <strong class="text-ink font-semibold">{{ formatWon(result.amount.actualTotal) }}</strong>
      </span>
      <strong class="text-body-strong text-decrease tabular-nums">
        {{ formatWon(result.amount.savedAmount) }} 덜 냄
      </strong>
    </div>
  </GpCard>
</template>
