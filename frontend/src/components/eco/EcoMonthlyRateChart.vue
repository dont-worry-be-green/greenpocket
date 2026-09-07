<script setup>
/*
 * 달마다 얼마나 줄였나 — WF-07 · WF-10 (B-4-08 · B-5-02). 두 화면이 같은 배열을 쓴다.
 *
 * `rows` 는 `monthlyRates[]` 그대로다: `{ yearMonth, rate, achieved }`.
 *
 * ⚠️ **음수를 0 으로 깔지 않는다.** `rate` 는 음수가 증가라(부호 규약), 7월 −5% 를 바닥에 붙이면
 * "조금 줄였다"로 읽힌다. 0 선을 그리고 그 아래로 내린다.
 *
 * ⚠️ **목표선(점선)을 그리지 않는다.** 목표를 넘겼는지는 막대 색이 이미 말하고, 그 규칙은
 * `caption` 한 줄이 밝힌다(시안 WF-07 · WF-10). 점선까지 겹치면 기준선이 둘로 보인다.
 *
 * 막대 높이는 값에 따라 달라져서 Tailwind 클래스로 만들 수 없다 — 이 파일의 인라인 style 은
 * 그 좌표 계산에만 쓴다(색·간격은 전부 토큰).
 */
import { computed } from 'vue'

import GpCard from '@/components/ui/GpCard.vue'
import GpDelta from '@/components/ui/GpDelta.vue'
import { changeRateParts } from '@/utils/format'

const props = defineProps({
  rows: { type: Array, default: () => [] },
  title: { type: String, default: '달마다 얼마나 줄였나' },
  // 막대 색 규칙을 밝히는 한 줄. 그래프 위에 온다
  caption: { type: String, default: '' },
  // 계산 근거·주의사항. 그래프 아래에 온다 (핵심 규칙 7)
  footnote: { type: String, default: '' },
  compact: { type: Boolean, default: false },
})

/** 0 선 위아래로 얼마씩 필요한지 */
const scale = computed(() => {
  const values = props.rows.map((row) => row.rate)
  const top = Math.max(0, ...values)
  const bottom = Math.max(0, ...values.map((value) => -value))
  // 전부 0 이면 나눗셈이 깨진다
  return { bottom, span: top + bottom || 1 }
})

/** 0 선의 바닥 기준 위치(%) */
const zeroBottom = computed(() => (scale.value.bottom / scale.value.span) * 100)

const barStyle = (rate) => {
  const height = (Math.abs(rate) / scale.value.span) * 100
  return rate >= 0
    ? { bottom: `${zeroBottom.value}%`, height: `${height}%` }
    : { top: `${100 - zeroBottom.value}%`, height: `${height}%` }
}

const monthLabel = (yearMonth) => `${Number(yearMonth.split('-')[1])}월`
const delta = (rate) => changeRateParts(rate)
</script>

<template>
  <GpCard :title="title" :caption="caption">
    <template v-if="rows.length">
      <!-- 값은 막대 위 한 줄에 나란히 둔다. 막대 끝에 붙이면 짧은 달에서 겹친다 -->
      <div class="grid grid-cols-6" :class="compact ? 'gap-1' : 'gap-2'">
        <div v-for="row in rows" :key="row.yearMonth" class="min-w-0 text-center">
          <span
            v-if="compact"
            class="text-caption-sm inline-block whitespace-nowrap font-semibold tabular-nums"
            :class="delta(row.rate).direction === 'up' ? 'text-increase' : 'text-decrease'"
          >
            {{ delta(row.rate).direction === 'up' ? '↑' : '↓' }} {{ delta(row.rate).value }}%
          </span>
          <GpDelta v-else :value="row.rate" size="sm" :show-word="false" />
        </div>
      </div>

      <div class="relative mt-2" :class="compact ? 'h-24' : 'h-30'">
        <!-- 0 선. 음수 막대가 어디서부터 내려간 것인지 보여준다 -->
        <div
          class="bg-divider absolute inset-x-0 h-px"
          :style="{ bottom: `${zeroBottom}%` }"
          aria-hidden="true"
        />

        <div
          class="absolute inset-0 grid grid-cols-6 items-stretch"
          :class="compact ? 'gap-1.5' : 'gap-2'"
        >
          <div v-for="row in rows" :key="row.yearMonth" class="relative flex-1">
            <div
              class="absolute inset-x-0 rounded-xs"
              :class="row.achieved ? 'bg-primary' : 'bg-track'"
              :style="barStyle(row.rate)"
            />
          </div>
        </div>
      </div>

      <div class="mt-2 grid grid-cols-6" :class="compact ? 'gap-1' : 'gap-2'">
        <div
          v-for="row in rows"
          :key="row.yearMonth"
          class="text-muted min-w-0 text-center"
          :class="compact ? 'text-caption-sm' : 'text-caption'"
        >
          {{ monthLabel(row.yearMonth) }}
        </div>
      </div>

      <p v-if="footnote" class="text-caption text-muted mt-3 mb-0">{{ footnote }}</p>
    </template>

    <!-- 아직 반영된 달이 없다. 빈 그래프를 그리지 않는다 (COM-08) -->
    <p v-else class="text-body text-ink-soft m-0">
      아직 반영된 달이 없어요. 고지서를 등록하면 달마다의 페이스가 쌓여요.
    </p>
  </GpCard>
</template>
