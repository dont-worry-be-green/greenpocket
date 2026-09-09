<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppTabLayout from '@/components/layout/AppTabLayout.vue'
import SingleHouseholdComparisonCard from '@/components/analysis/SingleHouseholdComparisonCard.vue'
import GpButton from '@/components/ui/GpButton.vue'
import IconChevronDown from '@/components/ui/icons/IconChevronDown.vue'
import IconDrop from '@/components/ui/icons/IconDrop.vue'
import IconFlame from '@/components/ui/icons/IconFlame.vue'
import IconLightning from '@/components/ui/icons/IconLightning.vue'
import billIcon from '@/assets/icons/bill.svg'
import { useAnalysisStore } from '@/stores/analysis'
import { formatMonth, formatMonthOnly, formatSignedWon, formatUtilityType, formatWon } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useAnalysisStore()
const selectedMonth = ref('')
const isMonthMenuOpen = ref(false)
const monthDropdown = ref(null)
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
const targetMonthOnlyLabel = computed(() => formatMonthOnly(targetYearMonth.value))
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

function utilityCostLabel(utilityType) {
  return utilityType === 'GAS' ? '도시가스' : `${formatUtilityType(utilityType)}세`
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
      <div ref="monthDropdown" class="relative shrink-0">
        <button
          type="button"
          class="border-divider bg-surface text-ink flex min-h-11 items-center gap-1 rounded-md border px-3 py-2 text-label font-semibold shadow-xs disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="store.isLoading || store.areMonthsLoading || !store.diagnosisMonths.length"
          aria-haspopup="listbox"
          :aria-expanded="isMonthMenuOpen"
          @click="toggleMonthMenu"
        >
          {{ formatMonth(selectedMonth || targetYearMonth) }}
          <IconChevronDown
            :size="14"
            class="text-muted transition-transform"
            :class="isMonthMenuOpen ? 'rotate-180' : ''"
            aria-hidden="true"
          />
        </button>

        <ul
          v-if="isMonthMenuOpen"
          class="border-divider bg-surface absolute top-full right-0 z-30 mt-2 min-w-full list-none overflow-hidden rounded-md border p-1 shadow-lg"
          role="listbox"
          aria-label="등록된 청구 월"
        >
          <li v-for="month in store.diagnosisMonths" :key="month.yearMonth" role="option">
            <button
              type="button"
              class="text-ink hover:bg-primary-bg focus-visible:bg-primary-bg min-h-11 w-full rounded-sm border-0 bg-transparent px-3 py-2 text-left text-label font-semibold whitespace-nowrap outline-hidden"
              :class="selectedMonth === month.yearMonth ? 'text-primary bg-primary-bg' : ''"
              :aria-selected="selectedMonth === month.yearMonth"
              @click="changeMonth(month.yearMonth)"
            >
              {{ formatMonth(month.yearMonth) }}
            </button>
          </li>
        </ul>
      </div>
    </template>
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
        사진에서 청구 월을 자동으로 확인해요.<br />등록하면 전년 동월·1인 가구 평균과 바로 비교해드려요.
      </p>

      <GpButton @click="goToRegistration">고지서 등록하기</GpButton>
    </section>

    <template v-else-if="diagnosis?.summary">
      <section class="analysis-summary relative mt-7 overflow-hidden rounded-xl px-6 py-7 text-white">
        <div class="relative mb-4 flex items-center justify-between gap-3">
          <p class="text-body-strong m-0 text-white/60">
            {{ targetMonthOnlyLabel }} 생활요금 합계
          </p>
          <span
            v-if="diagnosis.summary.hasPreviousYear"
            class="rounded-full bg-white/12 px-3 py-2 text-label font-semibold whitespace-nowrap"
          >
            작년보다 {{ formatSignedWon(diagnosis.summary.diffLastYearTotal) }}
          </span>
        </div>
        <div class="relative flex items-center justify-between gap-3">
          <strong class="block text-[38px] leading-none font-bold tracking-tight tabular-nums">
            {{ formatWon(diagnosis.summary.currentTotal) }}
          </strong>
          <button
            type="button"
            class="h-9 shrink-0 rounded-sm border border-white/25 bg-white/12 px-2.5 text-caption font-semibold text-white active:scale-[0.985]"
            @click="goToRegistration"
          >
            고지서 등록
          </button>
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

      <SingleHouseholdComparisonCard
        v-if="diagnosis.singleHouseholdComparison"
        :comparison="diagnosis.singleHouseholdComparison"
      />
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
