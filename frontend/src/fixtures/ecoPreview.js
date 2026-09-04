/*
 * 목표 미리보기 계산 — POST /eco/rounds/{roundId}/goal/preview 의 대역. **API 연동 전까지만 쓴다.**
 *
 * 여기만 상수가 아니라 **순수 함수**다. 구간 칩을 눌러도 숫자가 안 변하면 WF-04 는 볼 것이 없다.
 *
 * 계산은 기능명세서 `## Green What-if 계산식` 과 백엔드 `EcoGoalService.calculate()` 를
 * 그대로 옮긴 것이다. 이 파일이 **서버 공식과 어긋날 수 있는 유일한 지점**이라
 * 데모 케이스(1,340kWh · 108㎥ · 66㎥ → 11.322%)를 `__tests__/ecoPreview.spec.js` 로 못 박아 둔다.
 * 공식을 고치면 그 테스트부터 확인한다.
 *
 * 응답 모양은 EcoGoalPreviewResponse 와 1:1 이다.
 *   utilities[]  UtilityTarget   · combined Combined(+NextTier)
 *   missions     MissionSummary  · carbonFactors[] CarbonFactor
 */

import { ECO_GOAL_FORM } from './ecoGoal'

/**
 * 탄소 환산계수. AGENTS.md 「고정 상수」이자 DB `eco_round_utility.carbon_factor_g` 값이다.
 * **임의로 바꾸지 않는다.** 순서는 서버 응답 순서(ELECTRICITY · WATER · GAS)를 따른다
 * (EcoGoalService.carbonFactors). 요금 종류 순서(ELECTRICITY · GAS · WATER)와 다르다.
 */
export const ECO_CARBON_FACTORS = [
  { utilityType: 'ELECTRICITY', factorG: 424.0, unit: 'kWh' },
  { utilityType: 'WATER', factorG: 332.0, unit: 'm3' },
  { utilityType: 'GAS', factorG: 2240.0, unit: 'm3' },
]

const FACTOR_BY_UTILITY = Object.fromEntries(
  ECO_CARBON_FACTORS.map((factor) => [factor.utilityType, factor.factorG]),
)

/** 탄소 계산 순회 순서. 서버의 UtilityType.values() 와 같다 */
const UTILITY_TYPES = ['ELECTRICITY', 'GAS', 'WATER']

/**
 * 소수 3자리 반올림. 서버는 BigDecimal HALF_UP 이다 (EcoGoalService.scale).
 *
 * JS 는 이진 부동소수라 `1340 * 0.9` 가 1206.0000000000002 로, `66 * 0.95` 가
 * 62.699999999999996 으로 나온다. 1e-6 만큼 밀어 경계값이 아래로 내려앉는 것만 막는다.
 * 다루는 값이 전부 양수라 Math.round 로 HALF_UP 이 된다.
 */
const scale3 = (value) => Math.round(value * 1000 + 1e-6) / 1000

/** 금액·감축률 정수 반올림. 서버의 setScale(0, HALF_UP) */
const round0 = (value) => Math.round(value + 1e-6)

/**
 * 합산 감축률이 어느 구간에 드는지. 경계값은 **상위 구간**으로 간다
 * (핵심 비즈니스 규칙 · EcoGoalService.tierForRate). 5% 미만이면 구간이 없다 → null.
 */
function tierForRate(rate, tiers) {
  const sorted = [...tiers].sort((a, b) => b.targetRate - a.targetRate)
  return sorted.find((tier) => rate >= tier.targetRate) ?? null
}

/**
 * 다음 구간까지 남은 거리. 아직 못 넘은 구간 중 가장 낮은 것이다.
 * `gapPoint` 는 증감이 아니라 **퍼센트포인트**다 — GpDelta 에 넘기지 않는다(formatPoint 를 쓴다).
 */
function nextTier(rate, tiers) {
  const ahead = [...tiers]
    .sort((a, b) => a.targetRate - b.targetRate)
    .find((tier) => rate < tier.targetRate)
  if (!ahead) return null
  return {
    tier: ahead.tier,
    gapPoint: scale3(ahead.targetRate - rate),
    mileage: ahead.mileage,
  }
}

/** 요금 1종의 목표 사용량·예상 절감액 (EcoGoalService.calculateUtilityTarget) */
function calculateUtilityTarget(segment, tierOption) {
  const rateRatio = tierOption.targetRate / 100
  return {
    utilityType: segment.utilityType,
    targetRate: tierOption.targetRate,
    baselineUsage: segment.baselineUsage,
    targetUsage: scale3(segment.baselineUsage * (1 - rateRatio)),
    usageUnit: segment.usageUnit,
    baselineAmount: segment.baselineAmount,
    expectedSaving: segment.baselineAmount === null ? 0 : round0(segment.baselineAmount * rateRatio),
    // 서버는 preview 에서만 이 값을 준다. goal-form·리포트·결과에는 없다
    displayPrecision: segment.utilityType === 'ELECTRICITY' ? 0 : 1,
  }
}

/**
 * 미션 1건의 감축률 (EcoGoalService.rawMissionRate + applyMissionCap).
 *   raw = evidenceAmount ÷ (baselineUsage ÷ 6) × 100 → 상한 → **정수 반올림**
 * 기준 사용량이 없으면(미등록) null 이다. 0 이 아니다 — 합계에서 아예 빠진다.
 */
function computedMissionRate(mission, segment) {
  if (!segment || segment.baselineUsage === null || segment.baselineUsage === 0) return null
  const monthly = scale3(segment.baselineUsage / 6)
  const raw = (mission.evidenceAmount / monthly) * 100
  const cap = segment.missionRateCap
  return round0(cap === null || cap === undefined ? raw : Math.min(raw, cap))
}

/**
 * 고른 미션의 합계 처리. **같은 기기군(deviceGroup)에서는 가장 큰 것 하나만 센다** —
 * 냉방 미션 3개를 다 고른다고 냉방 전력이 3배로 줄지는 않기 때문이다.
 * 같은 값이면 먼저 고른 쪽이 남는다(서버 merge 규칙과 같다).
 */
function calculateMissions(selectedMissionIds, segments) {
  if (!selectedMissionIds?.length) return []

  const segmentByMissionId = new Map()
  const missionById = new Map()
  for (const segment of segments) {
    for (const mission of segment.missions ?? []) {
      missionById.set(mission.missionId, mission)
      segmentByMissionId.set(mission.missionId, segment)
    }
  }

  // 서버는 모르는 missionId 에 400 을 준다. 픽스처는 화면을 막지 않도록 걸러내기만 한다
  const distinctIds = [...new Set(selectedMissionIds)].filter((id) => missionById.has(id))
  const initial = distinctIds.map((id) => {
    const mission = missionById.get(id)
    return {
      mission,
      computedRate: computedMissionRate(mission, segmentByMissionId.get(id)),
    }
  })

  const largestByGroup = new Map()
  for (const item of initial) {
    if (item.computedRate === null) continue
    const kept = largestByGroup.get(item.mission.deviceGroup)
    if (!kept || kept.computedRate < item.computedRate) {
      largestByGroup.set(item.mission.deviceGroup, item)
    }
  }

  return initial.map((item) => {
    const largest = largestByGroup.get(item.mission.deviceGroup)
    const counted = largest?.mission.missionId === item.mission.missionId
    return {
      missionId: item.mission.missionId,
      computedRate: item.computedRate,
      counted,
      exclusionReason:
        item.computedRate !== null && !counted
          ? `${item.mission.deviceGroup} 겹침 · 합계 제외`
          : null,
    }
  })
}

/**
 * POST /eco/rounds/{roundId}/goal/preview 응답을 만든다.
 *
 * @param {{targets: {utilityType: string, tier: string}[], selectedMissionIds: number[]}} request
 * @param {object} goalForm  GET goal-form 응답. 기준 사용량·미션·구간 라벨이 전부 여기서 나온다
 *
 * 합산 감축률은 요금별 감축률의 평균이 아니라 **탄소 가중**이다.
 *   R = (기준 탄소 − 목표 탄소) ÷ 기준 탄소 × 100
 * 전기 탄소 비중이 82.5% 라 전기 구간을 올리는 쪽이 합산에 훨씬 크게 먹힌다.
 */
export function buildGoalPreview(request, goalForm = ECO_GOAL_FORM) {
  const tiers = goalForm.tiers
  const tierByCode = Object.fromEntries(tiers.map((tier) => [tier.tier, tier]))
  const segmentByType = Object.fromEntries(
    goalForm.segments.map((segment) => [segment.utilityType, segment]),
  )

  // 서버는 미등록 요금에 목표를 걸면 409 ECO_UTILITY_NOT_REGISTERED 다.
  // 화면이 그런 요청을 보내지 않는 게 맞고, 여기서는 조용히 걸러 데모를 막지 않는다
  const targets = (request?.targets ?? []).filter(
    (target) => segmentByType[target.utilityType]?.registered && tierByCode[target.tier],
  )

  const utilities = targets.map((target) =>
    calculateUtilityTarget(segmentByType[target.utilityType], tierByCode[target.tier]),
  )
  const targetByType = Object.fromEntries(utilities.map((item) => [item.utilityType, item]))

  let baselineCarbonG = 0
  let targetCarbonG = 0
  let baselineTotalAmount = 0
  const excludedUtilities = []
  for (const utilityType of UTILITY_TYPES) {
    const segment = segmentByType[utilityType]
    if (!segment?.registered) {
      excludedUtilities.push(utilityType)
      continue
    }
    const factorG = FACTOR_BY_UTILITY[utilityType]
    baselineCarbonG += segment.baselineUsage * factorG
    // 구간을 안 고른 요금은 기준 사용량 그대로 — 안 줄인 것으로 본다
    targetCarbonG += (targetByType[utilityType]?.targetUsage ?? segment.baselineUsage) * factorG
    baselineTotalAmount += segment.baselineAmount ?? 0
  }
  baselineCarbonG = scale3(baselineCarbonG)
  targetCarbonG = scale3(targetCarbonG)

  const combinedRate =
    baselineCarbonG === 0
      ? 0
      : scale3(((baselineCarbonG - targetCarbonG) / baselineCarbonG) * 100)
  const combinedTier = tierForRate(combinedRate, tiers)
  const totalExpectedSaving = utilities.reduce((sum, item) => sum + item.expectedSaving, 0)

  const missionItems = calculateMissions(request?.selectedMissionIds, goalForm.segments)
  const combinedMissionRate = scale3(
    missionItems
      .filter((item) => item.counted && item.computedRate !== null)
      .reduce((sum, item) => sum + item.computedRate, 0),
  )

  return {
    utilities,
    combined: {
      baselineCarbonG,
      targetCarbonG,
      combinedRate,
      tier: combinedTier?.tier ?? null,
      tierLabel: combinedTier?.label ?? null,
      expectedMileage: combinedTier?.mileage ?? 0,
      totalExpectedSaving,
      baselineTotalAmount,
      excludedUtilities,
      nextTier: nextTier(combinedRate, tiers),
    },
    missions: {
      combinedMissionRate,
      // 목표에서 모자란 만큼. 증감이 아니라 퍼센트포인트다
      shortfallPoint: scale3(Math.max(0, combinedRate - combinedMissionRate)),
      meetsTarget: combinedMissionRate >= combinedRate,
      items: missionItems,
    },
    carbonFactors: ECO_CARBON_FACTORS.filter((factor) => segmentByType[factor.utilityType]),
  }
}
