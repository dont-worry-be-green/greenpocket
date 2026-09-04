import { describe, it, expect } from 'vitest'
import {
  formatWon,
  formatSignedWon,
  formatMileage,
  formatChangeRate,
  formatUsage,
  formatMonth,
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
