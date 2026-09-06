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
 *
 * ── 월 이동은 신호만 낸다 ────────────────────────────────────────────────
 * 어느 달까지 갈 수 있는지(`canPrev` · `canNext`)는 **뷰가 정해서 내려준다.** 이 카드가
 * 오늘 날짜나 기준 연도를 직접 보면 판정이 두 벌이 되고, 목록(`GET /greenlife/items`)이
 * 따라오지 않는 달로 혼자 넘어갈 수 있다.
 *
 * 제목은 연도를 접는다(`8월 실천 현황`). 화살표 둘과 건수 배지가 같은 줄에 서서
 * `2026년 8월 실천 현황` 은 393px 에서 넘친다. 연도는 아래 「올해 받은 금액」이 말한다.
 *
 * ⚠️ `GpCard` 의 `title` 을 쓰지 않고 헤더를 직접 그린다 — 제목 **양옆**에 버튼이 와야 하는데
 * `GpCard` 는 제목을 통째로 그리고 오른쪽 `action` 슬롯만 연다. 화면 하나 때문에 공용
 * 컴포넌트에 슬롯을 늘리지 않는다(`EcoSettlementCard` 와 같은 판단).
 *
 * ⚠️ **화살표 둘은 같은 아이콘을 뒤집어 쓴다.** `IconChevronRight` 는 Phosphor 에서 뽑은
 * **채운 캐럿**이고 `IconChevronLeft` 는 손으로 그린 **선 꺾쇠**라, 나란히 두면 좌우 모양이
 * 다르다. 목록 행의 `›` 는 채운 캐럿이 맞으므로 그쪽을 바꾸지 않고 여기서만 맞춘다.
 *
 * 건수 배지는 `absolute` 로 오른쪽에 붙인다. 흐름 안에 두면 제목 블록이 남은 폭의 가운데로
 * 밀려 카드 기준으로는 왼쪽으로 치우친다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconChevronLeft from '@/components/ui/icons/IconChevronLeft.vue'
import { formatMonthOnly, formatWon } from '@/utils/format'

const props = defineProps({
  month: { type: String, default: '' },
  monthSummary: { type: Object, default: null },
  annual: { type: Object, default: null },
  /** 이동 가능 범위는 뷰가 정한다. 위 주석 참고 */
  canPrev: { type: Boolean, default: false },
  canNext: { type: Boolean, default: false },
})
defineEmits(['prev', 'next'])

const title = computed(() => `${formatMonthOnly(props.month)} 실천 현황`)

const barWidth = computed(() => {
  const percent = Number(props.annual?.progressPercent ?? 0)
  if (Number.isNaN(percent)) return 0
  return Math.min(100, Math.max(0, percent))
})
</script>

<template>
  <GpCard v-if="monthSummary">
    <header class="relative mb-3 flex min-h-7 items-center justify-center">
      <div class="flex items-center gap-1">
        <button
          type="button"
          class="text-ink disabled:text-disabled-text flex size-7 flex-none cursor-pointer items-center justify-center rounded-full border-0 bg-transparent disabled:cursor-not-allowed"
          :disabled="!canPrev"
          aria-label="이전 달"
          @click="$emit('prev')"
        >
          <IconChevronLeft :size="18" />
        </button>

        <h2 class="text-section tracking-display m-0 truncate">{{ title }}</h2>

        <button
          type="button"
          class="text-ink disabled:text-disabled-text flex size-7 flex-none cursor-pointer items-center justify-center rounded-full border-0 bg-transparent disabled:cursor-not-allowed"
          :disabled="!canNext"
          aria-label="다음 달"
          @click="$emit('next')"
        >
          <!-- 같은 꺾쇠를 뒤집는다. 좌우가 거울처럼 맞아야 한다 -->
          <IconChevronLeft :size="18" class="rotate-180" />
        </button>
      </div>

      <GpTag tone="primary" class="absolute right-0">{{ monthSummary.activityCount }}건</GpTag>
    </header>

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
