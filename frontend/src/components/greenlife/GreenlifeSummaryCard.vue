<script setup>
/*
 * 「N월 적립」 카드 (C-2-01 · C-2-02 · BN-02 · 결정 C-32)
 *
 * 행 2개 — 라벨 왼쪽 · 금액 오른쪽. 색만으로 나누지 않고 라벨이 단계를 말한다(COM-06).
 *   적립 예정  참여기업 실적 제출 전. 아직 현금이 아니고 포켓 잔액에도 없다(C-2-05) → 회색 금액
 *   지급 완료  누리집 지급 확정분 · 포켓 입금 대상 → 잉크색 금액. 어느 달 분인지(`paidMonth`)를 옆에 단다
 * 그 아래 올해 한도 바(진행률은 서버 `progressPercent`, 여기서 계산하지 않는다)와
 * 「1인당 연 최대 70,000원 · 실적 반영까지 …」 캡션. 반영 지연 문구는 적립 예정 옆이 제자리다.
 *
 * ── 월 이동 ─────────────────────────────────────────────────────────────
 * 이동 가능 범위(`canPrev` · `canNext`)는 뷰가 정해서 내려준다. 명세 밖 기능(#110)이라 제목 옆
 * 작은 화살표로만 남긴다. 아이콘은 얇은 캐럿 하나를 뒤집어 좌우를 맞춘다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import IconCaretRight from '@/components/ui/icons/IconCaretRight.vue'
import { formatMonthOnly, formatWon } from '@/utils/format'

const props = defineProps({
  month: { type: String, default: '' },
  monthSummary: { type: Object, default: null },
  annual: { type: Object, default: null },
  delayNotice: { type: String, default: '' },
  canPrev: { type: Boolean, default: false },
  canNext: { type: Boolean, default: false },
})
defineEmits(['prev', 'next'])

const title = computed(() => `${formatMonthOnly(props.month)} 적립`)
const paidMonthLabel = computed(() =>
  props.monthSummary?.paidMonth ? `${formatMonthOnly(props.monthSummary.paidMonth)}분` : '',
)

const barWidth = computed(() => {
  const percent = Number(props.annual?.progressPercent ?? 0)
  if (Number.isNaN(percent)) return 0
  return Math.min(100, Math.max(0, percent))
})

const caption = computed(() => {
  const parts = []
  if (props.annual) parts.push(`1인당 연 최대 ${formatWon(props.annual.limitAmount)}`)
  if (props.delayNotice) parts.push(props.delayNotice)
  return parts.join(' · ')
})
</script>

<template>
  <GpCard v-if="monthSummary">
    <template #action>
      <div class="flex flex-1 items-center justify-between">
        <button
          type="button"
          class="text-ink disabled:text-disabled-text flex size-7 cursor-pointer items-center justify-center rounded-full border-0 bg-transparent disabled:cursor-not-allowed"
          :disabled="!canPrev"
          aria-label="이전 달"
          @click="$emit('prev')"
        >
          <IconCaretRight :size="16" class="rotate-180" />
        </button>
        <h2 class="text-section tracking-display m-0">{{ title }}</h2>
        <button
          type="button"
          class="text-ink disabled:text-disabled-text flex size-7 cursor-pointer items-center justify-center rounded-full border-0 bg-transparent disabled:cursor-not-allowed"
          :disabled="!canNext"
          aria-label="다음 달"
          @click="$emit('next')"
        >
          <IconCaretRight :size="16" />
        </button>
      </div>
    </template>

    <ul class="divide-divider -mt-1 m-0 list-none divide-y p-0">
      <li class="flex items-center justify-between gap-3 py-3">
        <span class="text-list-title text-ink">적립 예정</span>
        <span class="text-section text-muted tabular-nums">{{
          formatWon(monthSummary.pendingAmount)
        }}</span>
      </li>
      <li class="flex items-center justify-between gap-3 py-3">
        <span class="text-list-title text-ink">
          지급 완료
          <span v-if="paidMonthLabel" class="text-caption text-muted ml-1 font-medium">{{
            paidMonthLabel
          }}</span>
        </span>
        <span class="text-section text-ink tabular-nums">{{
          formatWon(monthSummary.paidAmount)
        }}</span>
      </li>
    </ul>

    <div v-if="annual" class="border-divider mt-1 border-t pt-3.5">
      <div class="flex items-baseline justify-between gap-2">
        <span class="text-caption text-muted">올해 받은 금액</span>
        <span class="text-list-title text-ink tabular-nums">
          {{ formatWon(annual.paidAmount).replace('원', '') }}
          <span class="text-caption text-muted font-medium"
            >/ {{ formatWon(annual.limitAmount) }}</span
          >
        </span>
      </div>
      <div class="bg-track mt-2 h-2 overflow-hidden rounded-sm">
        <div class="bg-primary-soft h-full rounded-sm" :style="{ width: `${barWidth}%` }" />
      </div>
      <p v-if="caption" class="text-caption text-muted mt-2.5 mb-0">{{ caption }}</p>
    </div>
  </GpCard>
</template>
