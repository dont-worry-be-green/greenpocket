<script setup>
/*
 * What-if 탭 홈 — WF-01(연동 전) · WF-02(연동 중) · WF-03(목표 미설정)
 *
 * 셋은 라우트가 아니라 **한 탭 홈의 세 상태**다(api-spec.md 10.1). 그래서 경로는 /whatif 하나이고
 * 이 뷰가 screen 값으로 분기한다. WF-06(목표 설정 후)은 아직 없다.
 *
 * 표시 데이터는 fixtures/eco.js 에 있다. 필드명은 api-spec.md 8절 · 백엔드 DTO 와 같아서
 * 연동할 때 import 만 스토어 호출로 바꾸면 아래 화면 코드는 그대로 둔다.
 *
 * ── API 연동 시 교체할 지점 ──────────────────────────────────────────
 *   screen ref         → GET /eco/home 의 screen
 *   ECO_STATUS         → GET /eco/status            (linkable · B-1-09)
 *   startLinking()     → POST /eco/link → GET /eco/link/{linkJobId} 폴링 (20초 · B-1-03)
 *   ECO_CURRENT_ROUND  → GET /eco/rounds/current
 * ────────────────────────────────────────────────────────────────────
 * 엔드포인트는 백엔드에 이미 있지만 데모 키를 발급할 POST /users 가 없어 지금은 붙이지 않는다.
 * 화면 진입만으로 네트워크 요청을 만들지 않는다.
 */
import { computed, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import EcoBaselinePanel from '@/components/eco/EcoBaselinePanel.vue'
import EcoLinkingPanel from '@/components/eco/EcoLinkingPanel.vue'
import EcoUnlinkedPanel from '@/components/eco/EcoUnlinkedPanel.vue'
import AppTabLayout from '@/components/layout/AppTabLayout.vue'
import { ECO_CURRENT_ROUND, ECO_LINK_UTILITIES, ECO_STATUS } from '@/fixtures/eco'
import { formatRoundPeriod } from '@/utils/format'

// GET /eco/home 의 screen 중 이 뷰가 그릴 수 있는 것 (WhatIfScreen enum)
const SCREENS = ['WF_01_UNLINKED', 'WF_02_LINKING', 'WF_03_NO_GOAL']
const UTILITY_ORDER = ECO_LINK_UTILITIES.map((item) => item.utilityType)

const route = useRoute()

const screen = ref(previewFromQuery(route.query.preview))
const linkable = ref(ECO_STATUS.linkable)
const actionMessage = ref('')

const initialUtilities = () => ECO_LINK_UTILITIES.map((item) => ({ ...item }))
const linkingUtilities = ref(initialUtilities())

let timers = []
function clearTimers() {
  timers.forEach(window.clearTimeout)
  timers = []
}
onUnmounted(clearTimers)

// ?preview=WF_03_NO_GOAL 로 각 상태에 바로 들어갈 수 있게 한다. API 가 붙으면 지운다.
function previewFromQuery(value) {
  return SCREENS.includes(value) ? value : SCREENS[0]
}
watch(
  () => route.query.preview,
  (value) => {
    clearTimers()
    linkingUtilities.value = initialUtilities()
    screen.value = previewFromQuery(value)
  },
)

/*
 * 연동 흐름 재현. 전기 → 도시가스 → 수도 순으로 채운 뒤 WF-03 으로 넘어간다.
 * URL 은 바꾸지 않는다 — 뒤로가기 히스토리에 중간 상태를 남기지 않기 위해서다.
 */
function startLinking() {
  clearTimers()
  linkingUtilities.value = initialUtilities()
  screen.value = 'WF_02_LINKING'

  UTILITY_ORDER.forEach((utilityType, index) => {
    timers.push(
      window.setTimeout(() => {
        linkingUtilities.value = linkingUtilities.value.map((item) => {
          if (item.utilityType === utilityType) return { ...item, status: 'SUCCEEDED' }
          const isNext = item.utilityType === UTILITY_ORDER[index + 1]
          return isNext ? { ...item, status: 'RUNNING' } : item
        })
      }, 900 * (index + 1)),
    )
  })
  // 첫 항목은 시작과 동시에 진행 중으로 보인다
  linkingUtilities.value = linkingUtilities.value.map((item, index) =>
    index === 0 ? { ...item, status: 'RUNNING' } : item,
  )
  timers.push(window.setTimeout(() => (screen.value = 'WF_03_NO_GOAL'), 900 * 4))
}

// WF-04 목표 정하기는 아직 없다. 라우트를 미리 만들지 않는다.
function showGoalNotice() {
  actionMessage.value = '목표 정하기는 준비 중이에요.'
  window.setTimeout(() => (actionMessage.value = ''), 2200)
}

// 헤더 문구가 화면마다 달라 meta 대신 뷰가 직접 넘긴다
const subtitle = computed(() => {
  if (screen.value === 'WF_02_LINKING') return '작년 사용량을 불러오는 중이에요'
  if (screen.value === 'WF_03_NO_GOAL') {
    const period = formatRoundPeriod(ECO_CURRENT_ROUND.periodStart, ECO_CURRENT_ROUND.periodEnd)
    return `내 평가 기간 ${period} · 목표를 아직 정하지 않았어요`
  }
  return '작년 사용량을 불러오면 목표를 정할 수 있어요'
})
</script>

<template>
  <AppTabLayout tab="whatif" title="Green What-if" :subtitle="subtitle">
    <EcoUnlinkedPanel
      v-if="screen === 'WF_01_UNLINKED'"
      :linkable="linkable"
      @link="startLinking"
    />
    <EcoLinkingPanel v-else-if="screen === 'WF_02_LINKING'" :utilities="linkingUtilities" />
    <EcoBaselinePanel v-else :round="ECO_CURRENT_ROUND" @set-goal="showGoalNotice" />

    <div
      v-if="actionMessage"
      class="bg-ink text-on-primary shadow-float text-caption fixed bottom-24 left-1/2 z-70 w-max max-w-[calc(100%-32px)] -translate-x-1/2 rounded-full px-4 py-3"
    >
      {{ actionMessage }}
    </div>
  </AppTabLayout>
</template>
