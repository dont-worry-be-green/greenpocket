<script setup>
/*
 * What-if 탭 홈 — WF-01(연동 전) · WF-02(연동 중) · WF-03(목표 미설정) · WF-06(목표 설정 후)
 *
 * 넷은 라우트가 아니라 **한 탭 홈의 네 상태**다(api-spec.md 10.1). 경로는 /whatif 하나이고
 * `GET /eco/home` 의 `screen` 이 무엇을 그릴지 정한다.
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
 * ── WF-06 이 홈을 두 번 부르는 이유 ─────────────────────────────────────
 * `home.goal` 은 `goalSet · combinedTargetRate · tier · expectedMileage` 넷뿐이라
 * 시안의 요금별 3열이 나오지 않는다. 그건 `GET /eco/rounds/{roundId}/goal` 의 `utilities[]` 다.
 */
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import EcoApplicationBanner from '@/components/eco/EcoApplicationBanner.vue'
import EcoBaselinePanel from '@/components/eco/EcoBaselinePanel.vue'
import EcoGoalCard from '@/components/eco/EcoGoalCard.vue'
import EcoLatestReportCard from '@/components/eco/EcoLatestReportCard.vue'
import EcoLinkingPanel from '@/components/eco/EcoLinkingPanel.vue'
import EcoProgressPanel from '@/components/eco/EcoProgressPanel.vue'
import EcoTodayMissions from '@/components/eco/EcoTodayMissions.vue'
import EcoUnlinkedPanel from '@/components/eco/EcoUnlinkedPanel.vue'
import AppTabLayout from '@/components/layout/AppTabLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import { useEcoStore } from '@/stores/eco'
import { formatRoundPeriod } from '@/utils/format'

// WhatIfScreen enum (api-spec.md 3절). WF-09 결산 모달은 배치 4 라 지금은 본문만 WF-06 과 같다
const SCREENS = [
  'WF_01_UNLINKED',
  'WF_02_LINKING',
  'WF_03_NO_GOAL',
  'WF_06_IN_PROGRESS',
  'WF_09_RESULT_READY',
]
const IN_PROGRESS_SCREENS = ['WF_06_IN_PROGRESS', 'WF_09_RESULT_READY']

/** 연동 폴링 간격. 서버가 20초쯤 걸린다고 안내한다(B-1-03) */
const POLL_INTERVAL_MS = 900

const route = useRoute()
const router = useRouter()
const store = useEcoStore()

const previewScreen = computed(() =>
  SCREENS.includes(route.query.preview) ? route.query.preview : null,
)
const screen = computed(() => previewScreen.value ?? store.screen)

const isInProgress = computed(() => IN_PROGRESS_SCREENS.includes(screen.value))

// 연동 중에는 폴링 응답의 utilityStatus 를 그대로 그린다. 진행 단계를 화면이 만들지 않는다
const linkingUtilities = computed(() => store.linkJob?.utilityStatus ?? [])

const goalUtilities = computed(() => store.goal?.utilities ?? null)

// 첫 응답이 오기 전. 그릴 수 있는 것이 아직 없다
const isBootstrapping = computed(() => !store.home && store.isLoading)
const hasFatalError = computed(() => !store.home && !store.isLoading && Boolean(store.error))

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

/*
 * 화면이 정해진 뒤에 그 화면이 쓰는 것만 더 받는다.
 * roundId 는 홈이 와야 생기므로 함께 지켜본다 — ?preview 로 바로 들어오면 순서가 뒤집힌다.
 */
watch(
  [screen, () => store.roundId],
  ([value, roundId]) => {
    if (value === 'WF_03_NO_GOAL' && !store.currentRound) store.fetchCurrentRound()
    if (!IN_PROGRESS_SCREENS.includes(value) || !roundId) return
    if (!store.todayMissions) store.fetchTodayMissions(roundId)
    if (!store.goal) store.fetchGoal(roundId)
  },
  { immediate: true },
)

// ── 연동 (B-1-02 · B-1-03) ────────────────────────────────────────────

let pollTimer = null
function stopPolling() {
  if (pollTimer) window.clearInterval(pollTimer)
  pollTimer = null
}
onUnmounted(stopPolling)

async function startLink() {
  const linkJobId = await store.startLink()
  if (!linkJobId) return

  // 서버가 화면을 WF-02 로 바꾼다. 화면이 스스로 바꾸지 않는다
  await store.fetchHome()
  await pollOnce()
  if (pollTimer === null && store.linkJob?.status === 'RUNNING') {
    pollTimer = window.setInterval(pollOnce, POLL_INTERVAL_MS)
  }
}

async function pollOnce() {
  const job = await store.pollLinkJob()
  // RUNNING 이 아니면 끝난 것이다. 성공·실패 어느 쪽이든 홈을 다시 받아 화면을 넘긴다
  if (job?.status === 'RUNNING') return
  stopPolling()
  await store.fetchHome()
}

// ── 사용자 동작 ───────────────────────────────────────────────────────

/** WF-04 목표 정하기. 회차 번호는 경로에 싣지 않는다 — 그 화면이 스토어에서 가져온다 */
function goToGoalSetting() {
  router.push('/whatif/goal')
}

function goToReport() {
  router.push('/whatif/report')
}

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
 * 실제 신청은 누리집에서 한다(B-4-05). 여기서는 신청했다고 표시만 한다.
 * 새 탭을 여는 것과 표시는 별개라 둘 다 한다.
 */
function onApply(externalUrl) {
  if (externalUrl) window.open(externalUrl, '_blank', 'noopener')
  if (store.roundId) store.applyForRound(store.roundId)
}

function retry() {
  store.fetchHome()
}
</script>

<template>
  <AppTabLayout tab="whatif" title="Green What-if" :subtitle="subtitle">
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
      @link="startLink"
    />

    <EcoLinkingPanel v-else-if="screen === 'WF_02_LINKING'" :utilities="linkingUtilities" />

    <!-- WF-06 · WF-09. 결산 모달은 배치 4 에서 이 위에 얹는다 -->
    <div v-else-if="isInProgress" class="space-y-5">
      <EcoProgressPanel :progress="store.home.progress" />

      <EcoLatestReportCard :report="store.home.latestReport" @detail="goToReport" />

      <EcoApplicationBanner
        v-if="store.home.application?.showBanner"
        :application="store.home.application"
        :loading="store.applicationLoading"
        @apply="onApply"
      />

      <EcoGoalCard
        :goal="store.home.goal"
        :utilities="goalUtilities"
        @edit="goToGoalSetting"
      />

      <EcoTodayMissions
        :data="store.todayMissions"
        :saving="store.missionSaveLoading"
        @change="onMissionChange"
      />

      <p v-if="store.home.links?.movingNotice" class="text-caption text-muted m-0 px-1">
        이사했다면
        <RouterLink to="/mypage" class="text-primary-on-soft underline">마이페이지</RouterLink>에서
        주소를 바꿔주세요. 바꾸지 않으면 지금 살지 않는 집의 사용량과 비교돼요.
      </p>
    </div>

    <!-- WF-03. 기준 사용량은 회차 조회에서 온다 -->
    <EcoBaselinePanel
      v-else-if="store.currentRound"
      :round="store.currentRound"
      :show-moving-notice="store.home?.links?.movingNotice ?? true"
      @set-goal="goToGoalSetting"
    />

    <GpCard v-else>
      <p class="text-body text-muted m-0">불러오는 중이에요…</p>
    </GpCard>
  </AppTabLayout>
</template>
