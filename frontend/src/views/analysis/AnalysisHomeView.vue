<script setup>
/*
 * 진단 홈 (AN-01 빈 상태 · AN-07 결과) — 기능명세서 A-3
 * v3 디자인 토큰. 정보 구조는 「얼마 냈나 → 작년 8월과 비교 → 1인 가구 평균과 비교」 카드 셋.
 *   ① A-3-05 월 생활요금 합계 — 히어로 금액 + 항목 3열(요금 배지 · 금액). 작년 대비 배지는 ② 카드의 총 차액 칩이 맡는다
 *   ② A-3-06 작년 동월 비교 — 작년(회색)·올해(브랜드 초록) 그룹 막대, 막대별 실제 금액 라벨, 항목별 원화 차이
 *   ③ A-3-07 1인 가구 평균 비교 — SingleHouseholdComparisonCard
 * 헤더: 청구 월 드롭다운(A-3-09) + 고지서 등록 「+」 알약. 둘 다 결과가 있을 때만.
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppTabLayout from '@/components/layout/AppTabLayout.vue'
import SingleHouseholdComparisonCard from '@/components/analysis/SingleHouseholdComparisonCard.vue'
import GpButton from '@/components/ui/GpButton.vue'
import IconChart from '@/components/ui/icons/IconChart.vue'
import IconChartLineUp from '@/components/ui/icons/IconChartLineUp.vue'
import IconChevronDown from '@/components/ui/icons/IconChevronDown.vue'
import IconPlus from '@/components/ui/icons/IconPlus.vue'
import IconReceipt from '@/components/ui/icons/IconReceipt.vue'
import { useAnalysisStore } from '@/stores/analysis'
import { formatMonth, formatMonthOnly, formatNumber, formatUtilityType, formatWon } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useAnalysisStore()
const selectedMonth = ref('')
const isMonthMenuOpen = ref(false)
const monthDropdown = ref(null)

/* 요금 배지 — 글자색 AA, 바탕은 옅게(design-system 2-5). 색만으로 구분하지 않고 글자가 정체를 든다 */
const UTILITY_TONE = {
  ELECTRICITY: 'bg-elec-bg text-elec',
  GAS: 'bg-gas-bg text-gas',
  WATER: 'bg-water-bg text-water',
}
const UTILITY_ORDER = ['ELECTRICITY', 'GAS', 'WATER']

/* 그룹 막대 좌표계 — viewBox 321×132, 바닥 118, 최대 막대 92. 쌍 사이 12 를 둬 금액 라벨이 안 겹친다 */
const BAR = { width: 321, base: 118, plot: 92, bar: 30, gap: 12, minHeight: 6 }

const isEmptyPreview = computed(() => route.query.preview === 'empty')
const isConfirmedPreview = computed(() => route.query.preview === 'confirmed')
const diagnosis = computed(() =>
  isEmptyPreview.value
    ? { empty: true, targetYearMonth: '2026-08', screen: 'AN-01' }
    : store.diagnosis,
)

const targetYearMonth = computed(
  () => diagnosis.value?.yearMonth ?? store.targetMonth?.targetYearMonth ?? diagnosis.value?.targetYearMonth,
)
const targetMonthOnlyLabel = computed(() => formatMonthOnly(targetYearMonth.value))

const summaryItems = computed(() =>
  [...(diagnosis.value?.summary?.items ?? [])].sort(
    (left, right) => UTILITY_ORDER.indexOf(left.utilityType) - UTILITY_ORDER.indexOf(right.utilityType),
  ),
)

const lastYear = computed(() => diagnosis.value?.lastYearComparison ?? null)
const lastYearMax = computed(() => {
  const amounts = (lastYear.value?.items ?? []).flatMap((item) => [item.lastYearAmount, item.thisYearAmount])
  return Math.max(...amounts.filter((value) => value !== null && value !== undefined), 1)
})

/* 작년·올해 막대. 값이 없는 쪽(N/A)은 막대를 그리지 않는다 (A-3-06 한쪽만 있는 항목) */
const lastYearBars = computed(() =>
  (lastYear.value?.items ?? []).map((item, groupIndex) => {
    const center = (BAR.width / 6) * (2 * groupIndex + 1)
    const toBar = (amount, isThisYear) => {
      if (amount === null || amount === undefined) return null
      const height = Math.max((amount / lastYearMax.value) * BAR.plot, BAR.minHeight)
      const x = isThisYear ? center + BAR.gap / 2 : center - BAR.bar - BAR.gap / 2
      const y = BAR.base - height
      return {
        amount,
        x,
        y,
        labelX: x + BAR.bar / 2,
        path: `M${x},${BAR.base} V${y + 4} a4,4 0 0 1 4,-4 H${x + BAR.bar - 4} a4,4 0 0 1 4,4 V${BAR.base} Z`,
      }
    }
    return {
      utilityType: item.utilityType,
      diff: item.diff,
      lastYear: toBar(item.lastYearAmount, false),
      thisYear: toBar(item.thisYearAmount, true),
    }
  }),
)

function diffParts(amount) {
  if (amount === null || amount === undefined) return null
  if (amount === 0) return { direction: 'same', label: '작년과 같아요' }
  return {
    direction: amount < 0 ? 'down' : 'up',
    label: formatWon(Math.abs(amount)),
  }
}

onMounted(async () => {
  document.addEventListener('pointerdown', closeMonthMenuOutside)
  document.addEventListener('keydown', closeMonthMenuWithEscape)

  if (isEmptyPreview.value || isConfirmedPreview.value) return

  const requestedMonth = typeof route.query.month === 'string' ? route.query.month : undefined
  await Promise.all([store.fetchHome(requestedMonth), store.fetchDiagnosisMonths()])
  selectedMonth.value = store.diagnosis?.yearMonth ?? requestedMonth ?? ''
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', closeMonthMenuOutside)
  document.removeEventListener('keydown', closeMonthMenuWithEscape)
})

function closeMonthMenuOutside(event) {
  if (!monthDropdown.value?.contains(event.target)) isMonthMenuOpen.value = false
}

function closeMonthMenuWithEscape(event) {
  if (event.key === 'Escape') isMonthMenuOpen.value = false
}

function toggleMonthMenu() {
  if (store.isLoading || store.areMonthsLoading || !store.diagnosisMonths.length) return
  isMonthMenuOpen.value = !isMonthMenuOpen.value
}

async function changeMonth(month) {
  isMonthMenuOpen.value = false
  if (!month || month === diagnosis.value?.yearMonth) return

  selectedMonth.value = month
  await router.replace({ query: { ...route.query, month } })
  await store.fetchHome(month)
}

function goToRegistration() {
  router.push({ path: '/analysis/bills/new', query: { month: targetYearMonth.value } })
}
</script>

<template>
  <AppTabLayout tab="analysis" title="진단">
    <template v-if="diagnosis?.summary" #headerAction>
      <div class="flex shrink-0 items-center gap-1.5">
        <div ref="monthDropdown" class="relative">
          <button
            type="button"
            class="bg-surface shadow-card text-caption text-ink-soft flex min-h-9 cursor-pointer items-center gap-0.5 rounded-full border-0 py-2 pr-2.5 pl-3 font-bold tabular-nums disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="store.isLoading || store.areMonthsLoading || !store.diagnosisMonths.length"
            aria-haspopup="listbox"
            :aria-expanded="isMonthMenuOpen"
            @click="toggleMonthMenu"
          >
            {{ formatMonth(selectedMonth || targetYearMonth) }}
            <IconChevronDown
              :size="13"
              class="text-icon-off transition-transform"
              :class="isMonthMenuOpen ? 'rotate-180' : ''"
              aria-hidden="true"
            />
          </button>

          <ul
            v-if="isMonthMenuOpen"
            class="bg-surface shadow-card absolute top-full right-0 z-30 mt-2 min-w-full list-none overflow-hidden rounded-lg p-1"
            role="listbox"
            aria-label="등록된 청구 월"
          >
            <li v-for="month in store.diagnosisMonths" :key="month.yearMonth" role="option">
              <button
                type="button"
                class="text-ink hover:bg-primary-bg focus-visible:bg-primary-bg min-h-11 w-full rounded-sm border-0 bg-transparent px-3 py-2 text-left text-label font-semibold whitespace-nowrap outline-hidden"
                :class="selectedMonth === month.yearMonth ? 'text-primary-on-soft bg-primary-bg' : ''"
                :aria-selected="selectedMonth === month.yearMonth"
                @click="changeMonth(month.yearMonth)"
              >
                {{ formatMonth(month.yearMonth) }}
              </button>
            </li>
          </ul>
        </div>

        <button
          type="button"
          class="bg-surface shadow-card text-primary-on-soft flex size-9 cursor-pointer items-center justify-center rounded-full border-0 p-0"
          aria-label="고지서 등록"
          @click="goToRegistration"
        >
          <IconPlus :size="14" />
        </button>
      </div>
    </template>

    <div v-if="store.isLoading && !isEmptyPreview" class="bg-surface rounded-card h-72 animate-pulse" />

    <section
      v-else-if="store.error && !isEmptyPreview"
      class="bg-surface rounded-card shadow-card px-5 py-8 text-center"
    >
      <p class="text-body-strong text-ink mt-0 mb-1">정보를 불러오지 못했어요</p>
      <p class="text-caption text-muted mt-0 mb-5">{{ store.error.message }}</p>
      <GpButton variant="wide" size="wide" @click="store.fetchHome">다시 시도</GpButton>
    </section>

    <!-- AN-01 · A-3-04 빈 상태 -->
    <template v-else-if="diagnosis?.empty">
      <section class="bg-surface rounded-card shadow-card p-5">
        <div class="flex flex-col items-center pt-3.5 text-center">
          <span
            class="bg-primary-bg text-primary-soft flex size-16 items-center justify-center rounded-xl"
            aria-hidden="true"
          >
            <IconReceipt :size="30" />
          </span>
          <h2 class="text-section tracking-title text-ink mt-4 mb-0">아직 등록된 고지서가 없어요</h2>
          <p class="text-caption text-muted mx-auto mt-2 mb-0 max-w-70 break-keep">
            전기·수도·도시가스가 포함된 관리비 고지서도 한 번에 분석할 수 있어요
          </p>
        </div>
        <GpButton class="mt-5" @click="goToRegistration">{{ targetMonthOnlyLabel }} 고지서 등록하기</GpButton>
      </section>

      <section class="bg-surface rounded-card shadow-card mt-3 p-5">
        <h2 class="text-section tracking-title text-ink m-0">등록하면 보이는 것</h2>
        <ul class="mt-3 mb-0 flex list-none flex-col gap-2.5 p-0">
          <li class="flex items-center gap-3">
            <span class="bg-surface-sub text-primary-soft flex size-10 shrink-0 items-center justify-center rounded-md">
              <IconReceipt :size="20" />
            </span>
            <div>
              <strong class="text-body-strong text-ink block">항목별 요금</strong>
              <span class="text-caption text-muted block">전기 · 도시가스 · 수도 금액과 합계</span>
            </div>
          </li>
          <li class="flex items-center gap-3">
            <span class="bg-surface-sub text-primary-soft flex size-10 shrink-0 items-center justify-center rounded-md">
              <IconChart :size="20" />
            </span>
            <div>
              <strong class="text-body-strong text-ink block">작년 같은 달과 비교</strong>
              <span class="text-caption text-muted block">에코마일리지 연동 뒤 원화 차이</span>
            </div>
          </li>
          <li class="flex items-center gap-3">
            <span class="bg-surface-sub text-primary-soft flex size-10 shrink-0 items-center justify-center rounded-md">
              <IconChartLineUp :size="20" />
            </span>
            <div>
              <strong class="text-body-strong text-ink block">1인 가구 평균과 비교</strong>
              <span class="text-caption text-muted block">사용량 차이와 최근 6개월 추이</span>
            </div>
          </li>
        </ul>
      </section>
    </template>

    <!-- AN-07 -->
    <template v-else-if="diagnosis?.summary">
      <!-- ① A-3-05 월 생활요금 합계 -->
      <section class="bg-surface rounded-card shadow-card p-5">
        <div class="flex items-start justify-between gap-2.5">
          <div class="min-w-0">
            <p class="text-caption text-muted m-0 font-semibold">{{ targetMonthOnlyLabel }} 생활요금</p>
            <p class="text-amount-hero tracking-display text-ink mt-0.5 mb-0 tabular-nums">
              {{ formatWon(diagnosis.summary.currentTotal) }}
            </p>
          </div>
          <span
            class="bg-primary-bg text-primary-soft flex size-13 shrink-0 items-center justify-center rounded-xl"
            aria-hidden="true"
          >
            <IconChart :size="26" />
          </span>
        </div>

        <ul class="border-divider mt-4 mb-0 grid list-none grid-cols-3 border-t p-0 pt-3.5">
          <li
            v-for="item in summaryItems"
            :key="item.utilityType"
            class="border-divider min-w-0 border-l pl-3 first:border-l-0 first:pl-0"
          >
            <span
              class="text-badge tracking-normal inline-flex items-center rounded-xs px-[5px] py-0.5"
              :class="UTILITY_TONE[item.utilityType]"
            >
              {{ formatUtilityType(item.utilityType) }}
            </span>
            <strong class="text-list-title tracking-body text-ink mt-1.5 block whitespace-nowrap tabular-nums">
              {{ formatWon(item.amount) }}
            </strong>
          </li>
        </ul>
      </section>

      <!-- ② A-3-06 작년 동월 비교 -->
      <section v-if="lastYear" class="bg-surface rounded-card shadow-card mt-3 p-5">
        <div class="flex items-start justify-between gap-2.5">
          <div>
            <h2 class="text-section tracking-title text-ink m-0">작년 {{ targetMonthOnlyLabel }}과 비교</h2>
            <p class="text-caption text-muted mt-0.5 mb-0">청구 금액 · 작년은 에코마일리지 연동값</p>
          </div>
          <span
            v-if="lastYear.available && diffParts(lastYear.totalDiff)"
            class="text-label mt-0.5 inline-flex shrink-0 items-center gap-1 rounded-sm px-2.5 py-1.5 font-bold tabular-nums"
            :class="
              diffParts(lastYear.totalDiff).direction === 'up'
                ? 'bg-increase-bg text-increase'
                : 'bg-decrease-bg text-decrease'
            "
          >
            <svg
              v-if="diffParts(lastYear.totalDiff).direction !== 'same'"
              class="size-[1em]"
              viewBox="0 0 24 24"
              fill="currentColor"
              aria-hidden="true"
            >
              <path
                v-if="diffParts(lastYear.totalDiff).direction === 'down'"
                d="M11 3h2v12.6l4.3-4.3 1.4 1.4L12 19.4l-6.7-6.7 1.4-1.4L11 15.6z"
              />
              <path v-else d="M13 21h-2V8.4l-4.3 4.3-1.4-1.4L12 4.6l6.7 6.7-1.4 1.4L13 8.4z" />
            </svg>
            총 {{ diffParts(lastYear.totalDiff).label }}
          </span>
        </div>

        <template v-if="lastYear.available">
          <div class="text-caption-sm text-muted mt-2.5 flex justify-end gap-3">
            <span class="flex items-center gap-1.5"><i class="bg-icon-off size-2 rounded-xs" />작년</span>
            <span class="flex items-center gap-1.5"><i class="bg-primary-soft size-2 rounded-xs" />올해</span>
          </div>

          <svg
            class="mt-1 block w-full overflow-visible"
            :viewBox="`0 0 ${BAR.width} 132`"
            role="img"
            :aria-label="`작년 ${targetMonthOnlyLabel}과 올해 ${targetMonthOnlyLabel} 항목별 청구 금액 비교`"
            data-testid="last-year-bars"
          >
            <line x1="0" :y1="BAR.base + 0.5" :x2="BAR.width" :y2="BAR.base + 0.5" class="stroke-divider" stroke-width="1" />
            <template v-for="group in lastYearBars" :key="group.utilityType">
              <template v-if="group.lastYear">
                <path :d="group.lastYear.path" class="fill-icon-off" data-testid="last-year-bar" />
                <text
                  :x="group.lastYear.labelX"
                  :y="group.lastYear.y - 6"
                  text-anchor="middle"
                  class="fill-muted text-badge tabular-nums"
                >
                  {{ formatNumber(group.lastYear.amount) }}
                </text>
              </template>
              <template v-if="group.thisYear">
                <path :d="group.thisYear.path" class="fill-primary-soft" data-testid="this-year-bar" />
                <text
                  :x="group.thisYear.labelX"
                  :y="group.thisYear.y - 6"
                  text-anchor="middle"
                  class="fill-ink text-badge tabular-nums"
                >
                  {{ formatNumber(group.thisYear.amount) }}
                </text>
              </template>
            </template>
          </svg>

          <div class="mt-1.5 grid grid-cols-3">
            <div
              v-for="group in lastYearBars"
              :key="group.utilityType"
              class="flex flex-col items-center gap-1"
            >
              <span
                class="text-badge tracking-normal inline-flex items-center rounded-xs px-[5px] py-0.5"
                :class="UTILITY_TONE[group.utilityType]"
              >
                {{ formatUtilityType(group.utilityType) }}
              </span>
              <span
                v-if="diffParts(group.diff)"
                class="text-caption-sm inline-flex items-center gap-0.5 font-bold tabular-nums"
                :class="
                  diffParts(group.diff).direction === 'up'
                    ? 'text-increase'
                    : diffParts(group.diff).direction === 'down'
                      ? 'text-decrease'
                      : 'text-muted'
                "
              >
                <svg
                  v-if="diffParts(group.diff).direction !== 'same'"
                  class="size-[1em]"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                  aria-hidden="true"
                >
                  <path
                    v-if="diffParts(group.diff).direction === 'down'"
                    d="M11 3h2v12.6l4.3-4.3 1.4 1.4L12 19.4l-6.7-6.7 1.4-1.4L11 15.6z"
                  />
                  <path v-else d="M13 21h-2V8.4l-4.3 4.3-1.4-1.4L12 4.6l6.7 6.7-1.4 1.4L13 8.4z" />
                </svg>
                {{ diffParts(group.diff).label }}
              </span>
              <span v-else class="text-caption-sm text-muted">작년 값 없음</span>
            </div>
          </div>
        </template>
        <p v-else class="text-body-sm text-muted my-8 text-center">
          What-if에서 에코마일리지를 연동하면 작년과 비교돼요
        </p>
      </section>

      <!-- ③ A-3-07 1인 가구 평균 비교 -->
      <SingleHouseholdComparisonCard
        v-if="diagnosis.singleHouseholdComparison"
        :comparison="diagnosis.singleHouseholdComparison"
        :year-month="diagnosis.yearMonth"
      />
    </template>

    <section v-else class="bg-surface rounded-card shadow-card px-5 py-8 text-center">
      <p class="text-body-strong text-ink mt-0 mb-1">생활비 분석 결과를 준비하고 있어요</p>
    </section>
  </AppTabLayout>
</template>
