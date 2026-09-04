<script setup>
/*
 * WF-06 평가 기간 진행 (B-4-01)
 *
 * `progress` 는 GET /eco/home 의 `progress` 그대로다. 필드명을 바꾸지 않는다.
 *
 * ⚠️ **구간 판정을 화면에서 하지 않는다.** `tiers[].state` 가 'CURRENT' | 'TARGET' | 'NONE'
 * 문자열로 온다(EcoProgressService). `currentTier` 와 `targetTier` 를 비교해 직접 판정하면
 * 같은 회차에서 서버와 어긋난다.
 *
 * ⚠️ `gapToNextTierPoint` 는 증감이 아니라 **퍼센트포인트**다. GpDelta 에 넘기면
 * "0.957% 줄었어요" 가 되어 뜻이 뒤집힌다 — `formatPoint` 를 쓴다.
 *
 * 사다리의 마일리지는 전부 **아직 확정되지 않은 금액**이라 `예상` 라벨을 함께 단다(핵심 규칙 2).
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import GpTag from '@/components/ui/GpTag.vue'
import { formatMileage, formatMonth, formatPoint, formatTier } from '@/utils/format'

const props = defineProps({
  progress: { type: Object, required: true },
})

const coveredMonths = computed(() => props.progress.coveredMonths ?? [])

/** '2026년 4월 ~ 7월 고지서 4개월이 반영됐어요' — 반영 범위를 숫자와 함께 밝힌다(핵심 규칙 7) */
const coveredLabel = computed(() => {
  const months = coveredMonths.value
  if (months.length === 0) return '아직 반영된 고지서가 없어요'
  const first = formatMonth(months[0])
  const last = formatMonth(months[months.length - 1])
  const range = months.length === 1 ? first : `${first} ~ ${last}`
  return `${range} 고지서 ${months.length}개월이 반영됐어요`
})

const STATE_TAG = {
  CURRENT: { label: '지금', tone: 'primary' },
  TARGET: { label: '목표', tone: 'positive' },
}

// 0 이나 null 이면 문장이 성립하지 않는다. 이미 목표 구간에 있다는 뜻이다
const showGap = computed(() => Number(props.progress.gapToNextTierPoint) > 0)
</script>

<template>
  <GpCard title="평가 기간 진행">
    <p class="text-caption text-muted mt-0 mb-2">{{ coveredLabel }}</p>
    <GpDelta :value="progress.cumulativeRate" size="lg" word="줄이는 중이에요" />

    <ul class="border-divider mt-4 mb-0 list-none border-t p-0">
      <li
        v-for="row in progress.tiers"
        :key="row.tier"
        class="border-divider flex items-center gap-2 border-b py-3 last:border-b-0"
        :class="row.state === 'NONE' ? 'text-muted' : 'text-ink'"
      >
        <span class="text-body-strong min-w-0 flex-1">{{ formatTier(row.tier) }} 줄이면</span>
        <GpTag v-if="STATE_TAG[row.state]" :tone="STATE_TAG[row.state].tone" small>
          {{ STATE_TAG[row.state].label }}
        </GpTag>
        <span class="text-body-strong tabular-nums">{{ formatMileage(row.mileage) }}</span>
      </li>
    </ul>

    <p v-if="showGap" class="text-body text-ink-soft border-divider mt-3 mb-0 border-t pt-3">
      <!-- 조사 「만」 앞에서 줄을 바꾸면 공백이 끼어 "0.957%p 만" 으로 렌더된다 -->
      <strong class="font-semibold">{{ formatPoint(progress.gapToNextTierPoint) }}</strong
      >만 더 줄이면 {{ formatMileage(progress.nextTierMileage) }} 구간이에요.
    </p>

    <p class="text-caption text-muted mt-2 mb-0 flex items-center gap-1.5">
      <GpTag tone="estimated" small>예상</GpTag>
      확정 금액이 아니에요. 평가가 끝나야 정해져요.
    </p>
  </GpCard>
</template>
