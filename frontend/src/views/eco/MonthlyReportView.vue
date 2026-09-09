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
 *
 * ── 카드 순서는 시안이 아니라 명세다 ──────────────────────────────────────
 * B-4-07 이 ① 결과 → ② 원인 → ③ 처방 순서로 못 박는다. 그래프는 그 뒤에 온다 —
 * 원인을 보기 전에 막대부터 나오면 "왜 그랬는지" 없이 "얼마나" 만 남는다.
 *
 * 「미션 다시 고르기」는 처방 카드 안이 아니라 **화면 하단 고정 CTA** 다(시안 WF-07). 목표 관리(WF-04)로 간다.
 * 스크롤을 끝까지 내려야 보이면 정작 조정이 필요한 사람이 못 찾는다.
 */
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import EcoMonthlyRateChart from '@/components/eco/EcoMonthlyRateChart.vue'
import EcoReportCauseList from '@/components/eco/EcoReportCauseList.vue'
import EcoReportPrescription from '@/components/eco/EcoReportPrescription.vue'
import EcoReportResultCard from '@/components/eco/EcoReportResultCard.vue'
import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import ReportDialogLayout from '@/components/layout/ReportDialogLayout.vue'
import GpCard from '@/components/ui/GpCard.vue'
import GpButton from '@/components/ui/GpButton.vue'
import { useEcoStore } from '@/stores/eco'
import {
  formatUnit,
  formatWon,
  formatUsage,
  usagePrecision,
  formatMonthOnly,
  formatMonthDay,
  formatPercent,
  formatRoundPeriod,
  formatUtilityType,
} from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useEcoStore()

const props = defineProps({
  embedded: { type: Boolean, default: false },
  month: { type: String, default: '' },
})
const emit = defineEmits(['close'])

const report = computed(() => store.monthlyReport)
const hasResult = computed(() => Boolean(report.value?.result))

/** '7월분 페이스' — 어느 달 이야기인지 제목이 먼저 말한다(시안 WF-07) */
const title = computed(() =>
  report.value?.reportMonth
    ? `${formatMonthOnly(report.value.reportMonth)}분 페이스`
    : '전달 리포트',
)
const dialogTitle = computed(() => {
  const month = report.value?.reportMonth || props.month
  return month ? `${formatMonthOnly(month)} 월간 리포트` : '월간 리포트'
})
const layout = computed(() => (props.embedded ? ReportDialogLayout : AppSubLayout))

/*
 * '7월 고지서 · 8월 3일 등록 · 평가 기간 2026-04 ~ 09'.
 *
 * ⚠️ 평가 기간은 `monthly-report` 응답에 **없다**(api-spec 10.3). 홈이나 회차 조회로 이미
 * 받아 둔 것이 있을 때만 붙인다 — 없는 기간을 지어내지 않는다(핵심 규칙 8).
 */
const subtitle = computed(() => {
  const data = report.value
  if (!data?.reportMonth) return ''
  const parts = [`${formatMonthOnly(data.reportMonth)} 고지서`]
  if (data.billRegisteredAt) parts.push(`${formatMonthDay(data.billRegisteredAt)} 등록`)

  const period = store.home?.header ?? store.currentRound
  if (period?.periodStart) {
    parts.push(`평가 기간 ${formatRoundPeriod(period.periodStart, period.periodEnd)}`)
  }
  return parts.join(' · ')
})

/** 하단 고정 CTA. 서버가 고른 조정 대상을 그대로 문구에 넣는다 */
const adjustLabel = computed(() => {
  const utilityType = report.value?.prescription?.adjustTargetUtility
  return utilityType ? `${formatUtilityType(utilityType)} 미션 다시 고르기` : '미션 다시 고르기'
})

const chartCaption = computed(() =>
  hasResult.value
    ? `목표 ${formatPercent(report.value.result.targetRate)}를 넘긴 달은 진한 초록이에요`
    : '',
)

const monthlyBills = ref([])
const billsLoading = ref(false)
const billsError = ref(null)
let loadId = 0

async function load() {
  const id = ++loadId
  monthlyBills.value = []
  billsError.value = null
  billsLoading.value = true
  const month = props.month || route.query.month
  try {
    const data = await store.fetchMonthlyReport(month ? { month } : {})
    if (id !== loadId) return
    const selectedMonth = data?.reportMonth ?? month
    if (data && !data.result && selectedMonth) {
      const bills = await store.fetchMonthlyBills(selectedMonth)
      if (id === loadId) monthlyBills.value = bills
    }
  } catch (error) {
    if (id === loadId) billsError.value = error
  } finally {
    if (id === loadId) billsLoading.value = false
  }
}
watch(() => props.month || route.query.month, load, { immediate: true })

/*
 * 미션을 다시 고르는 곳은 목표 관리(WF-04)다 — 별도 실천 조정 화면(WF-08)은 없앴다(2026-09-09 수현).
 * `?utility=` 로 처방이 지목한 요금의 미션 탭을 바로 연다. **쿼리 키는 `utility` 다**(응답 필드 `utilityType` 과 다르다).
 */
function goAdjust(utilityType) {
  router.push({ path: '/whatif/goal', query: utilityType ? { utility: utilityType } : {} })
}
</script>

<template>
  <component
    :is="layout"
    :title="embedded ? dialogTitle : title"
    :subtitle="embedded ? '' : subtitle"
    :back="route.query.from === 'archive' ? '/mypage/reports' : '/whatif'"
    :has-footer="hasResult && !embedded"
    @close="emit('close')"
  >
    <!-- 로딩·실패·빈 결과를 남기지 않는다 (COM-08) -->
    <p
      v-if="billsLoading || (store.isLoading && !report)"
      class="text-caption text-muted py-10 text-center"
    >
      리포트를 불러오는 중이에요
    </p>

    <div v-else-if="!report" class="py-10 text-center">
      <p class="text-caption text-muted mt-0 mb-4">
        {{ store.error?.message || '리포트를 불러오지 못했어요' }}
      </p>
      <GpButton variant="pill" size="pill" @click="load">다시 시도</GpButton>
    </div>

    <div v-else-if="!hasResult && billsError" class="py-10 text-center">
      <p class="text-body text-ink-soft mb-4">
        {{ billsError.message || '고지서를 불러오지 못했어요' }}
      </p>
      <GpButton variant="pill" size="pill" @click="load">다시 시도</GpButton>
    </div>

    <div v-else-if="!hasResult && monthlyBills.length" class="space-y-4 pt-1">
      <GpCard>
        <h2 class="text-section text-ink">{{ formatMonthOnly(report.reportMonth) }}분 감축률</h2>
        <p class="text-amount text-muted my-4">—</p>
        <p class="text-caption text-muted">
          비교 데이터가 없어 감축률은 아직 표시할 수 없어요. 등록된 고지서 정보부터 보여드려요.
        </p>
        <div class="border-divider mt-4 flex justify-between border-t pt-4 text-body">
          <span>평가 기간 누적 감축률</span><span>—</span>
        </div>
      </GpCard>
      <GpCard>
        <h2 class="text-section text-ink">이번 달 요금과 사용량</h2>
        <div
          v-for="bill in monthlyBills"
          :key="bill.recordId"
          class="border-divider border-b py-4 last:border-b-0"
        >
          <div class="flex items-center justify-between gap-3">
            <span class="text-body-strong">{{ formatUtilityType(bill.utilityType) }}</span>
            <span class="text-body-strong">{{
              bill.amount == null ? '—' : formatWon(bill.amount)
            }}</span>
          </div>
          <p class="text-body text-muted mt-2">
            사용량
            {{
              bill.usage == null
                ? '—'
                : formatUsage(
                    bill.usage,
                    usagePrecision(bill.usageUnit),
                    formatUnit(bill.usageUnit),
                  )
            }}
          </p>
          <p v-if="bill.registeredAt" class="text-caption text-muted">
            {{ formatMonthDay(bill.registeredAt) }} 등록
          </p>
          <p class="text-caption text-muted">감축률 — · 목표 달성 여부 —</p>
        </div>
      </GpCard>
      <GpCard>
        <h2 class="text-section text-ink">남은 기간 필요 감축률</h2>
        <p class="text-amount text-muted my-4">—</p>
      </GpCard>
      <GpCard>
        <h2 class="text-section text-ink">달마다 얼마나 줄였나</h2>
        <p class="text-amount text-muted my-4">—</p>
      </GpCard>
    </div>

    <!-- 고지서 조회까지 성공한 뒤 빈 달을 안내한다. -->
    <div v-else-if="!hasResult" class="py-10 text-center">
      <p class="text-body text-ink-soft mt-0 mb-4">
        아직 올린 고지서가 없어요. 진단 탭에서 고지서를 등록하면 그 달의 결과를 알려드려요.
      </p>
      <GpButton variant="pill" size="pill" @click="router.push('/analysis')">
        고지서 등록하러 가기
      </GpButton>
    </div>

    <!-- 순서는 B-4-07 ① 결과 → ② 원인 → ③ 처방. 그래프는 그 뒤다 -->
    <div v-else class="space-y-4 pt-1">
      <EcoReportResultCard
        :result="report.result"
        :report-month="report.reportMonth"
        :baseline-description="report.baselineDescription"
        :bill-registered-at="report.billRegisteredAt"
      />

      <EcoReportCauseList v-if="report.cause" :cause="report.cause" />

      <EcoReportPrescription
        v-if="report.prescription"
        :prescription="report.prescription"
        :cause="report.cause"
        :target-rate="report.result.targetRate"
      />

      <EcoMonthlyRateChart
        :rows="report.monthlyRates"
        :compact="embedded"
        :caption="chartCaption"
        footnote="진단 탭에 등록한 고지서로 계산했어요. 월 평가는 페이스를 보려고 우리가 나눈 값이고, 실제 평가는 6개월 누적이에요."
      />
    </div>

    <template v-if="hasResult && !embedded" #footer>
      <div
        class="bg-canvas border-divider fixed inset-x-0 bottom-0 z-20 mx-auto max-w-(--gp-viewport-w) border-t px-(--gp-gutter) pt-3 pb-[max(12px,env(safe-area-inset-bottom))]"
      >
        <GpButton @click="goAdjust(report.prescription?.adjustTargetUtility)">
          {{ adjustLabel }}
        </GpButton>
      </div>
    </template>
  </component>
</template>
