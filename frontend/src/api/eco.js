/*
 * 에코마일리지 API — api-spec.md 8~11절. 화면 WF-01 ~ WF-11.
 *
 * ── 픽스처 shim ──────────────────────────────────────────────────────────
 * 백엔드는 엔드포인트가 다 있지만 시드 데이터(mission_catalog · 고지서 · CONFIRMED 회차)가
 * 없어서 실 호출은 200 을 주고도 화면을 그릴 수 없다. 그동안 `src/fixtures/` 로 대신한다.
 *
 * **연동할 때 고칠 파일은 여기 하나다.** `USE_FIXTURES` 를 false 로 두면 스토어·뷰는 그대로 산다.
 *   grep -n USE_FIXTURES src/api/eco.js
 *
 * `fake()` 를 async + 지연으로 둔 이유는 로딩 스피너와 await 순서를 **실제로 돌리기** 위해서다.
 * 스토어에 분기를 두면 즉시 return 이라 로딩 경로가 한 번도 실행되지 않는다.
 *
 * **`src/fixtures/` 를 import 할 수 있는 파일은 이 파일 하나다.**
 * (`__tests__/eco.spec.js` 가 `src/` 전체를 훑어 확인한다)
 */

import {
  ECO_CURRENT_ROUND,
  ECO_LINK_UTILITIES,
  ECO_STATUS,
} from '@/fixtures/eco'
import {
  ECO_GOAL,
  ECO_GOAL_FORM,
  ECO_GOAL_FORM_UNREGISTERED,
  ECO_GOAL_SAVED,
} from '@/fixtures/ecoGoal'
import {
  ECO_APPLICATION_APPLIED,
  ECO_HOME_IN_PROGRESS,
  ECO_TODAY_MISSIONS,
} from '@/fixtures/ecoHome'
import { buildGoalPreview } from '@/fixtures/ecoPreview'

import client from './client'

const USE_FIXTURES = true

/** 실제 호출처럼 지연을 준다. 값 대신 함수를 넘기면 호출 시점에 계산한다 */
const fake = async (value, ms = 220) => {
  await new Promise((resolve) => setTimeout(resolve, ms))
  return typeof value === 'function' ? value() : value
}

/*
 * 픽스처만으로는 못 만드는 "상태"를 여기 둔다.
 *   linkPollCount  연동 진행 단계 — 호출 N 회차 = 진행 N 단계
 *   linking        연동 작업이 도는 중. GET /eco/home 이 WF_02_LINKING 을 돌려주는 근거다
 *   linked         연동 완료 여부. GET /eco/home 의 screen 이 여기서 갈린다
 *   savedGoal      저장한 목표. 다시 들어왔을 때 고른 구간·미션이 남아 있어야 한다(B-2 편집)
 *   applied        참여 신청 여부. 신청하면 배너가 사라져야 한다(B-4-05)
 *   completedMissionIds  오늘 체크한 실천. null 이면 픽스처 기본값을 쓴다
 * 새로고침하면 사라진다. 서버가 붙으면 통째로 사라질 코드다.
 */
const demoState = {
  linkPollCount: 0,
  linking: false,
  linked: false,
  savedGoal: null,
  applied: false,
  completedMissionIds: null,
}

// ── 8절 · 연동 ──────────────────────────────────────────────────────────

/** GET /eco/status — 연동 상태 (B-1-01 · B-1-09) */
export function getEcoStatus() {
  if (USE_FIXTURES) return fake(ECO_STATUS)
  return client.get('/eco/status')
}

/**
 * POST /eco/link — 연동 시작 (B-1-02).
 * 서버는 202 지만 인터셉터가 `data` 만 넘겨줘 status 를 볼 수 없다. `linkJobId` 로 판단한다.
 */
export function linkEco(payload = {}) {
  if (USE_FIXTURES) {
    demoState.linkPollCount = 0
    demoState.linking = true
    return fake({ linkJobId: 'demo-link-job', status: 'RUNNING', estimatedSeconds: 12 }, 400)
  }
  return client.post('/eco/link', payload)
}

/**
 * GET /eco/link/{linkJobId} — 연동 진행 폴링 (B-1-03).
 *
 * 진행 단계를 **여기서** 만든다. 뷰에 setTimeout 을 두면 그 타이머가 화면 코드로 남아
 * 연동 시점에 지울 곳을 놓친다. 뷰는 그리기만 하고 스토어는 폴링만 한다.
 */
export function getEcoLinkJob(linkJobId) {
  if (USE_FIXTURES) {
    return fake(() => {
      const step = ++demoState.linkPollCount
      const done = step > ECO_LINK_UTILITIES.length
      if (done) {
        demoState.linking = false
        demoState.linked = true
        return {
          linkJobId,
          status: 'SUCCEEDED',
          linkedAt: '2026-09-04T21:00:00+09:00',
          roundId: ECO_CURRENT_ROUND.roundId,
          registeredUtilities: ECO_LINK_UTILITIES.map((item) => item.utilityType),
          baselineMonthsLoaded: 24,
          ecoAddress: {
            label: '서울 성동구 왕십리로 000',
            sidoCode: '11',
            sigunguCode: '11200',
            registeredAt: '2026-03-15',
          },
          nextScreen: 'WF-03',
        }
      }
      return {
        linkJobId,
        status: 'RUNNING',
        elapsedSeconds: step * 3,
        utilityStatus: ECO_LINK_UTILITIES.map((item, index) => ({
          ...item,
          status: index < step - 1 ? 'SUCCEEDED' : index === step - 1 ? 'RUNNING' : 'PENDING',
        })),
      }
    }, 200)
  }
  return client.get(`/eco/link/${linkJobId}`)
}

/** GET /eco/rounds/current — 현재 회차·기준 사용량 (B-1-05 ~ B-1-07) */
export function getCurrentRound() {
  if (USE_FIXTURES) {
    return fake(() => ({
      ...ECO_CURRENT_ROUND,
      goalSet: demoState.savedGoal !== null,
      roundStatus: demoState.savedGoal ? 'GOAL_SET' : ECO_CURRENT_ROUND.roundStatus,
      nextScreen: demoState.savedGoal ? 'WF-06' : ECO_CURRENT_ROUND.nextScreen,
    }))
  }
  return client.get('/eco/rounds/current')
}

// ── 9절 · 목표·미션 ─────────────────────────────────────────────────────

/**
 * GET /eco/rounds/{roundId}/goal-form — 목표 정하기 폼 (B-2-01 · WF-04 · WF-05).
 *
 * 저장한 적이 있으면 `selectedTier` 와 미션 `selected` 에 그 값이 실려 온다.
 * 화면이 기억하는 게 아니라 **서버가 기억한다** — 새로고침해도 고른 것이 남아야 한다.
 *
 * `?preview=WF-05` 로 수도 미등록 상태를 열어볼 수 있다. **이 판정을 뷰에 두지 않는다** —
 * 화면이 쿼리로 데이터를 갈아끼우기 시작하면 연동 후 지울 곳이 뷰에 흩어진다.
 */
export function getGoalForm(roundId) {
  if (USE_FIXTURES) {
    return fake(() => applySavedGoal(previewGoalForm(), demoState.savedGoal))
  }
  return client.get(`/eco/rounds/${roundId}/goal-form`)
}

/** POST /eco/rounds/{roundId}/goal/preview — 예상 마일리지·절감액 (B-2-04 ~ B-2-07) */
export function previewGoal(roundId, payload) {
  // goal-form 과 **같은 판**을 넘겨야 한다. 기본값(전부 등록)을 쓰면 WF-05 에서
  // 미등록 요금이 합산에 남아 excludedUtilities 가 비고 기준 요금이 부풀어 오른다
  if (USE_FIXTURES) return fake(() => buildGoalPreview(payload, previewGoalForm()), 180)
  return client.post(`/eco/rounds/${roundId}/goal/preview`, payload)
}

/** POST /eco/rounds/{roundId}/goal — 목표 최초 저장 (B-2-08) */
export function createGoal(roundId, payload) {
  if (USE_FIXTURES) return fake(() => saveGoalFixture(payload), 400)
  return client.post(`/eco/rounds/${roundId}/goal`, payload)
}

/** PUT /eco/rounds/{roundId}/goal — 목표 수정. `goalSet` 이 true 면 이쪽이다 */
export function updateGoal(roundId, payload) {
  if (USE_FIXTURES) return fake(() => saveGoalFixture(payload), 400)
  return client.put(`/eco/rounds/${roundId}/goal`, payload)
}

/** GET /eco/rounds/{roundId}/goal — 저장된 목표 (B-4-06) */
export function getGoal(roundId) {
  if (USE_FIXTURES) return fake(ECO_GOAL)
  return client.get(`/eco/rounds/${roundId}/goal`)
}

/**
 * GET /eco/rounds/{roundId}/missions/today — 오늘의 실천 (B-3-05).
 * 목표를 정하지 않았으면 목록이 비고 `emptyReason` 이 온다. **에러가 아니다**(핵심 규칙 8).
 */
export function getTodayMissions(roundId, params = {}) {
  if (USE_FIXTURES) return fake(todayMissionsFixture)
  return client.get(`/eco/rounds/${roundId}/missions/today`, { params })
}

/**
 * PUT /eco/rounds/{roundId}/mission-logs/{date} — 실천 체크 (B-3-06).
 * 하루치를 **통째로** 덮어쓴다(`completedMissionIds` 전량). 토글 1건을 보내는 게 아니다.
 */
export function saveMissionLog(roundId, date, payload) {
  if (USE_FIXTURES) {
    return fake(() => {
      demoState.completedMissionIds = [...(payload?.completedMissionIds ?? [])]
      const today = todayMissionsFixture()
      return { date, completedCount: today.completedCount, totalCount: today.totalCount }
    }, 180)
  }
  return client.put(`/eco/rounds/${roundId}/mission-logs/${date}`, payload)
}

/**
 * GET /eco/rounds/{roundId}/mission-adjust — 실천 재선택 (B-3-08 · WF-08).
 * ⚠️ **쿼리는 `utility`, 응답 필드는 `utilityType` 이다.** 같은 요청/응답에서 이름이 다르다.
 */
export function getMissionAdjust(roundId, params = {}) {
  // 배치 3(WF-08)
  return client.get(`/eco/rounds/${roundId}/mission-adjust`, { params })
}

/** PUT /eco/rounds/{roundId}/missions — 실천 미션 갱신 (B-4-09) */
export function updateMissions(roundId, payload) {
  // 배치 3(WF-08)
  return client.put(`/eco/rounds/${roundId}/missions`, payload)
}

// ── 10절 · 진행 현황·전달 리포트 ────────────────────────────────────────

/**
 * GET /eco/home — What-if 홈. `screen` 으로 WF-01 ~ WF-09 를 가른다 (B-4-01).
 *
 * **`screen` 판정을 뷰에 두지 않는다.** 서버가 정하는 값이라 화면이 흉내 내기 시작하면
 * 연동 후 두 판정이 어긋난다. 여기서 데모 상태를 보고 서버처럼 답한다.
 */
export function getEcoHome() {
  if (USE_FIXTURES) {
    return fake(() => {
      if (demoState.linking) return { ...ECO_HOME_IN_PROGRESS, screen: 'WF_02_LINKING' }
      if (!demoState.linked) return { ...ECO_HOME_IN_PROGRESS, screen: 'WF_01_UNLINKED' }
      if (!demoState.savedGoal) return { ...ECO_HOME_IN_PROGRESS, screen: 'WF_03_NO_GOAL' }
      return {
        ...ECO_HOME_IN_PROGRESS,
        application: {
          ...ECO_HOME_IN_PROGRESS.application,
          applicationStatus: demoState.applied ? 'APPLIED' : 'NOT_APPLIED',
          showBanner: !demoState.applied,
        },
        todayMissions: {
          completedCount: todayMissionsFixture().completedCount,
          totalCount: ECO_TODAY_MISSIONS.totalCount,
        },
      }
    })
  }
  return client.get('/eco/home')
}

/** GET /eco/monthly-report — 전달 리포트 (B-4-02 · B-4-07 · WF-07) */
export function getMonthlyReport(params = {}) {
  // 배치 3(WF-07)
  return client.get('/eco/monthly-report', { params })
}

// ── 11절 · 평가 결과·마일리지 ───────────────────────────────────────────

/** GET /eco/rounds/{roundId}/result — 회차 평가 결과 (B-5-02 · WF-10) */
export function getRoundResult(roundId) {
  // 배치 4(WF-10)
  return client.get(`/eco/rounds/${roundId}/result`)
}

/**
 * POST /eco/rounds/{roundId}/result/view — 결산 모달을 봤다고 표시 (B-5-01 · WF-09).
 *
 * ⚠️ **204 라 응답 본문이 `undefined` 다.** `if (data)` 로 판정하면 성공을 실패로 읽는다.
 * 호출부는 try/catch 로만 판정한다. (백엔드 구현 완료 — EcoController.viewResult)
 */
export function markResultViewed(roundId) {
  return client.post(`/eco/rounds/${roundId}/result/view`)
}

/** GET /eco/rounds/{roundId}/settlement — 마일리지 적립 내역 (B-5-03 · WF-11) */
export function getSettlement(roundId) {
  // 배치 4(WF-11)
  return client.get(`/eco/rounds/${roundId}/settlement`)
}

/** POST /eco/rounds/{roundId}/application — 에코마일리지 회원 신청 (B-4-05) */
export function applyRound(roundId) {
  if (USE_FIXTURES) {
    return fake(() => {
      demoState.applied = true
      return { ...ECO_APPLICATION_APPLIED, roundId }
    }, 400)
  }
  return client.post(`/eco/rounds/${roundId}/application`)
}

// ── 픽스처 전용 헬퍼. USE_FIXTURES 를 끄면 아래는 아무도 부르지 않는다 ──

/**
 * 오늘 체크한 실천을 반영해 돌려준다. `completedCount` 를 화면이 세지 않고 **서버가 준 값**을
 * 쓰도록, 여기서 서버처럼 다시 세어 넣는다.
 */
function todayMissionsFixture() {
  const checked = demoState.completedMissionIds
  const missions = ECO_TODAY_MISSIONS.missions.map((mission) =>
    checked === null ? mission : { ...mission, completed: checked.includes(mission.missionId) },
  )
  return {
    ...ECO_TODAY_MISSIONS,
    missions,
    completedCount: missions.filter((mission) => mission.completed).length,
  }
}

/** WF-04(전부 등록) 이 기본이고 `?preview=WF-05` 일 때만 수도 미등록판을 준다 */
function previewGoalForm() {
  const search = typeof window === 'undefined' ? '' : window.location.search
  return new URLSearchParams(search).get('preview') === 'WF-05'
    ? ECO_GOAL_FORM_UNREGISTERED
    : ECO_GOAL_FORM
}

/** 저장한 목표를 goal-form 에 되돌려 넣는다 */
function applySavedGoal(goalForm, savedGoal) {
  if (!savedGoal) return goalForm
  const tierByUtility = Object.fromEntries(
    savedGoal.targets.map((target) => [target.utilityType, target.tier]),
  )
  const selectedIds = new Set(savedGoal.selectedMissionIds)
  return {
    ...goalForm,
    segments: goalForm.segments.map((segment) => ({
      ...segment,
      selectedTier: tierByUtility[segment.utilityType] ?? segment.selectedTier,
      missions: segment.missions.map((mission) => ({
        ...mission,
        selected: selectedIds.has(mission.missionId),
      })),
    })),
  }
}

/** POST·PUT /goal 공통. 저장값은 preview 와 같은 계산에서 뽑아야 화면과 어긋나지 않는다 */
function saveGoalFixture(payload) {
  demoState.savedGoal = {
    targets: payload?.targets ?? [],
    selectedMissionIds: payload?.selectedMissionIds ?? [],
  }
  const preview = buildGoalPreview(payload, previewGoalForm())
  return {
    ...ECO_GOAL_SAVED,
    combinedTargetRate: preview.combined.combinedRate,
    expectedMileage: preview.combined.expectedMileage,
    expectedSavingAmount: preview.combined.totalExpectedSaving,
    savedMissionCount: preview.missions.items.length,
  }
}
