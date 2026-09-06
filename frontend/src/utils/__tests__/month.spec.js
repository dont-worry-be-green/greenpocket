import { describe, expect, it } from 'vitest'

import { currentMonth, isMonth, shiftMonth } from '../month'

describe('isMonth', () => {
  it('YYYY-MM 만 통과시킨다', () => {
    expect(isMonth('2026-08')).toBe(true)
    expect(isMonth('2026-12')).toBe(true)
  })

  // URL 쿼리로 들어오는 값이라 형식을 믿지 않는다. 서버는 이런 값에 400 을 준다
  it.each([['2026-13'], ['2026-00'], ['2026-8'], ['26-08'], ['bad'], [''], [null], [undefined]])(
    '%s 는 막는다',
    (value) => {
      expect(isMonth(value)).toBe(false)
    },
  )
})

describe('shiftMonth', () => {
  it('앞뒤로 옮긴다', () => {
    expect(shiftMonth('2026-08', -1)).toBe('2026-07')
    expect(shiftMonth('2026-08', 1)).toBe('2026-09')
    expect(shiftMonth('2026-08', 0)).toBe('2026-08')
  })

  // 손으로 12 를 넘기면 틀린다. Date 에 맡겼는지 확인한다
  it('연을 넘어간다', () => {
    expect(shiftMonth('2026-12', 1)).toBe('2027-01')
    expect(shiftMonth('2026-01', -1)).toBe('2025-12')
    expect(shiftMonth('2026-01', -13)).toBe('2024-12')
  })

  it('형식이 아니면 그대로 돌려준다', () => {
    expect(shiftMonth('bad', 1)).toBe('bad')
  })
})

describe('currentMonth', () => {
  /*
   * 값 자체는 오늘이 언제냐에 따라 달라져 고정할 수 없다.
   * **형식과 KST 기준이라는 것**만 지킨다 — 브라우저 로컬 시간을 쓰면 표준시가 다른 기기에서
   * 서버가 고르는 달과 갈린다.
   */
  it('YYYY-MM 형식이고 KST 기준이다', () => {
    const month = currentMonth()
    expect(isMonth(month)).toBe(true)

    const kst = new Intl.DateTimeFormat('ko-KR', {
      timeZone: 'Asia/Seoul',
      year: 'numeric',
      month: '2-digit',
    }).formatToParts(new Date())
    const year = kst.find((part) => part.type === 'year').value
    expect(month.startsWith(year)).toBe(true)
  })
})
