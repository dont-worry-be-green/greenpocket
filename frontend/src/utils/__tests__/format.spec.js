import { describe, it, expect } from 'vitest'
import {
  formatWon,
  formatSignedWon,
  formatMileage,
  formatChangeRate,
  changeRateParts,
  formatDifficulty,
  formatSeasonTags,
  formatUsage,
  formatMonth,
  formatPercent,
  formatRoundPeriod,
  formatRoundPeriodChip,
  formatUtilityType,
  formatPoint,
  formatTier,
  formatUnit,
  usagePrecision,
  formatUnitPrice,
  formatShortDate,
  formatRewardStatus,
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

  it('서버가 소수 3자리로 줘도 화면은 한 자리까지다', () => {
    expect(formatChangeRate(12.0)).toBe('↓12% 줄었어요')
    expect(formatChangeRate(11.322)).toBe('↓11.3% 줄었어요')
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

  it('소수 한 자리까지만 남긴다. 꼬리 0 은 접는다', () => {
    expect(changeRateParts(11.322).value).toBe('11.3')
    expect(changeRateParts(12.0).value).toBe('12')
  })

  /*
   * 반올림이 값을 0 으로 만들면 "↓0% 줄었어요" 가 되어 뜻이 뒤집힌다.
   * 0 이 아니라는 사실이 자릿수보다 중요하다.
   */
  it('한 자리로 깎으면 0 이 되는 값도 0 으로 만들지 않는다', () => {
    expect(changeRateParts(0.02).value).toBe('0.02')
    expect(changeRateParts(0.02).direction).toBe('down')
    expect(changeRateParts(0.004).value).toBe('0.004')
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
  it('불필요한 0 은 떼고 소수 한 자리까지 보여준다', () => {
    expect(formatPercent(64.0)).toBe('64%')
    expect(formatPercent(11.322)).toBe('11.3%')
    expect(formatPercent(12.499)).toBe('12.5%')
    expect(formatPercent(1.039)).toBe('1%')
  })

  it('0 은 그대로 0% 다', () => {
    expect(formatPercent(0)).toBe('0%')
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

  it('칩 표기는 점 구분·공백 없음이고 접는 규칙은 같다', () => {
    expect(formatRoundPeriodChip('2026-04', '2026-09')).toBe('2026.04~09')
    expect(formatRoundPeriodChip('2025-10', '2026-03')).toBe('2025.10~2026.03')
    expect(formatRoundPeriodChip(null, '2026-09')).toBe('-')
  })

  it('값이 없으면 - 로 표시한다', () => {
    expect(formatRoundPeriod(null, '2026-09')).toBe('-')
    expect(formatRoundPeriod('2026-04', undefined)).toBe('-')
  })
})

describe('formatPoint', () => {
  it('퍼센트포인트를 붙이고 소수 한 자리까지 보여준다', () => {
    expect(formatPoint(1.678)).toBe('1.7%p')
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

describe('formatUnitPrice', () => {
  it('단가와 단위를 붙인다', () => {
    expect(formatUnitPrice(10, '건')).toBe('10원/건')
    expect(formatUnitPrice(300, '개')).toBe('300원/개')
    expect(formatUnitPrice(10000, '회')).toBe('10,000원/회')
  })

  /*
   * 미래세대실천행동은 단가 0 · rewardUnit '운영계획' 이다(시드).
   * 그대로 조립하면 '0원/운영계획' 이 되어 단가가 있는 것처럼 읽힌다.
   */
  it('단가가 0 이면 서버가 저장해 둔 문구만 보여준다', () => {
    expect(formatUnitPrice(0, '운영계획')).toBe('운영계획')
    expect(formatUnitPrice(0, '')).toBe('-')
  })

  it('단위가 없으면 금액만 붙인다', () => {
    expect(formatUnitPrice(500, '')).toBe('500원')
  })

  it('값이 없으면 - 로 표시한다', () => {
    expect(formatUnitPrice(null, '건')).toBe('-')
  })
})

describe('formatShortDate', () => {
  it('같은 달 목록이라 연도를 접는다', () => {
    expect(formatShortDate('2026-08-28T13:20:00+09:00')).toBe('08.28')
    expect(formatShortDate('2026-12-01T00:00:00+09:00')).toBe('12.01')
  })

  // UTC 로 오면 KST 로 옮겨야 날짜가 하루 밀리지 않는다
  it('KST 기준으로 자른다', () => {
    expect(formatShortDate('2026-08-28T20:00:00Z')).toBe('08.29')
  })

  it('값이 없거나 파싱되지 않으면 - 로 표시한다', () => {
    expect(formatShortDate(null)).toBe('-')
    expect(formatShortDate('없는날짜')).toBe('-')
  })
})

describe('formatRewardStatus', () => {
  // PENDING 은 아직 현금이 아니다. '적립 완료' 처럼 확정으로 읽히는 말을 쓰지 않는다 (C-2-05)
  it('실적 상태 enum 을 라벨로 바꾼다', () => {
    expect(formatRewardStatus('PENDING')).toBe('적립 예정')
    expect(formatRewardStatus('PAID')).toBe('지급 완료')
  })

  it('값이 없으면 - 로 표시한다', () => {
    expect(formatRewardStatus(null)).toBe('-')
  })
})

describe('formatSeasonTags — 계절 한정 미션 칩', () => {
  it('한 계절이면 그 이름, 둘이면 · 로 잇는다', () => {
    expect(formatSeasonTags(['SUMMER'])).toBe('여름')
    expect(formatSeasonTags(['WINTER', 'AUTUMN'])).toBe('가을·겨울')
  })

  it('사계절이거나 비어 있으면 빈 문자열이다 — 늘 보이는 미션엔 칩을 달지 않는다', () => {
    expect(formatSeasonTags(['SPRING', 'SUMMER', 'AUTUMN', 'WINTER'])).toBe('')
    expect(formatSeasonTags([])).toBe('')
    expect(formatSeasonTags(undefined)).toBe('')
  })
})
