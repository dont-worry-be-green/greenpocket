<script setup>
/*
 * 적립 스탯 타일 셋 — WF-11 (B-5-03)
 *
 * 헤더(동전·금액) 바로 아래 가로로 「사용량 감축률 · 지급 구간 · 덜 낸 요금」 세 타일.
 * 진단 탭 「나 / 1인 가구 평균」 타일(`SingleHouseholdComparisonCard`)과 같은 문법이다 —
 * 라벨 위, 값 아래, 가운데 정렬(2026-09-10 수현). 세 값 모두 `GET /eco/rounds/{roundId}/settlement` 에 이미 있다.
 *
 * ── 왜 이 셋인가 ─────────────────────────────────────────────────────────
 * 마일리지는 **사용량 감축률 → 지급 구간** 순서로 정해진다(계산식 5~7 · 결정 C-38). 앞의 두 타일이
 * 그 인과이고, 셋째 「덜 낸 요금」은 원화 성과 표시다(핵심 규칙 1·7). 요금이 마일리지의 근거가
 * 아니라는 것을 타일 순서(감축률 → 구간 → 요금)가 말한다.
 *
 * ── ⚠️ 덜 낸 요금은 포켓 잔액이 아니다 (핵심 규칙 2·3) ─────────────────────
 * `savedAmount` 는 성과 표시일 뿐 전환·출금 대상이 아니다. 그래서 헤더의 30,000M 과 같은 크기로
 * 두지 않고 타일 값 크기(`text-list-title`)에 머문다. 더 냈으면 「더 낸 요금」 빨강, 같으면 회색.
 * 방향은 `saved` 부호 하나로 판정한다 — `baseline - actual` 을 여기서 다시 빼지 않는다.
 *
 * 감축률 자릿수·방향은 `changeRateParts` 한 곳에서 받는다(컴포넌트가 숫자를 직접 다듬지 않는다).
 */
import { computed } from 'vue'

import { changeRateParts, formatTier, formatWon } from '@/utils/format'

const props = defineProps({
  /** 평가 기간 누적 감축률(%). 양수 = 줄었다 */
  cumulativeRate: { type: Number, default: null },
  /** 지급 구간 enum (TIER_5 · TIER_10 · TIER_15) */
  tier: { type: String, default: '' },
  /** 덜 낸 요금(원). 음수 = 더 냈다 */
  saved: { type: Number, default: null },
})

const rate = computed(() => changeRateParts(props.cumulativeRate))

const savedTile = computed(() => {
  if (props.saved === null || props.saved === undefined) {
    return { label: '덜 낸 요금', value: '-', text: 'text-muted' }
  }
  if (props.saved > 0) return { label: '덜 낸 요금', value: formatWon(props.saved), text: 'text-on-positive' }
  if (props.saved < 0) return { label: '더 낸 요금', value: formatWon(-props.saved), text: 'text-increase' }
  return { label: '덜 낸 요금', value: formatWon(0), text: 'text-muted' }
})
</script>

<template>
  <dl class="m-0 grid grid-cols-3 gap-2">
    <div class="bg-surface shadow-card rounded-lg px-2 pt-3 pb-2.5 text-center">
      <dt class="text-caption-sm text-muted font-semibold">사용량 감축률</dt>
      <dd
        class="text-list-title mt-1 mb-0 tabular-nums"
        :class="rate.direction === 'up' ? 'text-increase' : rate.direction === 'down' ? 'text-decrease' : 'text-muted'"
      >
        <template v-if="rate.direction === 'down' || rate.direction === 'up'">{{ rate.value }}%</template>
        <template v-else>{{ rate.value }}</template>
      </dd>
    </div>

    <div class="bg-surface shadow-card rounded-lg px-2 pt-3 pb-2.5 text-center">
      <dt class="text-caption-sm text-muted font-semibold">지급 구간</dt>
      <dd class="text-list-title text-ink mt-1 mb-0 tabular-nums">{{ formatTier(tier) }}</dd>
    </div>

    <div class="bg-surface shadow-card rounded-lg px-2 pt-3 pb-2.5 text-center">
      <dt class="text-caption-sm text-muted font-semibold">{{ savedTile.label }}</dt>
      <dd class="text-list-title mt-1 mb-0 tabular-nums" :class="savedTile.text">{{ savedTile.value }}</dd>
    </div>
  </dl>
</template>
