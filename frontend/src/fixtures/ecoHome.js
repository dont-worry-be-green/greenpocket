/*
 * What-if 홈 미리보기 데이터 — **API 연동 전까지만 쓴다.**
 *
 * 필드명·타입을 백엔드 DTO 와 1:1 대조했다(2026-09-05 기준).
 *   ECO_HOME_IN_PROGRESS ← EcoHomeResponse           GET /eco/home
 *   ECO_TODAY_MISSIONS   ← EcoTodayMissionsResponse   GET /eco/rounds/{roundId}/missions/today
 *   ECO_APPLICATION      ← EcoApplicationResponse     POST /eco/rounds/{roundId}/application
 *
 * ⚠️ **`home.goal` 에는 요금별 내역이 없다.** `EcoHomeResponse.Goal` 은
 * `goalSet · combinedTargetRate · tier · expectedMileage` 네 개뿐이라
 * 시안의 「전기 10% · 도시가스 15% · 수도 5%」 3열은 여기서 나오지 않는다.
 * 그건 `GET /eco/rounds/{roundId}/goal`(`ECO_GOAL.utilities[]`) 쪽이라 WF-06 이 두 번 부른다.
 *
 * `ECO_RESULT_MODAL` 은 `ecoResult.js` 에서 가져온다. 픽스처끼리의 상대 경로 import 라
 * `api/eco.js` 하나만 `@/fixtures` 를 쓴다는 불변식을 깨지 않는다.
 *
 * ── 시안과 다른 값 ──────────────────────────────────────────────────────
 * 시안의 합산 목표 `10.5%` 는 계산과 맞지 않는다(결정 C-13). `ECO_GOAL` 과 같은
 * **11.322%** 를 쓴다. 두 응답이 어긋나면 같은 화면 안에서 숫자가 갈린다.
 */

import { ECO_RESULT_MODAL } from './ecoResult'

/*
 * 평가 기간 진행 (B-4-01).
 *
 * 누적 9.043% 는 5~10% 구간이라 지금 확정하면 10,000M 이다. 목표는 11.322% → TIER_10.
 * 그래서 사다리가 `CURRENT` = TIER_5, `TARGET` = TIER_10 이고
 * 다음 구간까지 `10.000 - 9.043 = 0.957`%p 다. **이 셋은 서로 맞물려 있으니 하나만 고치지 않는다.**
 *
 * `state` 는 서버가 준 문자열을 그대로 쓴다(EcoProgressService). currentTier·targetTier 를
 * 비교해 화면이 직접 판정하면 둘이 같은 회차에서 어긋난다.
 */
const PROGRESS = {
  cumulativeRate: 9.043,
  coveredMonths: ['2026-04', '2026-05', '2026-06', '2026-07'],
  currentTier: 'TIER_5',
  targetTier: 'TIER_10',
  tiers: [
    { tier: 'TIER_5', mileage: 10000, state: 'CURRENT' },
    { tier: 'TIER_10', mileage: 30000, state: 'TARGET' },
    { tier: 'TIER_15', mileage: 50000, state: 'NONE' },
  ],
  gapToNextTierPoint: 0.957,
  nextTierMileage: 30000,
}

/*
 * 전달 리포트 요약 (B-4-02).
 * 7월 한 달만 보면 1.284% 라 목표(11.322%)에 못 미친다 — `achieved: false`.
 * 누적(9.043%)과 다른 값이다. **월 단위와 누적을 섞지 않는다.**
 */
const LATEST_REPORT = {
  available: true,
  reportMonth: '2026-07',
  billRegisteredAt: '2026-08-03T10:24:00+09:00',
  monthlyRate: 1.284,
  targetRate: 11.322,
  achieved: false,
}

/** 아직 고지서를 올리지 않은 달. **에러가 아니라 안내다**(핵심 규칙 8) */
export const ECO_LATEST_REPORT_UNAVAILABLE = {
  available: false,
  reportMonth: null,
  billRegisteredAt: null,
  monthlyRate: null,
  targetRate: null,
  achieved: null,
}

/** GET /eco/home — WF-06 목표 설정 후 진행 중 */
export const ECO_HOME_IN_PROGRESS = {
  screen: 'WF_06_IN_PROGRESS',
  roundId: 7,
  header: {
    periodStart: '2026-04',
    periodEnd: '2026-09',
    remainingMonths: 2,
    remainingLabelMonths: [8, 9],
  },
  progress: PROGRESS,
  latestReport: LATEST_REPORT,
  application: {
    applicationStatus: 'NOT_APPLIED',
    showBanner: true,
    externalUrl: 'https://ecomileage.seoul.go.kr',
  },
  goal: {
    goalSet: true,
    combinedTargetRate: 11.322,
    tier: 'TIER_10',
    expectedMileage: 30000,
  },
  todayMissions: { completedCount: 3, totalCount: 5 },
  resultModal: null,
  links: { benefitTab: true, pocketTab: true, movingNotice: true },
}

/*
 * GET /eco/home — WF-09 결산 알림.
 *
 * ⚠️ **모달의 회차는 진행 중인 회차가 아니다.** `WF_09_RESULT_READY` 는 *직전* 회차가
 * CONFIRMED 인 상태라(api-spec.md 10.1), 홈은 7 을 진행 중으로 보여 주면서 모달만 6 을 알린다.
 * 둘을 같은 번호로 두면 한 회차가 진행 중이면서 확정된 것이 된다.
 *
 * 숫자를 여기 다시 적지 않고 `ecoResult.js` 에서 끌어온다 — 따로 적으면 모달과 WF-10 이
 * 다른 감축률을 보인다.
 */
export const ECO_HOME_RESULT_READY = {
  ...ECO_HOME_IN_PROGRESS,
  screen: 'WF_09_RESULT_READY',
  resultModal: ECO_RESULT_MODAL,
}

/*
 * GET /eco/rounds/{roundId}/missions/today — 오늘의 실천 (B-3-05).
 *
 * 목표를 정할 때 고른 미션 중 **오늘 계절에 맞는 것만** 내려온다(`season`).
 * 그래서 `ECO_GOAL.missions` 보다 목록이 짧을 수 있다.
 *
 * ⚠️ 필드가 다섯 개뿐이다 — `computedRate`·`evidenceText`·`calculationBasis` 가 없다.
 * `GpMissionRow` 는 그 셋을 전제하므로 **여기에 재사용하지 않는다.**
 */
export const ECO_TODAY_MISSIONS = {
  date: '2026-08-14',
  season: 'SUMMER',
  completedCount: 3,
  totalCount: 5,
  missions: [
    {
      missionId: 12,
      title: '냉방 온도 26℃로 맞추기',
      utilityType: 'ELECTRICITY',
      difficulty: 'EASY',
      completed: true,
    },
    {
      missionId: 13,
      title: '에어컨 하루 1시간 줄이기',
      utilityType: 'ELECTRICITY',
      difficulty: 'NORMAL',
      completed: true,
    },
    {
      missionId: 31,
      title: '온수 온도 55℃ → 40℃로 낮추기',
      utilityType: 'GAS',
      difficulty: 'EASY',
      completed: true,
    },
    {
      missionId: 21,
      title: '양치할 때 컵 사용하기',
      utilityType: 'WATER',
      difficulty: 'EASY',
      completed: false,
    },
    {
      missionId: 24,
      title: '설거지통에 물 받아서 하기',
      utilityType: 'WATER',
      difficulty: 'EASY',
      completed: false,
    },
  ],
  emptyReason: null,
}

/*
 * 목표를 정하지 않아 고른 미션이 없는 날. 200 정상 응답이다(핵심 규칙 8).
 * `emptyReason` 은 서버가 문장이 아니라 코드로 준다 — 문구는 화면이 만든다.
 */
export const ECO_TODAY_MISSIONS_EMPTY = {
  date: '2026-08-14',
  season: 'SUMMER',
  completedCount: 0,
  totalCount: 0,
  missions: [],
  emptyReason: 'NO_GOAL',
}

/** POST /eco/rounds/{roundId}/application — 참여 신청 (B-4-05) */
export const ECO_APPLICATION_APPLIED = {
  roundId: 7,
  applicationStatus: 'APPLIED',
  appliedAt: '2026-09-05T10:00:00+09:00',
  showBanner: false,
}
