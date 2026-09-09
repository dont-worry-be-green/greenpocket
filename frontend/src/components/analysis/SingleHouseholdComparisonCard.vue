<script setup>
/*
 * 1인 가구 평균 사용량 비교 카드 (AN-07 · 기능명세서 A-3-07)
 * 세그먼트(전기·도시가스·수도) → 나/평균 타일 → 차이 문장 → 최근 6개월 꺾은선 → 출처.
 * 평균은 점선(icon-off), 나는 실선(primary-soft). 고지서가 없는 달은 선을 끊고 점을 그리지 않는다(C-35).
 * 마지막 달에는 두 값을 그래프 위에 바로 적는다 — 툴팁 없이도 「지금」이 읽히도록.
 */
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
  yearMonth: { type: String, default: '' },
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
const monthLabel = computed(() => (props.yearMonth ? `${formatMonthOnly(props.yearMonth)} ` : '이번 달 '))
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

/* 마지막 달 값 라벨. 두 점이 가까우면 위·아래로 갈라 붙인다 */
const lastIndex = computed(() => series.value.length - 1)
const lastPoint = computed(() => (lastIndex.value >= 0 ? series.value[lastIndex.value] : null))
const lastLabels = computed(() => {
  const point = lastPoint.value
  if (!point) return []
  const myY = pointY(point.myUsage)
  const avgY = pointY(point.averageUsage)
  const labels = []
  if (avgY !== null) {
    const above = myY === null || avgY <= myY
    labels.push({ key: 'avg', y: above ? avgY - 7 : avgY + 13, text: usageLabel(point.averageUsage), mine: false })
  }
  if (myY !== null) {
    const above = avgY === null || myY < avgY
    labels.push({ key: 'me', y: above ? myY - 7 : myY + 13, text: usageLabel(point.myUsage), mine: true })
  }
  return labels
})

function usageLabel(value) {
  return formatUsage(value, 1, usageUnit.value)
}

function myUsageLabel(value) {
  return value === null || value === undefined ? '고지서 없음' : usageLabel(value)
}

function activatePoint(index) {
  activePointIndex.value = index
}

const difference = computed(() => {
  const tab = selectedTab.value
  if (!tab?.available || tab.differenceUsage === null || tab.differenceUsage === undefined) return null
  const value = Number(tab.differenceUsage)
  if (value === 0) return { direction: 'same', text: '1인 가구 평균과 같아요' }
  const rate = formatPercent(Math.abs(Number(tab.differenceRate)))
  return {
    direction: value > 0 ? 'up' : 'down',
    text: `평균보다 ${usageLabel(Math.abs(value))} (${rate}) ${value > 0 ? '더 많이' : '더 적게'} 썼어요`,
  }
})
</script>

<template>
  <section v-if="comparison" class="bg-surface rounded-card shadow-card mt-3 p-5">
    <div>
      <h2 class="text-section tracking-title text-ink m-0">1인 가구 평균과 비교</h2>
      <p class="text-caption text-muted mt-0.5 mb-0">
        사용량 · 최근 6개월<template v-if="selectedTab?.comparisonLabel"> · {{ selectedTab.comparisonLabel }}</template>
      </p>
    </div>

    <div class="bg-surface-sub mt-3.5 grid grid-cols-3 gap-1 rounded-md p-1" role="tablist" aria-label="공과금 종류">
      <button
        v-for="tab in tabs"
        :key="tab.utilityType"
        type="button"
        role="tab"
        class="h-8.5 cursor-pointer rounded-sm border-0 text-label"
        :class="
          selectedUtilityType === tab.utilityType
            ? 'bg-surface text-ink font-extrabold'
            : 'text-muted bg-transparent font-semibold'
        "
        :aria-selected="selectedUtilityType === tab.utilityType"
        @click="selectedUtilityType = tab.utilityType"
      >
        {{ formatUtilityType(tab.utilityType) }}
      </button>
    </div>

    <template v-if="selectedTab?.available">
      <div class="mt-3.5 grid grid-cols-2 gap-2">
        <div class="bg-primary-bg rounded-lg px-3.5 pt-3.5 pb-3">
          <span class="text-caption text-muted block font-semibold">{{ monthLabel }}나</span>
          <strong class="text-amount tracking-display text-primary-on-soft mt-1.5 block tabular-nums">
            {{ usageLabel(selectedTab.myUsage) }}
          </strong>
        </div>
        <div class="bg-surface-sub rounded-lg px-3.5 pt-3.5 pb-3">
          <span class="text-caption text-muted block font-semibold">1인 가구 평균</span>
          <strong class="text-amount tracking-display text-ink mt-1.5 block tabular-nums">
            {{ usageLabel(selectedTab.averageUsage) }}
          </strong>
        </div>
      </div>

      <p
        v-if="difference"
        class="text-body-strong mt-3.5 mb-0 flex items-center gap-1 tabular-nums"
        :class="
          difference.direction === 'up'
            ? 'text-increase'
            : difference.direction === 'down'
              ? 'text-decrease'
              : 'text-muted'
        "
      >
        <svg
          v-if="difference.direction !== 'same'"
          class="size-3.5 flex-none"
          viewBox="0 0 24 24"
          fill="currentColor"
          aria-hidden="true"
        >
          <path
            v-if="difference.direction === 'down'"
            d="M11 3h2v12.6l4.3-4.3 1.4 1.4L12 19.4l-6.7-6.7 1.4-1.4L11 15.6z"
          />
          <path v-else d="M13 21h-2V8.4l-4.3 4.3-1.4-1.4L12 4.6l6.7 6.7-1.4 1.4L13 8.4z" />
        </svg>
        {{ difference.text }}
      </p>

      <div class="text-caption-sm text-muted mt-3.5 flex justify-end gap-3">
        <span class="flex items-center gap-1.5">
          <i class="border-icon-off block w-3.5 border-t-2 border-dashed" />1인 가구 평균
        </span>
        <span class="flex items-center gap-1.5"><i class="border-primary-soft block w-3.5 border-t-[2.4px]" />나</span>
      </div>

      <div class="relative" @mouseleave="activePointIndex = null">
        <div
          v-if="activePoint"
          class="bg-ink text-caption text-surface shadow-card pointer-events-none absolute top-0 z-10 min-w-36 -translate-x-1/2 rounded-md px-3 py-2"
          :style="{ left: `${tooltipPosition}%` }"
          role="status"
          data-testid="usage-tooltip"
        >
          <strong class="block">{{ formatMonthOnly(activePoint.yearMonth) }}</strong>
          <span class="mt-1 block">나 {{ myUsageLabel(activePoint.myUsage) }}</span>
          <span class="block">1인 가구 평균 {{ usageLabel(activePoint.averageUsage) }}</span>
        </div>

        <svg
          class="mt-1 h-40 w-full overflow-visible"
          viewBox="0 0 300 100"
          role="img"
          :aria-label="`${formatUtilityType(selectedTab.utilityType)} 최근 6개월 1인 가구 평균 사용량 비교 그래프`"
        >
          <line
            v-for="y in [8, 50, 92]"
            :key="y"
            x1="17"
            :y1="y"
            x2="283"
            :y2="y"
            class="stroke-divider"
            stroke-width="0.8"
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
            class="stroke-icon-off"
            stroke-width="2"
            stroke-dasharray="4 4"
            stroke-linecap="round"
            stroke-linejoin="round"
            data-testid="average-line"
          />
          <path
            :d="linePath('myUsage')"
            fill="none"
            class="stroke-primary-soft"
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
              class="fill-icon-off"
              data-testid="average-point"
            />
            <circle
              v-if="point.myUsage !== null && point.myUsage !== undefined"
              :cx="pointX(index)"
              :cy="pointY(point.myUsage)"
              r="3.2"
              class="fill-primary-soft stroke-surface"
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
          <text
            v-for="label in lastLabels"
            :key="label.key"
            :x="pointX(lastIndex)"
            :y="label.y"
            text-anchor="end"
            class="text-badge tabular-nums"
            :class="label.mine ? 'fill-ink' : 'fill-muted'"
            data-testid="last-label"
          >
            {{ label.text }}
          </text>
        </svg>
        <div class="grid grid-cols-6">
          <button
            v-for="(point, index) in series"
            :key="point.yearMonth"
            type="button"
            class="text-caption-sm min-h-11 border-0 bg-transparent p-0"
            :class="index === lastIndex ? 'text-ink font-extrabold' : 'text-muted'"
            data-testid="month-axis"
            @mouseenter="activatePoint(index)"
            @focus="activatePoint(index)"
            @click="activatePoint(index)"
          >
            {{ formatMonthOnly(point.yearMonth) }}
          </button>
        </div>
      </div>

      <p v-if="series.some((point) => point.myUsage === null)" class="text-caption-sm text-muted mt-2 mb-0">
        고지서가 없는 달은 나의 사용량 선을 연결하지 않았어요.
      </p>

      <p class="border-divider text-caption-sm text-muted mt-3 mb-0 border-t pt-3 leading-relaxed">
        <span class="text-ink-soft font-bold">출처 · {{ shortSourceName }}</span>
        <template v-if="selectedTab.calculationBasis"> {{ selectedTab.calculationBasis }}.</template>
        <template v-if="selectedTab.note"> {{ selectedTab.note }}</template>
      </p>
    </template>

    <p v-else class="text-body-sm text-muted my-10 text-center">
      1인 가구 비교 데이터를 준비하고 있어요.
    </p>
  </section>
</template>
