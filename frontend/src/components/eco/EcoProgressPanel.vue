<script setup>
/*
 * WF-06 평가 기간 진행 (B-4-03)
 *
 * `progress` 는 GET /eco/home 의 `progress` 그대로다. 필드명을 바꾸지 않는다.
 *
 * ⚠️ **구간 판정을 화면에서 하지 않는다.** `tiers[].state` 가 'CURRENT' | 'TARGET' | 'NONE'
 * 문자열로 온다(EcoProgressService). `currentTier` 와 `targetTier` 를 비교해 직접 판정하면
 * 같은 회차에서 서버와 어긋난다.
 *
 * ⚠️ `gapToNextTierPoint` 는 증감이 아니라 **퍼센트포인트**다. GpDelta 에 넘기면
 * "1% 줄었어요" 가 되어 뜻이 뒤집힌다 — `formatPoint` 를 쓴다.
 *
 * ── 구간은 세로 목록이 아니라 계단 3칸이다 ────────────────────────────────
 * B-4-03 이 "구간 계단 3칸(현재 구간 채움 + 지금, 목표 구간 테두리 + 목표)" 로 못 박는다.
 * **`GpBandPicker` 를 쓰지 않는다** — 그건 WF-04 에서 사용자가 고르는 라디오라
 * 누를 수 있는 것처럼 보인다. 여기 3칸은 서버가 정한 상태를 읽기만 한다.
 *
 * 사다리의 마일리지는 전부 아직 확정되지 않은 금액이라 `예상` 라벨을 함께 단다(핵심 규칙 2).
 * 시안에는 그 라벨이 없지만 색만으로 구분하지 않는 것이 COM-06 규칙이라 한 줄로 줄여 남겼다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import GpTag from '@/components/ui/GpTag.vue'
import { formatMileage, formatMonthOnly, formatPoint, formatTier } from '@/utils/format'

const props = defineProps({
  progress: { type: Object, required: true },
  /** 헤더 배지에 다는 회차 기간. 홈 응답의 `header` 를 뷰가 조립해 넘긴다 */
  period: { type: String, default: '' },
})

/** '4~7월 누적' — 반영 범위를 숫자와 함께 밝힌다(핵심 규칙 7) */
const coveredLabel = computed(() => {
  const months = props.progress.coveredMonths ?? []
  if (months.length === 0) return '아직 반영된 고지서가 없어요'
  const first = formatMonthOnly(months[0])
  const last = formatMonthOnly(months[months.length - 1])
  return months.length === 1 ? `${first} 누적` : `${first}~${last} 누적`
})

/*
 * 칸 색이 곧 상태다. CURRENT 는 채우고 TARGET 은 테두리만 둘러 둘을 구별한다 —
 * 아래 '지금'·'목표' 라벨과 짝지어야 색맹 환경에서도 갈린다(COM-06).
 */
const CELL = {
  CURRENT: 'bg-primary text-on-primary',
  TARGET: 'bg-surface text-primary-on-soft shadow-[inset_0_0_0_2px_var(--color-primary)]',
  NONE: 'bg-surface-sub text-icon-off',
}
const STATE_LABEL = { CURRENT: '지금', TARGET: '목표' }

// 0 이나 null 이면 문장이 성립하지 않는다. 이미 목표 구간에 있다는 뜻이다
const showGap = computed(() => Number(props.progress.gapToNextTierPoint) > 0)
</script>

<template>
  <GpCard title="평가 기간 진행" :badge="period">
    <GpDelta :value="progress.cumulativeRate" size="lg" word="줄이는 중" />

    <p class="text-caption text-muted mt-2 mb-0">{{ coveredLabel }} · 등록한 고지서 기준</p>
    <!-- 시차 규칙(핵심 규칙 10). 화면 숫자와 누리집 확정값이 다른 이유를 미리 밝힌다 -->
    <p class="text-caption text-muted mt-0.5 mb-0">검침 확정분은 2~3개월 뒤에 반영돼요</p>

    <ul class="mt-4 mb-0 grid list-none grid-cols-3 gap-2 p-0" aria-label="마일리지 구간">
      <li
        v-for="row in progress.tiers"
        :key="row.tier"
        class="rounded-md px-1 py-2.5 text-center"
        :class="CELL[row.state] ?? CELL.NONE"
      >
        <span class="text-caption-sm block font-semibold">{{ formatTier(row.tier) }}</span>
        <span class="text-body-strong mt-0.5 block font-bold tabular-nums">
          {{ formatMileage(row.mileage) }}
        </span>
        <span v-if="STATE_LABEL[row.state]" class="text-caption-sm mt-1 block font-semibold">
          {{ STATE_LABEL[row.state] }}
        </span>
      </li>
    </ul>

    <p
      v-if="showGap"
      class="bg-confirmed-bg text-caption text-on-confirmed mt-3 mb-0 rounded-md px-3 py-2.5"
    >
      <!-- 조사 「만」 앞에서 줄을 바꾸면 공백이 끼어 "1%p 만" 으로 렌더된다 -->
      <strong class="font-bold">{{ formatPoint(progress.gapToNextTierPoint) }}</strong
      >만 더 줄이면 {{ formatMileage(progress.nextTierMileage) }} 구간이에요
    </p>

    <p class="text-caption text-muted mt-2 mb-0 flex items-center gap-1.5">
      <GpTag tone="estimated" small>예상</GpTag>
      평가가 끝나야 확정돼요
    </p>
  </GpCard>
</template>
