/*
 * 마이페이지 목데이터 — api-spec.md 5.2 · 6.6 · 14.1 · 14.2. 화면 MY-01 ~ MY-04.
 *
 * ── 왜 픽스처가 필요한가 ─────────────────────────────────────────────────
 * BE 4개(`GET /mypage` · `GET /profile` · `GET /bills` · `GET /reports`)는 이미 다 있고
 * 응답 필드도 스펙과 1:1 이다. 그런데 **보관함 두 화면은 실 서버에서 비어 있다** —
 *   · 고지서: 데모 시드에 고지서가 없다. 진단 탭에서 OCR 로 직접 등록해야 생긴다
 *   · 리포트: 월별·ECO_MONTHLY 는 그 고지서에서 나오고, ECO_RESULT 는 확정된 회차가
 *     있어야 하는데 `round_status = 'CONFIRMED'` 를 쓰는 코드가 백엔드에 없다
 * 그래서 실 API 로는 목록이 채워진 화면을 한 번도 볼 수 없다. 시안 대조를 위한 목데이터다.
 *
 * ── 값의 출처 ────────────────────────────────────────────────────────────
 * **지어낸 필드가 없다.** 모양은 api-spec 예시 그대로고, 수치는 시안(`docs.local/screens/mypage`)
 * 의 행을 그대로 옮겼다. 시안에 없는 나머지 건수는 `counts` 합(전기 5·수도 5·가스 4 = 14)과
 * `totalElements: 14` 를 맞추기 위해 같은 형태로 이어 붙인 것이다.
 *
 * 프로필은 api-spec 14.1 예시(김수현 · 관악구 · 원룸 · 10평 이하)를 쓴다. 시안의 이름·구는
 * 아트보드 예시라 계약과 무관하고, 실제로는 ONB-01 에서 사용자가 입력한 이름이 온다.
 */

/** GET /mypage (14.1). `pocketAccountNo`·`integration` 은 화면이 쓰지 않지만 응답에는 있다 */
export const MYPAGE = {
  profile: {
    name: '김수현',
    sidoName: '서울특별시',
    sigunguName: '관악구',
    housingType: 'ONE_ROOM',
    areaBand: 'UNDER_10',
    profileSummary: '서울 관악구 · 원룸 · 10평 이하',
  },
  links: {
    billArchive: { count: 14, screen: 'MY-03' },
    reportArchive: { count: 15, screen: 'MY-04' },
  },
  ecoAddress: {
    label: '서울 관악구',
    registeredAt: '2026-03',
    matchesProfile: true,
    notice: '이사했다면 꼭 바꿔주세요. 바꾸지 않으면 지금 살지 않는 집의 사용량과 비교돼요',
  },
  integration: {
    ecoLinkStatus: 'LINKED',
    ecoLinkedAt: '2026-09-01T09:00:00+09:00',
    greenlifeParticipating: true,
    greenlifeLinkedAt: '2026-09-01T09:12:00+09:00',
    registeredUtilities: ['ELECTRICITY', 'GAS', 'WATER'],
  },
  pocketAccountNo: '1005-1234-5678-90',
}

/** GET /profile (5.2). MY-02 가 프리필에 쓴다 — `GET /mypage` 에는 지역 **코드**가 없다 */
export const PROFILE = {
  name: '김수현',
  sidoCode: '11',
  sidoName: '서울특별시',
  sigunguCode: '11620',
  sigunguName: '관악구',
  housingType: 'ONE_ROOM',
  areaBand: 'UNDER_10',
  profileSummary: '서울 관악구 · 원룸 · 10평 이하',
  seoulResident: true,
  onboardingCompleted: true,
}

/*
 * GET /bills (6.6) 원본 14건. 최신 월 우선 정렬은 **여기서 미리 해 둔다** — 서버가 그렇게 주고
 * 화면은 다시 정렬하지 않는다.
 *
 * 시안의 7행(전기 5·수도 1·가스 1)이 앞쪽에 그대로 있고, 나머지 7행은 `counts` 를 맞추는 몫이다.
 * 9월 전기 한 건만 `REVIEW_REQUIRED`(확인 대기)다 — 상태 배지가 두 가지로 보여야 확인이 된다.
 */
const bill = (recordId, billingMonth, utilityType, amount, usage, registeredAt, extra = {}) => ({
  recordId,
  billingMonth,
  utilityType,
  billType: utilityType,
  amount,
  usage,
  usageUnit: utilityType === 'ELECTRICITY' ? 'kWh' : 'm3',
  inputSource: 'OCR',
  recordStatus: 'CONFIRMED',
  registeredAt,
  ...extra,
})

const BILLS = [
  bill(64, '2026-09', 'ELECTRICITY', 31_540, 210.0, '2026-09-25T10:12:00+09:00', {
    recordStatus: 'REVIEW_REQUIRED',
  }),
  bill(63, '2026-09', 'WATER', 18_200, 23.0, '2026-09-20T09:40:00+09:00'),
  bill(62, '2026-09', 'GAS', 22_430, 32.0, '2026-09-18T20:05:00+09:00'),
  bill(58, '2026-08', 'ELECTRICITY', 35_200, 225.0, '2026-08-25T10:22:00+09:00'),
  bill(57, '2026-08', 'WATER', 17_600, 22.0, '2026-08-20T09:30:00+09:00'),
  bill(56, '2026-08', 'GAS', 19_800, 28.0, '2026-08-18T19:50:00+09:00'),
  bill(52, '2026-07', 'ELECTRICITY', 28_630, 198.0, '2026-07-25T11:05:00+09:00'),
  bill(51, '2026-07', 'WATER', 16_900, 21.0, '2026-07-20T10:00:00+09:00'),
  bill(50, '2026-07', 'GAS', 14_200, 19.0, '2026-07-18T18:30:00+09:00'),
  bill(46, '2026-06', 'ELECTRICITY', 27_410, 185.0, '2026-06-25T10:40:00+09:00', {
    inputSource: 'MANUAL',
  }),
  bill(45, '2026-06', 'WATER', 15_800, 19.0, '2026-06-20T09:15:00+09:00'),
  bill(44, '2026-06', 'GAS', 12_600, 16.0, '2026-06-18T18:10:00+09:00'),
  bill(40, '2026-05', 'ELECTRICITY', 25_890, 172.0, '2026-05-25T10:30:00+09:00'),
  bill(39, '2025-12', 'WATER', 15_200, 18.0, '2025-12-20T09:20:00+09:00'),
]

/**
 * 서버의 필터·페이징을 흉내 낸다. 여기서 하지 않으면 탭·연도를 눌러도 목록이 그대로여서
 * **필터가 붙었는지 확인할 수 없다.** `counts` 는 필터와 무관한 전체 집계다(api-spec 6.6).
 */
export function buildBillArchive({ utility, year, page = 0, size = 20 } = {}) {
  const filtered = BILLS.filter(
    (item) =>
      (!utility || item.utilityType === utility) &&
      (!year || item.billingMonth.startsWith(String(year))),
  )

  const from = page * size
  const content = filtered.slice(from, from + size)
  const totalPages = filtered.length === 0 ? 0 : Math.ceil(filtered.length / size)

  return {
    content,
    page,
    size,
    totalElements: filtered.length,
    totalPages,
    hasNext: page + 1 < totalPages,
    counts: {
      ALL: BILLS.length,
      ELECTRICITY: BILLS.filter((item) => item.utilityType === 'ELECTRICITY').length,
      WATER: BILLS.filter((item) => item.utilityType === 'WATER').length,
      GAS: BILLS.filter((item) => item.utilityType === 'GAS').length,
    },
  }
}

/*
 * GET /reports (14.2) 원본 15건.
 *
 * `reportId` 는 `타입:키` 합성 ID 이고 실제 PK 가 아니다. `targetScreen`·`targetParams` 로
 * 화면을 찾아가는 것이 MVP 범위 전부고, `downloadable` 은 항상 false 다(E-2-02 는 P2).
 *
 * 2025년 행을 6건 둔 이유 — 시안이 연도 그룹을 두 개 보여주고 한 그룹 안에 「더 보기」가 있다.
 * 연도가 하나뿐이면 그 두 가지를 한 번도 확인할 수 없다.
 */
const monthlyDiagnosis = (yearMonth, createdAt) => ({
  reportId: `MONTHLY_DIAGNOSIS:${yearMonth}`,
  type: 'MONTHLY_DIAGNOSIS',
  yearMonth,
  title: `${Number(yearMonth.slice(5))}월 생활비 진단`,
  createdAt,
  targetScreen: 'AN-07',
  targetParams: { month: yearMonth },
  downloadable: false,
})

const ecoMonthly = (yearMonth, createdAt) => ({
  reportId: `ECO_MONTHLY:${yearMonth}`,
  type: 'ECO_MONTHLY',
  yearMonth,
  title: `${Number(yearMonth.slice(5))}월분 전달 리포트`,
  createdAt,
  targetScreen: 'WF-07',
  targetParams: { month: yearMonth },
  downloadable: false,
})

const REPORTS = [
  monthlyDiagnosis('2026-08', '2026-09-01T10:22:00+09:00'),
  monthlyDiagnosis('2026-07', '2026-08-03T09:10:00+09:00'),
  monthlyDiagnosis('2026-06', '2026-07-02T09:05:00+09:00'),
  monthlyDiagnosis('2026-05', '2026-06-02T09:05:00+09:00'),
  monthlyDiagnosis('2026-04', '2026-05-02T09:05:00+09:00'),
  monthlyDiagnosis('2025-12', '2026-01-02T09:05:00+09:00'),
  monthlyDiagnosis('2025-11', '2025-12-02T09:05:00+09:00'),
  monthlyDiagnosis('2025-10', '2025-11-02T09:05:00+09:00'),
  ecoMonthly('2026-07', '2026-08-03T00:00:00+09:00'),
  ecoMonthly('2026-06', '2026-07-03T00:00:00+09:00'),
  ecoMonthly('2026-05', '2026-06-03T00:00:00+09:00'),
  ecoMonthly('2026-04', '2026-05-03T00:00:00+09:00'),
  ecoMonthly('2025-12', '2026-01-03T00:00:00+09:00'),
  ecoMonthly('2025-11', '2025-12-03T00:00:00+09:00'),
  {
    reportId: 'ECO_RESULT:6',
    type: 'ECO_RESULT',
    yearMonth: '2026-03',
    title: '2025-10 ~ 2026-03 평가 결과',
    createdAt: '2026-06-05T00:00:00+09:00',
    targetScreen: 'WF-10',
    targetParams: { roundId: 6 },
    downloadable: false,
  },
]

/** 정렬은 서버 규칙(월 최신순 → 생성 최신순)을 그대로 흉내 낸다 */
export function buildReportArchive({ type, year, page = 0, size = 20 } = {}) {
  const filtered = REPORTS.filter(
    (item) => (!type || item.type === type) && (!year || item.yearMonth.startsWith(String(year))),
  ).sort((a, b) => b.yearMonth.localeCompare(a.yearMonth) || b.createdAt.localeCompare(a.createdAt))

  const from = page * size
  const content = filtered.slice(from, from + size)
  const totalPages = filtered.length === 0 ? 0 : Math.ceil(filtered.length / size)

  return {
    content,
    page,
    size,
    totalElements: filtered.length,
    totalPages,
    hasNext: page + 1 < totalPages,
  }
}
