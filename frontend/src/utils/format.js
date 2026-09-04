/*
 * 표시 포맷터 (기능명세서 COM-06 · frontend/AGENTS.md 6절)
 *
 * 서버는 숫자와 enum만 내려준다(api-spec.md 1.4). 문자열 조립은 전부 여기서 한다.
 * 컴포넌트에서 toLocaleString 을 직접 부르면 화면마다 표기가 갈린다.
 *
 * 값이 없을 수 있다. `available: false` 응답은 오류가 아니라 정상 상태이므로
 * null·undefined 는 '-' 로 떨어뜨린다.
 */

const EMPTY = '-'

const isBlank = (value) => value === null || value === undefined || Number.isNaN(value)

/** 43200 → '43,200원' */
export function formatWon(amount) {
  if (isBlank(amount)) return EMPTY
  return `${Math.round(amount).toLocaleString('ko-KR')}원`
}

/** 비교 차액에만 쓴다. 4300 → '+4,300원' · -4300 → '-4,300원' */
export function formatSignedWon(amount) {
  if (isBlank(amount)) return EMPTY
  const rounded = Math.round(amount)
  const sign = rounded > 0 ? '+' : ''
  return `${sign}${rounded.toLocaleString('ko-KR')}원`
}

/** 마일리지는 원이 아니라 M 이다. 1M = 1원. 30000 → '30,000M' */
export function formatMileage(mileage) {
  if (isBlank(mileage)) return EMPTY
  return `${Math.round(mileage).toLocaleString('ko-KR')}M`
}

/**
 * 증감은 부호 대신 화살표 + 말로 쓴다.
 * 양수가 감소, **음수가 증가**다 (api-spec.md 1.4 · 11절).
 *   12.000 → '↓12% 줄었어요'  ·  -2.000 → '↑2% 늘었어요'  ·  0 → '지난달과 같아요'
 * 서버는 소수 3자리로 주지만 12.000 을 '12%' 로 보여야 하므로 끝자리 0만 떨어뜨린다.
 */
export function formatChangeRate(rate) {
  if (isBlank(rate)) return EMPTY
  if (rate === 0) return '지난달과 같아요'
  const value = Number(Math.abs(rate).toFixed(3))
  return rate > 0 ? `↓${value}% 줄었어요` : `↑${value}% 늘었어요`
}

/**
 * 사용량. 단위와 소수 자리수는 서버가 `displayPrecision` 으로 알려준다.
 * (전기 0 = 정수 kWh, 수도·가스 1 = 소수 첫째 자리)
 */
export function formatUsage(value, displayPrecision = 0, unit = '') {
  if (isBlank(value)) return EMPTY
  const text = Number(value).toLocaleString('ko-KR', {
    minimumFractionDigits: displayPrecision,
    maximumFractionDigits: displayPrecision,
  })
  return unit ? `${text}${unit}` : text
}

/** '2026-08' → '2026년 8월' */
export function formatMonth(yearMonth) {
  if (!yearMonth) return EMPTY
  const [year, month] = yearMonth.split('-')
  return `${year}년 ${Number(month)}월`
}
