<script setup>
/*
 * 에코마일리지 평가 결과 — WF-10 (B-5-02)
 *
 * ── ⚠️ 회차는 URL 에서 온다 ────────────────────────────────────────────────
 * `store.roundId` 는 **진행 중인 회차**라 여기 쓰면 안 된다. 확정된 것은 지난 회차이고,
 * 진행 중인 회차를 넣으면 `ECO_RESULT_NOT_CONFIRMED`(409) 다.
 * `route.params.roundId` 를 그대로 넘긴다. 새 회차는 응답의 `nextRound.roundId` 다.
 *
 * ── 달마다의 페이스는 WF-07 과 같은 컴포넌트로 그린다 ──────────────────────
 * `monthlyRates` 배열 모양이 `monthly-report` 와 같아서 `EcoMonthlyRateChart` 를 그대로 쓴다.
 * 여기서 다시 그리면 같은 데이터가 두 화면에서 다르게 보인다.
 *
 * ── 확정 근거는 헤더 부제에 둔다 (핵심 규칙 7 · 10) ────────────────────────
 * `confirmedSource`("에코마일리지 누리집 기준") · `confirmedAt` 은 화면 전체에 걸린 출처다.
 * 월 페이스는 진단 탭 고지서로 재지만 **최종 확정은 누리집 기준**이라, 다른 화면 숫자와
 * 다를 수 있다는 것을 여기서 밝히지 않으면 어느 쪽이 맞는지 알 수 없다.
 *
 * 리포트 화면이므로 다음 회차 목표 CTA 는 두지 않는다. 적립 화면(WF-11)으로 가는 길은
 * 마일리지 카드가 맡는다.
 */
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import EcoMonthlyRateChart from '@/components/eco/EcoMonthlyRateChart.vue'
import EcoResultMileageCard from '@/components/eco/EcoResultMileageCard.vue'
import EcoResultSummary from '@/components/eco/EcoResultSummary.vue'
import EcoUtilityResultTable from '@/components/eco/EcoUtilityResultTable.vue'
import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import ReportDialogLayout from '@/components/layout/ReportDialogLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import { useEcoStore } from '@/stores/eco'
import { formatPercent, formatRoundPeriod, formatRoundPeriodChip } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useEcoStore()

const props = defineProps({
  embedded: { type: Boolean, default: false },
  reportRoundId: { type: [String, Number], default: null },
  reportData: { type: Object, default: null },
})
const emit = defineEmits(['close'])

const roundId = computed(() => props.reportRoundId ?? route.params.roundId)
const result = computed(() => props.reportData ?? store.result)
const layout = computed(() => (props.embedded ? ReportDialogLayout : AppSubLayout))

function load() {
  if (props.reportData) return
  store.fetchRoundResult(roundId.value)
}
watch([roundId, () => props.reportData], load, { immediate: true })

/**
 * 화면 제목 「평가 결과」 오른쪽 끝 기간 칩 '2025.10~2026.03'. 홈 감축률 카드 칩과 같은 문법.
 * ⚠️ 확정일(`confirmedAt`)·확정 기준(`confirmedSource` 「에코마일리지 누리집 기준」) 부제는
 * **수현 결정으로 뺐다**(2026-09-10 · 결정 C-40). 핵심 규칙 7·10 의 출처 표시와 어긋나므로 명세에 적어 뒀다.
 */
const periodChip = computed(() =>
  result.value ? formatRoundPeriodChip(result.value.periodStart, result.value.periodEnd) : '',
)
const dialogSubtitle = computed(() => {
  const data = result.value
  return data ? `평가 기간: ${formatRoundPeriod(data.periodStart, data.periodEnd)}` : ''
})

const chartCaption = computed(() =>
  result.value ? `목표 ${formatPercent(result.value.targetRate)}를 넘긴 달은 진한 초록이에요` : '',
)

const goSettlement = () => router.push(`/whatif/rounds/${roundId.value}/settlement`)
</script>

<template>
  <component
    :is="layout"
    :title="embedded ? 'ECO 리포트' : '평가 결과'"
    :subtitle="embedded ? dialogSubtitle : ''"
    back="/whatif"
    @close="emit('close')"
  >
    <!-- 제목 오른쪽 끝 기간 칩 (결정 C-40). 다이얼로그(보관함)에서는 부제가 기간을 말한다 -->
    <template v-if="!embedded && periodChip" #titleAction>
      <span
        class="bg-surface-sub text-caption-sm text-ink-soft mt-1 shrink-0 rounded-full px-2.5 py-[5px] font-bold tabular-nums"
      >
        {{ periodChip }}
      </span>
    </template>
    <!-- 로딩·실패를 남기지 않는다 (COM-08) -->
    <p v-if="store.isLoading && !result" class="text-caption text-muted py-10 text-center">
      평가 결과를 불러오는 중이에요
    </p>

    <div v-else-if="!result" class="py-10 text-center">
      <p class="text-caption text-muted mt-0 mb-4">
        {{ store.error?.message || '평가 결과를 불러오지 못했어요' }}
      </p>
      <GpButton variant="pill" size="pill" @click="load">다시 시도</GpButton>
    </div>

    <div v-else class="space-y-4 pt-1">
      <EcoResultSummary :result="result" :report-mode="embedded" />

      <!-- 적립 화면(WF-11)으로 가는 통로. 여기서 전환을 실행하지 않는다 (핵심 규칙 4) -->
      <EcoResultMileageCard
        v-if="!embedded"
        :mileage="result.confirmedMileage"
        :interactive="!embedded"
        @open="goSettlement"
      />

      <EcoUtilityResultTable :rows="result.utilityResults" :report-mode="embedded" />

      <EcoMonthlyRateChart
        :rows="result.monthlyRates"
        :compact="embedded"
        title="달마다 얼마나 줄였나"
        :caption="chartCaption"
        footnote="검침 주기가 요금마다 달라 월 구분은 검침 반영 시점 기준이에요"
      />
    </div>

  </component>
</template>
