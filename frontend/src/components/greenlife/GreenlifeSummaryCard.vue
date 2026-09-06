<script setup>
/*
 * 월 실천 현황 + 연간 한도 (C-2-01 · C-2-02 · BN-02)
 *
 * ── 두 금액을 색으로만 나누지 않는다 ─────────────────────────────────────
 * `적립 예정` 은 참여기업이 실적을 제출하기 전이라 **아직 현금이 아니고 포켓 잔액에도
 * 들어가지 않는다**(C-2-05 · 핵심 규칙 3). `지급 완료` 만 포켓 입금 대상이다.
 * 라벨(GpTag)을 반드시 함께 넣는다(COM-06).
 *
 * 시안은 적립 예정을 연한 초록으로 칠했지만 초록은 절감 성공·브랜드색이다. 아직 확정이
 * 아닌 돈에 쓰면 「돈의 3단계」가 섞이므로 `estimated`(회색)로 둔다. 지급 완료의 크림색은
 * `confirmed-bg`(앰버) 그대로라 시안과 같다.
 *
 * ── 진행률을 여기서 계산하지 않는다 ──────────────────────────────────────
 * `progressPercent` 는 서버가 준다. 한도 도달이면 서버가 100 으로 고정해 내려주므로
 * 바 너비만 0~100 으로 자른다 — 음수·초과가 와도 레이아웃이 깨지지 않게 하는 방어다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import { formatMonth, formatWon } from '@/utils/format'

const props = defineProps({
  month: { type: String, default: '' },
  monthSummary: { type: Object, default: null },
  annual: { type: Object, default: null },
})

const title = computed(() => `${formatMonth(props.month)} 실천 현황`)

const barWidth = computed(() => {
  const percent = Number(props.annual?.progressPercent ?? 0)
  if (Number.isNaN(percent)) return 0
  return Math.min(100, Math.max(0, percent))
})
</script>

<template>
  <GpCard v-if="monthSummary" :title="title">
    <template #action>
      <GpTag tone="primary">{{ monthSummary.activityCount }}건</GpTag>
    </template>

    <div class="grid grid-cols-2 gap-3">
      <!-- 아직 현금이 아니다. 회색 + 아웃라인으로 확정과 구분한다 -->
      <div class="bg-estimated-bg border-estimated-border rounded-md border p-4">
        <GpTag tone="estimated" small>적립 예정</GpTag>
        <p class="text-amount text-ink mt-2 mb-0">{{ formatWon(monthSummary.pendingAmount) }}</p>
      </div>

      <!-- 누리집 지급 확정분. 포켓 입금 대상이다 -->
      <div class="bg-confirmed-bg rounded-md p-4">
        <GpTag tone="confirmed" small>지급 완료</GpTag>
        <p class="text-amount text-on-confirmed mt-2 mb-0">
          {{ formatWon(monthSummary.paidAmount) }}
        </p>
      </div>
    </div>

    <div v-if="annual" class="border-divider mt-4 border-t pt-4">
      <div class="flex items-baseline justify-between gap-2">
        <span class="text-body-sm text-ink-soft">올해 받은 금액</span>
        <span class="text-body-strong text-ink tabular-nums">
          {{ formatWon(annual.paidAmount) }} / {{ formatWon(annual.limitAmount) }}
        </span>
      </div>

      <div class="bg-track mt-2.5 h-2 overflow-hidden rounded-sm">
        <div class="bg-confirmed h-full rounded-sm" :style="{ width: `${barWidth}%` }" />
      </div>

      <p class="text-caption text-muted mt-2 mb-0">
        1인당 연 최대 {{ formatWon(annual.limitAmount) }} · 녹색생활실천 누리집 기준
      </p>
    </div>
  </GpCard>
</template>
