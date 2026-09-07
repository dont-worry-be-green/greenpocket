<script setup>
/*
 * 고지서 보관함 — MY-03 (A-2-12)
 *
 * ── 필터는 URL 이 들고 있다 ──────────────────────────────────────────────
 * `/mypage/bills?utility=ELECTRICITY&year=2026`. 화면 상태로 두면 새로고침·공유·데모 도구
 * 바로가기에서 전체 탭으로 되돌아간다(`BenefitHomeView` 의 `month` 와 같은 이유).
 * 쿼리는 밖에서 들어오는 값이라 형식을 믿지 않는다 — 아는 값이 아니면 전체로 떨어뜨린다.
 *
 * ── 건수는 서버가 센다 ──────────────────────────────────────────────────
 * 탭 배지는 `counts` 그대로다(api-spec 6.6 "counts 는 탭 배지"). 목록 길이로 세면 필터·페이징이
 * 걸린 뒤 실제와 어긋나 A-2-12 완료 조건("필터 적용 시 건수가 실제 데이터와 일치한다")이 깨진다.
 *
 * ── 연도 선택지 ─────────────────────────────────────────────────────────
 * API 가 연도 목록을 주지 않아 **전체 연도로 조회했을 때의 청구 월에서 뽑아 기억한다.**
 * 연도를 고른 뒤에는 그 연도 것만 오므로 그때 다시 뽑으면 선택지가 한 개로 줄어든다.
 *
 * ── 행은 아직 누를 수 없다 ──────────────────────────────────────────────
 * 상세(AN-08 · A-2-13)가 없어서다. 자세한 사정은 `BillArchiveRow` 주석에 있다.
 */
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import BillArchiveRow from '@/components/mypage/BillArchiveRow.vue'
import BillYearSelect from '@/components/mypage/BillYearSelect.vue'
import MypageSegments from '@/components/mypage/MypageSegments.vue'
import MypageState from '@/components/mypage/MypageState.vue'
import GpButton from '@/components/ui/GpButton.vue'
import IconChevronDown from '@/components/ui/icons/IconChevronDown.vue'
import { useMypageStore } from '@/stores/mypage'
import { formatMonth, formatUtilityType } from '@/utils/format'

/** 시안 순서. 'ALL' 은 쿼리에 싣지 않는다 — 서버는 `utility` 를 생략하면 전체를 준다 */
const TABS = ['ALL', 'ELECTRICITY', 'WATER', 'GAS']

const route = useRoute()
const router = useRouter()
const store = useMypageStore()

const utility = computed(() => {
  const raw = route.query.utility
  return TABS.includes(raw) && raw !== 'ALL' ? raw : null
})

const year = computed(() => (/^\d{4}$/.test(route.query.year) ? route.query.year : null))

/** 전체 연도로 조회했을 때 뽑아 둔 선택지. 위 주석 참고 */
const years = ref([])

const tabs = computed(() =>
  TABS.map((key) => ({
    key,
    label: key === 'ALL' ? '전체' : formatUtilityType(key),
    count: store.billCounts?.[key] ?? null,
  })),
)

const bills = computed(() => store.bills?.content ?? [])
const expandedMonths = ref(new Set())

/** 같은 청구 월의 고지서를 한 카드에 모은다. 서버의 최신순은 그대로 유지한다. */
const billGroups = computed(() => {
  const byMonth = new Map()
  bills.value.forEach((bill) => {
    if (!byMonth.has(bill.billingMonth)) byMonth.set(bill.billingMonth, [])
    byMonth.get(bill.billingMonth).push(bill)
  })
  return [...byMonth.entries()].map(([month, items]) => ({ month, items }))
})

function toggleMonth(month) {
  const next = new Set(expandedMonths.value)
  if (next.has(month)) next.delete(month)
  else next.add(month)
  expandedMonths.value = next
}

/** 첫 응답 전. `billsLoading` 만 보면 첫 렌더 한 틱에 빈 목록이 스친다 */
const bootstrapping = computed(() => !store.bills && !store.billsError)

const params = computed(() => ({
  ...(utility.value ? { utility: utility.value } : {}),
  ...(year.value ? { year: year.value } : {}),
}))

/** 히스토리를 쌓지 않는다. 탭을 네 번 누른 뒤 뒤로가기를 네 번 눌러야 나가면 갇힌 느낌이다 */
function setQuery(next) {
  router.replace({ path: '/mypage/bills', query: { ...route.query, ...next } })
}

function onTab(key) {
  setQuery({ utility: key === 'ALL' ? undefined : key })
}

function onYear(value) {
  setQuery({ year: value ?? undefined })
}

watch(
  params,
  async (value) => {
    await store.fetchBills(value)
    // 선택지는 전체 연도로 받았을 때만 갱신한다
    if (!value.year) {
      years.value = [
        ...new Set((store.bills?.content ?? []).map((bill) => bill.billingMonth.slice(0, 4))),
      ].sort((a, b) => b.localeCompare(a))
    }
  },
  { immediate: true },
)
</script>

<template>
  <AppSubLayout title="고지서 보관함" back="/mypage" center-title has-footer>
    <MypageSegments :items="tabs" :model-value="utility ?? 'ALL'" @update:model-value="onTab" />

    <div class="mt-4">
      <BillYearSelect :years="years" :model-value="year" @update:model-value="onYear" />
    </div>

    <div class="mt-4">
      <MypageState
        :loading="bootstrapping"
        :error="store.bills ? null : store.billsError"
        :empty="bills.length === 0"
        empty-message="이 조건에 맞는 고지서가 없어요."
        @retry="store.fetchBills(params)"
      >
        <div class="space-y-4">
          <section
            v-for="group in billGroups"
            :key="group.month"
            :data-billing-month="group.month"
            class="bg-surface rounded-lg px-4"
          >
            <h2 class="m-0">
              <button
                type="button"
                class="text-section text-ink flex min-h-16 w-full cursor-pointer items-center justify-between border-0 bg-transparent p-0 text-left"
                :aria-expanded="expandedMonths.has(group.month)"
                :aria-controls="`bills-${group.month}`"
                @click="toggleMonth(group.month)"
              >
                <span>{{ formatMonth(group.month) }}</span>
                <IconChevronDown
                  :size="18"
                  class="text-icon-off transition-transform"
                  :class="expandedMonths.has(group.month) ? 'rotate-180' : ''"
                />
              </button>
            </h2>
            <ul
              v-show="expandedMonths.has(group.month)"
              :id="`bills-${group.month}`"
              class="divide-divider border-divider m-0 list-none divide-y border-t p-0"
            >
              <BillArchiveRow v-for="bill in group.items" :key="bill.recordId" :bill="bill" />
            </ul>
          </section>
        </div>

        <div v-if="store.bills?.hasNext" class="mt-3 flex justify-center">
          <GpButton
            variant="pill"
            size="pill"
            :disabled="store.billsLoading"
            @click="store.appendBills(params)"
          >
            {{ store.billsLoading ? '불러오는 중이에요' : '더 보기' }}
          </GpButton>
        </div>

        <template #emptyAction>
          <GpButton variant="pill" size="pill" @click="router.push('/analysis/bills/new')">
            고지서 등록하러 가기
          </GpButton>
        </template>
      </MypageState>
    </div>

    <template #footer>
      <div
        class="bg-canvas border-divider fixed inset-x-0 bottom-0 z-20 mx-auto max-w-(--gp-viewport-w) border-t px-(--gp-gutter) pt-3 pb-[max(12px,env(safe-area-inset-bottom))]"
      >
        <GpButton @click="router.push('/analysis/bills/new')">새 고지서 등록하기</GpButton>
      </div>
    </template>
  </AppSubLayout>
</template>
