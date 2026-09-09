const cards = [
  {
    policyId: '20260722005400213265',
    title: '2026 지역가치창업가 양성사업',
    category: 'JOB',
    subCategory: '창업',
    supportSummary: '창업교육과 컨설팅, 창업자금 최대 3천만원을 지원해요.',
    applicationStatus: 'OPEN',
    applicationEndDate: '2026-12-31',
    matchStatus: 'ELIGIBLE',
    matchScore: 100,
    matchReasons: ['에코마일리지 연동 지역과 일치해요', '지원 연령에 해당해요'],
    regionScope: 'SIGUNGU',
  },
  {
    policyId: '20260614005400213232',
    title: '청년 부동산 중개보수 및 이사비 지원사업',
    category: 'HOUSING',
    subCategory: '전월세 및 주거급여 지원',
    supportSummary: '중개보수와 이사비를 최대 40만원까지 실비로 지원해요.',
    applicationStatus: 'OPEN',
    applicationEndDate: '2026-12-31',
    matchStatus: 'CHECK_REQUIRED',
    matchScore: 90,
    matchReasons: ['서울특별시 관악구 지역과 일치해요', '세부 소득 조건은 확인이 필요해요'],
    regionScope: 'SIGUNGU',
  },
  {
    policyId: '20250521005400110863',
    title: '주거안정장학금',
    category: 'EDUCATION',
    subCategory: '교육비 지원',
    supportSummary: '원거리 대학생의 주거 관련 비용을 월 최대 20만원 지원해요.',
    applicationStatus: 'UNKNOWN',
    applicationEndDate: null,
    matchStatus: 'CHECK_REQUIRED',
    matchScore: 70,
    matchReasons: ['전국 대상 정책이에요', '학력 조건은 공고에서 확인해 주세요'],
    regionScope: 'NATIONAL',
  },
]

export const POLICY_PREFERENCES = {
  birthDate: '1998-03-15',
  currentStatus: 'EMPLOYED',
  annualIncomeBand: 'FROM_24M_TO_36M',
  educationStatus: 'UNIVERSITY_GRADUATE',
  interestCategories: ['JOB', 'HOUSING'],
  ecoAddress: { label: '서울특별시 관악구', sidoCode: '11', sigunguCode: '11620' },
  birthDateEditable: false,
  regionEditable: false,
  completed: true,
}

let editedPolicyPreferences = null

export function getFixturePolicyPreferences() {
  const preferences = { ...POLICY_PREFERENCES, ...editedPolicyPreferences }
  return {
    ...preferences,
    interestCategories: [...preferences.interestCategories],
  }
}

export function updateFixturePolicyPreferences(payload) {
  editedPolicyPreferences = {
    ...payload,
    interestCategories: [...payload.interestCategories],
    completed: true,
  }
  return getFixturePolicyPreferences()
}

export function buildPolicyList(params = {}) {
  const keyword = String(params.keyword ?? '')
    .trim()
    .toLowerCase()
  const filtered = cards.filter(
    (item) =>
      (!keyword || `${item.title} ${item.supportSummary}`.toLowerCase().includes(keyword)) &&
      (!params.category || item.category === params.category) &&
      (!params.categories?.length || params.categories.includes(item.category)) &&
      (!params.matchStatus || item.matchStatus === params.matchStatus),
  )
  const page = Number(params.page ?? 0)
  const size = Number(params.size ?? 20)
  return {
    content: filtered.slice(page * size, page * size + size),
    page,
    size,
    totalElements: filtered.length,
    totalPages: filtered.length ? Math.ceil(filtered.length / size) : 0,
    hasNext: (page + 1) * size < filtered.length,
    region: {
      linked: true,
      label: '서울특별시 관악구',
      appliedLevels: ['NATIONAL', 'SIDO', 'SIGUNGU'],
    },
    lastSyncedAt: '2026-09-09T02:29:35+09:00',
  }
}

export function findPolicy(policyId) {
  const card = cards.find((item) => item.policyId === policyId)
  if (!card) return null
  return {
    ...card,
    description: `${card.title}을 통해 청년의 자립과 생활 안정을 지원하는 정책이에요.`,
    supportContent: card.supportSummary,
    application: {
      status: card.applicationStatus,
      periodType: 'LIMITED',
      startDate: '2026-01-01',
      endDate: card.applicationEndDate,
      method: '운영기관 홈페이지에서 신청해 주세요.',
      url: 'https://www.youthcenter.go.kr',
    },
    organizations: { supervising: '서울특별시', operating: '청년지원센터' },
    conditions: {
      age: '만 19세~39세',
      income: '세부 소득 조건 확인',
      employment: '세부 취업 상태 확인',
      education: '제한 없음',
      major: '제한 없음',
      marriage: '제한 없음',
      special: '세부 공고 확인',
    },
    match: { status: card.matchStatus, score: card.matchScore, reasons: card.matchReasons },
    referenceUrls: ['https://www.youthcenter.go.kr'],
    source: '온통청년',
    lastSyncedAt: '2026-09-09T02:29:35+09:00',
  }
}

export { cards as POLICY_CARDS }
