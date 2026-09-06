<script setup>
/*
 * 혜택 탭 홈 — BN-01(미참여) · BN-02(참여 중)
 *
 * 둘은 라우트가 아니라 **한 탭 홈의 두 상태**다(api-spec.md 12.1). 경로는 /benefit 하나이고
 * `GET /greenlife/status` 의 `screen` 이 무엇을 그릴지 정한다.
 *
 * ── 화면이 하지 않는 것 ─────────────────────────────────────────────────
 * **`participating` 을 보고 여기서 다시 판정하지 않는다.** 서버 판정과 두 벌이 되어 조용히
 * 어긋난다(`views/eco/WhatIfHomeView.vue` 와 같은 규칙).
 *
 * **숫자를 만들지 않는다.** 적립 예정·지급 완료·연간 진행률·건수는 전부 응답 필드 그대로다.
 *
 * ── 보고 있는 달은 URL 이 들고 있다 ─────────────────────────────────────
 * `/benefit?month=2026-08`. 화면 상태로 두면 새로고침·공유·데모 도구 바로가기에서 이번 달로
 * 되돌아간다. 쿼리가 없으면 KST 기준 이번 달이고, 그건 서버가 `month` 를 생략했을 때 고르는
 * 달과 같다(api-spec 12.1 · 12.3).
 *
 * **이동 범위를 화면이 정한다.** 서버는 미래 달도 참여 이전 달도 `200` + 0건으로 준다.
 * 막지 않으면 「11월 실천 현황 0건」을 무한히 넘길 수 있다. 위는 이번 달, 아래는 기준 연도
 * 1월이다 — 연간 한도 카드(C-2-02)가 같은 해 기준이라 한 화면 안에서 말이 맞는다.
 * `linkedAt` 은 하한으로 쓸 수 없다. 연동일보다 앞선 달에도 누리집 실적이 있다.
 *
 * ⚠️ **현황과 목록을 같은 달로 함께 받는다.** 둘이 갈리면 카드 합계와 목록 합계가 어긋나
 * C-2-01 완료 조건("두 금액이 실적 내역 합계와 일치한다")이 깨진다.
 *
 * ── 복귀 시 자동 재조회를 넣지 않았다 ───────────────────────────────────
 * C-1-01 은 "누리집 복귀 시 상태를 갱신한다" 지만 `focus`·`visibilitychange` 는 탭 전환·알림·
 * 화면잠금에도 걸린다. 시연 중 화면이 제멋대로 다시 로딩되는 쪽이 위험이 크다. 시안에 있는
 * 「연동 상태 새로고침」 버튼이 그 조건을 이미 충족한다.
 */
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import GreenlifeItemList from '@/components/greenlife/GreenlifeItemList.vue'
import GreenlifeItemRow from '@/components/greenlife/GreenlifeItemRow.vue'
import GreenlifeProgramCard from '@/components/greenlife/GreenlifeProgramCard.vue'
import GreenlifeState from '@/components/greenlife/GreenlifeState.vue'
import GreenlifeStepList from '@/components/greenlife/GreenlifeStepList.vue'
import GreenlifeSummaryCard from '@/components/greenlife/GreenlifeSummaryCard.vue'
import AppTabLayout from '@/components/layout/AppTabLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconChevronDown from '@/components/ui/icons/IconChevronDown.vue'
import IconExternalLink from '@/components/ui/icons/IconExternalLink.vue'
import IconRefresh from '@/components/ui/icons/IconRefresh.vue'
import { useGreenlifeStore } from '@/stores/greenlife'
import { formatDateTime } from '@/utils/format'
import { currentMonth, isMonth, shiftMonth } from '@/utils/month'

const route = useRoute()
const router = useRouter()
const store = useGreenlifeStore()

const participating = computed(() => store.screen === 'BN-02')

// ── 보고 있는 달 ────────────────────────────────────────────────────────

/** 이번 달보다 미래는 없다. 실적이 아직 생길 수 없는 달이다 */
const maxMonth = currentMonth()

/**
 * 기준 연도 1월. `standardYear` 는 응답이 와야 알 수 있어 그전에는 올해로 둔다 —
 * 그동안은 「이전 달」 버튼이 한 칸 더 열려 있을 뿐 데이터는 정상이다.
 */
const minMonth = computed(() => `${store.status?.standardYear ?? maxMonth.slice(0, 4)}-01`)

/*
 * 쿼리는 밖에서 들어오는 값이라 형식을 믿지 않는다(서버는 이런 값에 400 을 준다).
 * 위쪽만 자른다 — 아래쪽까지 자르면 `standardYear` 가 도착하며 달이 바뀌어 한 번 더 조회한다.
 */
const month = computed(() => {
  const raw = route.query.month
  if (!isMonth(raw)) return maxMonth
  return raw > maxMonth ? maxMonth : raw
})

const canPrev = computed(() => month.value > minMonth.value)
const canNext = computed(() => month.value < maxMonth)

/** 히스토리를 쌓지 않는다. 탭 홈에서 뒤로가기가 달 이동을 되짚으면 탭을 벗어나기 어렵다 */
function goMonth(delta) {
  router.replace({ path: '/benefit', query: { ...route.query, month: shiftMonth(month.value, delta) } })
}

/*
 * 첫 응답 전. `isLoading` 을 조건에 넣지 않는다 — 첫 렌더는 onMounted 보다 먼저라
 * 그 한 틱 동안 status 도 null, isLoading 도 아직 false 다.
 */
const bootstrapping = computed(() => !store.status && !store.error)

const programInfo = computed(() => store.status?.programInfo ?? null)

/** BN-01 은 `featuredItems`, 「전체 보기」를 누르면 12.3 의 17개로 바뀐다 */
const featuredItems = computed(() => store.items?.items ?? store.status?.featuredItems ?? [])
const featuredExpanded = computed(() => Boolean(store.items))

watch(month, (value) => store.fetchStatus({ month: value }), { immediate: true })

/*
 * 목록은 참여 중일 때만 자동으로 받는다. 미참여 화면은 대표 항목 4개로 충분하고,
 * 「전체 보기」를 누른 사람에게만 17개를 부른다.
 *
 * 달이 바뀌면 이미 받아 둔 목록이 있어도 **다시 받는다.** 위 카드만 새 달로 바뀌고 목록이
 * 지난달인 채로 남으면 합계가 어긋난다.
 */
watch(
  [participating, month],
  ([isParticipating, value]) => {
    if (isParticipating) store.fetchItems({ month: value })
  },
  { immediate: true },
)

/** 상세도 같은 달을 봐야 한다 — C-2-04 「상세 건수가 목록 건수와 일치한다」 */
function goToItem(itemId) {
  router.push({ path: `/benefit/items/${itemId}`, query: { month: month.value } })
}

/** 실제 가입은 누리집에서 한다(C-1-01). 여기서는 새 탭을 열어줄 뿐이다 */
function openExternal() {
  const url = programInfo.value?.externalUrl
  if (url) window.open(url, '_blank', 'noopener')
}
</script>

<template>
  <AppTabLayout
    tab="benefit"
    title="혜택"
    :subtitle="participating ? '탄소중립포인트 녹색생활실천' : '친환경 실천으로 받는 혜택을 확인해요'"
  >
    <template v-if="participating" #headerAction>
      <GpTag tone="primary">누리집 연동</GpTag>
    </template>

    <GreenlifeState
      :loading="bootstrapping"
      :error="store.status ? null : store.error"
      @retry="store.fetchStatus({ month })"
    >
      <!-- ── BN-02 참여 중 ─────────────────────────────────────────── -->
      <div v-if="participating" class="space-y-5">
        <!-- 달은 서버 응답의 것을 쓴다. 요청한 달과 응답이 갈리면 응답이 사실이다 -->
        <GreenlifeSummaryCard
          :month="store.status.month"
          :month-summary="store.status.monthSummary"
          :annual="store.status.annual"
          :can-prev="canPrev"
          :can-next="canNext"
          @prev="goMonth(-1)"
          @next="goMonth(1)"
        />

        <p v-if="store.status.delayNotice" class="text-caption text-muted m-0 px-1">
          {{ store.status.delayNotice }}
        </p>

        <GreenlifeState
          :loading="store.itemsLoading && !store.items"
          :error="store.itemsError"
          @retry="store.fetchItems({ month })"
        >
          <GreenlifeItemList
            v-if="store.items"
            :items="store.items.items"
            :total-count="store.items.totalCount"
            :collapsed-after="store.items.collapsedAfter"
            @select="goToItem"
          />
        </GreenlifeState>

        <div class="text-caption text-muted space-y-1 pt-1 text-center">
          <p class="m-0">{{ store.status.standardYear }}년 공식 적립 기준</p>
          <button
            type="button"
            class="mx-auto flex items-center gap-1.5"
            :disabled="store.linkLoading"
            @click="store.refreshLink({ month })"
          >
            <span>최근 연동 {{ formatDateTime(store.status.linkedAt) }}</span>
            <IconRefresh :size="14" :class="store.linkLoading && 'animate-spin'" />
          </button>
        </div>
      </div>

      <!-- ── BN-01 미참여 ──────────────────────────────────────────── -->
      <div v-else-if="programInfo" class="space-y-4">
        <GreenlifeProgramCard :program-info="programInfo" />

        <GpCard title="참여 방법">
          <GreenlifeStepList :steps="programInfo.joinSteps" />
        </GpCard>

        <GpCard title="대표 실천 항목">
          <div class="space-y-2">
            <GreenlifeItemRow v-for="item in featuredItems" :key="item.itemId" :item="item" compact />
          </div>

          <div v-if="!featuredExpanded" class="mt-3 flex justify-center">
            <GpButton
              variant="pill"
              size="pill"
              :disabled="store.itemsLoading"
              @click="store.fetchItems({ month })"
            >
              {{ programInfo.itemCount }}개 전체 보기
              <IconChevronDown :size="12" class="ml-1" />
            </GpButton>
          </div>
        </GpCard>

        <GpButton @click="openExternal">
          공식 누리집에서 참여하기
          <IconExternalLink :size="16" class="ml-1.5" />
        </GpButton>

        <div class="space-y-1 pt-1 text-center">
          <p class="text-caption text-muted m-0">이미 참여하고 있다면</p>
          <button
            type="button"
            class="text-body-strong text-primary-on-soft mx-auto flex items-center gap-1.5"
            :disabled="store.linkLoading"
            @click="store.refreshLink({ month })"
          >
            <IconRefresh :size="16" :class="store.linkLoading && 'animate-spin'" />
            연동 상태 새로고침
          </button>
        </div>
      </div>

      <!--
        새로고침했는데 여전히 미참여. 오류가 아니라 정상 응답이라(12.2 · 핵심 규칙 8)
        에러 화면이 아니라 안내로 남긴다.
      -->
      <p v-if="store.linkNotice" class="text-caption text-ink-soft mt-3 mb-0 text-center">
        {{ store.linkNotice }}
      </p>
      <p v-else-if="store.linkError" class="text-caption text-negative mt-3 mb-0 text-center">
        {{ store.linkError.message }}
      </p>
    </GreenlifeState>
  </AppTabLayout>
</template>
