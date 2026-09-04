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
 * 증감을 방향·숫자·말 세 조각으로 나눈다.
 * 화살표를 SVG 아이콘으로 그리는 컴포넌트(GpDelta)가 조각을 따로 써야 하기 때문이다.
 * 문자열이 필요하면 아래 formatChangeRate 를 쓴다. 계산 규칙은 여기 한 곳에만 있다.
 *
 * 양수가 감소, **음수가 증가**다 (api-spec.md 1.4 · 11절).
 *   direction: 'down'(줄었다) · 'up'(늘었다) · 'same'(0) · 'none'(값 없음)
 */
export function changeRateParts(rate) {
  if (isBlank(rate)) return { direction: 'none', value: EMPTY, word: '' }
  if (rate === 0) return { direction: 'same', value: '0', word: '지난달과 같아요' }
  // 서버는 소수 3자리로 준다. 12.000 은 '12' 로 보이되 11.322 의 자릿수는 지킨다 (결정 C-13)
  const value = String(Number(Math.abs(rate).toFixed(3)))
  return rate > 0
    ? { direction: 'down', value, word: '줄었어요' }
    : { direction: 'up', value, word: '늘었어요' }
}

/**
 * 증감은 부호 대신 화살표 + 말로 쓴다.
 *   12.000 → '↓12% 줄었어요'  ·  -2.000 → '↑2% 늘었어요'  ·  0 → '지난달과 같아요'
 */
export function formatChangeRate(rate) {
  const { direction, value, word } = changeRateParts(rate)
  if (direction === 'none') return EMPTY
  if (direction === 'same') return word
  return `${direction === 'down' ? '↓' : '↑'}${value}% ${word}`
}

/**
 * 미션 난이도 enum 을 한국어 라벨로 바꾼다.
 * enum 은 api-spec.md 2절 `Difficulty`, 라벨 문구는 기능명세서 B-3-01 이 근거다.
 * 서버는 표기를 하지 않으므로(api-spec.md 1.4) 이 매핑은 프론트 책임이다.
 */
const DIFFICULTY_LABEL = { EASY: '쉬움', NORMAL: '보통', HARD: '어려움' }

export function formatDifficulty(difficulty) {
  return DIFFICULTY_LABEL[difficulty] ?? EMPTY
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
