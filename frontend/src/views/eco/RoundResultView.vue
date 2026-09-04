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
 */
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import EcoAmountBreakdown from '@/components/eco/EcoAmountBreakdown.vue'
import EcoMonthlyRateChart from '@/components/eco/EcoMonthlyRateChart.vue'
import EcoResultSummary from '@/components/eco/EcoResultSummary.vue'
import EcoUtilityResultTable from '@/components/eco/EcoUtilityResultTable.vue'
import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import { useEcoStore } from '@/stores/eco'
import { formatRoundPeriod } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useEcoStore()

const roundId = computed(() => route.params.roundId)
const result = computed(() => store.result)

function load() {
  store.fetchRoundResult(roundId.value)
}
watch(roundId, load, { immediate: true })

const goSettlement = () => router.push(`/whatif/rounds/${roundId.value}/settlement`)
</script>

<template>
  <AppSubLayout title="평가 결과" back="/whatif" has-footer>
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
      <p class="text-caption text-muted mt-0 mb-0">
        {{ formatRoundPeriod(result.periodStart, result.periodEnd) }} 평가 기간
      </p>

      <EcoResultSummary :result="result" />

      <EcoMonthlyRateChart
        :rows="result.monthlyRates"
        :target-rate="result.targetRate"
        title="달마다의 페이스"
      />

      <EcoUtilityResultTable :rows="result.utilityResults" />

      <!-- 「덜 낸 요금」은 포켓 잔액이 아니다 (핵심 규칙 3) -->
      <EcoAmountBreakdown
        title="줄인 요금"
        :baseline="result.amount.baselineTotal"
        :actual="result.amount.actualTotal"
        :saved="result.amount.savedAmount"
        :pocket-eligible="result.amount.savedIsPocketEligible"
      />

      <!-- 다음 회차. 목표를 아직 안 정했으면 거기로 보낸다 -->
      <p v-if="result.nextRound && !result.nextRound.goalSet" class="text-caption mt-0 mb-0">
        <button
          type="button"
          class="text-label text-primary-on-soft cursor-pointer border-0 bg-transparent p-0"
          @click="router.push('/whatif/goal')"
        >
          {{ formatRoundPeriod(result.nextRound.periodStart, result.nextRound.periodEnd) }} 목표
          정하러 가기
        </button>
      </p>
    </div>

    <template #footer>
      <div
        class="bg-canvas border-divider fixed inset-x-0 bottom-0 z-20 mx-auto max-w-(--gp-viewport-w) border-t px-(--gp-gutter) pt-3 pb-[max(12px,env(safe-area-inset-bottom))]"
      >
        <GpButton :disabled="!result" @click="goSettlement">마일리지 적립 보기</GpButton>
      </div>
    </template>
  </AppSubLayout>
</template>
