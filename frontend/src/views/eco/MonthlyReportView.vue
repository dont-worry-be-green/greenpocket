<script setup>
/*
 * 전달 리포트 — WF-07 (B-4-02 · B-4-07 · B-4-08)
 *
 * ── 빈 달은 에러가 아니다 ────────────────────────────────────────────────
 * 그 달 고지서를 안 올렸으면 서버가 **200 + `result: null` + `emptyReason: "NO_BILL"`** 을 준다
 * (핵심 규칙 8). `store.error` 로 떨어지지 않으므로 에러 화면과 빈 화면을 따로 그린다.
 *
 * ── `?month=` 는 선택이다 ────────────────────────────────────────────────
 * 없으면 서버가 「가장 최근에 채점 가능한 달」을 고른다. 홈의 「자세히」는 달을 붙이지 않고 오고,
 * 그래프의 막대에서 들어올 때만 붙는다. 그래서 쿼리가 바뀌면 다시 부른다.
 */
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import EcoMonthlyRateChart from '@/components/eco/EcoMonthlyRateChart.vue'
import EcoReportCauseList from '@/components/eco/EcoReportCauseList.vue'
import EcoReportPrescription from '@/components/eco/EcoReportPrescription.vue'
import EcoReportResultCard from '@/components/eco/EcoReportResultCard.vue'
import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import { useEcoStore } from '@/stores/eco'

const route = useRoute()
const router = useRouter()
const store = useEcoStore()

const report = computed(() => store.monthlyReport)
const hasResult = computed(() => Boolean(report.value?.result))

function load() {
  const month = route.query.month
  store.fetchMonthlyReport(month ? { month } : {})
}
watch(() => route.query.month, load, { immediate: true })

/** 실천 조정으로 넘긴다. **쿼리 키는 `utility` 다**(응답 필드 `utilityType` 과 이름이 다르다) */
function goAdjust(utilityType) {
  router.push({
    path: '/whatif/missions',
    query: { utility: utilityType, month: report.value?.reportMonth },
  })
}
</script>

<template>
  <AppSubLayout title="전달 리포트" back="/whatif">
    <!-- 로딩·실패·빈 결과를 남기지 않는다 (COM-08) -->
    <p v-if="store.isLoading && !report" class="text-caption text-muted py-10 text-center">
      리포트를 불러오는 중이에요
    </p>

    <div v-else-if="!report" class="py-10 text-center">
      <p class="text-caption text-muted mt-0 mb-4">
        {{ store.error?.message || '리포트를 불러오지 못했어요' }}
      </p>
      <GpButton variant="pill" size="pill" @click="load">다시 시도</GpButton>
    </div>

    <!-- 고지서를 안 올린 달. 에러가 아니라 안내다 -->
    <div v-else-if="!hasResult" class="py-10 text-center">
      <p class="text-body text-ink-soft mt-0 mb-4">
        아직 올린 고지서가 없어요. 진단 탭에서 고지서를 등록하면 그 달의 결과를 알려드려요.
      </p>
      <GpButton variant="pill" size="pill" @click="router.push('/analysis')">
        고지서 등록하러 가기
      </GpButton>
    </div>

    <div v-else class="space-y-4 pt-1">
      <EcoReportResultCard
        :result="report.result"
        :report-month="report.reportMonth"
        :baseline-description="report.baselineDescription"
        :bill-registered-at="report.billRegisteredAt"
      />

      <EcoMonthlyRateChart :rows="report.monthlyRates" :target-rate="report.result.targetRate" />

      <EcoReportCauseList v-if="report.cause" :cause="report.cause" />

      <EcoReportPrescription
        v-if="report.prescription"
        :prescription="report.prescription"
        @adjust="goAdjust"
      />
    </div>
  </AppSubLayout>
</template>
