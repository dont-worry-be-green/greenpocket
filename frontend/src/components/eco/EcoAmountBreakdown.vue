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
 * ── ⚠️ 「덜 낸 요금」 옆에 버튼을 두지 않는다 (핵심 규칙 3) ────────────────
 * `savedAmount` 는 **포켓 잔액이 아니다.** 요금을 그만큼 덜 냈다는 성과 표시일 뿐이라
 * 전환·출금 대상이 아니다(`savedIsPocketEligible: false`). 그 사실을 문장으로 함께 둔다 —
 * 숫자만 크게 두면 적립된 돈으로 읽힌다.
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
  /** `calculation.note` 처럼 서버가 문장으로 준 기준선 설명 */
  note: { type: String, default: '' },
  /** `savedIsPocketEligible`. true 여도 전환은 포켓 도메인의 일이라 여기서 버튼을 만들지 않는다 */
  pocketEligible: { type: Boolean, default: false },
})
</script>

<template>
  <GpCard :title="title" :caption="note">
    <dl class="m-0 space-y-2">
      <div class="flex items-baseline justify-between gap-3">
        <dt class="text-body text-ink-soft">기준 요금</dt>
        <dd class="text-body m-0 tabular-nums">{{ formatWon(baseline) }}</dd>
      </div>
      <div class="flex items-baseline justify-between gap-3">
        <dt class="text-body text-ink-soft">평가 기간 요금</dt>
        <dd class="text-body m-0 tabular-nums">{{ formatWon(actual) }}</dd>
      </div>
      <div class="border-divider flex items-baseline justify-between gap-3 border-t pt-2">
        <dt class="text-body-strong">덜 낸 요금</dt>
        <dd class="text-list-title text-on-positive m-0 tabular-nums">{{ formatWon(saved) }}</dd>
      </div>
    </dl>

    <p v-if="!pocketEligible" class="text-caption text-muted mt-3 mb-0">
      덜 낸 요금은 요금이 그만큼 줄었다는 뜻이에요. 포켓에 쌓이는 돈은 마일리지 쪽이에요.
    </p>
  </GpCard>
</template>
