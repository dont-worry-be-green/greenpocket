<script setup>
/*
 * WF-07 그 달의 결과 (B-4-02)
 *
 * `result` 는 GET /eco/monthly-report 의 `result` 그대로다.
 *
 * ⚠️ **`monthlyRate` 와 `cumulativeRate` 를 섞지 않는다.** 앞은 그 달 하나, 뒤는 등록된 달 전부다.
 * 7월은 1.284% 인데 누적은 9.043% 라, 한쪽 자리에 다른 쪽을 넣으면 티가 나지 않고 틀린다.
 *
 * `targetRate` 는 **회차 전체 목표**다. 달마다의 목표가 아니라서 "이 달만 보면" 이라고 못 박는다.
 *
 * 기준선이 무엇인지(`baselineDescription`)를 함께 적는다 — 판정 근거를 숨기지 않는다(핵심 규칙 7).
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import { formatDateTime, formatMonth, formatPercent } from '@/utils/format'

const props = defineProps({
  result: { type: Object, required: true },
  reportMonth: { type: String, default: '' },
  baselineDescription: { type: String, default: '' },
  billRegisteredAt: { type: String, default: null },
})

const verdict = computed(() =>
  props.result.achieved
    ? '이 달은 목표를 넘겼어요'
    : `이 달만 보면 목표 ${formatPercent(props.result.targetRate)}에 못 미쳤어요`,
)

const cumulativeMonths = computed(() => props.result.cumulativeMonths ?? [])
</script>

<template>
  <GpCard :title="`${formatMonth(reportMonth)} 페이스`">
    <GpDelta :value="result.monthlyRate" size="lg" />
    <p class="text-body text-ink-soft mt-2 mb-0">{{ verdict }}</p>

    <div class="border-divider mt-4 border-t pt-3">
      <div class="flex items-baseline justify-between gap-2">
        <span class="text-body text-ink-soft"
          >지금까지 {{ cumulativeMonths.length }}개월 누적</span
        >
        <GpDelta :value="result.cumulativeRate" size="md" :show-word="false" />
      </div>
      <p v-if="baselineDescription" class="text-caption text-muted mt-2 mb-0">
        기준은 {{ baselineDescription }}이에요.
      </p>
      <p v-if="billRegisteredAt" class="text-caption text-muted mt-1 mb-0">
        {{ formatDateTime(billRegisteredAt) }} 등록
      </p>
    </div>
  </GpCard>
</template>
