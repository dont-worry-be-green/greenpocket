<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppTabLayout from '@/components/layout/AppTabLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import IconReceipt from '@/components/ui/icons/IconReceipt.vue'
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
  <AppTabLayout
    tab="analysis"
    :eyebrow="targetMonthLabel"
    title="생활비 분석"
    subtitle="고지서를 등록하면 지난달과 지역 평균을 비교해드려요."
  >
    <div v-if="store.isLoading && !isEmptyPreview" class="bg-surface h-72 animate-pulse rounded-lg" />

    <section v-else-if="store.error && !isEmptyPreview" class="bg-surface rounded-lg px-5 py-8 text-center">
      <p class="text-body-strong text-ink mt-0 mb-1">정보를 불러오지 못했어요</p>
      <p class="text-caption text-muted mt-0 mb-5">{{ store.error.message }}</p>
      <GpButton variant="wide" size="wide" @click="store.fetchHome">다시 시도</GpButton>
    </section>

    <section v-else-if="diagnosis?.empty" class="bg-surface rounded-lg px-5 py-10 text-center">
      <div
        class="bg-primary-bg text-primary mx-auto flex size-28 flex-col items-center justify-center rounded-full"
        aria-hidden="true"
      >
        <IconReceipt :size="28" />
        <span class="bg-primary-bg-strong mt-4 h-2 w-14 rounded-full" />
      </div>

      <h2 class="text-section text-ink mt-7 mb-2">아직 등록된 고지서가 없어요</h2>
      <p class="text-body-sm text-muted mx-auto mt-0 mb-7 max-w-72 break-keep">
        전기·수도·도시가스가 포함된 관리비 고지서도 한 번에 분석할 수 있어요.
      </p>

      <GpButton @click="goToRegistration">{{ targetMonthOnlyLabel }} 고지서 등록하기</GpButton>
    </section>

    <section v-else class="bg-surface rounded-lg px-5 py-8 text-center">
      <p class="text-body-strong text-ink mt-0 mb-1">등록된 고지서를 확인했어요</p>
      <p class="text-caption text-muted mt-0 mb-5">생활비 분석 결과를 불러오는 중이에요.</p>
    </section>
  </AppTabLayout>
</template>
