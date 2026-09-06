<script setup>
/*
 * WF-07 그 달의 결과 (B-4-02 · B-4-07 ①)
 *
 * `result` 는 GET /eco/monthly-report 의 `result` 그대로다.
 *
 * ⚠️ **`monthlyRate` 와 `cumulativeRate` 를 섞지 않는다.** 앞은 그 달 하나, 뒤는 등록된 달 전부다.
 * 7월은 1.284% 인데 누적은 9.043% 라, 한쪽 자리에 다른 쪽을 넣으면 티가 나지 않고 틀린다.
 *
 * `targetRate` 는 **회차 전체 목표**다. 달마다의 목표가 아니라서 기준선 설명과 한 줄에 묶어
 * "…평균보다 · 목표는 10% 줄이기였어요" 로 적는다 — 판정 근거를 숨기지 않는다(핵심 규칙 7).
 *
 * 미달일 때만 「6개월 누적」 문구를 둔다(B-4-07 ①). 달성한 달에 "미끄러져도" 를 붙이면
 * 잘한 결과를 변명처럼 읽게 만든다.
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import { formatMonthDay, formatMonthOnly, formatPercent } from '@/utils/format'

const props = defineProps({
  result: { type: Object, required: true },
  reportMonth: { type: String, default: '' },
  baselineDescription: { type: String, default: '' },
  billRegisteredAt: { type: String, default: null },
})

/** '7월 고지서 · 8월 3일 등록' — 무엇으로 계산했는지 제목 옆에 붙인다 */
const sourceBadge = computed(() => {
  const parts = [`${formatMonthOnly(props.reportMonth)} 고지서`]
  if (props.billRegisteredAt) parts.push(`${formatMonthDay(props.billRegisteredAt)} 등록`)
  return parts.join(' · ')
})

const basisLabel = computed(() => {
  const target = `목표는 ${formatPercent(props.result.targetRate)} 줄이기였어요`
  return props.baselineDescription ? `${props.baselineDescription}보다 · ${target}` : target
})

/** '평가 기간 누적 (4~7월)' */
const cumulativeLabel = computed(() => {
  const months = props.result.cumulativeMonths ?? []
  if (months.length === 0) return '평가 기간 누적'
  const first = formatMonthOnly(months[0])
  const last = formatMonthOnly(months[months.length - 1])
  return months.length === 1 ? `평가 기간 누적 (${first})` : `평가 기간 누적 (${first}~${last})`
})
</script>

<template>
  <GpCard :title="`${formatMonthOnly(reportMonth)}분 감축률`" :badge="sourceBadge">
    <GpDelta :value="result.monthlyRate" size="lg" />
    <p class="text-caption text-muted mt-2 mb-0">{{ basisLabel }}</p>

    <div class="border-divider mt-4 border-t pt-3">
      <div class="flex items-baseline justify-between gap-2">
        <span class="text-body text-ink-soft">{{ cumulativeLabel }}</span>
        <GpDelta :value="result.cumulativeRate" size="md" />
      </div>
      <p v-if="!result.achieved" class="text-caption text-muted mt-2 mb-0">
        한 달 미끄러져도 평가는 6개월 누적이에요. 아직 만회할 수 있어요.
      </p>
    </div>
  </GpCard>
</template>
