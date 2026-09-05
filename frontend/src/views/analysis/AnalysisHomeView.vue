<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppTabLayout from '@/components/layout/AppTabLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import billIcon from '@/assets/icons/bill.svg'
import { useAnalysisStore } from '@/stores/analysis'
import { formatMonth, formatMonthOnly } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useAnalysisStore()

const isEmptyPreview = computed(() => route.query.preview === 'empty')
const diagnosis = computed(() =>
  isEmptyPreview.value
    ? { empty: true, targetYearMonth: '2026-08', screen: 'AN-01' }
    : store.diagnosis,
)

const targetYearMonth = computed(
  () => store.targetMonth?.targetYearMonth ?? diagnosis.value?.targetYearMonth,
)
const targetMonthLabel = computed(() => formatMonth(targetYearMonth.value))
const targetMonthOnlyLabel = computed(() => formatMonthOnly(targetYearMonth.value))

onMounted(() => {
  if (!isEmptyPreview.value) store.fetchHome()
})

function goToRegistration() {
  router.push({ path: '/analysis/bills/new', query: { month: targetYearMonth.value } })
}
</script>

<template>
  <AppTabLayout tab="analysis" title="진단">
    <p class="text-section text-ink mt-8 mb-2">{{ targetMonthLabel }}</p>

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

    <section v-else class="bg-surface rounded-lg px-5 py-8 text-center">
      <p class="text-body-strong text-ink mt-0 mb-1">등록된 고지서를 확인했어요</p>
      <p class="text-caption text-muted mt-0 mb-5">생활비 분석 결과를 불러오는 중이에요.</p>
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
