<script setup>
/*
 * 달마다 얼마나 줄였나 — WF-07 (B-4-08). **WF-10 결과 화면이 같은 배열을 쓴다.**
 *
 * `rows` 는 `monthlyRates[]` 그대로다: `{ yearMonth, rate, achieved }`.
 *
 * ⚠️ **음수를 0 으로 깔지 않는다.** `rate` 는 음수가 증가라(부호 규약), 7월 −5% 를 바닥에 붙이면
 * "조금 줄였다"로 읽힌다. 0 선을 그리고 그 아래로 내린다.
 *
 * 막대 높이는 값에 따라 달라져서 Tailwind 클래스로 만들 수 없다 — 이 파일의 인라인 style 은
 * 그 좌표 계산에만 쓴다(색·간격은 전부 토큰).
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import { formatPercent } from '@/utils/format'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  // 회차 전체 목표. 있으면 기준선을 함께 그린다
  targetRate: { type: Number, default: null },
  title: { type: String, default: '달마다 얼마나 줄였나' },
})

/** 0 선 위아래로 얼마씩 필요한지. 목표선도 화면 밖으로 나가지 않게 범위에 넣는다 */
const scale = computed(() => {
  const values = props.rows.map((row) => row.rate)
  if (props.targetRate !== null) values.push(props.targetRate)
  const top = Math.max(0, ...values)
  const bottom = Math.max(0, ...values.map((value) => -value))
  // 전부 0 이면 나눗셈이 깨진다
  const span = top + bottom || 1
  return { bottom, span }
})

/** 0 선의 바닥 기준 위치(%) */
const zeroBottom = computed(() => (scale.value.bottom / scale.value.span) * 100)

const barStyle = (rate) => {
  const height = (Math.abs(rate) / scale.value.span) * 100
  return rate >= 0
    ? { bottom: `${zeroBottom.value}%`, height: `${height}%` }
    : { top: `${100 - zeroBottom.value}%`, height: `${height}%` }
}

const targetBottom = computed(() =>
  props.targetRate === null
    ? null
    : ((props.targetRate + scale.value.bottom) / scale.value.span) * 100,
)

const monthLabel = (yearMonth) => `${Number(yearMonth.split('-')[1])}월`
</script>

<template>
  <GpCard :title="title">
    <div v-if="rows.length" class="relative h-30">
      <!-- 0 선. 음수 막대가 어디서부터 내려간 것인지 보여준다 -->
      <div
        class="bg-divider absolute inset-x-0 h-px"
        :style="{ bottom: `${zeroBottom}%` }"
        aria-hidden="true"
      />
      <!-- 회차 목표. 넘긴 달과 못 미친 달이 눈으로 갈린다 -->
      <div
        v-if="targetBottom !== null"
        class="border-primary absolute inset-x-0 border-t border-dashed"
        :style="{ bottom: `${targetBottom}%` }"
        aria-hidden="true"
      />

      <div class="absolute inset-0 flex items-stretch gap-2">
        <div v-for="row in rows" :key="row.yearMonth" class="relative flex-1">
          <div
            class="absolute inset-x-0 rounded-xs"
            :class="row.achieved ? 'bg-primary' : 'bg-surface-sub'"
            :style="barStyle(row.rate)"
          />
        </div>
      </div>
    </div>

    <div v-if="rows.length" class="mt-2 flex gap-2">
      <div v-for="row in rows" :key="row.yearMonth" class="flex-1 text-center">
        <span class="text-caption block" :class="row.achieved ? 'text-ink' : 'text-muted'">
          {{ formatPercent(row.rate) }}
        </span>
        <span class="text-caption-sm text-muted block">{{ monthLabel(row.yearMonth) }}</span>
      </div>
    </div>

    <p v-if="targetRate !== null && rows.length" class="text-caption text-muted mt-3 mb-0">
      점선이 회차 목표 {{ formatPercent(targetRate) }}예요.
    </p>

    <!-- 아직 반영된 달이 없다. 빈 그래프를 그리지 않는다 (COM-08) -->
    <p v-if="!rows.length" class="text-body text-ink-soft m-0">
      아직 반영된 달이 없어요. 고지서를 등록하면 달마다의 페이스가 쌓여요.
    </p>
  </GpCard>
</template>
