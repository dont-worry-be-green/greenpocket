/*
 * ecoResult 픽스처가 데모 고정값과 맞는지 확인한다.
 *
 * WF-09·WF-10·WF-11 이 같은 회차를 서로 다른 숫자로 보여 주면 시연이 그 자리에서 무너진다.
 * 기준값은 AGENTS.md 「고정 상수」의 데모 케이스(결정 C-13)와 핵심 비즈니스 규칙 3 이다.
 */

import { describe, expect, it } from 'vitest'

import { ECO_HOME_IN_PROGRESS, ECO_HOME_RESULT_READY } from '../ecoHome'
import { ECO_RESULT, ECO_RESULT_MODAL, ECO_SETTLEMENT } from '../ecoResult'

describe('ECO_RESULT — 최종 감축률', () => {
  it('탄소 가중 합산은 12.499% 다 — 시안의 12% 가 아니다 (결정 C-13)', () => {
    expect(ECO_RESULT.finalRate).toBe(12.499)
  })

  it('요금별 감축률은 사용량에서 나온다', () => {
    const rateOf = (utilityType) =>
      ECO_RESULT.utilityResults.find((row) => row.utilityType === utilityType).finalRate
    expect(rateOf('ELECTRICITY')).toBe(13)
    expect(rateOf('GAS')).toBe(12.002)
    expect(rateOf('WATER')).toBe(5)
  })

  it('기준 사용량은 1,340kWh · 108㎥ · 66㎥ 고정값이다', () => {
    expect(ECO_RESULT.utilityResults.map((row) => row.baselineUsage)).toEqual([1340, 108, 66])
  })

  it('achieved 는 요금별 목표와 비교한 결과다 — 도시가스만 미달이다', () => {
    expect(ECO_RESULT.utilityResults.map((row) => row.achieved)).toEqual([true, false, true])
  })

  it('10~15% 구간이라 30,000M 이다', () => {
    expect(ECO_RESULT.tier).toBe('TIER_10')
    expect(ECO_RESULT.confirmedMileage).toBe(30000)
  })
})

describe('ECO_RESULT — 금액', () => {
  it('덜 낸 요금은 기준 요금과 평가 기간 요금의 차다', () => {
    const { baselineTotal, actualTotal, savedAmount } = ECO_RESULT.amount
    expect(baselineTotal - actualTotal).toBe(savedAmount)
    expect(savedAmount).toBe(50500)
  })

  it('덜 낸 요금은 포켓 잔액이 아니다 (핵심 규칙 3)', () => {
    expect(ECO_RESULT.amount.savedIsPocketEligible).toBe(false)
  })

  it('적립 화면의 계산 근거도 같은 금액을 쓴다', () => {
    expect(ECO_SETTLEMENT.calculation.baselineAmount).toBe(ECO_RESULT.amount.baselineTotal)
    expect(ECO_SETTLEMENT.calculation.actualAmount).toBe(ECO_RESULT.amount.actualTotal)
    expect(ECO_SETTLEMENT.calculation.savedAmount).toBe(ECO_RESULT.amount.savedAmount)
  })
})

describe('ECO_SETTLEMENT', () => {
  it('적립된 마일리지는 아직 현금이 아니다 (핵심 규칙 2)', () => {
    expect(ECO_SETTLEMENT.isCash).toBe(false)
    expect(ECO_SETTLEMENT.statusLabel).toBe('확인')
  })

  it('확정 마일리지는 결과 화면과 같다', () => {
    expect(ECO_SETTLEMENT.confirmedMileage).toBe(ECO_RESULT.confirmedMileage)
  })
})

describe('회차 번호', () => {
  it('확정 회차와 진행 중 회차는 다르다 — 한 회차가 둘 다일 수 없다', () => {
    expect(ECO_RESULT.roundId).not.toBe(ECO_HOME_IN_PROGRESS.roundId)
  })

  it('다음 회차가 홈이 보여 주는 진행 중 회차다', () => {
    expect(ECO_RESULT.nextRound.roundId).toBe(ECO_HOME_IN_PROGRESS.roundId)
  })

  it('결산 모달은 지난 회차를 알린다', () => {
    expect(ECO_HOME_RESULT_READY.resultModal.roundId).toBe(ECO_RESULT.roundId)
    expect(ECO_HOME_RESULT_READY.roundId).toBe(ECO_HOME_IN_PROGRESS.roundId)
  })
})

describe('ECO_RESULT_MODAL', () => {
  it('결과 화면과 같은 숫자를 보여 준다 — 따로 적으면 어긋난다', () => {
    expect(ECO_RESULT_MODAL.finalRate).toBe(ECO_RESULT.finalRate)
    expect(ECO_RESULT_MODAL.mileage).toBe(ECO_RESULT.confirmedMileage)
    expect(ECO_RESULT_MODAL.tier).toBe(ECO_RESULT.tier)
    expect(ECO_RESULT_MODAL.confirmedAt).toBe(ECO_RESULT.confirmedAt)
  })
})
