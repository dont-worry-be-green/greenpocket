/*
 * 평가 결과·마일리지 적립 픽스처 — api-spec.md 11.1 · 11.2. 화면 WF-09 · WF-10 · WF-11.
 *
 * ── 회차 번호를 6 으로 둔 이유 ──────────────────────────────────────────────
 * `WF_09_RESULT_READY` 는 **직전 회차가 CONFIRMED** 인 상태다(api-spec.md 10.1 화면 표).
 * 진행 중인 회차는 7(`ECO_HOME_IN_PROGRESS`, 남은 달 2개)이라 **7 을 확정 회차로 두면
 * 같은 회차가 진행 중이면서 확정된 것이 된다.** 그래서 확정은 직전 회차 6 이고,
 * `nextRound` 가 진행 중인 7 을 가리킨다 — 홈이 보여 주는 회차와 정확히 이어진다.
 *
 * ⚠️ **WF-10·WF-11 의 `roundId` 는 `store.roundId`(현재 회차 7)가 아니다.**
 * 뷰는 `route.params.roundId` 를 쓴다.
 *
 * ── 숫자는 기능명세서 고정 상수다 ──────────────────────────────────────────
 * 전기 1,340kWh · 도시가스 108㎥ · 수도 66㎥ → 최종 **12.499%** · **30,000M**,
 * 기준 420,600원 = 268,000 + 96,600 + 56,000.
 * **시안의 `12%` 는 틀린 값이다**(결정 C-13). 기간만 직전 회차로 옮겼고 수치는 고정값 그대로다.
 *
 * 감축률을 손으로 적지 않고 사용량에서 탄소 가중으로 유도한다. 상수로 두면 사용량만 고쳤을 때
 * 요금별 값과 합산이 조용히 갈린다. (`__tests__/ecoResult.spec.js` 가 12.499 를 못 박는다)
 */

import { ECO_CARBON_FACTORS } from './ecoPreview'

const scale3 = (value) => Math.round(value * 1000 + 1e-6) / 1000

const FACTOR_BY_UTILITY = Object.fromEntries(
  ECO_CARBON_FACTORS.map((factor) => [factor.utilityType, factor.factorG]),
)

const ROUND_ID = 6
const PERIOD_START = '2025-10'
const PERIOD_END = '2026-03'

/** 회차 목표. 합산 12.499% 가 10~15% 구간에 들어가 30,000M 이 확정된다 */
const ROUND_TARGET_RATE = 10.0

/*
 * 요금별 결과. `targetRate` 는 그 회차에 요금별로 걸었던 구간의 하한이다.
 * ⚠️ **달성 판정을 합산 목표로 하면 틀린다** — 도시가스는 12.002% 를 줄였지만
 * 15% 구간을 걸었으므로 미달이다. 요금별로 각자의 `targetRate` 와 비교한다.
 */
const UTILITY_USAGE = [
  {
    utilityType: 'ELECTRICITY',
    baselineUsage: 1340.0,
    actualUsage: 1165.8,
    usageUnit: 'kWh',
    targetRate: 10.0,
  },
  {
    utilityType: 'GAS',
    baselineUsage: 108.0,
    actualUsage: 95.038,
    usageUnit: 'm3',
    targetRate: 15.0,
  },
  {
    utilityType: 'WATER',
    baselineUsage: 66.0,
    actualUsage: 62.7,
    usageUnit: 'm3',
    targetRate: 5.0,
  },
]

const carbonOf = (usage, utilityType) => usage * FACTOR_BY_UTILITY[utilityType]
const sumCarbon = (key) =>
  UTILITY_USAGE.reduce((sum, row) => sum + carbonOf(row[key], row.utilityType), 0)

const BASELINE_CARBON_G = sumCarbon('baselineUsage')
const ACTUAL_CARBON_G = sumCarbon('actualUsage')

/** 합산 최종 감축률. 요금별 감축률의 평균이 아니라 **탄소 총량 비교**다 */
const FINAL_RATE = scale3(((BASELINE_CARBON_G - ACTUAL_CARBON_G) / BASELINE_CARBON_G) * 100)

const UTILITY_RESULTS = UTILITY_USAGE.map((row) => {
  const finalRate = scale3(((row.baselineUsage - row.actualUsage) / row.baselineUsage) * 100)
  return {
    utilityType: row.utilityType,
    baselineUsage: row.baselineUsage,
    actualUsage: row.actualUsage,
    usageUnit: row.usageUnit,
    finalRate,
    targetRate: row.targetRate,
    achieved: finalRate >= row.targetRate,
  }
})

/*
 * 달마다의 페이스. `EcoMonthlyRateChart` 가 WF-07 과 **같은 컴포넌트**로 그린다.
 * 합산(12.499%)과 이 값들의 평균이 딱 맞지는 않는다 — 달마다 기준 사용량이 달라
 * 분모가 다르기 때문이고, 서버도 같은 이유로 따로 계산한다.
 */
const MONTHLY_RATES = [
  { yearMonth: '2025-10', rate: 8.0 },
  { yearMonth: '2025-11', rate: 10.5 },
  { yearMonth: '2025-12', rate: 12.0 },
  { yearMonth: '2026-01', rate: 13.2 },
  { yearMonth: '2026-02', rate: 14.1 },
  { yearMonth: '2026-03', rate: 17.0 },
].map((row) => ({ ...row, achieved: row.rate >= ROUND_TARGET_RATE }))

const BASELINE_AMOUNT = 420600
const ACTUAL_AMOUNT = 370100
const CONFIRMED_MILEAGE = 30000
const CONFIRMED_AT = '2026-06-05T00:00:00+09:00'

/**
 * GET /eco/rounds/{roundId}/result — 회차 평가 결과 (B-5-02 · WF-10).
 *
 * ⚠️ `savedIsPocketEligible: false` — 「덜 낸 요금」은 **성과 표시 전용**이다.
 * 포켓 잔액에 더하지 않고 옆에 전환·출금 버튼을 두지 않는다(핵심 규칙 3).
 */
export const ECO_RESULT = {
  roundId: ROUND_ID,
  periodStart: PERIOD_START,
  periodEnd: PERIOD_END,
  confirmedAt: CONFIRMED_AT,
  // 시차 규칙(핵심 규칙 10) — 최종 확정은 고지서가 아니라 누리집 기준이다
  confirmedSource: '에코마일리지 누리집 기준',
  finalRate: FINAL_RATE,
  targetRate: ROUND_TARGET_RATE,
  achieved: FINAL_RATE >= ROUND_TARGET_RATE,
  tier: 'TIER_10',
  tierLabel: '10~15% 구간',
  confirmedMileage: CONFIRMED_MILEAGE,
  amount: {
    baselineTotal: BASELINE_AMOUNT,
    actualTotal: ACTUAL_AMOUNT,
    savedAmount: BASELINE_AMOUNT - ACTUAL_AMOUNT,
    savedIsPocketEligible: false,
  },
  utilityResults: UTILITY_RESULTS,
  monthlyRates: MONTHLY_RATES,
  mileageConverted: false,
  // 진행 중인 회차(ECO_HOME_IN_PROGRESS.roundId) 를 가리킨다
  nextRound: { roundId: 7, periodStart: '2026-04', periodEnd: '2026-09', goalSet: true },
}

/**
 * GET /eco/rounds/{roundId}/settlement — 마일리지 적립 (B-5-03 · WF-11).
 *
 * ⚠️ `isCash: false` — **아직 현금이 아니다**(돈의 3단계 중 ②). `statusLabel` 은 `확인` 이고
 * 현금 전환은 포켓 도메인(`POST /pocket/conversions`)이다. 여기서는 보내기만 한다.
 */
export const ECO_SETTLEMENT = {
  roundId: ROUND_ID,
  periodStart: PERIOD_START,
  periodEnd: PERIOD_END,
  confirmedMileage: CONFIRMED_MILEAGE,
  statusLabel: '확인',
  cumulativeRate: FINAL_RATE,
  tier: 'TIER_10',
  calculation: {
    baselineAmount: BASELINE_AMOUNT,
    actualAmount: ACTUAL_AMOUNT,
    savedAmount: BASELINE_AMOUNT - ACTUAL_AMOUNT,
    note: '전기·도시가스·수도를 직전 2년 같은 기간(10~3월) 평균과 비교했어요',
  },
  isCash: false,
  convertible: true,
  externalUrl: 'https://ecomileage.seoul.go.kr',
  otherUses: ['서울시 세금', '상품권', '관리비 납부'],
}

/**
 * `home.resultModal` (api-spec.md 10.1). WF-10 과 **같은 상수에서 뽑는다** —
 * 따로 적으면 모달과 결과 화면이 다른 숫자를 보인다.
 */
export const ECO_RESULT_MODAL = {
  roundId: ECO_RESULT.roundId,
  periodStart: ECO_RESULT.periodStart,
  periodEnd: ECO_RESULT.periodEnd,
  finalRate: ECO_RESULT.finalRate,
  tier: ECO_RESULT.tier,
  mileage: ECO_RESULT.confirmedMileage,
  confirmedAt: ECO_RESULT.confirmedAt,
}
