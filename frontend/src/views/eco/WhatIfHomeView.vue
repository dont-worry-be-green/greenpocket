<script setup>
/*
 * 에코 연동 진입 · What-if 홈 — WF-01(연동 전) · WF-02(연동 중) · WF-03(기준 사용량) ·
 * WF-06(목표 설정 후)
 *
 * 넷은 라우트가 아니라 `GET /eco/home`의 `screen`으로 정해지는 상태다. WF-01~03은 로그인 직후
 * `/analysis/eco-link`에서 진단 탭으로, WF-06은 `/whatif`에서 What-if 탭으로 보여 준다.
 *
 * ── 화면이 하지 않는 것 ─────────────────────────────────────────────────
 * **`screen` 을 화면이 판정하지 않는다.** 연동 여부·목표 저장 여부를 보고 여기서 분기하기
 * 시작하면 서버 판정과 두 벌이 되어 조용히 어긋난다. 연동 중(WF-02)도 마찬가지라,
 * `POST /eco/link` 뒤에 홈을 다시 받아 서버가 정한 값을 쓴다.
 *
 * **숫자를 만들지 않는다.** 진행률·구간·완료 수는 전부 응답 필드 그대로다.
 *
 * ── ?preview= ───────────────────────────────────────────────────────────
 * `screen` **하나만** 덮어쓴다. 데이터는 덮지 않으며 `fetchHome()` 은 preview 여부와
 * 무관하게 항상 부른다. (WF-04·WF-05 는 목표 화면 쪽 preview 라 여기서 다루지 않는다)
 *
 * 그래서 WF-09 결산 모달은 `?preview=` 로 열리지 않는다 — `resultModal` 은 screen 이 아니라
 * **데이터**라, 그 상태를 만드는 일은 `api/eco.js` 의 픽스처 shim 이 한다.
 * 여기서는 `store.showResultModal` 만 본다.
 *
 * ── WF-09 결산 모달 ─────────────────────────────────────────────────────
 * ⚠️ `resultModal.roundId` 는 **지난 회차**다. 홈이 보여 주는 진행 중 회차(`store.roundId`)가
 * 아니라 방금 확정된 직전 회차라, 결과 화면으로 보낼 때 반드시 모달의 번호를 쓴다.
 *
 * ── WF-06 이 홈 말고 셋을 더 부르는 이유 ─────────────────────────────────
 *   오늘의 실천  GET /eco/rounds/{roundId}/missions/today
 *   목표        GET /eco/rounds/{roundId}/goal — 하단 「월 약 N원」의 expectedSavingAmount
 *   월 리포트   GET /eco/monthly-report — 감축률 카드가 쓰는 `prescription.requiredRate`(남은 달
 *              매달 필요한 감축률)와 `achievable`(고른 실천으로 회복 가능한지). 홈 응답에는 이 둘이
 *              없어서 한 번 더 부른다. 홈 응답에 들어오면(BE 요청) 이 호출은 뺀다.
 *
 * ── 홈은 헤더 + 감축률 카드 + 오늘의 실천, 둘뿐이다 ──────────────────────
 * 전달 리포트 · 목표 카드는 감축률 카드 하단 링크(월 리포트 → WF-07 · 내 목표 → WF-04)로 간다.
 * 참여신청 배너(B-4-05)는 홈에서 뺐다(2026-09-09 수현 결정, 기능명세 B-4-04~06 갱신). 입구는 보류 상태다.
 */
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import EcoBaselinePanel from '@/components/eco/EcoBaselinePanel.vue'
import EcoBaselineHeader from '@/components/eco/EcoBaselineHeader.vue'
import EcoHomeHeader from '@/components/eco/EcoHomeHeader.vue'
import EcoLinkingPanel from '@/components/eco/EcoLinkingPanel.vue'
import EcoPaceCard from '@/components/eco/EcoPaceCard.vue'
import EcoResultModal from '@/components/eco/EcoResultModal.vue'
import EcoTodayMissions from '@/components/eco/EcoTodayMissions.vue'
import EcoUnlinkedPanel from '@/components/eco/EcoUnlinkedPanel.vue'
import { derivePace } from '@/components/eco/ecoPace'
import AppTabLayout from '@/components/layout/AppTabLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import { DEMO_ECO_REPORT_ID } from '@/data/demoEcoReport'
import GpCard from '@/components/ui/GpCard.vue'
import { useAuthStore } from '@/stores/auth'
import { useEcoStore } from '@/stores/eco'
import { formatRoundPeriod, formatRoundPeriodChip } from '@/utils/format'

// WhatIfScreen enum (api-spec.md 3절). WF-09 는 본문이 WF-06 과 같고 그 위에 결산 모달만 얹힌다
const SCREENS = [
  'WF_01_UNLINKED',
  'WF_02_LINKING',
  'WF_03_NO_GOAL',
  'WF_06_IN_PROGRESS',
  'WF_09_RESULT_READY',
]
const LINK_ENTRY_SCREENS = ['WF_01_UNLINKED', 'WF_02_LINKING']
const IN_PROGRESS_SCREENS = ['WF_06_IN_PROGRESS', 'WF_09_RESULT_READY']

/** 연동 폴링 간격. 서버가 20초쯤 걸린다고 안내한다(B-1-03) */
const POLL_INTERVAL_MS = 1000

const route = useRoute()
const router = useRouter()
const store = useEcoStore()
const auth = useAuthStore()

const props = defineProps({
  // 로그인 직후 진단 탭에서 WF-01~03을 재사용하는 진입 흐름인지 구분한다.
  diagnosisEntry: { type: Boolean, default: false },
})

const activeTab = computed(() => (props.diagnosisEntry ? 'analysis' : 'whatif'))

const previewScreen = computed(() =>
  SCREENS.includes(route.query.preview) ? route.query.preview : null,
)
const screen = computed(() => previewScreen.value ?? store.screen)

const isInProgress = computed(() => IN_PROGRESS_SCREENS.includes(screen.value))

// 연동 중에는 폴링 응답의 utilityStatus 를 그대로 그린다. 진행 단계를 화면이 만들지 않는다
const linkingUtilities = computed(() => store.linkJob?.utilityStatus ?? [])

/*
 * 첫 응답이 오기 전. 그릴 수 있는 것이 아직 없다.
 *
 * ⚠️ **`isLoading` 을 조건에 넣지 않는다.** `fetchHome()` 은 `onMounted` 에서 부르는데
 * 첫 렌더는 그보다 먼저다. 그 한 틱 동안 `home` 은 null 인데 `isLoading` 도 아직 false 라,
 * `isLoading` 을 보면 두 분기가 모두 빠지고 본문이 `store.home.progress` 를 읽어 터진다.
 * `?preview=` 로 바로 들어오면 화면이 이미 정해져 있어서 반드시 그 경로를 탄다.
 */
const isBootstrapping = computed(() => !store.home && !store.error)
const hasFatalError = computed(() => !store.home && Boolean(store.error))

/** 감축률 카드 우상단 기간 칩 '2026.04~09'. 「누적」이 무엇의 누적인지 밝힌다(핵심 규칙 7) */
const periodLabel = computed(() => {
  const header = store.home?.header
  if (!header) return ''
  return formatRoundPeriodChip(header.periodStart, header.periodEnd)
})

/** 헤드라인 「오늘 미션 N개가 남았어요」. 오늘의 실천 응답이 오면 그쪽이 더 새 값이다 */
const todaySummary = computed(() => store.todayMissions ?? store.home?.todayMissions ?? null)

/** 캐릭터 표정 = 감축률 카드의 페이스. 두 컴포넌트가 같은 판정을 쓴다 */
const pace = computed(() =>
  derivePace(store.home?.progress, store.monthlyReport?.prescription ?? null),
)

const subtitle = computed(() => {
  if (screen.value === 'WF_02_LINKING') return '작년 사용량을 불러오는 중이에요'

  const header = store.home?.header
  if (isInProgress.value && header) {
    const period = formatRoundPeriod(header.periodStart, header.periodEnd)
    const months = header.remainingLabelMonths?.join('·')
    return months
      ? `내 평가 기간 ${period} · ${months}월 ${header.remainingMonths}달 남았어요`
      : `내 평가 기간 ${period}`
  }

  if (screen.value === 'WF_03_NO_GOAL' && store.currentRound) {
    const period = formatRoundPeriod(store.currentRound.periodStart, store.currentRound.periodEnd)
    return `내 평가 기간 ${period} · 목표를 아직 정하지 않았어요`
  }

  return '작년 사용량을 불러오면 목표를 정할 수 있어요'
})

// ── 데이터 ────────────────────────────────────────────────────────────

onMounted(() => {
  // preview 여부와 무관하게 항상 부른다. 쿼리가 데이터를 갈아끼우면 안 된다
  store.fetchHome()
  store.fetchStatus()
})

// ── 연동 (B-1-02 · B-1-03) ────────────────────────────────────────────

/*
 * **연동을 이 화면이 시작한다.** 한동안은 본인확인 화면(WF-01a)이 시작했는데, 본인확인이
 * 회원가입으로 옮겨가면서(이슈 #121) 그 화면이 없어졌다. 시작하는 곳과 진행을 그리는 곳이
 * 같아졌지만 폴링 규칙은 그대로다.
 *
 * 폴링이 한 벌만 돌아야 해서 타이머가 아니라 `polling` 플래그로 막는다 — 첫 `pollOnce()` 를
 * await 하는 동안에는 타이머가 아직 없어 watch 가 두 번 들어올 수 있다. 연동 중에 다른 탭을
 * 다녀와 이 화면이 다시 마운트되는 경로도 그래서 안전하다.
 */
let pollTimer = null
let polling = false

function stopPolling() {
  polling = false
  if (pollTimer) window.clearInterval(pollTimer)
  pollTimer = null
}
onUnmounted(stopPolling)

async function ensurePolling() {
  if (polling || !store.linkJobId) return
  polling = true

  await pollOnce()
  if (polling && store.linkJob?.status === 'RUNNING') {
    pollTimer = window.setInterval(pollOnce, POLL_INTERVAL_MS)
  }
}

/**
 * WF-01 「연동하기」 (B-1-02). 서버가 `linkJobId` 를 주면 홈을 다시 받아 WF-02 로 넘어간다 —
 * **화면이 스스로 WF-02 로 바꾸지 않는다.** 그러면 서버 판정과 두 벌이 된다.
 */
async function startLink() {
  const linkJobId = await store.startLink()
  if (linkJobId) await store.fetchHome()
}

/** 누리집(외부). 실제 가입은 거기서 한다 — 여기서 대신해 주지 않는다 */
function openEcoSite() {
  const url = store.status?.externalUrl
  if (url) window.open(url, '_blank', 'noopener')
}

async function pollOnce() {
  const job = await store.pollLinkJob()
  // RUNNING 이 아니면 끝난 것이다. 성공·실패 어느 쪽이든 홈을 다시 받아 화면을 넘긴다
  if (job?.status === 'RUNNING') return
  stopPolling()
  await store.fetchHome()
}

/*
 * ⚠️ **폴링 선언 아래에 둔다.** `immediate: true` 라 setup 도중에 한 번 돈다. 위로 올리면
 * `?preview=WF_02_LINKING` 으로 바로 들어왔을 때 `ensurePolling()` 이 아직 초기화되지 않은
 * `polling`(let) 을 읽어 ReferenceError 로 첫 렌더가 통째로 죽는다. 함수 선언은 끌어올려지지만
 * `let` 은 아니다.
 *
 * 화면이 정해진 뒤에 그 화면이 쓰는 것만 더 받는다.
 * roundId 는 홈이 와야 생기므로 함께 지켜본다 — ?preview 로 바로 들어오면 순서가 뒤집힌다.
 */
watch(
  [screen, () => store.roundId, () => route.path],
  ([value, roundId, path]) => {
    // 에코 연동 전·진행 중 화면은 진단 탭에서만 보여 준다.
    // 두 경로가 같은 컴포넌트를 써서 탭 이동 때 인스턴스가 재사용되므로 실제 경로도 감시한다.
    if (path === '/whatif' && LINK_ENTRY_SCREENS.includes(value)) {
      router.replace('/analysis/eco-link')
      return
    }
    // 연동·목표 설정이 끝나 WF-06 이상이면 진단 탭은 고지서 진단 화면으로 이동한다.
    if (path === '/analysis/eco-link' && IN_PROGRESS_SCREENS.includes(value)) {
      router.replace('/analysis')
      return
    }
    if (value === 'WF_02_LINKING') ensurePolling()
    if (value === 'WF_03_NO_GOAL' && !store.currentRound) store.fetchCurrentRound()
    if (!IN_PROGRESS_SCREENS.includes(value) || !roundId) return
    if (!store.todayMissions) store.fetchTodayMissions(roundId)
    // 오늘의 실천 하단 「월 약 N원」의 근거(expectedSavingAmount)
    if (!store.goal) store.fetchGoal(roundId)
    // 페이스·상태 문장의 근거. 최신 등록 월 기준(쿼리 없음)
    if (!store.monthlyReport) store.fetchMonthlyReport()
  },
  { immediate: true },
)

// ── 사용자 동작 ───────────────────────────────────────────────────────

/** WF-04 목표 정하기. 회차 번호는 경로에 싣지 않는다 — 그 화면이 스토어에서 가져온다 */
function goToGoalSetting() {
  router.push(props.diagnosisEntry ? '/analysis' : '/whatif/goal')
}

function goToReport() {
  router.push('/whatif/report')
}

/** WF-08 실천 다시 고르기. 감축률 카드 하단의 세 링크 중 하나 */

/**
 * 하루치를 통째로 올린다(B-3-06). 응답의 completedCount 로 홈 요약도 함께 맞춘다 —
 * 화면이 따로 세면 저장이 실패했을 때 숫자만 앞서간다.
 */
function onMissionChange(completedMissionIds) {
  const date = store.todayMissions?.date
  if (!store.roundId || !date) return
  store.saveTodayMissionLog(store.roundId, date, completedMissionIds)
}

/**
 * WF-09 결산 모달 닫기. 서버에도 알려 다음 홈 응답이 WF-06 으로 내려오게 한다(B-5-01).
 * ⚠️ 회차는 **모달의 것**이다 — `store.roundId` 는 진행 중인 회차라 엉뚱한 회차를 읽음 처리한다.
 */
function onDismissResultModal() {
  store.dismissResultModal(store.home?.resultModal?.roundId)
}

/**
 * 결과 화면으로. 보러 가는 것도 확인한 것이라 함께 닫는다 — 돌아왔을 때 같은 모달이 다시 서 있으면
 * 방금 본 것을 또 보라는 말이 된다.
 */
function goToResult() {
  onDismissResultModal()
  router.push({ path: '/mypage/reports', query: { tab: 'ECO', report: DEMO_ECO_REPORT_ID } })
}

function retry() {
  store.fetchHome()
}
</script>

<template>
  <!-- WF-06 은 홈 전용 헤더(EcoHomeHeader)를 쓴다. 나머지 상태는 공통 페이지 헤더다 -->
  <AppTabLayout
    :tab="activeTab"
    :title="
      isInProgress || (!diagnosisEntry && screen === 'WF_03_NO_GOAL')
        ? ''
        : diagnosisEntry
          ? '진단'
          : 'Green What-if'
    "
    :subtitle="isInProgress || diagnosisEntry ? '' : subtitle"
  >
    <GpCard v-if="isBootstrapping">
      <p class="text-body text-muted m-0">불러오는 중이에요…</p>
    </GpCard>

    <div v-else-if="hasFatalError" class="space-y-4">
      <GpCard>
        <p class="text-body text-ink-soft m-0">
          {{ store.error?.message || '잠시 후 다시 시도해 주세요.' }}
        </p>
      </GpCard>
      <GpButton @click="retry">다시 시도하기</GpButton>
    </div>

    <EcoUnlinkedPanel
      v-else-if="screen === 'WF_01_UNLINKED'"
      :linkable="store.status?.linkable ?? true"
      :external-url="store.status?.externalUrl ?? ''"
      :loading="store.isLoading"
      @link="startLink"
      @open-site="openEcoSite"
    />

    <EcoLinkingPanel v-else-if="screen === 'WF_02_LINKING'" :utilities="linkingUtilities" />

    <!-- WF-06 · WF-09. 본문은 같고 WF-09 만 아래 결산 모달이 위에 얹힌다 -->
    <div v-else-if="isInProgress">
      <EcoHomeHeader :name="auth.user?.name ?? ''" :today-missions="todaySummary" :pace="pace" />

      <!-- 캐릭터가 뒤에 서므로 카드 묶음이 앞(z-1)이다 -->
      <div class="relative z-[1] flex flex-col gap-(--gp-card-gap)">
        <EcoPaceCard
          :progress="store.home.progress"
          :goal="store.home.goal"
          :prescription="store.monthlyReport?.prescription ?? null"
          :period="periodLabel"
          :remaining-months="store.home.header?.remainingMonths ?? null"
          @goal="goToGoalSetting"
          @report="goToReport"
        />

        <EcoTodayMissions
          :data="store.todayMissions"
          :saving="store.missionSaveLoading"
          :expected-saving-amount="store.goal?.expectedSavingAmount ?? null"
          :selected-count="store.goal?.missions?.length ?? null"
          @change="onMissionChange"
        />

        <!-- 「이사했다면 마이에서 주소를…」 안내는 뺐다(2026-09-09 수현). WF-03 기준 사용량 패널에는 남아 있다 -->
      </div>
    </div>

    <!-- WF-03. 기준 사용량은 회차 조회에서 온다 -->
    <div v-else-if="store.currentRound">
      <EcoBaselineHeader v-if="!diagnosisEntry" :name="auth.user?.name ?? ''" />
      <EcoBaselinePanel
        class="relative z-[1]"
        :round="store.currentRound"
        :show-moving-notice="store.home?.links?.movingNotice ?? true"
        :action-label="diagnosisEntry ? '고지서 등록하기' : '목표 설정하기'"
        :compact-amount="!diagnosisEntry"
        :floating-linked-status="!diagnosisEntry"
        :linked-at="store.status?.linkedAt ?? store.linkJob?.linkedAt ?? ''"
        @set-goal="goToGoalSetting"
      />
    </div>

    <GpCard v-else>
      <p class="text-body text-muted m-0">불러오는 중이에요…</p>
    </GpCard>

    <!--
      WF-09. 본문 밖에 두는 이유 — 모달은 어느 분기에 있든 데이터(`resultModal`)만 있으면 뜬다.
      본문 안에 넣으면 로딩·실패 분기에서 사라진다.
    -->
    <EcoResultModal
      :modal="store.home?.resultModal"
      :open="store.showResultModal"
      @close="onDismissResultModal"
      @view="goToResult"
    />
  </AppTabLayout>
</template>
