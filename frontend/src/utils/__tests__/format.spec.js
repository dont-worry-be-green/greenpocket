import { describe, it, expect } from 'vitest'
import {
  formatWon,
  formatSignedWon,
  formatMileage,
  formatChangeRate,
  changeRateParts,
  formatDifficulty,
  formatUsage,
  formatMonth,
  formatPercent,
  formatRoundPeriod,
  formatUtilityType,
  formatPoint,
  formatTier,
  formatUnit,
  usagePrecision,
} from '../format'

describe('formatWon', () => {
  it('천 단위 구분기호와 원을 붙인다', () => {
    expect(formatWon(43200)).toBe('43,200원')
    expect(formatWon(0)).toBe('0원')
  })

  it('값이 없으면 - 로 표시한다', () => {
    expect(formatWon(null)).toBe('-')
    expect(formatWon(undefined)).toBe('-')
  })
})

describe('formatSignedWon', () => {
  it('비교 차액에는 부호를 붙인다', () => {
    expect(formatSignedWon(4300)).toBe('+4,300원')
    expect(formatSignedWon(-4300)).toBe('-4,300원')
    expect(formatSignedWon(0)).toBe('0원')
  })
})

describe('formatMileage', () => {
  it('원이 아니라 M 을 붙인다', () => {
    expect(formatMileage(30000)).toBe('30,000M')
  })
})

describe('formatChangeRate', () => {
  it('양수는 감소, 음수는 증가다', () => {
    expect(formatChangeRate(12)).toBe('↓12% 줄었어요')
    expect(formatChangeRate(-2)).toBe('↑2% 늘었어요')
  })

  it('소수 3자리로 오는 12.000 은 12% 로 보여준다', () => {
    expect(formatChangeRate(12.0)).toBe('↓12% 줄었어요')
    expect(formatChangeRate(11.322)).toBe('↓11.322% 줄었어요')
  })

  it('0 은 화살표를 쓰지 않는다', () => {
    expect(formatChangeRate(0)).toBe('지난달과 같아요')
  })
})

describe('changeRateParts', () => {
  it('방향과 숫자와 말을 나눠 준다', () => {
    expect(changeRateParts(12)).toEqual({ direction: 'down', value: '12', word: '줄었어요' })
    expect(changeRateParts(-2)).toEqual({ direction: 'up', value: '2', word: '늘었어요' })
  })

  it('0 은 화살표 없는 same 이다. ↓0% 로 그리면 안 된다', () => {
    expect(changeRateParts(0).direction).toBe('same')
    expect(changeRateParts(0).word).toBe('지난달과 같아요')
  })

  it('값이 없으면 none 이다', () => {
    expect(changeRateParts(null).direction).toBe('none')
  })

  it('11.322 의 자릿수를 깎지 않는다 (결정 C-13)', () => {
    expect(changeRateParts(11.322).value).toBe('11.322')
    expect(changeRateParts(12.0).value).toBe('12')
  })
})

describe('formatDifficulty', () => {
  it('난이도 enum 을 한국어로 바꾼다', () => {
    expect(formatDifficulty('EASY')).toBe('쉬움')
    expect(formatDifficulty('NORMAL')).toBe('보통')
    expect(formatDifficulty('HARD')).toBe('어려움')
  })

  it('모르는 값은 - 로 떨어뜨린다', () => {
    expect(formatDifficulty(undefined)).toBe('-')
  })
})

describe('formatPercent', () => {
  it('불필요한 0 은 떼고 유효 자릿수는 지킨다', () => {
    expect(formatPercent(64.0)).toBe('64%')
    expect(formatPercent(11.322)).toBe('11.322%')
  })

  it('값이 없으면 - 로 표시한다', () => {
    expect(formatPercent(null)).toBe('-')
  })
})

describe('formatUtilityType', () => {
  it('요금 종류 enum 을 한국어로 바꾼다', () => {
    expect(formatUtilityType('ELECTRICITY')).toBe('전기')
    expect(formatUtilityType('GAS')).toBe('도시가스')
    expect(formatUtilityType('WATER')).toBe('수도')
  })

  it('모르는 값은 - 로 떨어뜨린다', () => {
    expect(formatUtilityType(undefined)).toBe('-')
  })
})

describe('formatUsage', () => {
  it('displayPrecision 만큼만 소수를 보여준다', () => {
    expect(formatUsage(1340.0, 0, 'kWh')).toBe('1,340kWh')
    expect(formatUsage(108.5, 1, '㎥')).toBe('108.5㎥')
  })
})

describe('formatMonth', () => {
  it('YYYY-MM 을 한국어로 바꾼다', () => {
    expect(formatMonth('2026-08')).toBe('2026년 8월')
  })
})

describe('formatRoundPeriod', () => {
  it('같은 해면 뒤쪽 연도를 접는다 (B-1-07)', () => {
    expect(formatRoundPeriod('2026-04', '2026-09')).toBe('2026-04 ~ 09')
  })

  it('해를 넘기면 연도를 그대로 둔다', () => {
    expect(formatRoundPeriod('2025-10', '2026-03')).toBe('2025-10 ~ 2026-03')
  })

  it('값이 없으면 - 로 표시한다', () => {
    expect(formatRoundPeriod(null, '2026-09')).toBe('-')
    expect(formatRoundPeriod('2026-04', undefined)).toBe('-')
  })
})

describe('formatPoint', () => {
  it('퍼센트포인트를 붙이고 꼬리 0 을 접는다', () => {
    expect(formatPoint(1.678)).toBe('1.678%p')
    expect(formatPoint(2.0)).toBe('2%p')
  })

  it('값이 없으면 - 로 표시한다', () => {
    expect(formatPoint(null)).toBe('-')
  })
})

describe('formatTier', () => {
  it('구간 enum 을 라벨로 바꾼다', () => {
    expect(formatTier('TIER_5')).toBe('5~10%')
    expect(formatTier('TIER_10')).toBe('10~15%')
    expect(formatTier('TIER_15')).toBe('15% 이상')
  })

  it('값이 없으면 - 로 표시한다', () => {
    expect(formatTier(null)).toBe('-')
  })
})

describe('formatUnit', () => {
  it('m3 만 ㎥ 로 바꾸고 나머지는 원문을 둔다', () => {
    expect(formatUnit('m3')).toBe('㎥')
    expect(formatUnit('kWh')).toBe('kWh')
  })

  it('값이 없으면 빈 문자열이다 — 단위는 숫자 뒤에 붙어서 - 를 쓰면 안 된다', () => {
    expect(formatUnit(null)).toBe('')
  })
})

describe('usagePrecision', () => {
  it('kWh 는 정수, ㎥ 는 소수 첫째 자리다 (preview 의 displayPrecision 규칙)', () => {
    expect(usagePrecision('kWh')).toBe(0)
    expect(usagePrecision('m3')).toBe(1)
  })
})
