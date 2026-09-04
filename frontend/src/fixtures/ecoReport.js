/*
 * 전달 리포트 · 실천 조정 미리보기 데이터 — WF-07 · WF-08. **API 연동 전까지만 쓴다.**
 *
 * 필드명·타입을 api-spec.md 10.3~10.5 와 백엔드 DTO 에 1:1 대조했다(2026-09-05 기준).
 *   buildMonthlyReport()  ← EcoMonthlyReportResponse  GET /eco/monthly-report
 *   buildMissionAdjust()  ← EcoMissionAdjustResponse  GET /eco/rounds/{roundId}/mission-adjust
 *   buildMissionUpdate()  ← EcoMissionUpdateResponse  PUT /eco/rounds/{roundId}/missions
 *
 * ── 왜 상수가 아니라 함수인가 ───────────────────────────────────────────
 * WF-08 은 미션을 체크하는 화면이다. 체크해도 합계가 안 변하면 볼 것이 없다.
 * `fixtures/ecoPreview.js` 와 같은 이유이고, 합계 계산도 그 파일의 `buildGoalPreview` 를
 * 그대로 재사용한다 — **기기군 중복 제외 규칙을 두 벌 만들지 않는다.**
 *
 * 리포트도 함수다. `prescription.selectedMissionRate` 는 WF-04 에서 실제로 고른 미션의
 * 합계라서, 상수로 굳히면 미션을 다 지워도 「41% 예상」이 남는다.
 *
 * ── ECO_HOME_IN_PROGRESS 와 맞물려 있다 ────────────────────────────────
 * 홈의 `latestReport.monthlyRate` 1.284 · `progress.cumulativeRate` 9.043 은
 * 아래 7월 사용량에서 나온 값이다. **하나만 고치면 같은 화면 안에서 숫자가 갈린다.**
 */

import { buildGoalPreview, ECO_CARBON_FACTORS } from './ecoPreview'
import { ECO_GOAL_FORM } from './ecoGoal'

/** 소수 3자리 반올림. 서버는 BigDecimal HALF_UP 이다 (ecoPreview.js 의 scale3 과 같다) */
const scale3 = (value) => Math.round(value * 1000 + 1e-6) / 1000

const REPORT_MONTH = '2026-07'
const TARGET_RATE = 11.322

/*
 * 7월 실사용량. 월 기준값(연 기준 ÷ 6개월)은 goal-form 의 `monthlyBaselineUsage` 와 같다.
 *   전기 223.333kWh · 도시가스 18.0㎥ · 수도 11.0㎥
 *
 * 전기만 기준보다 늘었다 — 이 달의 이야기가 「전기가 발목을 잡았다」이고,
 * WF-08 의 CTA(`adjustTargetUtility`)가 전기를 가리키는 근거다.
 */
const JULY_USAGE = [
  { utilityType: 'ELECTRICITY', baselineUsage: 223.333, actualUsage: 234.865, usageUnit: 'kWh' },
  { utilityType: 'GAS', baselineUsage: 18.0, actualUsage: 15.2, usageUnit: 'm3' },
  { utilityType: 'WATER', baselineUsage: 11.0, actualUsage: 9.8, usageUnit: 'm3' },
]

const FACTOR_BY_UTILITY = Object.fromEntries(
  ECO_CARBON_FACTORS.map((factor) => [factor.utilityType, factor.factorG]),
)

/** 요금 1종의 감축률. **음수는 증가다**(B-4-07) — GpDelta 가 이 부호 규약을 지킨다 */
const rateOf = (row) => scale3(((row.baselineUsage - row.actualUsage) / row.baselineUsage) * 100)

const baselineCarbonG = JULY_USAGE.reduce(
  (sum, row) => sum + row.baselineUsage * FACTOR_BY_UTILITY[row.utilityType],
  0,
)

/**
 * 「우리 집 온실가스의 68%가 전기예요」의 근거. `baselineUsage × 계수` 의 비중이다.
 * 시안의 83% 는 전기만 있는 가정의 값이라 맞지 않는다(결정 C-13) — 세 요금을 다 넣으면 68.289% 다.
 */
const CARBON_SHARE = Object.fromEntries(
  JULY_USAGE.map((row) => [
    row.utilityType,
    scale3((row.baselineUsage * FACTOR_BY_UTILITY[row.utilityType] * 100) / baselineCarbonG),
  ]),
)

const JULY_RATE = Object.fromEntries(JULY_USAGE.map((row) => [row.utilityType, rateOf(row)]))

/**
 * 그 달의 합산 감축률. 요금별 감축률의 평균이 아니라 **탄소 가중**이다 —
 * 전기가 −5.164% 인데도 합계가 +1.284% 인 이유가 여기 있다.
 * 홈의 `latestReport.monthlyRate` 와 같은 값이어야 한다.
 */
const JULY_COMBINED_RATE = scale3(
  ((baselineCarbonG -
    JULY_USAGE.reduce(
      (sum, row) => sum + row.actualUsage * FACTOR_BY_UTILITY[row.utilityType],
      0,
    )) /
    baselineCarbonG) *
    100,
)

/*
 * 달마다 얼마나 줄였나 (B-4-08 그래프). 6·7월이 목표에 못 미쳐 **2회 연속 미달**이고,
 * 그래서 WF-08 이 구간 하향을 제안한다(핵심 규칙 9 — 제안만 하고 바꾸지 않는다).
 *
 * 네 달 평균이 홈의 누적 9.043% 다. 달마다 기준 탄소가 같아 산술평균과 탄소가중이 일치한다.
 */
const MONTHLY_RATES = [
  { yearMonth: '2026-04', rate: 13.204 },
  { yearMonth: '2026-05', rate: 11.762 },
  { yearMonth: '2026-06', rate: 9.922 },
  { yearMonth: REPORT_MONTH, rate: JULY_COMBINED_RATE },
].map((row) => ({ ...row, achieved: row.rate >= TARGET_RATE }))

const REMAINING_MONTHS = 2

/**
 * 남은 달에 필요한 감축률 (api-spec.md 10.3 계산 규칙).
 *   requiredRate = (targetRate × 6 − Σ monthlyRate) ÷ remainingMonths
 * `remainingMonths === 0` 이면 서버가 null 을 준다 — **0 으로 나누지 않는다.**
 */
const REQUIRED_RATE = scale3(
  (TARGET_RATE * 6 - MONTHLY_RATES.reduce((sum, row) => sum + row.rate, 0)) / REMAINING_MONTHS,
)

/**
 * 요금 1종이 혼자 떠안아야 할 몫. 나머지 요금이 **7월과 같은 속도를 유지한다**는 가정이다.
 *   required = (전체 필요분 − Σ 나머지 비중 × 나머지 감축률) ÷ 이 요금의 비중
 * 전기 비중이 68% 라 전기 16.2% ≈ 전체 15.9% 가 된다. 비중을 빼고 단순 배분하면 틀린다.
 */
function requiredRateFor(utilityType) {
  const others = JULY_USAGE.filter((row) => row.utilityType !== utilityType).reduce(
    (sum, row) => sum + (CARBON_SHARE[row.utilityType] / 100) * JULY_RATE[row.utilityType],
    0,
  )
  return scale3((REQUIRED_RATE - others) / (CARBON_SHARE[utilityType] / 100))
}

/** '도시가스 15.6%, 수도 10.9% 감축을 지금처럼 유지할 때예요' — 가정을 숨기지 않는다(핵심 규칙 7) */
const UTILITY_LABEL = { ELECTRICITY: '전기', GAS: '도시가스', WATER: '수도' }

function assumptionFor(utilityType) {
  const others = JULY_USAGE.filter((row) => row.utilityType !== utilityType).map(
    (row) => `${UTILITY_LABEL[row.utilityType]} ${JULY_RATE[row.utilityType].toFixed(1)}%`,
  )
  return `${others.join(', ')} 감축을 지금처럼 유지할 때예요`
}

/**
 * 그 요금에 건 목표 감축률. goal-form 의 `selectedTier` 를 `tiers[]` 로 되짚는다.
 * 구간을 안 골랐거나 미등록이면 목표가 없다 — 0 으로 두어 「달성」으로 본다.
 */
function targetRateOf(utilityType, goalForm) {
  const segment = goalForm.segments.find((item) => item.utilityType === utilityType)
  return goalForm.tiers.find((tier) => tier.tier === segment?.selectedTier)?.targetRate ?? 0
}

/** goal-form 의 `selected` 플래그에서 고른 미션 id 를 모은다. 미등록 요금은 선택이 없다 */
function selectedMissionIds(goalForm, utilityType = null) {
  return goalForm.segments
    .filter((segment) => utilityType === null || segment.utilityType === utilityType)
    .flatMap((segment) => segment.missions ?? [])
    .filter((mission) => mission.selected)
    .map((mission) => mission.missionId)
}

/** 고른 미션의 합계 감축률. 기기군 중복 제외는 buildGoalPreview 가 이미 한다 */
function combinedMissionRate(ids, goalForm) {
  return buildGoalPreview({ targets: [], selectedMissionIds: ids }, goalForm).missions
    .combinedMissionRate
}

/**
 * GET /eco/monthly-report 응답을 만든다.
 *
 * @param {object} goalForm  GET goal-form 응답. 고른 미션 합계(`selectedMissionRate`)가 여기서 나온다
 */
export function buildMonthlyReport(goalForm = ECO_GOAL_FORM) {
  const selectedRate = combinedMissionRate(selectedMissionIds(goalForm), goalForm)
  const byUtility = JULY_USAGE.map((row) => {
    /*
     * ⚠️ 요금별 `achieved` 는 **그 요금에 건 목표**(goal-form 의 selectedTier)로 판정한다.
     * 합산 목표(11.322%)로 재면 수도가 10.909% 라 미달이 되는데, 수도에 건 목표는 5% 라
     * 실제로는 달성이다. 합산과 요금별 기준을 섞지 않는다.
     */
    const rate = JULY_RATE[row.utilityType]
    const achieved = rate >= targetRateOf(row.utilityType, goalForm)
    return {
      ...row,
      rate,
      achieved,
      carbonSharePercent: CARBON_SHARE[row.utilityType],
      // 미달 항목은 펼치고 달성 항목은 접는다. **서버가 정해서 내려준다**(B-4-07 ②)
      expanded: !achieved,
    }
  })

  return {
    reportMonth: REPORT_MONTH,
    roundId: 7,
    billRegisteredAt: '2026-08-03T10:24:00+09:00',
    baselineDescription: '2024·2025년 7월 평균',
    result: {
      monthlyRate: MONTHLY_RATES[MONTHLY_RATES.length - 1].rate,
      targetRate: TARGET_RATE,
      achieved: false,
      cumulativeRate: 9.043,
      cumulativeMonths: MONTHLY_RATES.map((row) => row.yearMonth),
    },
    cause: {
      byUtility,
      largestCarbonUtility: 'ELECTRICITY',
      carbonFactors: ECO_CARBON_FACTORS,
    },
    prescription: {
      remainingMonths: REMAINING_MONTHS,
      remainingMonthLabels: [8, 9],
      requiredRate: REQUIRED_RATE,
      // 고른 실천만으로 남은 몫을 덮을 수 있으면 '하면 돼요', 아니면 가능성 문구다(B-4-08)
      achievable: selectedRate >= REQUIRED_RATE,
      requiredByUtility: [
        {
          utilityType: 'ELECTRICITY',
          requiredRate: requiredRateFor('ELECTRICITY'),
          assumption: assumptionFor('ELECTRICITY'),
        },
      ],
      selectedMissionRate: selectedRate,
      adjustTargetUtility: 'ELECTRICITY',
    },
    monthlyRates: MONTHLY_RATES,
    emptyReason: null,
  }
}

/*
 * 그 달 고지서를 아직 올리지 않은 상태. **404 가 아니라 200 이다**(핵심 규칙 8).
 * `result` 가 null 이라 화면은 숫자를 그리지 않고 안내와 등록 CTA 만 띄운다.
 */
export const ECO_MONTHLY_REPORT_EMPTY = {
  reportMonth: null,
  roundId: 7,
  billRegisteredAt: null,
  baselineDescription: null,
  result: null,
  cause: null,
  prescription: null,
  monthlyRates: [],
  emptyReason: 'NO_BILL',
}

/**
 * 추천 판정 (B-4-09).
 * 「부족분을 메울 수 있고 **이미 고른 미션과 기기군이 겹치지 않는** 것」이다.
 * 같은 기기군에서는 합계에 하나만 들어가므로 **후보 중에서도 가장 큰 것 하나만** 추천한다 —
 * 세탁 미션 둘을 나란히 추천하면 합계가 늘지 않는데 늘 것처럼 보인다.
 */
function recommendedIds(missions) {
  const takenGroups = new Set(
    missions.filter((mission) => mission.selected).map((mission) => mission.deviceGroup),
  )
  const bestByGroup = new Map()
  for (const mission of missions) {
    if (mission.selected || takenGroups.has(mission.deviceGroup)) continue
    if (!mission.computedRate) continue
    const kept = bestByGroup.get(mission.deviceGroup)
    if (!kept || kept.computedRate < mission.computedRate) bestByGroup.set(mission.deviceGroup, mission)
  }
  return new Set([...bestByGroup.values()].map((mission) => mission.missionId))
}

/**
 * GET /eco/rounds/{roundId}/mission-adjust 응답을 만든다.
 *
 * ⚠️ **쿼리는 `utility`, 응답 필드는 `utilityType` 이다.** 같은 요청/응답에서 이름이 다르다.
 *
 * @param {string} utilityType  'ELECTRICITY' | 'GAS' | 'WATER'
 * @param {object} goalForm     GET goal-form 응답. 미션 목록과 현재 선택이 여기서 나온다
 */
export function buildMissionAdjust(utilityType, goalForm = ECO_GOAL_FORM) {
  const segment = goalForm.segments.find((item) => item.utilityType === utilityType)
  if (!segment) return null

  const recommended = recommendedIds(segment.missions)
  const missions = segment.missions.map((mission) => ({
    missionId: mission.missionId,
    title: mission.title,
    description: mission.description,
    computedRate: mission.computedRate,
    difficulty: mission.difficulty,
    deviceGroup: mission.deviceGroup,
    evidenceText: mission.evidenceText,
    calculationBasis: mission.calculationBasis,
    sourceOrg: mission.sourceOrg,
    selected: mission.selected,
    recommended: recommended.has(mission.missionId),
    capped: mission.capped,
  }))

  const selectedIds = missions.filter((mission) => mission.selected).map((m) => m.missionId)
  const currentRate = combinedMissionRate(selectedIds, goalForm)
  const withRecommendedRate = combinedMissionRate([...selectedIds, ...recommended], goalForm)
  const requiredRate = requiredRateFor(utilityType)

  return {
    roundId: goalForm.roundId,
    utilityType,
    reportMonth: REPORT_MONTH,
    requiredRate,
    requiredAssumption: assumptionFor(utilityType),
    carbonSharePercent: CARBON_SHARE[utilityType],
    // 고를 때 기대했던 감축률과 실제로 나온 감축률. 둘의 간격이 이 화면의 출발점이다
    comparison: { selectedExpectedRate: currentRate, actualRate: JULY_RATE[utilityType] },
    currentSelectedCount: selectedIds.length,
    missions,
    preview: {
      currentRate,
      withRecommendedRate,
      coversRequired: withRecommendedRate >= requiredRate,
    },
    // **2회 연속 미달일 때만 제안한다.** 자동으로 구간을 낮추지 않는다(핵심 규칙 9)
    tierDowngrade: buildTierDowngrade(),
  }
}

function buildTierDowngrade() {
  let consecutiveMisses = 0
  for (let i = MONTHLY_RATES.length - 1; i >= 0; i -= 1) {
    if (MONTHLY_RATES[i].achieved) break
    consecutiveMisses += 1
  }
  const suggest = consecutiveMisses >= 2
  return {
    suggest,
    consecutiveMisses,
    message: suggest
      ? `${consecutiveMisses}달 연속 목표에 못 미쳤어요. 5~10% 구간으로 낮추는 것도 방법이에요`
      : '한 달 미끄러진 것만으로 10~15% 구간을 포기하기엔 일러요',
  }
}

/**
 * PUT /eco/rounds/{roundId}/missions 응답을 만든다.
 *
 * 목표 구간(`targetTier`)은 건드리지 않는다. **미션만 교체한다**(api-spec.md 10.5).
 * 합계와 제외 사유는 goal/preview 와 같은 계산에서 나온다 — 두 화면이 다른 숫자를 보이면 안 된다.
 */
export function buildMissionUpdate(selectedIds, goalForm = ECO_GOAL_FORM) {
  const { combinedMissionRate: rate, items } = buildGoalPreview(
    { targets: [], selectedMissionIds: selectedIds },
    goalForm,
  ).missions

  return {
    roundId: goalForm.roundId,
    combinedMissionRate: rate,
    items,
    // 오늘의 실천 목록이 바뀐다 → WF-06 으로 돌아가면 다시 받아야 한다
    todayMissionsUpdated: true,
  }
}
