/*
 * 리포트·실천 조정 픽스처 검증.
 *
 * 여기서 지키려는 것은 예쁜 숫자가 아니라 **화면끼리 어긋나지 않는 것**이다.
 * WF-06 홈이 이미 「7월 1.284% · 누적 9.043%」를 보여주고 있어서,
 * WF-07 이 다른 값을 내면 같은 세션 안에서 숫자가 갈린다.
 */

import { describe, expect, it } from 'vitest'

import { ECO_HOME_IN_PROGRESS } from '../ecoHome'
import {
  buildMissionAdjust,
  buildMissionUpdate,
  buildMonthlyReport,
  ECO_MONTHLY_REPORT_EMPTY,
} from '../ecoReport'

const report = buildMonthlyReport()
const rateOf = (utilityType) =>
  report.cause.byUtility.find((row) => row.utilityType === utilityType).rate

describe('전달 리포트 — 홈과 같은 숫자를 쓴다', () => {
  it('7월 감축률이 홈의 latestReport 와 같다', () => {
    expect(report.result.monthlyRate).toBe(ECO_HOME_IN_PROGRESS.latestReport.monthlyRate)
    expect(report.result.targetRate).toBe(ECO_HOME_IN_PROGRESS.latestReport.targetRate)
  })

  it('누적 감축률이 홈의 progress 와 같다', () => {
    expect(report.result.cumulativeRate).toBe(ECO_HOME_IN_PROGRESS.progress.cumulativeRate)
  })

  it('반영된 달이 홈의 coveredMonths 와 같다', () => {
    expect(report.result.cumulativeMonths).toEqual(ECO_HOME_IN_PROGRESS.progress.coveredMonths)
  })

  it('달마다의 감축률 평균이 누적과 맞는다 — 달마다 기준 탄소가 같다', () => {
    const sum = report.monthlyRates.reduce((total, row) => total + row.rate, 0)
    expect(sum / report.monthlyRates.length).toBeCloseTo(report.result.cumulativeRate, 3)
  })
})

describe('어디서 발목을 잡았나 (B-4-07)', () => {
  it('전기만 늘었다 — 음수는 증가다', () => {
    expect(rateOf('ELECTRICITY')).toBe(-5.164)
    expect(rateOf('GAS')).toBe(15.556)
    expect(rateOf('WATER')).toBe(10.909)
  })

  /*
   * 요금별 감축률의 평균은 (-5.164 + 15.556 + 10.909) / 3 = 7.100 이다.
   * 합산은 1.284 다. **평균이 아니라 탄소 가중**이라 다르고, 이 차이가 이 화면의 핵심이다.
   */
  it('합산은 요금별 감축률의 평균이 아니다', () => {
    const mean = report.cause.byUtility.reduce((sum, row) => sum + row.rate, 0) / 3
    expect(mean).not.toBeCloseTo(report.result.monthlyRate, 1)
  })

  it('탄소 비중 합이 100% 다', () => {
    const total = report.cause.byUtility.reduce((sum, row) => sum + row.carbonSharePercent, 0)
    expect(total).toBeCloseTo(100, 3)
  })

  it('가장 큰 비중이 전기다 — 시안의 83% 가 아니라 68% 다', () => {
    const electricity = report.cause.byUtility.find((row) => row.utilityType === 'ELECTRICITY')
    expect(electricity.carbonSharePercent).toBeCloseTo(68.289, 3)
    expect(report.cause.largestCarbonUtility).toBe('ELECTRICITY')
  })

  it('미달 항목만 펼쳐서 내려온다 — 화면이 판정하지 않는다', () => {
    expect(report.cause.byUtility.map((row) => row.expanded)).toEqual([true, false, false])
  })
})

describe('남은 두 달 처방 (B-4-08)', () => {
  it('필요 감축률은 (목표 × 6 − 지금까지의 합) ÷ 남은 달 이다', () => {
    const sum = report.monthlyRates.reduce((total, row) => total + row.rate, 0)
    expect(report.prescription.requiredRate).toBeCloseTo(
      (report.result.targetRate * 6 - sum) / report.prescription.remainingMonths,
      3,
    )
    expect(report.prescription.requiredRate).toBe(15.88)
  })

  it('전기 혼자 떠안을 몫은 탄소 비중으로 나눠서 전체보다 크다', () => {
    const electricity = report.prescription.requiredByUtility[0]
    expect(electricity.utilityType).toBe('ELECTRICITY')
    expect(electricity.requiredRate).toBeGreaterThan(report.prescription.requiredRate)
    expect(electricity.assumption).toBe('도시가스 15.6%, 수도 10.9% 감축을 지금처럼 유지할 때예요')
  })

  it('고른 실천 합계가 필요분을 덮으면 achievable 이다', () => {
    expect(report.prescription.selectedMissionRate).toBeGreaterThanOrEqual(
      report.prescription.requiredRate,
    )
    expect(report.prescription.achievable).toBe(true)
  })

  it('조정 대상은 발목을 잡은 요금이다', () => {
    expect(report.prescription.adjustTargetUtility).toBe('ELECTRICITY')
  })
})

describe('고지서를 안 올린 달', () => {
  it('에러가 아니라 result: null + emptyReason 이다', () => {
    expect(ECO_MONTHLY_REPORT_EMPTY.result).toBeNull()
    expect(ECO_MONTHLY_REPORT_EMPTY.emptyReason).toBe('NO_BILL')
  })
})

describe('실천 조정 (WF-08)', () => {
  const adjust = buildMissionAdjust('ELECTRICITY')

  it('쿼리는 utility 지만 응답 필드는 utilityType 이다', () => {
    expect(adjust.utilityType).toBe('ELECTRICITY')
  })

  it('고를 때 기대한 값과 실제로 나온 값을 나란히 준다', () => {
    expect(adjust.comparison.selectedExpectedRate).toBe(18)
    expect(adjust.comparison.actualRate).toBe(-5.164)
  })

  /*
   * 이미 고른 12·13 이 「냉방」이라 14·15 는 추천에서 빠진다 — 합계에 하나만 들어가기 때문이다.
   * 17·18 은 둘 다 「세탁」이라 큰 쪽(먼저 온 17) 하나만 남는다.
   */
  it('이미 고른 기기군과 겹치지 않는 것만 추천한다', () => {
    const recommended = adjust.missions.filter((m) => m.recommended).map((m) => m.missionId)
    expect(recommended).toEqual([16, 17, 19])
  })

  it('추천을 다 반영하면 합계가 늘고 필요분을 덮는다', () => {
    expect(adjust.preview.currentRate).toBe(18)
    expect(adjust.preview.withRecommendedRate).toBe(25)
    expect(adjust.preview.withRecommendedRate).toBeGreaterThan(adjust.requiredRate)
    expect(adjust.preview.coversRequired).toBe(true)
  })

  it('2회 연속 미달이라 구간 하향을 제안한다 — 제안만 한다', () => {
    expect(adjust.tierDowngrade.consecutiveMisses).toBe(2)
    expect(adjust.tierDowngrade.suggest).toBe(true)
  })

  it('없는 요금이면 null 이다', () => {
    expect(buildMissionAdjust('HEATING')).toBeNull()
  })
})

describe('선택 미션 갱신 (PUT missions)', () => {
  it('같은 기기군은 가장 큰 것 하나만 합계에 든다 — goal/preview 와 같은 계산이다', () => {
    const saved = buildMissionUpdate([12, 13, 16])
    expect(saved.combinedMissionRate).toBe(23)

    const excluded = saved.items.find((item) => item.missionId === 12)
    expect(excluded.counted).toBe(false)
    expect(excluded.exclusionReason).toBe('냉방 겹침 · 합계 제외')
  })

  it('미션을 바꾸면 오늘의 실천도 바뀐다', () => {
    expect(buildMissionUpdate([16]).todayMissionsUpdated).toBe(true)
  })

  it('체크할수록 합계가 늘어난다 — 상수가 아니다', () => {
    expect(buildMissionUpdate([13, 16, 19]).combinedMissionRate).toBeGreaterThan(
      buildMissionUpdate([13]).combinedMissionRate,
    )
  })
})
