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
    matchStatus: 'ELIGIBLE',
    matchScore: 100,
    matchReasons: ['서울특별시 관악구 지역과 일치해요', '소득 없음 조건에 해당해요'],
    regionScope: 'SIGUNGU',
    detail: {
      description: '서울로 전입하거나 서울 안에서 이사한 청년의 주거비 부담을 줄이는 사업이에요.',
      supportContent: '부동산 중개보수와 이사비를 합산해 최대 40만원까지 실비로 지원해요.',
      applicationMethod: '청년몽땅정보통에서 모집 일정과 제출서류를 확인해 온라인으로 신청해 주세요.',
      applicationUrl: 'https://youth.seoul.go.kr/youthConts.do?key=2310100044',
      income: '기준 중위소득 150% 이하',
      special: '서울 전입·서울 내 이사 및 무주택 여부 등 공고 확인',
      supervising: '서울특별시 청년사업담당관',
      operating: '서울특별시',
    },
  },
  {
    policyId: '20250226005400110565',
    title: '청년주택드림청약통장',
    category: 'HOUSING',
    subCategory: '주택 및 거주지',
    supportSummary: '무주택 청년의 청약과 자산 형성을 위해 우대금리와 소득공제를 제공해요.',
    applicationStatus: 'OPEN',
    applicationEndDate: null,
    matchStatus: 'ELIGIBLE',
    matchScore: 100,
    matchReasons: ['전국 대상 정책이에요', '소득 없음 조건에 해당해요'],
    regionScope: 'NATIONAL',
    detail: {
      description: '무주택 청년의 주택 구입과 자산 형성을 지원하는 청약통장이에요.',
      supportContent: '최대 연 4.5% 이율, 월 100만원 납입한도, 납입액 40% 소득공제와 청년주택드림대출 연계를 제공해요.',
      applicationMethod: '주택도시기금 수탁은행에서 가입해 주세요.',
      applicationUrl: 'https://nhuf.molit.go.kr/FP/FP07/FP0701/FP07010301.jsp',
      income: '연소득 5,000만원 이하',
      special: '무주택 여부 확인',
      supervising: '국토교통부',
      operating: '주택도시기금 수탁은행',
    },
  },
  {
    policyId: '20250316005400210626',
    title: '청년 매입임대주택 사업',
    category: 'HOUSING',
    subCategory: '주택 및 거주지',
    supportSummary: '무주택 저소득 청년에게 매입임대주택을 시세보다 저렴하게 공급해요.',
    applicationStatus: 'OPEN',
    applicationEndDate: null,
    matchStatus: 'ELIGIBLE',
    matchScore: 100,
    matchReasons: ['서울특별시 지역과 일치해요', '미취업 조건에 해당해요'],
    regionScope: 'SIDO',
    detail: {
      description: '서울주택도시개발공사가 매입한 주택을 무주택 청년에게 저렴하게 공급하는 사업이에요.',
      supportContent: '다가구·다세대 등 매입주택을 대학생과 취업준비생 등 청년에게 공급해요.',
      applicationMethod: '서울주택도시개발공사 모집공고에서 주택과 접수 일정을 확인해 신청해 주세요.',
      applicationUrl: 'https://www.i-sh.co.kr/',
      income: '도시근로자 월평균소득 기준 확인',
      special: '무주택·자산 기준 등 공고 확인',
      supervising: '서울특별시 주택정책관',
      operating: '서울주택도시개발공사',
    },
  },
  {
    policyId: '20250316005400210632',
    title: '청년안심주택 공급',
    category: 'HOUSING',
    subCategory: '주택 및 거주지',
    supportSummary: '역세권을 중심으로 청년과 신혼부부에게 공공·민간 임대주택을 공급해요.',
    applicationStatus: 'OPEN',
    applicationEndDate: null,
    matchStatus: 'ELIGIBLE',
    matchScore: 100,
    matchReasons: ['서울특별시 지역과 일치해요', '지원 연령에 해당해요'],
    regionScope: 'SIDO',
    detail: {
      description: '교통이 편리한 역세권에 청년층을 위한 임대주택을 공급하는 서울시 사업이에요.',
      supportContent: '청년안심주택 공공임대와 민간임대를 시세보다 낮은 임대료로 공급해요.',
      applicationMethod: '청년안심주택 모집공고에서 공급 주택과 접수 일정을 확인해 신청해 주세요.',
      applicationUrl: 'https://soco.seoul.go.kr/',
      income: '도시근로자 월평균소득 기준 확인',
      special: '무주택·자산 기준 등 공고 확인',
      supervising: '서울특별시 건축기획관',
      operating: '서울주택도시개발공사',
    },
  },
  {
    policyId: '20250316005400210633',
    title: '청년안심주택 임차보증금 무이자지원',
    category: 'HOUSING',
    subCategory: '전월세 및 주거급여 지원',
    supportSummary: '청년안심주택 입주 청년에게 임차보증금 일부를 무이자로 지원해요.',
    applicationStatus: 'OPEN',
    applicationEndDate: null,
    matchStatus: 'ELIGIBLE',
    matchScore: 100,
    matchReasons: ['서울특별시 지역과 일치해요', '소득 없음 조건에 해당해요'],
    regionScope: 'SIDO',
    detail: {
      description: '청년안심주택 입주자의 초기 보증금 부담을 낮추는 서울시 지원사업이에요.',
      supportContent: '임차보증금에 따라 보증금의 30~50%를 청년 최대 4,500만원까지 무이자로 지원해요.',
      applicationMethod: '임대차계약 후 청년안심주택 안내에 따라 소득심사 서류를 제출해 주세요.',
      applicationUrl: 'https://soco.seoul.go.kr/',
      income: '도시근로자 가구당 월평균소득 100% 이하',
      special: '청년안심주택 입주 및 자산 기준 확인',
      supervising: '서울특별시 건축기획관',
      operating: '서울주택도시개발공사',
    },
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
    description:
      card.detail?.description ?? `${card.title}을 통해 청년의 자립과 생활 안정을 지원하는 정책이에요.`,
    supportContent: card.detail?.supportContent ?? card.supportSummary,
    application: {
      status: card.applicationStatus,
      periodType: card.applicationEndDate ? 'LIMITED' : 'ALWAYS',
      startDate: card.applicationEndDate ? '2026-01-01' : null,
      endDate: card.applicationEndDate,
      method: card.detail?.applicationMethod ?? '운영기관 홈페이지에서 신청해 주세요.',
      url: card.detail?.applicationUrl ?? 'https://www.youthcenter.go.kr',
    },
    organizations: {
      supervising: card.detail?.supervising ?? '서울특별시',
      operating: card.detail?.operating ?? '청년지원센터',
    },
    conditions: {
      age: '만 19세~39세',
      income: card.detail?.income ?? '세부 소득 조건 확인',
      employment: '세부 취업 상태 확인',
      education: '제한 없음',
      major: '제한 없음',
      marriage: '제한 없음',
      special: card.detail?.special ?? '세부 공고 확인',
    },
    match: { status: card.matchStatus, score: card.matchScore, reasons: card.matchReasons },
    referenceUrls: [card.detail?.applicationUrl ?? 'https://www.youthcenter.go.kr'],
    source: '온통청년',
    lastSyncedAt: '2026-09-09T02:29:35+09:00',
  }
}

export { cards as POLICY_CARDS }
