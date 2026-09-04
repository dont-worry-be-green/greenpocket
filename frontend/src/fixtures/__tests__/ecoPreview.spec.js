/*
 * buildGoalPreview 가 백엔드 EcoGoalService 와 같은 숫자를 내는지 확인한다.
 *
 * 픽스처 계산은 서버 코드와 따로 떨어져 있어서 조용히 어긋날 수 있다.
 * 기준값은 AGENTS.md 「고정 상수」의 데모 케이스와 api-spec.md 9.2 응답 예시다.
 */

import { describe, expect, it } from 'vitest'

import { ECO_GOAL_FORM, ECO_GOAL_FORM_UNREGISTERED } from '../ecoGoal'
import { buildGoalPreview, ECO_CARBON_FACTORS } from '../ecoPreview'

/** 시안의 기본 선택 — 전기 10% · 도시가스 15% · 수도 5% */
const DEMO_TARGETS = [
  { utilityType: 'ELECTRICITY', tier: 'TIER_10' },
  { utilityType: 'GAS', tier: 'TIER_15' },
  { utilityType: 'WATER', tier: 'TIER_5' },
]

describe('ECO_CARBON_FACTORS', () => {
  it('탄소 환산계수는 AGENTS.md 고정 상수다', () => {
    expect(ECO_CARBON_FACTORS).toEqual([
      { utilityType: 'ELECTRICITY', factorG: 424.0, unit: 'kWh' },
      { utilityType: 'WATER', factorG: 332.0, unit: 'm3' },
      { utilityType: 'GAS', factorG: 2240.0, unit: 'm3' },
    ])
  })
})

describe('buildGoalPreview — 데모 케이스', () => {
  const preview = buildGoalPreview({ targets: DEMO_TARGETS, selectedMissionIds: [] })

  it('기준 탄소는 1,340kWh·108㎥·66㎥ 를 가중한 831,992g 이다', () => {
    expect(preview.combined.baselineCarbonG).toBe(831992)
    expect(preview.combined.targetCarbonG).toBe(737792.4)
  })

  it('합산 감축률은 11.322% 다 — 시안의 10.5% 가 아니다 (결정 C-13)', () => {
    expect(preview.combined.combinedRate).toBe(11.322)
  })

  it('11.322% 는 10~15% 구간이라 30,000M 이다', () => {
    expect(preview.combined.tier).toBe('TIER_10')
    expect(preview.combined.tierLabel).toBe('10~15%')
    expect(preview.combined.expectedMileage).toBe(30000)
  })

  it('다음 구간까지 3.678%p 남았다', () => {
    expect(preview.combined.nextTier).toEqual({
      tier: 'TIER_15',
      gapPoint: 3.678,
      mileage: 50000,
    })
  })

  it('절감액 합계는 44,090원이고 기준 요금은 420,600원이다', () => {
    expect(preview.combined.totalExpectedSaving).toBe(44090)
    expect(preview.combined.baselineTotalAmount).toBe(420600)
  })

  it('요금별 목표 사용량과 절감액', () => {
    expect(preview.utilities).toEqual([
      {
        utilityType: 'ELECTRICITY',
        targetRate: 10.0,
        baselineUsage: 1340.0,
        targetUsage: 1206.0,
        usageUnit: 'kWh',
        baselineAmount: 268000,
        expectedSaving: 26800,
        displayPrecision: 0,
      },
      {
        utilityType: 'GAS',
        targetRate: 15.0,
        baselineUsage: 108.0,
        targetUsage: 91.8,
        usageUnit: 'm3',
        baselineAmount: 96600,
        expectedSaving: 14490,
        displayPrecision: 1,
      },
      {
        utilityType: 'WATER',
        targetRate: 5.0,
        baselineUsage: 66.0,
        targetUsage: 62.7,
        usageUnit: 'm3',
        baselineAmount: 56000,
        expectedSaving: 2800,
        displayPrecision: 1,
      },
    ])
  })

  it('절감액 합계가 요금별 합과 맞는다', () => {
    const sum = preview.utilities.reduce((total, item) => total + item.expectedSaving, 0)
    expect(sum).toBe(preview.combined.totalExpectedSaving)
  })

  it('세 요금이 다 등록되어 있으면 제외 요금이 없다', () => {
    expect(preview.combined.excludedUtilities).toEqual([])
  })
})

describe('buildGoalPreview — 구간을 바꾸면 숫자가 움직인다', () => {
  const all = (tier) =>
    buildGoalPreview({
      targets: [
        { utilityType: 'ELECTRICITY', tier },
        { utilityType: 'GAS', tier },
        { utilityType: 'WATER', tier },
      ],
      selectedMissionIds: [],
    })

  it('세 요금을 같은 구간으로 맞추면 감축률이 그 구간 그대로다', () => {
    // 탄소 가중이라 셋을 같이 올리면 가중평균이 원래 비율로 돌아온다
    expect(all('TIER_5').combined.combinedRate).toBe(5.0)
    expect(all('TIER_10').combined.combinedRate).toBe(10.0)
    expect(all('TIER_15').combined.combinedRate).toBe(15.0)
  })

  it('5 → 10 → 15% 로 올리면 마일리지가 10,000 → 30,000 → 50,000M 이 된다', () => {
    expect(all('TIER_5').combined.expectedMileage).toBe(10000)
    expect(all('TIER_10').combined.expectedMileage).toBe(30000)
    expect(all('TIER_15').combined.expectedMileage).toBe(50000)
  })

  it('구간 경계값은 상위 구간으로 간다', () => {
    // 10.000 은 TIER_5 가 아니라 TIER_10 이다
    expect(all('TIER_10').combined.tier).toBe('TIER_10')
    expect(all('TIER_15').combined.tier).toBe('TIER_15')
  })

  it('최고 구간이면 다음 구간이 없다', () => {
    expect(all('TIER_15').combined.nextTier).toBeNull()
  })

  it('구간을 안 고른 요금은 안 줄인 것으로 본다', () => {
    const partial = buildGoalPreview({
      targets: [{ utilityType: 'ELECTRICITY', tier: 'TIER_10' }],
      selectedMissionIds: [],
    })
    // 전기만 10% → 줄어든 탄소 56,816g ÷ 831,992g
    expect(partial.combined.combinedRate).toBe(6.829)
    expect(partial.combined.tier).toBe('TIER_5')
    expect(partial.combined.totalExpectedSaving).toBe(26800)
  })

  it('전기 탄소가 전체의 68% 라 전기만 15% 를 걸어도 합산이 10% 를 넘는다', () => {
    // 1,340kWh × 424 = 568,160g ÷ 831,992g = 68.289%
    const electricityOnly = buildGoalPreview({
      targets: [{ utilityType: 'ELECTRICITY', tier: 'TIER_15' }],
      selectedMissionIds: [],
    })
    expect(electricityOnly.combined.combinedRate).toBe(10.243)
    expect(electricityOnly.combined.tier).toBe('TIER_10')
  })

  it('5% 미만이면 구간이 없고 마일리지는 0 이다', () => {
    const tiny = buildGoalPreview({
      targets: [{ utilityType: 'WATER', tier: 'TIER_5' }],
      selectedMissionIds: [],
    })
    expect(tiny.combined.combinedRate).toBe(0.132)
    expect(tiny.combined.tier).toBeNull()
    expect(tiny.combined.tierLabel).toBeNull()
    expect(tiny.combined.expectedMileage).toBe(0)
    expect(tiny.combined.nextTier.tier).toBe('TIER_5')
  })
})

describe('buildGoalPreview — 미션 합계', () => {
  const withMissions = (ids) =>
    buildGoalPreview({ targets: DEMO_TARGETS, selectedMissionIds: ids })

  it('같은 기기군에서는 가장 큰 것 하나만 센다', () => {
    // 12(냉방 3%) · 13(냉방 18%) — 둘 다 냉방이라 18% 만 센다
    const preview = withMissions([12, 13])
    expect(preview.missions.combinedMissionRate).toBe(18)

    const excluded = preview.missions.items.find((item) => item.missionId === 12)
    expect(excluded.counted).toBe(false)
    expect(excluded.exclusionReason).toBe('냉방 겹침 · 합계 제외')
    expect(preview.missions.items.find((item) => item.missionId === 13).counted).toBe(true)
  })

  it('기기군이 다르면 더한다', () => {
    // 13(냉방 18%) + 16(대기전력 5%) + 21(양치 11%)
    expect(withMissions([13, 16, 21]).missions.combinedMissionRate).toBe(34)
  })

  it('상한에 걸린 미션은 상한값으로 센다', () => {
    // 14 는 raw 53.7% 지만 전기 상한 30% 다
    expect(withMissions([14]).missions.items[0].computedRate).toBe(30)
  })

  it('감축률은 정수로 반올림된다 (EcoGoalService.applyMissionCap)', () => {
    // 40 ÷ (1340 ÷ 6) × 100 = 17.910 → 18
    expect(withMissions([13]).missions.items[0].computedRate).toBe(18)
  })

  it('합계가 목표에 못 미치면 shortfallPoint 가 남는다', () => {
    const preview = withMissions([12])
    expect(preview.missions.combinedMissionRate).toBe(3)
    expect(preview.missions.shortfallPoint).toBe(8.322)
    expect(preview.missions.meetsTarget).toBe(false)
  })

  it('목표를 넘기면 부족분이 0 이고 meetsTarget 이 true 다', () => {
    const preview = withMissions([13])
    expect(preview.missions.shortfallPoint).toBe(0)
    expect(preview.missions.meetsTarget).toBe(true)
  })

  it('아무것도 안 고르면 빈 배열이다', () => {
    const preview = withMissions([])
    expect(preview.missions.items).toEqual([])
    expect(preview.missions.combinedMissionRate).toBe(0)
  })
})

describe('buildGoalPreview — 수도 미등록 (WF-05)', () => {
  const preview = buildGoalPreview(
    {
      targets: [
        { utilityType: 'ELECTRICITY', tier: 'TIER_10' },
        { utilityType: 'GAS', tier: 'TIER_15' },
      ],
      selectedMissionIds: [],
    },
    ECO_GOAL_FORM_UNREGISTERED,
  )

  it('미등록 요금은 합산에서 빠지고 excludedUtilities 에 담긴다', () => {
    expect(preview.combined.excludedUtilities).toEqual(['WATER'])
    expect(preview.utilities.map((item) => item.utilityType)).toEqual(['ELECTRICITY', 'GAS'])
  })

  it('수도 탄소가 빠져 기준 탄소가 810,080g 으로 줄고 감축률은 올라간다', () => {
    // 831,992 − 21,912(수도) = 810,080. 수도 5% 가 빠지니 합산이 11.322 → 11.493 으로 오른다
    expect(preview.combined.baselineCarbonG).toBe(810080)
    expect(preview.combined.combinedRate).toBe(11.493)
    expect(preview.combined.baselineTotalAmount).toBe(364600)
    expect(preview.combined.totalExpectedSaving).toBe(41290)
  })

  it('미등록 요금에 목표를 걸어도 합산에 끼어들지 않는다', () => {
    const forced = buildGoalPreview(
      { targets: [...DEMO_TARGETS], selectedMissionIds: [] },
      ECO_GOAL_FORM_UNREGISTERED,
    )
    expect(forced.combined.combinedRate).toBe(preview.combined.combinedRate)
    expect(forced.utilities).toHaveLength(2)
  })

  it('미등록 요금의 미션은 감축률이 null 이라 합계에 들어가지 않는다', () => {
    const preview = buildGoalPreview(
      {
        targets: [{ utilityType: 'ELECTRICITY', tier: 'TIER_10' }],
        selectedMissionIds: [21],
      },
      ECO_GOAL_FORM_UNREGISTERED,
    )
    expect(preview.missions.items[0]).toEqual({
      missionId: 21,
      computedRate: null,
      counted: false,
      exclusionReason: null,
    })
    expect(preview.missions.combinedMissionRate).toBe(0)
  })
})

describe('buildGoalPreview — carbonFactors', () => {
  it('서버 순서(전기·수도·가스) 그대로 내려준다', () => {
    const preview = buildGoalPreview({ targets: DEMO_TARGETS, selectedMissionIds: [] })
    expect(preview.carbonFactors.map((factor) => factor.utilityType)).toEqual([
      'ELECTRICITY',
      'WATER',
      'GAS',
    ])
  })

  it('goal-form 의 세그먼트 순서(전기·가스·수도)와 다르다', () => {
    expect(ECO_GOAL_FORM.segments.map((segment) => segment.utilityType)).toEqual([
      'ELECTRICITY',
      'GAS',
      'WATER',
    ])
  })
})
