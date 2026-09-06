/*
 * 'YYYY-MM' 월 계산.
 *
 * `utils/format.js` 는 **보여줄 문자열을 만드는 곳**이고 여기는 **값을 옮기는 곳**이다.
 * 섞어 두면 화면 문구를 고치다 이동 범위 계산이 함께 흔들린다.
 *
 * ⚠️ **기준 시각은 KST 다.** `new Date().getMonth()` 는 브라우저 로컬 시간이라 시연 기기가
 * 다른 표준시에 있으면 서버가 고르는 '이번 달'(Asia/Seoul)과 하루 차이로 갈린다.
 * 서버의 `month` 기본값이 KST 이므로 여기도 KST 로 맞춘다.
 *
 * 'YYYY-MM' 은 **사전순이 곧 시간순**이라 비교는 문자열 그대로 한다(`a < b`).
 * 별도 비교 함수를 두지 않는 이유다.
 */

/** 'YYYY-MM' 형식인지. URL 쿼리처럼 밖에서 들어온 값을 그대로 믿지 않는다 */
export function isMonth(value) {
  return typeof value === 'string' && /^\d{4}-(0[1-9]|1[0-2])$/.test(value)
}

/**
 * KST 기준 이번 달. 서버가 `month` 를 생략했을 때 고르는 달과 같다.
 *
 * ⚠️ **`month: '2-digit'` 을 믿지 않는다.** `ko-KR` 은 연·월만 요청하면 그 옵션을 무시하고
 * '9' 를 준다(일까지 함께 요청할 때만 '09' 로 채운다). 그래서 자리 채움은 직접 한다.
 */
export function currentMonth() {
  const parts = new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: 'numeric',
  }).formatToParts(new Date())
  const pick = (type) => parts.find((part) => part.type === type)?.value ?? ''
  return `${pick('year')}-${pick('month').padStart(2, '0')}`
}

/**
 * 달을 옮긴다. 연을 넘어가는 계산은 `Date` 에 맡긴다 — 12월 +1 을 손으로 다루면 틀린다.
 *   ('2026-08', -1) → '2026-07'  ·  ('2026-12', 1) → '2027-01'
 */
export function shiftMonth(yearMonth, delta) {
  if (!isMonth(yearMonth)) return yearMonth
  const [year, month] = yearMonth.split('-').map(Number)
  // UTC 로 계산한다. 로컬 표준시가 끼면 월초/월말에서 한 달이 밀린다
  const moved = new Date(Date.UTC(year, month - 1 + delta, 1))
  return `${moved.getUTCFullYear()}-${String(moved.getUTCMonth() + 1).padStart(2, '0')}`
}
