<script setup>
/*
 * 리포트 보관함 — MY-04 (E-2-01)
 *
 * ── 탭 두 개가 타입 셋을 나눠 담는다 ────────────────────────────────────
 * 시안의 탭은 「월별 리포트 / ECO 리포트」 둘인데 `type` enum 은 셋이다.
 *   월별 리포트 = MONTHLY_DIAGNOSIS
 *   ECO 리포트  = ECO_RESULT (6개월 실제 평가 결과)
 * `type` 쿼리는 단일값이라 두 번 부르는 대신 **`type` 없이 한 번 받아 여기서 가른다.**
 * 서버가 이미 세 타입을 합쳐 최신순으로 정렬해 준다(api-spec 14.2).
 *
 * ── 「다운로드」는 없다 ─────────────────────────────────────────────────
 * `downloadable` 이 항상 false 다 — 자동 생성·다운로드(E-2-02)는 P2 다. 시안의 안내 문구
 * "확인하고 다운로드할 수 있어요" 도 지금 할 수 있는 것으로 고쳐 적는다.
 *
 * 「보기」는 페이지를 이동하지 않고 현재 목록 위에 전체 화면 리포트 다이얼로그를 연다.
 * 서버의 `targetScreen` 이 지원되는 리포트만 버튼을 활성화한다.
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import BillYearSelect from '@/components/mypage/BillYearSelect.vue'
import MypageState from '@/components/mypage/MypageState.vue'
import ReportArchiveYear from '@/components/mypage/ReportArchiveYear.vue'
import GpCard from '@/components/ui/GpCard.vue'
import IconLeaf from '@/components/ui/icons/IconLeaf.vue'
import { useMypageStore } from '@/stores/mypage'
import MonthlyReportView from '@/views/eco/MonthlyReportView.vue'
import RoundResultView from '@/views/eco/RoundResultView.vue'

const TABS = [
  { key: 'MONTHLY', label: '월별 리포트', types: ['MONTHLY_DIAGNOSIS'] },
  { key: 'ECO', label: 'ECO 리포트', types: ['ECO_RESULT'] },
]

const VIEWABLE_SCREENS = new Set(['AN-07', 'WF-07', 'WF-10'])

const route = useRoute()
const store = useMypageStore()

const activeTab = computed(() =>
  TABS.some((tab) => tab.key === route.query.tab) ? route.query.tab : TABS[0].key,
)
const pageTitle = computed(() => (activeTab.value === 'ECO' ? 'ECO 리포트' : '월별 리포트'))
const selectedYear = ref(null)
const selectedReport = ref(null)
const years = computed(() => [...new Set(
  (store.reports?.content ?? []).map((report) => report.yearMonth.slice(0, 4)),
)].sort((a, b) => b.localeCompare(a)))

const bootstrapping = computed(() => !store.reports && !store.reportsError)

const rows = computed(() => {
  const types = TABS.find((tab) => tab.key === activeTab.value)?.types ?? []
  return (store.reports?.content ?? []).filter((report) => types.includes(report.type) && (!selectedYear.value || report.yearMonth.startsWith(selectedYear.value)))
})

/** 연도별로 묶는다. 목록은 이미 최신순이라 순서를 다시 만들지 않는다 (E-2-01) */
const groups = computed(() => {
  const byYear = new Map()
  rows.value.forEach((report) => {
    const year = report.yearMonth.slice(0, 4)
    if (!byYear.has(year)) byYear.set(year, [])
    byYear.get(year).push(report)
  })
  return [...byYear.entries()].map(([year, reports]) => ({ year, reports }))
})

/** 갈 곳이 없는 것 — 버튼을 잠근다 */
const unroutableIds = computed(() =>
  rows.value
    .filter((report) => !VIEWABLE_SCREENS.has(report.targetScreen))
    .map((report) => report.reportId),
)

function open(report) {
  if (VIEWABLE_SCREENS.has(report.targetScreen)) selectedReport.value = report
}

onMounted(() => {
  if (!store.reports) store.fetchReports()
})
</script>

<template>
  <AppSubLayout
    :title="pageTitle"
    center-title
    back="/mypage"
  >
    <div class="space-y-4 pt-1">
      <BillYearSelect v-model="selectedYear" :years="years" />

      <!-- E-2-01 이 요구하는 안내 문구 -->
      <GpCard>
        <div class="flex items-start gap-3">
          <span class="text-primary-soft mt-0.5 flex-none" aria-hidden="true">
            <IconLeaf :size="20" />
          </span>
          <div>
            <p class="text-body-strong text-ink mt-0 mb-1">{{ activeTab === 'ECO' ? '6개월 평가 결과를 확인해요.' : '매월 리포트가 자동으로 저장돼요.' }}</p>
            <p class="text-caption text-muted m-0">
              {{ activeTab === 'ECO' ? '에코마일리지의 6개월 실제 평가 결과를 정리한 리포트예요.' : '생활비 진단 결과와 What-if 실천 내역을 정리한 리포트예요.' }}
            </p>
          </div>
        </div>
      </GpCard>

      <MypageState
        :loading="bootstrapping"
        :error="store.reports ? null : store.reportsError"
        :empty="groups.length === 0"
        :empty-message="selectedYear ? '선택한 연도에 저장된 리포트가 없어요.' : activeTab === 'ECO' ? '아직 저장된 ECO 리포트가 없어요.' : '아직 저장된 리포트가 없어요. 고지서를 등록한 달부터 쌓여요.'"
        @retry="store.fetchReports()"
      >
        <div class="space-y-6">
          <ReportArchiveYear
            v-for="group in groups"
            :key="group.year"
            :year="group.year"
            :reports="group.reports"
            :unroutable-ids="unroutableIds"
            @open="open"
          />
        </div>
      </MypageState>
    </div>

    <MonthlyReportView
      v-if="selectedReport?.type === 'MONTHLY_DIAGNOSIS' || selectedReport?.type === 'ECO_MONTHLY'"
      embedded
      :month="selectedReport.targetParams?.month ?? selectedReport.yearMonth"
      @close="selectedReport = null"
    />
    <RoundResultView
      v-else-if="selectedReport?.type === 'ECO_RESULT'"
      embedded
      :report-round-id="selectedReport.targetParams?.roundId"
      @close="selectedReport = null"
    />
  </AppSubLayout>
</template>
