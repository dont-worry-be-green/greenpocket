/*
 * 에코마일리지 스토어 — WF-01 ~ WF-11 여덟 화면이 함께 쓴다.
 *
 * **스토어를 쪼개지 않는다.** `roundId` 가 `GET /eco/home` 에서만 나오는데 나머지 화면이 전부
 * 그걸 필요로 해서, 나누면 스토어끼리 서로를 부르게 된다.
 *
 * ── 여기에 두지 않는 것 ──────────────────────────────────────────────────
 * WF-04·WF-08 의 **목표 초안**(고른 구간 맵 · 미션 체크 집합)은 서버 데이터가 아니라
 * 한 화면에서만 살다 죽는 폼 상태다. 뷰 로컬 `ref` 에 둔다.
 *
 * ── computed 는 고르기만 한다 ────────────────────────────────────────────
 * 계산은 서버(연동 후) 또는 `fixtures/ecoPreview.js`(그 전) 몫이다.
 * 여기서 숫자를 만들면 서버와 두 벌이 되어 조용히 어긋난다.
 */

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  applyRound,
  createGoal,
  getCurrentRound,
  getEcoHome,
  getEcoLinkJob,
  getEcoStatus,
  getGoal,
  getGoalForm,
  getTodayMissions,
  linkEco,
  markResultViewed,
  previewGoal,
  saveMissionLog,
  updateGoal,
} from '@/api/eco'

export const useEcoStore = defineStore('eco', () => {
  // 서버 응답을 원형 그대로 담는다. 가공한 값을 저장하지 않는다
  const home = ref(null)
  const status = ref(null)
  const linkJobId = ref(null)
  const linkJob = ref(null)
  const currentRound = ref(null)
  const goalForm = ref(null)
  const goalPreview = ref(null)
  const goal = ref(null)
  const goalSaveResult = ref(null)
  const todayMissions = ref(null)
  const resultModalDismissed = ref(false)

  const isLoading = ref(false)
  const error = ref(null)

  // 한 화면에서 **독립적으로 도는 것만** 따로 둔다 (stores/pocket.js 의 accountsLoading 선례)
  const previewLoading = ref(false)
  const previewError = ref(null)
  const goalSaveLoading = ref(false)
  const goalSaveError = ref(null)
  const todayMissionsLoading = ref(false)
  const missionSaveLoading = ref(false)
  const applicationLoading = ref(false)

  /** 늦게 도착한 preview 응답을 버린다. 칩을 빨리 누르면 순서가 뒤집힌다 */
  let previewSequence = 0

  async function run(task) {
    isLoading.value = true
    error.value = null
    try {
      return await task()
    } catch (nextError) {
      error.value = nextError
      return null
    } finally {
      isLoading.value = false
    }
  }

  // ── 고르기만 하는 computed ────────────────────────────────────────────

  /** WhatIfScreen enum. `?preview=` 로 덮어쓰는 것은 뷰의 몫이고 여기서는 서버 값만 본다 */
  const screen = computed(() => home.value?.screen ?? null)

  /**
   * 현재 회차 번호. 홈이 먼저 오지만 목표 화면에 직접 들어올 수도 있어 회차 조회로도 채운다.
   * ⚠️ **WF-10·WF-11 은 지난 회차라 이걸 쓰면 안 된다** — `route.params.roundId` 를 쓴다.
   */
  const roundId = computed(
    () => home.value?.roundId ?? currentRound.value?.roundId ?? goalForm.value?.roundId ?? null,
  )

  /** POST 냐 PUT 이냐를 가른다. 라우트나 진입 경로로 판단하면 새로고침 후 틀린다 */
  const goalSet = computed(
    () => home.value?.goal?.goalSet ?? currentRound.value?.goalSet ?? false,
  )

  /** `state` 는 'CURRENT' | 'TARGET' | 'NONE' 문자열 그대로 쓴다. 직접 판정하지 않는다 */
  const progressTiers = computed(() => home.value?.progress?.tiers ?? [])

  /** WF-09 결산 모달. 닫은 뒤에는 이 세션 동안 다시 뜨지 않는다 */
  const showResultModal = computed(
    () => Boolean(home.value?.resultModal) && !resultModalDismissed.value,
  )

  // ── 연동 ──────────────────────────────────────────────────────────────

  async function fetchHome() {
    const data = await run(getEcoHome)
    if (data) home.value = data
  }

  async function fetchStatus() {
    const data = await run(getEcoStatus)
    if (data) status.value = data
  }

  /** 202 지만 인터셉터가 `data` 만 준다. `linkJobId` 존재로 판단한다 */
  async function startLink(payload = {}) {
    linkJob.value = null
    const data = await run(() => linkEco(payload))
    if (!data?.linkJobId) return null
    linkJobId.value = data.linkJobId
    linkJob.value = data
    return data.linkJobId
  }

  /** 한 번 폴링한다. 900ms 간격과 종료 판정은 부르는 쪽이 한다 */
  async function pollLinkJob() {
    if (!linkJobId.value) return null
    try {
      const data = await getEcoLinkJob(linkJobId.value)
      linkJob.value = data
      return data
    } catch (nextError) {
      error.value = nextError
      return null
    }
  }

  async function fetchCurrentRound() {
    const data = await run(getCurrentRound)
    if (data) currentRound.value = data
    return data
  }

  // ── 목표 (WF-04 · WF-05) ──────────────────────────────────────────────

  async function fetchGoalForm(id) {
    const data = await run(() => getGoalForm(id))
    if (data) goalForm.value = data
    return data
  }

  async function fetchGoal(id) {
    const data = await run(() => getGoal(id))
    if (data) goal.value = data
    return data
  }

  /**
   * 구간·미션을 바꿀 때마다 부른다. 디바운스(250ms)는 뷰가 한다 —
   * 스토어가 타이머를 들면 테스트에서 시간을 흘려야 해서 검증이 어려워진다.
   */
  async function fetchGoalPreview(id, payload) {
    const sequence = ++previewSequence
    previewLoading.value = true
    previewError.value = null
    try {
      const data = await previewGoal(id, payload)
      // 먼저 보낸 요청이 늦게 오면 버린다
      if (sequence !== previewSequence) return null
      goalPreview.value = data
      return data
    } catch (nextError) {
      if (sequence === previewSequence) previewError.value = nextError
      return null
    } finally {
      if (sequence === previewSequence) previewLoading.value = false
    }
  }

  /** 이미 목표가 있으면 PUT, 없으면 POST 다 (`goalSet` 으로 판단한다) */
  async function saveGoal(id, payload) {
    goalSaveLoading.value = true
    goalSaveError.value = null
    try {
      const data = goalSet.value ? await updateGoal(id, payload) : await createGoal(id, payload)
      goalSaveResult.value = data
      // 저장하면 홈·회차의 goalSet 이 바뀐다. 다음 진입에서 다시 받도록 비운다
      home.value = null
      currentRound.value = null
      return data
    } catch (nextError) {
      goalSaveError.value = nextError
      return null
    } finally {
      goalSaveLoading.value = false
    }
  }

  // ── 오늘의 실천 · 참여 신청 (WF-06) ───────────────────────────────────

  /** 홈과 **따로 돈다.** 체크를 저장하면 이것만 다시 받고 화면 전체를 새로 그리지 않는다 */
  async function fetchTodayMissions(id, params = {}) {
    todayMissionsLoading.value = true
    try {
      todayMissions.value = await getTodayMissions(id, params)
      return todayMissions.value
    } catch (nextError) {
      error.value = nextError
      return null
    } finally {
      todayMissionsLoading.value = false
    }
  }

  /**
   * 하루치를 통째로 덮어쓴다 — 서버가 `completedMissionIds` 전량을 받는다(토글 1건이 아니다).
   *
   * 응답의 `completedCount` 로 홈의 요약(3/5)도 같이 맞춘다. 화면이 따로 세면
   * 저장이 실패했을 때 숫자만 앞서간다.
   */
  async function saveTodayMissionLog(id, date, completedMissionIds) {
    missionSaveLoading.value = true
    try {
      const data = await saveMissionLog(id, date, { completedMissionIds })
      await fetchTodayMissions(id, { date })
      if (data && home.value?.todayMissions) {
        home.value = {
          ...home.value,
          todayMissions: { completedCount: data.completedCount, totalCount: data.totalCount },
        }
      }
      return data
    } catch (nextError) {
      error.value = nextError
      return null
    } finally {
      missionSaveLoading.value = false
    }
  }

  /** 참여 신청 (B-4-05). 신청하면 배너가 사라지도록 홈의 application 을 응답으로 갈아끼운다 */
  async function applyForRound(id) {
    applicationLoading.value = true
    try {
      const data = await applyRound(id)
      if (data && home.value) {
        home.value = {
          ...home.value,
          application: {
            ...home.value.application,
            applicationStatus: data.applicationStatus,
            showBanner: data.showBanner,
          },
        }
      }
      return data
    } catch (nextError) {
      error.value = nextError
      return null
    } finally {
      applicationLoading.value = false
    }
  }

  // ── 결산 모달 (WF-09) ─────────────────────────────────────────────────

  /**
   * 로컬 플래그를 **먼저** 세우고 서버에는 알리기만 한다.
   * 닫는 동작이 네트워크를 기다리면 안 되고, 실패해도 모달은 이미 닫혀 있어야 한다.
   * 204 라 응답이 `undefined` 이므로 성공 판정은 try/catch 로만 한다.
   */
  async function dismissResultModal(id) {
    resultModalDismissed.value = true
    if (!id) return
    try {
      await markResultViewed(id)
    } catch {
      // 닫기는 이미 끝났다. 여기서 토스트를 띄우면 닫을 때마다 에러가 뜬다
    }
  }

  return {
    home,
    status,
    linkJobId,
    linkJob,
    currentRound,
    goalForm,
    goalPreview,
    goal,
    goalSaveResult,
    todayMissions,
    resultModalDismissed,
    isLoading,
    error,
    previewLoading,
    previewError,
    goalSaveLoading,
    goalSaveError,
    todayMissionsLoading,
    missionSaveLoading,
    applicationLoading,
    screen,
    roundId,
    goalSet,
    progressTiers,
    showResultModal,
    fetchHome,
    fetchStatus,
    startLink,
    pollLinkJob,
    fetchCurrentRound,
    fetchGoalForm,
    fetchGoal,
    fetchGoalPreview,
    saveGoal,
    fetchTodayMissions,
    saveTodayMissionLog,
    applyForRound,
    dismissResultModal,
  }
})
