<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppTabLayout from '@/components/layout/AppTabLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import IconDrop from '@/components/ui/icons/IconDrop.vue'
import IconFlame from '@/components/ui/icons/IconFlame.vue'
import IconLightning from '@/components/ui/icons/IconLightning.vue'
import billIcon from '@/assets/icons/bill.svg'
import { useAnalysisStore } from '@/stores/analysis'
import { formatMonth, formatMonthOnly, formatSignedWon, formatUtilityType, formatWon } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useAnalysisStore()
const selectedUtilityType = ref('ELECTRICITY')
const UTILITY_STYLE = {
  ELECTRICITY: { icon: IconLightning, tone: 'summary-electricity' },
  WATER: { icon: IconDrop, tone: 'summary-water' },
  GAS: { icon: IconFlame, tone: 'summary-gas' },
}

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
const targetMonthLabel = computed(() => formatMonth(targetYearMonth.value))
const targetMonthOnlyLabel = computed(() => formatMonthOnly(targetYearMonth.value))
const selectedRegionTab = computed(() =>
  diagnosis.value?.regionComparison?.tabs?.find(
    (tab) => tab.utilityType === selectedUtilityType.value,
  ),
)
const lastYearChartMax = computed(() => {
  const amounts = diagnosis.value?.lastYearComparison?.items?.flatMap((item) => [
    item.lastYearAmount,
    item.thisYearAmount,
  ]) ?? [1]
  return Math.max(...amounts, 1)
})

function barHeight(amount) {
  return `${Math.max((amount / lastYearChartMax.value) * 100, 4)}%`
}

function linePoints(series, key) {
  if (!series?.length) return ''
  const values = series.flatMap((point) => [point.mine, point.regionAvg])
  const min = Math.min(...values)
  const max = Math.max(...values)
  const range = max - min || 1
  return series
    .map((point, index) => {
      const x = 8 + (index * 84) / Math.max(series.length - 1, 1)
      const y = 82 - ((point[key] - min) / range) * 62
      return `${x},${y}`
    })
    .join(' ')
}

function utilityCostLabel(utilityType) {
  return utilityType === 'GAS' ? '도시가스' : `${formatUtilityType(utilityType)}세`
}

onMounted(() => {
  if (!isEmptyPreview.value && !isConfirmedPreview.value) store.fetchHome()
})

function goToRegistration() {
  router.push({ path: '/analysis/bills/new', query: { month: targetYearMonth.value } })
}
</script>

<template>
  <AppTabLayout tab="analysis" title="진단">
    <p v-if="!diagnosis?.summary" class="text-section text-ink mt-8 mb-2">{{ targetMonthLabel }}</p>

    <div v-if="store.isLoading && !isEmptyPreview" class="bg-surface h-72 animate-pulse rounded-lg" />

    <section v-else-if="store.error && !isEmptyPreview" class="bg-surface rounded-lg px-5 py-8 text-center">
      <p class="text-body-strong text-ink mt-0 mb-1">정보를 불러오지 못했어요</p>
      <p class="text-caption text-muted mt-0 mb-5">{{ store.error.message }}</p>
      <GpButton variant="wide" size="wide" @click="store.fetchHome">다시 시도</GpButton>
    </section>

    <section v-else-if="diagnosis?.empty" class="bg-surface rounded-lg px-5 py-12 text-center">
      <div
        class="bg-primary-bg mx-auto flex size-28 flex-col items-center justify-center rounded-xl"
        aria-hidden="true"
      >
        <span class="relative flex size-14 items-center justify-center">
          <img :src="billIcon" alt="" class="size-8 drop-shadow-md" />
          <span class="scan-line absolute left-1/2 block h-0.5 w-11 rounded-full" />
        </span>
      </div>

      <h2 class="text-section text-ink mt-7 mb-3">아직 등록된 고지서가 없어요</h2>
      <p class="text-body-sm text-muted mx-auto mt-0 mb-8 max-w-80 break-keep">
        등록하면 전년 동월·지역 평균과 바로 비교해드려요.
      </p>

      <GpButton @click="goToRegistration">{{ targetMonthOnlyLabel }} 고지서 등록하기</GpButton>
    </section>

    <template v-else-if="diagnosis?.summary">
      <section class="analysis-summary relative mt-7 overflow-hidden rounded-xl px-6 py-7 text-white">
        <p class="text-body-strong relative mt-0 mb-4 text-white/60">
          {{ targetMonthOnlyLabel }} 생활요금 합계
        </p>
        <div class="relative flex items-center justify-between gap-3">
          <strong class="block text-[38px] leading-none font-bold tracking-tight tabular-nums">
            {{ formatWon(diagnosis.summary.currentTotal) }}
          </strong>
          <span
            v-if="diagnosis.summary.hasPreviousYear"
            class="rounded-full bg-white/12 px-3 py-2 text-label font-semibold whitespace-nowrap"
          >
            작년보다 {{ formatSignedWon(diagnosis.summary.diffLastYearTotal) }}
          </span>
        </div>

        <ul class="relative mt-6 mb-0 grid list-none grid-cols-3 p-0">
          <li
            v-for="item in diagnosis.summary.items"
            :key="item.utilityType"
            class="border-divider/25 min-w-0 border-l px-3 first:border-l-0 first:pl-0 last:pr-0"
          >
            <span class="text-caption flex items-center gap-2 text-white/60">
              <component
                :is="UTILITY_STYLE[item.utilityType]?.icon"
                :size="17"
                :class="UTILITY_STYLE[item.utilityType]?.tone"
              />
              {{ utilityCostLabel(item.utilityType) }}
            </span>
            <strong
              class="text-list-title mt-3 block tabular-nums"
              :class="UTILITY_STYLE[item.utilityType]?.tone"
            >
              {{ formatWon(item.amount) }}
            </strong>
          </li>
        </ul>
      </section>

      <section v-if="diagnosis.lastYearComparison" class="bg-surface mt-5 rounded-xl px-5 py-6">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h2 class="text-section text-ink mt-0 mb-1">작년 동월과 비교</h2>
            <p class="text-caption text-muted m-0">작년과 올해 청구 금액</p>
          </div>
          <span
            v-if="diagnosis.lastYearComparison.available"
            class="bg-primary-bg text-primary rounded-full px-3 py-2 text-label font-semibold"
          >
            총 {{ formatSignedWon(diagnosis.lastYearComparison.totalDiff) }}
          </span>
        </div>

        <template v-if="diagnosis.lastYearComparison.available">
          <div class="text-caption text-muted mt-6 flex justify-end gap-4">
            <span class="flex items-center gap-1"><i class="bg-control-off size-2 rounded-xs" />작년</span>
            <span class="flex items-center gap-1"><i class="bg-primary size-2 rounded-xs" />올해</span>
          </div>
          <div class="mt-3 grid h-40 grid-cols-3 gap-4 border-b border-divider px-2">
            <div
              v-for="item in diagnosis.lastYearComparison.items"
              :key="item.utilityType"
              class="flex min-w-0 flex-col justify-end"
            >
              <div class="flex h-28 items-end justify-center gap-1.5">
                <div class="bg-control-off relative w-7 rounded-t-sm" :style="{ height: barHeight(item.lastYearAmount) }">
                  <span class="text-caption-sm text-muted absolute -top-5 left-1/2 -translate-x-1/2 tabular-nums">
                    {{ item.lastYearAmount.toLocaleString('ko-KR') }}
                  </span>
                </div>
                <div class="bg-primary relative w-7 rounded-t-sm" :style="{ height: barHeight(item.thisYearAmount) }">
                  <span class="text-caption-sm text-muted absolute -top-5 left-1/2 -translate-x-1/2 tabular-nums">
                    {{ item.thisYearAmount.toLocaleString('ko-KR') }}
                  </span>
                </div>
              </div>
              <span class="text-caption text-muted py-2 text-center">{{ utilityCostLabel(item.utilityType) }}</span>
            </div>
          </div>
        </template>
        <p v-else class="text-body-sm text-muted my-10 text-center">작년 비교 데이터를 준비하고 있어요.</p>
      </section>

      <section v-if="diagnosis.regionComparison" class="bg-surface mt-5 rounded-xl px-5 py-6">
        <div class="flex items-start justify-between gap-3">
          <div>
            <h2 class="text-section text-ink mt-0 mb-1">같은 지역 가구 평균</h2>
            <p class="text-caption text-muted m-0">최근 6개월 우리 집과 지역 평균 추이</p>
          </div>
          <span class="bg-primary-bg text-primary rounded-full px-3 py-2 text-label font-semibold">
            {{ diagnosis.regionComparison.regionLabel }}
          </span>
        </div>

        <div class="mt-5 grid grid-cols-3 gap-2">
          <button
            v-for="tab in diagnosis.regionComparison.tabs"
            :key="tab.utilityType"
            type="button"
            class="min-h-11 rounded-md border-0 text-label"
            :class="selectedUtilityType === tab.utilityType ? 'bg-primary text-white' : 'bg-confirmed-bg text-muted'"
            @click="selectedUtilityType = tab.utilityType"
          >
            {{ utilityCostLabel(tab.utilityType) }}
          </button>
        </div>

        <template v-if="selectedRegionTab?.available">
          <div class="mt-4 flex items-end justify-between gap-3">
            <strong class="text-title text-ink tabular-nums">{{ formatWon(selectedRegionTab.myAmount) }}</strong>
            <strong class="text-body-strong text-negative">
              지역 평균보다 {{ formatSignedWon(selectedRegionTab.diffRegion) }}
            </strong>
          </div>
          <div class="text-caption text-muted mt-5 flex justify-end gap-4">
            <span class="flex items-center gap-1"><i class="bg-primary size-2 rounded-full" />우리 집</span>
            <span class="flex items-center gap-1"><i class="bg-control-off size-2 rounded-full" />지역 평균</span>
          </div>
          <svg class="mt-2 h-40 w-full" viewBox="0 0 100 100" role="img" aria-label="최근 6개월 지역 평균 비교 그래프">
            <line v-for="y in [20, 50, 80]" :key="y" x1="6" :y1="y" x2="94" :y2="y" class="stroke-divider" stroke-width="0.5" />
            <polyline :points="linePoints(selectedRegionTab.series, 'regionAvg')" fill="none" class="stroke-control-off" stroke-width="2" />
            <polyline :points="linePoints(selectedRegionTab.series, 'mine')" fill="none" class="stroke-primary" stroke-width="2" />
            <circle
              v-for="(point, index) in selectedRegionTab.series"
              :key="point.yearMonth"
              :cx="8 + (index * 84) / Math.max(selectedRegionTab.series.length - 1, 1)"
              :cy="linePoints(selectedRegionTab.series, 'mine').split(' ')[index].split(',')[1]"
              r="2"
              class="fill-primary"
            />
          </svg>
          <div class="text-caption-sm text-muted -mt-2 flex justify-between px-2">
            <span v-for="point in selectedRegionTab.series" :key="point.yearMonth">
              {{ Number(point.yearMonth.split('-')[1]) }}월
            </span>
          </div>
        </template>
        <p v-else class="text-body-sm text-muted my-10 text-center">지역 비교 데이터를 준비하고 있어요.</p>
      </section>
    </template>

    <section v-else class="bg-surface rounded-lg px-5 py-8 text-center">
      <p class="text-body-strong text-ink mt-0 mb-1">생활비 분석 결과를 준비하고 있어요</p>
    </section>
  </AppTabLayout>
</template>

<style scoped>
.scan-line {
  top: 50%;
  background: var(--color-primary-soft);
  box-shadow: 0 0 8px color-mix(in srgb, var(--color-primary-soft) 55%, transparent);
  animation: scan-bill 1.6s var(--ease-standard) infinite alternate;
}

.analysis-summary {
  background: linear-gradient(135deg, #3a6e52 0%, #4f8d6d 100%);
}

.summary-electricity {
  color: #8cf0af;
}

.summary-water {
  color: #8fc3f2;
}

.summary-gas {
  color: #f5be7e;
}

@keyframes scan-bill {
  from {
    transform: translate(-50%, calc(-50% - 18px));
    opacity: 0.55;
  }
  to {
    transform: translate(-50%, calc(-50% + 18px));
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .scan-line {
    animation: none;
    transform: translate(-50%, 0);
  }
}
</style>
