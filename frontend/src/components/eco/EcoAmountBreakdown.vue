<script setup>
/*
 * 기준 요금 · 평가 기간 요금 · 덜 낸 요금 — WF-10 · WF-11 (B-5-02 · B-5-03)
 *
 * ── 필드명이 두 응답에서 다르다 ────────────────────────────────────────────
 * `result.amount` 는 `baselineTotal · actualTotal · savedAmount`,
 * `settlement.calculation` 은 `baselineAmount · actualAmount · savedAmount` 다.
 * 같은 세 숫자라 표는 하나면 되는데 이름이 달라, **객체가 아니라 숫자 세 개를 받는다.**
 * 여기서 두 이름을 다 뒤지면 오타가 나도 `undefined` 라 조용히 빈칸이 된다.
 *
 * ── ⚠️ 「줄인 금액」 옆에 버튼을 두지 않는다 (핵심 규칙 3) ──────────────────
 * `savedAmount` 는 **포켓 잔액이 아니다.** 요금을 그만큼 덜 냈다는 성과 표시일 뿐이라
 * 전환·출금 대상이 아니다(`savedIsPocketEligible: false`). 그래서 이 카드에는 CTA 가 없고,
 * 전환으로 가는 길은 화면 하단 고정 버튼 하나뿐이다.
 *
 * 예전에는 "덜 낸 요금은 포켓에 쌓이는 돈이 아니에요" 문장을 카드 안에 뒀는데,
 * WF-11 에서는 바로 아래 「아직 현금이 아니에요」 박스가 같은 말을 더 정확히 한다 —
 * 마일리지가 현금이 아니라는 것이 요점이지 「줄인 금액」의 성격이 요점이 아니다.
 *
 * 마일리지(M)가 아니라 **원**이다. 1M = 1원이라 숫자가 같아 바꿔 써도 그럴듯해 보인다.
 */
import GpCard from '@/components/ui/GpCard.vue'
import { formatWon } from '@/utils/format'

defineProps({
  title: { type: String, default: '줄인 요금' },
  baseline: { type: Number, required: true },
  actual: { type: Number, required: true },
  saved: { type: Number, required: true },
  /** `calculation.note` 처럼 서버가 문장으로 준 기준선 설명. 표 아래에 근거로 붙는다 */
  note: { type: String, default: '' },
})
</script>

<template>
  <GpCard :title="title">
    <dl class="m-0 space-y-2.5">
      <div class="flex items-baseline justify-between gap-3">
        <dt class="text-body text-ink-soft">기준 사용량 요금</dt>
        <dd class="text-body m-0 tabular-nums">{{ formatWon(baseline) }}</dd>
      </div>
      <div class="flex items-baseline justify-between gap-3">
        <dt class="text-body text-ink-soft">평가 기간 요금</dt>
        <dd class="text-body-strong m-0 tabular-nums">{{ formatWon(actual) }}</dd>
      </div>
      <div class="flex items-baseline justify-between gap-3">
        <dt class="text-body-strong">줄인 금액</dt>
        <dd class="text-body-strong text-on-positive m-0 tabular-nums">{{ formatWon(saved) }}</dd>
      </div>
    </dl>

    <!-- 무엇과 비교한 값인지 서버가 문장으로 준다 (핵심 규칙 7) -->
    <p v-if="note" class="border-divider text-caption text-muted mt-3 mb-0 border-t pt-3">
      {{ note }}
    </p>
  </GpCard>
</template>
