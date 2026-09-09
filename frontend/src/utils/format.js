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
 * 화면에 쓸 비율 자릿수. **소수 한 자리까지만 보여준다.**
 *
 * 서버는 `DECIMAL(7,3)` 으로 세 자리를 준다(api-spec.md 1.4). 그건 저장·계산 정밀도이고
 * 표시 자릿수를 정한 규칙은 명세에 없다(COM-06 은 "공통 포맷터로 구현한다" 까지다).
 * 11.322% 처럼 세 자리를 그대로 늘어놓으면 정작 읽어야 할 앞자리가 묻힌다.
 *
 * ⚠️ 결정 C-13 과 어긋나지 않는다. 그 결정은 **값의 출처**(시안 10.5% 가 아니라 계산값)에
 * 대한 것이고, 11.3% 는 여전히 시안 값이 아니라 계산값이다.
 *
 * ⚠️ **0 이 아닌 값을 0 으로 만들지 않는다.** 0.02 를 한 자리로 깎으면 0 이 되어
 * "0% 줄었어요" 나 "0%p만 더 줄이면" 처럼 뜻이 뒤집힌 문장이 나간다. 그럴 때만
 * 유효숫자 한 자리로 되돌려 0 이 아니라는 사실을 지킨다.
 *   11.322 → 11.3  ·  12.000 → 12  ·  1.039 → 1  ·  0.02 → 0.02
 */
function roundRate(rate) {
  const value = Number(rate)
  if (value === 0) return 0
  const rounded = Number(value.toFixed(1))
  return rounded === 0 ? Number(value.toPrecision(1)) : rounded
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
  // 서버는 소수 3자리로 준다. 화면은 한 자리까지다 — roundRate 주석 참고
  const value = String(roundRate(Math.abs(rate)))
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
 * 계절 enum(api-spec 3절 Season · mission_catalog.season_tags)을 한국어로.
 *   'SUMMER' → '여름'
 */
const SEASON_LABEL = { SPRING: '봄', SUMMER: '여름', AUTUMN: '가을', WINTER: '겨울' }
const ALL_SEASONS = Object.keys(SEASON_LABEL)

export function formatSeason(season) {
  return SEASON_LABEL[season] ?? EMPTY
}

/**
 * 미션의 계절 태그 배열을 한 덩어리로. **사계절이면 빈 문자열**이다 — 늘 보이는 미션에
 * 「봄·여름·가을·겨울」을 달면 계절 한정 미션이 안 보인다.
 *   ['SUMMER'] → '여름'  ·  ['AUTUMN','WINTER'] → '가을·겨울'  ·  네 계절 전부 → ''
 */
export function formatSeasonTags(seasonTags) {
  const tags = Array.isArray(seasonTags) ? seasonTags : []
  if (tags.length === 0 || ALL_SEASONS.every((season) => tags.includes(season))) return ''
  return ALL_SEASONS.filter((season) => tags.includes(season))
    .map((season) => SEASON_LABEL[season])
    .join('·')
}

/**
 * 방향이 없는 비율. 비중(shareRate)·목표 절감률처럼 그 자체가 값인 퍼센트에 쓴다.
 * 증감(늘었다/줄었다)에는 쓰지 않는다 — 그건 formatChangeRate 다.
 *   64.000 → '64%'  ·  11.322 → '11.3%'
 */
export function formatPercent(rate) {
  if (isBlank(rate)) return EMPTY
  return `${roundRate(rate)}%`
}

/**
 * 퍼센트'포인트'. 증감이 아니라 두 비율의 차이다.
 * `gapToNextTierPoint`(다음 구간까지 남은 %p) · `shortfallPoint`(미션 합계 부족분)에 쓴다.
 *
 * **GpDelta 에 넘기지 않는다.** 그러면 "1.678% 줄었어요"가 되어 뜻이 뒤집힌다.
 *   1.678 → '1.7%p'  ·  2.000 → '2%p'
 */
export function formatPoint(value) {
  if (isBlank(value)) return EMPTY
  return `${roundRate(value)}%p`
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
 * 목표 구간 enum 라벨 (api-spec.md 3절 TargetTier).
 *
 * **서버가 `label`·`tierLabel` 을 주면 그쪽이 우선이다.** goal-form 의 `tiers[]`,
 * preview 의 `combined.tierLabel`, 결과의 `tierLabel` 은 전부 서버 문구를 그대로 쓴다.
 * `GET /eco/home` 의 `progress.tiers[]` 에만 라벨이 없어서 그 자리에 쓰는 대체재다.
 */
const TIER_LABEL = { TIER_5: '5~10%', TIER_10: '10~15%', TIER_15: '15% 이상' }

export function formatTier(tier) {
  return TIER_LABEL[tier] ?? EMPTY
}

/**
 * 사용량 단위 표기. 서버는 ASCII `'m3'` 로 준다(api-spec.md 3절 UsageUnit).
 * **비교는 `'m3'` 로 하고 표기만 ㎥ 로 바꾼다.**
 */
const USAGE_UNIT_LABEL = { m3: '㎥' }

export function formatUnit(unit) {
  if (!unit) return ''
  return USAGE_UNIT_LABEL[unit] ?? unit
}

/**
 * 사용량 소수 자리수. 서버가 `displayPrecision` 을 주는 곳에서는 **그 값을 쓴다.**
 *
 * 그런데 `displayPrecision` 은 `POST .../goal/preview` 의 `utilities[]` 에만 있다.
 * `goal-form.segments[]` · `monthly-report.cause.byUtility[]` · `result.utilityResults[]` 에는
 * 없어서 undefined 가 formatUsage 의 기본값 0 으로 떨어지면 ㎥ 소수가 잘린다.
 * preview 가 쓰는 규칙(kWh 0 · m3 1)을 단위에서 되짚는 대체재다.
 */
export function usagePrecision(unit) {
  return unit === 'kWh' ? 0 : 1
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

/** '2026-08' → '8월' */
export function formatMonthOnly(yearMonth) {
  if (!yearMonth) return EMPTY
  const [, month] = yearMonth.split('-')
  return `${Number(month)}월`
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

/**
 * 평가 회차 기간 **칩** 표기. 홈 감축률 카드 우상단·WF-09 결산 모달의 알약에 쓴다.
 * 점 구분·공백 없음이라 좁은 칩에 들어간다. 같은 해면 뒤쪽 연도를 접는 규칙은 위와 같다.
 *   ('2026-04','2026-09') → '2026.04~09'  ·  ('2025-10','2026-03') → '2025.10~2026.03'
 */
export function formatRoundPeriodChip(periodStart, periodEnd) {
  if (!periodStart || !periodEnd) return EMPTY
  const [startYear] = periodStart.split('-')
  const [endYear, endMonth] = periodEnd.split('-')
  const start = periodStart.replace('-', '.')
  return `${start}~${startYear === endYear ? endMonth : periodEnd.replace('-', '.')}`
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

/**
 * 실천 항목 단가 (C-1-03 · BN-01 · BN-02 · BN-03).
 *   (10, '건') → '10원/건'  ·  (300, '개') → '300원/개'
 *
 * **`미래세대실천행동` 은 단가가 0 이고 `rewardUnit` 이 '운영계획' 이다**(시드 기준).
 * 그대로 조립하면 '0원/운영계획' 이 되어 단가가 있는 것처럼 읽힌다. 단가가 없는 항목은
 * 서버가 저장해 둔 문구만 보여준다.
 */
export function formatUnitPrice(unitPrice, rewardUnit) {
  if (isBlank(unitPrice)) return EMPTY
  if (unitPrice === 0) return rewardUnit || EMPTY
  const price = Math.round(unitPrice).toLocaleString('ko-KR')
  return rewardUnit ? `${price}원/${rewardUnit}` : `${price}원`
}

/**
 * 실천 내역의 짧은 날짜 (C-2-04 · BN-03). 같은 달 안의 목록이라 연도를 접는다.
 *   '2026-08-28T13:20:00+09:00' → '08.28'
 */
export function formatShortDate(dateTime) {
  if (!dateTime) return EMPTY
  const date = new Date(dateTime)
  if (Number.isNaN(date.getTime())) return EMPTY
  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul',
    month: '2-digit',
    day: '2-digit',
  })
    .format(date)
    .replace(/\.\s*$/, '')
    .replace(/\.\s*/g, '.')
}

/**
 * ISO-8601 일시 → '2026-12-05'. 시각을 뗀 날짜만.
 *
 * WF-10 헤더의 "2026-12-05 확정" 자리다. **`formatDateTime` 의 치환 사슬을 쓰지 않는다** —
 * 거기서 구분자를 문자열로 바꾸다 날짜·시각 사이까지 하이픈이 되는 버그가 났다(이슈 #83).
 * 여기서는 조각을 직접 받아 조립한다.
 */
export function formatDate(dateTime) {
  if (!dateTime) return EMPTY
  const date = new Date(dateTime)
  if (Number.isNaN(date.getTime())) return EMPTY
  const parts = new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(date)
  const pick = (type) => parts.find((part) => part.type === type)?.value ?? ''
  return `${pick('year')}-${pick('month')}-${pick('day')}`
}

/**
 * ISO-8601 일시 → '8월 3일'. 연·시각을 접은 짧은 날짜다.
 *
 * WF-06 전달 리포트가 「7월 고지서 · 8월 3일 등록」 한 줄에 쓴다(B-4-04). 같은 자리에
 * `formatDateTime` 을 쓰면 연도와 시각까지 붙어 한 줄이 두 줄로 넘어간다.
 */
export function formatMonthDay(dateTime) {
  if (!dateTime) return EMPTY
  const date = new Date(dateTime)
  if (Number.isNaN(date.getTime())) return EMPTY
  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul',
    month: 'long',
    day: 'numeric',
  }).format(date)
}

/**
 * 실적 상태 라벨 (C-2-05 · api-spec.md 3절 RewardStatus).
 *
 * 색만으로 구분하지 않고 이 문구를 반드시 함께 넣는다(COM-06). `PENDING` 은 아직 현금이
 * 아니고 포켓 잔액에도 들어가지 않는다 — '적립 완료' 처럼 확정으로 읽히는 말을 쓰지 않는다.
 */
const REWARD_STATUS_LABEL = { PENDING: '적립 예정', PAID: '지급 완료' }

export function formatRewardStatus(rewardStatus) {
  return REWARD_STATUS_LABEL[rewardStatus] ?? EMPTY
}

/**
 * 주거 형태 · 평수 구간 (api-spec.md 3절 HousingType · AreaBand).
 *
 * ONB-02 가 로컬 상수로 들고 있던 것을 여기로 올렸다. MY-01 이 값을 라벨로 되돌려야 하고
 * MY-02 가 ONB-02 와 **같은 선택지**를 그려야 해서(A-1-06 "ONB-01·02 폼 재사용") 세 화면이
 * 쓰는 값이 됐다. 선택지 배열과 라벨 표가 갈라지면 온보딩과 수정 화면의 문구가 조용히 달라진다.
 */
const HOUSING_TYPE_LABEL = {
  ONE_ROOM: '원룸',
  OFFICETEL: '오피스텔',
  APARTMENT: '아파트',
  MULTI_HOUSE: '다세대',
}
const AREA_BAND_LABEL = {
  UNDER_10: '10평 이하',
  FROM_10_TO_20: '10~20평',
  OVER_20: '20평 이상',
}

/** 선택 칩·라디오가 그대로 쓰는 `[{ value, label }]`. 순서가 곧 화면 순서다 */
const toOptions = (labels) => Object.entries(labels).map(([value, label]) => ({ value, label }))

export const HOUSING_TYPE_OPTIONS = toOptions(HOUSING_TYPE_LABEL)
export const AREA_BAND_OPTIONS = toOptions(AREA_BAND_LABEL)

export function formatHousingType(housingType) {
  return HOUSING_TYPE_LABEL[housingType] ?? EMPTY
}

export function formatAreaBand(areaBand) {
  return AREA_BAND_LABEL[areaBand] ?? EMPTY
}

/**
 * 마이페이지 기본 정보의 「주거 형태」 한 행 (E-1-01 · MY-01).
 *   ('ONE_ROOM', 'UNDER_10') → '원룸 · 10평 이하'
 *
 * `profileSummary` 는 지역까지 붙은 문장이라 이 행에 쓸 수 없다. 지역은 바로 위 행에 따로 있다.
 */
export function formatHousing(housingType, areaBand) {
  const parts = [formatHousingType(housingType), formatAreaBand(areaBand)].filter(
    (part) => part !== EMPTY,
  )
  return parts.length ? parts.join(' · ') : EMPTY
}

/**
 * 고지서 종류 (api-spec.md 3절 BillType). 보관함 제목 「2026년 9월 · 전기 고지서」에 쓴다.
 *
 * `UtilityType` 과 값이 겹치지만 **다른 enum 이다.** 관리비 고지서 한 장이 전기·수도·가스
 * 레코드 여러 건으로 쪼개지므로 `MANAGEMENT` 는 여기에만 있다(api-spec 6절 머리말).
 * 'GAS' 라벨은 formatUtilityType 과 같은 '도시가스' 로 맞춘다 — 화면마다 갈리지 않게.
 */
const BILL_TYPE_LABEL = {
  MANAGEMENT: '관리비',
  ELECTRICITY: '전기',
  GAS: '도시가스',
  WATER: '수도',
}

export function formatBillType(billType) {
  return BILL_TYPE_LABEL[billType] ?? EMPTY
}

/**
 * 고지서 레코드 상태 (api-spec.md 3절 RecordStatus).
 *
 * `REVIEW_REQUIRED` 는 OCR 신뢰도가 낮아 사람이 확인해야 하는 건이다. '실패' 가 아니라
 * '확인 대기' 다 — 값은 이미 저장돼 있고 진단에도 들어간다(핵심 규칙 11 과 다른 자리).
 */
const RECORD_STATUS_LABEL = { CONFIRMED: '등록 완료', REVIEW_REQUIRED: '확인 대기' }

export function formatRecordStatus(recordStatus) {
  return RECORD_STATUS_LABEL[recordStatus] ?? EMPTY
}

/**
 * ISO-8601 일시 → '2026.09.25'. 보관함 목록의 등록일·생성일 자리다(MY-03 · MY-04 시안).
 *
 * `formatDate` 와 값은 같고 구분자만 점이다. **치환으로 만들지 않는다** — 하이픈을 점으로
 * 바꾸다 날짜·시각 사이까지 함께 바뀌는 버그가 이미 있다(이슈 #83). 조각을 직접 조립한다.
 */
export function formatDotDate(dateTime) {
  if (!dateTime) return EMPTY
  const date = new Date(dateTime)
  if (Number.isNaN(date.getTime())) return EMPTY
  const parts = new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(date)
  const pick = (type) => parts.find((part) => part.type === type)?.value ?? ''
  return `${pick('year')}.${pick('month')}.${pick('day')}`
}
