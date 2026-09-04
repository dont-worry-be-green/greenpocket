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

/** 입력창처럼 단위 없이 숫자만 표시할 때 사용한다. 12400 → '12,400' */
export function formatNumber(value) {
  if (isBlank(value)) return EMPTY
  return Math.round(value).toLocaleString('ko-KR')
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
 * 방향이 없는 비율. 비중(shareRate)·목표 절감률처럼 그 자체가 값인 퍼센트에 쓴다.
 * 증감(늘었다/줄었다)에는 쓰지 않는다 — 그건 formatChangeRate 다.
 *   64.000 → '64%'  ·  11.322 → '11.322%'
 */
export function formatPercent(rate) {
  if (isBlank(rate)) return EMPTY
  return `${Number(Number(rate).toFixed(3))}%`
}

/**
 * 요금 종류 enum 을 한국어 라벨로 바꾼다 (api-spec.md 3절 UtilityType).
 * 화면마다 '가스'/'도시가스'로 갈리지 않게 한 곳에 둔다.
 */
const UTILITY_TYPE_LABEL = { ELECTRICITY: '전기', GAS: '도시가스', WATER: '수도' }

export function formatUtilityType(utilityType) {
  return UTILITY_TYPE_LABEL[utilityType] ?? EMPTY
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

/**
 * 평가 회차 기간. 같은 해면 뒤쪽 연도를 접는다 (B-1-07 완료 조건).
 *   ('2026-04','2026-09') → '2026-04 ~ 09'  ·  ('2025-10','2026-03') → '2025-10 ~ 2026-03'
 */
export function formatRoundPeriod(periodStart, periodEnd) {
  if (!periodStart || !periodEnd) return EMPTY
  const [startYear] = periodStart.split('-')
  const [endYear, endMonth] = periodEnd.split('-')
  return `${periodStart} ~ ${startYear === endYear ? endMonth : periodEnd}`
}

/** ISO-8601 일시 → '2026-08-02 14:22' */
export function formatDateTime(dateTime) {
  if (!dateTime) return EMPTY
  const date = new Date(dateTime)
  if (Number.isNaN(date.getTime())) return EMPTY
  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
    .format(date)
    .replace(/\. /g, '-')
    .replace(/\./g, '')
    .replace('24:', '00:')
}
