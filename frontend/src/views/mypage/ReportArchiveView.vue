<script setup>
/*
 * 리포트 보관함 — MY-04 (E-2-01)
 *
 * ── 탭 두 개가 타입 셋을 나눠 담는다 ────────────────────────────────────
 * 시안의 탭은 「월별 리포트 / ECO 리포트」 둘인데 `type` enum 은 셋이다.
 *   월별 리포트 = MONTHLY_DIAGNOSIS
 *   ECO 리포트  = ECO_MONTHLY + ECO_RESULT
 * `type` 쿼리는 단일값이라 두 번 부르는 대신 **`type` 없이 한 번 받아 여기서 가른다.**
 * 서버가 이미 세 타입을 합쳐 최신순으로 정렬해 준다(api-spec 14.2).
 *
 * ── 「다운로드」는 없다 ─────────────────────────────────────────────────
 * `downloadable` 이 항상 false 다 — 자동 생성·다운로드(E-2-02)는 P2 다. 시안의 안내 문구
 * "확인하고 다운로드할 수 있어요" 도 지금 할 수 있는 것으로 고쳐 적는다.
 *
 * ── 갈 곳은 서버가 정한다 ───────────────────────────────────────────────
 * `targetScreen` + `targetParams` 를 경로로 옮기는 표가 아래 `ROUTE_BY_SCREEN` 하나다.
 * 타입으로 추론하지 않는다 — 같은 타입이 다른 화면을 가리키게 되면 조용히 어긋난다.
 * 표에 없는 화면은 버튼을 잠근다(빈 화면으로 보내지 않는다).
 *
 * AN-07(생활비 진단)은 전용 라우트가 없어 진단 탭 홈(`/analysis`)으로 보낸다. 그 화면이
 * `?month=` 를 읽어 해당 월을 골라 주므로(#113) 목록에서 누른 달이 그대로 열린다.
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import MypageSegments from '@/components/mypage/MypageSegments.vue'
import MypageState from '@/components/mypage/MypageState.vue'
import ReportArchiveYear from '@/components/mypage/ReportArchiveYear.vue'
import GpCard from '@/components/ui/GpCard.vue'
import IconLeaf from '@/components/ui/icons/IconLeaf.vue'
import { useMypageStore } from '@/stores/mypage'

const TABS = [
  { key: 'MONTHLY', label: '월별 리포트', types: ['MONTHLY_DIAGNOSIS'] },
  { key: 'ECO', label: 'ECO 리포트', types: ['ECO_MONTHLY', 'ECO_RESULT'] },
]

/** `targetScreen` → 경로. `targetParams` 를 그대로 실어 보낸다 */
const ROUTE_BY_SCREEN = {
  'AN-07': (params) => ({ path: '/analysis', query: { month: params?.month } }),
  'WF-07': (params) => ({ path: '/whatif/report', query: { month: params?.month } }),
  'WF-10': (params) => ({ path: `/whatif/rounds/${params?.roundId}/result` }),
}

const router = useRouter()
const store = useMypageStore()

const activeTab = ref(TABS[0].key)

const bootstrapping = computed(() => !store.reports && !store.reportsError)

const rows = computed(() => {
  const types = TABS.find((tab) => tab.key === activeTab.value)?.types ?? []
  return (store.reports?.content ?? []).filter((report) => types.includes(report.type))
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
    .filter((report) => !ROUTE_BY_SCREEN[report.targetScreen])
    .map((report) => report.reportId),
)

const tabs = computed(() => TABS.map(({ key, label }) => ({ key, label })))

function open(report) {
  const to = ROUTE_BY_SCREEN[report.targetScreen]?.(report.targetParams)
  if (to) router.push(to)
}

onMounted(() => {
  if (!store.reports) store.fetchReports()
})
</script>

<template>
  <AppSubLayout
    title="리포트 보관함"
    subtitle="월별 리포트와 ECO 리포트를 확인할 수 있어요"
    back="/mypage"
  >
    <div class="space-y-4 pt-1">
      <MypageSegments v-model="activeTab" :items="tabs" />

      <!-- E-2-01 이 요구하는 안내 문구 -->
      <GpCard>
        <div class="flex items-start gap-3">
          <span class="text-primary-soft mt-0.5 flex-none" aria-hidden="true">
            <IconLeaf :size="20" />
          </span>
          <div>
            <p class="text-body-strong text-ink mt-0 mb-1">매월 리포트가 자동으로 저장돼요.</p>
            <p class="text-caption text-muted m-0">
              생활비 진단 결과와 What-if 실천 내역을 정리한 리포트예요.
            </p>
          </div>
        </div>
      </GpCard>

      <MypageState
        :loading="bootstrapping"
        :error="store.reports ? null : store.reportsError"
        :empty="groups.length === 0"
        empty-message="아직 저장된 리포트가 없어요. 고지서를 등록한 달부터 쌓여요."
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
  </AppSubLayout>
</template>
