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
const activePointIndex = ref(null)
const tabs = computed(() => props.comparison?.tabs ?? [])
const selectedTab = computed(
  () =>
    tabs.value.find((tab) => tab.utilityType === selectedUtilityType.value) ?? tabs.value[0] ?? null,
)
const series = computed(() => selectedTab.value?.series ?? [])
const usageUnit = computed(() => formatUnit(selectedTab.value?.usageUnit))
const activePoint = computed(() =>
  activePointIndex.value === null ? null : series.value[activePointIndex.value],
)
const tooltipPosition = computed(() => {
  if (activePointIndex.value === null || !series.value.length) return 50

  const centered = ((activePointIndex.value + 0.5) / series.value.length) * 100
  return Math.min(82, Math.max(18, centered))
})
const shortSourceName = computed(() => {
  const sourceName = selectedTab.value?.sourceName ?? ''
  if (sourceName.includes('에너지경제연구원')) return '에너지경제연구원'
  if (sourceName.includes('서울물연구원')) return '서울물연구원'
  return sourceName
})

watch(
  tabs,
  (nextTabs) => {
    if (!nextTabs.some((tab) => tab.utilityType === selectedUtilityType.value)) {
      selectedUtilityType.value = nextTabs[0]?.utilityType ?? 'ELECTRICITY'
    }
    activePointIndex.value = null
  },
  { immediate: true },
)

watch(selectedUtilityType, () => {
  activePointIndex.value = null
})

const chartBounds = computed(() => {
  const values = series.value.flatMap((point) =>
    [point.myUsage, point.averageUsage].filter((value) => value !== null && value !== undefined),
  )
  if (!values.length) return { min: 0, range: 1 }

  const minimum = Math.min(...values)
  const maximum = Math.max(...values)
  const padding = Math.max((maximum - minimum) * 0.06, maximum * 0.02, 0.5)
  const min = Math.max(0, minimum - padding)
  return { min, range: maximum + padding - min || 1 }
})

function pointX(index) {
  return 25 + index * 50
}

function pointY(value) {
  if (value === null || value === undefined) return null
  return 92 - ((Number(value) - chartBounds.value.min) / chartBounds.value.range) * 84
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

function myUsageLabel(value) {
  return value === null || value === undefined ? '고지서 없음' : usageLabel(value)
}

function activatePoint(index) {
  activePointIndex.value = index
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

      <div class="relative -mt-10" @mouseleave="activePointIndex = null">
        <div
          v-if="activePoint"
          class="bg-ink text-caption text-surface pointer-events-none absolute top-0 z-10 min-w-36 -translate-x-1/2 rounded-md px-3 py-2 shadow-card"
          :style="{ left: `${tooltipPosition}%` }"
          role="status"
          data-testid="usage-tooltip"
        >
          <strong class="block">{{ formatMonthOnly(activePoint.yearMonth) }}</strong>
          <span class="mt-1 block">나 {{ myUsageLabel(activePoint.myUsage) }}</span>
          <span class="block">1인 가구 평균 {{ usageLabel(activePoint.averageUsage) }}</span>
        </div>

        <svg
          class="h-40 w-full overflow-visible"
          viewBox="0 0 300 100"
          role="img"
          :aria-label="`${formatUtilityType(selectedTab.utilityType)} 최근 6개월 1인 가구 평균 사용량 비교 그래프`"
        >
          <line
            v-for="y in [8, 50, 92]"
            :key="y"
            x1="25"
            :y1="y"
            x2="275"
            :y2="y"
            class="stroke-divider"
            stroke-width="0.5"
          />
          <line
            v-if="activePointIndex !== null"
            :x1="pointX(activePointIndex)"
            y1="6"
            :x2="pointX(activePointIndex)"
            y2="94"
            class="stroke-divider"
            stroke-width="0.8"
            stroke-dasharray="2 2"
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
              r="2.8"
              class="fill-control-off"
              data-testid="average-point"
            />
            <circle
              v-if="point.myUsage !== null && point.myUsage !== undefined"
              :cx="pointX(index)"
              :cy="pointY(point.myUsage)"
              r="3.2"
              class="fill-primary stroke-surface"
              stroke-width="1"
              data-testid="my-point"
            />
            <rect
              :x="index * 50"
              y="0"
              width="50"
              height="100"
              fill="transparent"
              tabindex="0"
              :aria-label="`${formatMonthOnly(point.yearMonth)}, 나 ${myUsageLabel(point.myUsage)}, 1인 가구 평균 ${usageLabel(point.averageUsage)}`"
              data-testid="chart-hit-area"
              @mouseenter="activatePoint(index)"
              @focus="activatePoint(index)"
              @click="activatePoint(index)"
            />
          </template>
        </svg>
        <div class="grid grid-cols-6">
          <button
            v-for="(point, index) in series"
            :key="point.yearMonth"
            type="button"
            class="text-caption-sm text-muted min-h-11 border-0 bg-transparent p-0"
            data-testid="month-axis"
            @mouseenter="activatePoint(index)"
            @focus="activatePoint(index)"
            @click="activatePoint(index)"
          >
            {{ formatMonthOnly(point.yearMonth) }}
          </button>
        </div>
      </div>

      <p v-if="series.some((point) => point.myUsage === null)" class="text-caption text-muted mt-4 mb-0">
        고지서가 없는 달은 나의 사용량 선을 연결하지 않았어요.
      </p>

      <p class="border-divider text-caption text-muted mt-5 mb-0 border-t pt-4">
        출처 · {{ shortSourceName }}
      </p>
      <p v-if="selectedTab.note" class="text-caption-sm text-muted mt-3 mb-0 break-keep">
        {{ selectedTab.note }}
      </p>
    </template>

    <p v-else class="text-body-sm text-muted my-10 text-center">
      1인 가구 비교 데이터를 준비하고 있어요.
    </p>
  </section>
</template>
