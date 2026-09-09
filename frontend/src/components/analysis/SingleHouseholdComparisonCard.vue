<script setup>
import { computed, ref, watch } from 'vue'

import {
  formatMonthOnly,
  formatPercent,
  formatUnit,
  formatUsage,
  formatUtilityType,
} from '@/utils/format'

const props = defineProps({
  comparison: { type: Object, required: true },
})

const selectedUtilityType = ref('ELECTRICITY')
const tabs = computed(() => props.comparison?.tabs ?? [])
const selectedTab = computed(
  () =>
    tabs.value.find((tab) => tab.utilityType === selectedUtilityType.value) ?? tabs.value[0] ?? null,
)
const series = computed(() => selectedTab.value?.series ?? [])
const usageUnit = computed(() => formatUnit(selectedTab.value?.usageUnit))

watch(
  tabs,
  (nextTabs) => {
    if (!nextTabs.some((tab) => tab.utilityType === selectedUtilityType.value)) {
      selectedUtilityType.value = nextTabs[0]?.utilityType ?? 'ELECTRICITY'
    }
  },
  { immediate: true },
)

const chartBounds = computed(() => {
  const values = series.value.flatMap((point) =>
    [point.myUsage, point.averageUsage].filter((value) => value !== null && value !== undefined),
  )
  if (!values.length) return { min: 0, range: 1 }

  const minimum = Math.min(...values)
  const maximum = Math.max(...values)
  const padding = Math.max((maximum - minimum) * 0.15, maximum * 0.05, 1)
  const min = Math.max(0, minimum - padding)
  return { min, range: maximum + padding - min || 1 }
})

function pointX(index) {
  return 8 + (index * 84) / Math.max(series.value.length - 1, 1)
}

function pointY(value) {
  if (value === null || value === undefined) return null
  return 80 - ((Number(value) - chartBounds.value.min) / chartBounds.value.range) * 64
}

function linePath(key) {
  return series.value
    .map((point, index) => {
      const y = pointY(point[key])
      return y === null ? null : `${index === 0 || pointY(series.value[index - 1]?.[key]) === null ? 'M' : 'L'} ${pointX(index)} ${y}`
    })
    .filter(Boolean)
    .join(' ')
}

function usageLabel(value) {
  return formatUsage(value, 1, usageUnit.value)
}

const differenceLabel = computed(() => {
  const tab = selectedTab.value
  if (!tab?.available || tab.differenceUsage === null || tab.differenceUsage === undefined) return ''

  const difference = Number(tab.differenceUsage)
  if (difference === 0) return '1인 가구 평균과 같아요'

  const rate = formatPercent(Math.abs(Number(tab.differenceRate)))
  const direction = difference > 0 ? '더 많이' : '더 적게'
  return `평균보다 ${usageLabel(Math.abs(difference))} (${rate}) ${direction} 사용했어요`
})
</script>

<template>
  <section v-if="comparison" class="bg-surface mt-5 rounded-xl px-5 py-6">
    <div class="flex items-start justify-between gap-3">
      <div>
        <h2 class="text-section text-ink mt-0 mb-1">1인 가구 평균 사용량</h2>
        <p class="text-caption text-muted m-0">최근 6개월 나와 1인 가구 평균 추이</p>
      </div>
      <span
        v-if="selectedTab?.comparisonLabel"
        class="bg-primary-bg text-primary max-w-36 rounded-full px-3 py-2 text-center text-caption font-semibold break-keep"
      >
        {{ selectedTab.comparisonLabel }}
      </span>
    </div>

    <div class="mt-5 grid grid-cols-3 gap-2" role="tablist" aria-label="공과금 종류">
      <button
        v-for="tab in tabs"
        :key="tab.utilityType"
        type="button"
        role="tab"
        class="min-h-11 rounded-md border-0 text-label font-semibold"
        :class="selectedUtilityType === tab.utilityType ? 'bg-primary text-white' : 'bg-confirmed-bg text-muted'"
        :aria-selected="selectedUtilityType === tab.utilityType"
        @click="selectedUtilityType = tab.utilityType"
      >
        {{ formatUtilityType(tab.utilityType) }}
      </button>
    </div>

    <template v-if="selectedTab?.available">
      <div class="mt-5 grid grid-cols-2 gap-3">
        <div class="bg-canvas rounded-md p-4">
          <span class="text-caption text-muted block">이번 달 나의 사용량</span>
          <strong class="text-list-title text-ink mt-2 block tabular-nums">
            {{ usageLabel(selectedTab.myUsage) }}
          </strong>
        </div>
        <div class="bg-primary-bg rounded-md p-4">
          <span class="text-caption text-muted block">1인 가구 평균</span>
          <strong class="text-list-title text-primary mt-2 block tabular-nums">
            {{ usageLabel(selectedTab.averageUsage) }}
          </strong>
        </div>
      </div>

      <p
        class="text-body-strong mt-4 mb-0"
        :class="Number(selectedTab.differenceUsage) > 0 ? 'text-negative' : 'text-primary'"
      >
        {{ differenceLabel }}
      </p>

      <div class="text-caption text-muted mt-5 flex justify-end gap-4">
        <span class="flex items-center gap-1.5"><i class="bg-primary size-2 rounded-full" />나</span>
        <span class="flex items-center gap-1.5"><i class="bg-control-off size-2 rounded-full" />1인 가구 평균</span>
      </div>

      <svg
        class="mt-2 h-40 w-full overflow-visible"
        viewBox="0 0 100 100"
        role="img"
        :aria-label="`${formatUtilityType(selectedTab.utilityType)} 최근 6개월 1인 가구 평균 사용량 비교 그래프`"
      >
        <line
          v-for="y in [16, 48, 80]"
          :key="y"
          x1="6"
          :y1="y"
          x2="94"
          :y2="y"
          class="stroke-divider"
          stroke-width="0.5"
        />
        <path
          :d="linePath('averageUsage')"
          fill="none"
          class="stroke-control-off"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          data-testid="average-line"
        />
        <path
          :d="linePath('myUsage')"
          fill="none"
          class="stroke-primary"
          stroke-width="2.4"
          stroke-linecap="round"
          stroke-linejoin="round"
          data-testid="my-line"
        />
        <template v-for="(point, index) in series" :key="point.yearMonth">
          <circle
            :cx="pointX(index)"
            :cy="pointY(point.averageUsage)"
            r="1.6"
            class="fill-control-off"
          />
          <circle
            v-if="point.myUsage !== null && point.myUsage !== undefined"
            :cx="pointX(index)"
            :cy="pointY(point.myUsage)"
            r="2"
            class="fill-primary stroke-surface"
            stroke-width="1"
            data-testid="my-point"
          />
        </template>
      </svg>
      <div class="text-caption-sm text-muted -mt-2 flex justify-between px-2">
        <span v-for="point in series" :key="point.yearMonth">
          {{ formatMonthOnly(point.yearMonth) }}
        </span>
      </div>

      <p v-if="series.some((point) => point.myUsage === null)" class="text-caption text-muted mt-4 mb-0">
        고지서가 없는 달은 나의 사용량 선을 연결하지 않았어요.
      </p>

      <dl class="border-divider text-caption text-muted mt-5 grid grid-cols-[auto_1fr] gap-x-3 gap-y-2 border-t pt-4">
        <dt>기준</dt>
        <dd class="m-0 text-right">{{ selectedTab.referencePeriod }}</dd>
        <dt>출처</dt>
        <dd class="m-0 text-right">{{ selectedTab.sourceName }}</dd>
        <dt>산출</dt>
        <dd class="m-0 text-right break-keep">{{ selectedTab.calculationBasis }}</dd>
      </dl>
      <p v-if="selectedTab.note" class="text-caption-sm text-muted mt-3 mb-0 break-keep">
        {{ selectedTab.note }}
      </p>
    </template>

    <p v-else class="text-body-sm text-muted my-10 text-center">
      1인 가구 비교 데이터를 준비하고 있어요.
    </p>
  </section>
</template>
