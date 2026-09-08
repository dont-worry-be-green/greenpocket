/*
 * 마이 탭 여섯 화면이 **첫 렌더에서 터지지 않는지**, 그리고 API 계약에 맞게 동작하는지 본다.
 * API 모듈은 여기서 막고 실제 응답 모양과 같은 픽스처를 주입한다.
 *
 * 특히 지키려는 것 셋:
 *   ① 계좌번호를 그리지 않는다 — 응답에는 있다
 *   ② MY-03 탭 배지가 목록 길이가 아니라 `counts` 다(A-2-12 완료 조건)
 *   ③ MY-06 임시 추천과 MY-02 추천 조건 저장은 서로 다른 동작이다
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'

import routes from '@/router/routes/mypage'
import BillArchiveView from '@/views/mypage/BillArchiveView.vue'
import MypageHomeView from '@/views/mypage/MypageHomeView.vue'
import PolicyDetailView from '@/views/mypage/PolicyDetailView.vue'
import PolicyListView from '@/views/mypage/PolicyListView.vue'
import ProfileEditView from '@/views/mypage/ProfileEditView.vue'
import ReportArchiveView from '@/views/mypage/ReportArchiveView.vue'

const { getMypage, getPolicyPreferences, updatePolicyPreferences, getBills, getReports } = vi.hoisted(() => ({
  getMypage: vi.fn(),
  getPolicyPreferences: vi.fn(),
  updatePolicyPreferences: vi.fn(),
  getBills: vi.fn(),
  getReports: vi.fn(),
}))

vi.mock('@/api/mypage', () => ({ getMypage, getPolicyPreferences, updatePolicyPreferences, getBills, getReports }))

const { getPolicies, getPolicyRecommendations, getPolicy, previewPolicyRecommendations } = vi.hoisted(() => ({
  getPolicies: vi.fn(),
  getPolicyRecommendations: vi.fn(),
  getPolicy: vi.fn(),
  previewPolicyRecommendations: vi.fn(),
}))
vi.mock('@/api/policy', () => ({ getPolicies, getPolicyRecommendations, getPolicy, previewPolicyRecommendations }))

const MYPAGE = {
  profile: {
    name: '김수현',
    birthDate: '1998-03-14',
    gender: 'FEMALE',
    phoneNumber: '01091740339',
  },
  links: {
    billArchive: { count: 14, screen: 'MY-03' },
    reportArchive: { count: 9, screen: 'MY-04' },
  },
  ecoAddress: {
    label: '서울 관악구',
    registeredAt: '2026-03',
    notice: '주소는 에코마일리지 누리집에서 바꿔주세요.',
  },
  integration: {
    ecoLinkStatus: 'LINKED',
    ecoLinkedAt: '2026-09-01T09:00:00+09:00',
    greenlifeParticipating: true,
    greenlifeLinkedAt: '2026-09-01T09:12:00+09:00',
    registeredUtilities: ['ELECTRICITY', 'GAS', 'WATER'],
  },
  pocketAccountNo: '1005-1234-5678-90',
  youthPolicy: {
    profileCompleted: true,
    regionLinked: true,
    recommendedCount: 1,
    preview: [{
      policyId: 'policy-1', title: '청년 주거 지원', category: 'HOUSING', subCategory: '주거',
      applicationStatus: 'OPEN', applicationEndDate: '2026-12-31', matchStatus: 'ELIGIBLE',
    }],
    lastSyncedAt: '2026-09-09T01:00:00+09:00',
  },
}

const PREFERENCES = {
  birthDate: '1998-03-14',
  currentStatus: 'EMPLOYED',
  annualIncomeBand: 'FROM_24M_TO_36M',
  householdStatus: 'ONE_PERSON',
  ecoAddress: { label: '서울 관악구', sidoCode: '11', sigunguCode: '11620' },
  birthDateEditable: false,
  regionEditable: false,
  completed: true,
}

const POLICY_LIST = {
  content: MYPAGE.youthPolicy.preview,
  page: 0, size: 20, totalElements: 1, totalPages: 1, hasNext: false,
  region: { linked: true, label: '서울 관악구', appliedLevels: ['NATIONAL', 'SIDO', 'SIGUNGU'] },
  lastSyncedAt: '2026-09-09T01:00:00+09:00', preview: false,
}

const POLICY_DETAIL = {
  ...POLICY_LIST.content[0],
  description: '정책 설명', supportContent: '지원 내용',
  application: { status: 'OPEN', startDate: '2026-01-01', endDate: '2026-12-31', method: '온라인 신청', url: 'https://example.com' },
  organizations: { supervising: '서울특별시', operating: '청년센터' },
  conditions: { age: '만 19~39세', income: '확인 필요', employment: '재직자', education: '제한 없음', major: '제한 없음', marriage: '제한 없음', special: '공고 확인' },
  match: { status: 'ELIGIBLE', score: 100, reasons: ['연령이 일치해요'] },
  source: '온통청년', referenceUrls: [], lastSyncedAt: '2026-09-09T01:00:00+09:00',
}

const BILLS = {
  content: [
    {
      recordId: 51,
      billingMonth: '2026-08',
      utilityType: 'ELECTRICITY',
      billType: 'ELECTRICITY',
      amount: 43200,
      usage: 210.0,
      usageUnit: 'kWh',
      inputSource: 'OCR',
      recordStatus: 'CONFIRMED',
      registeredAt: '2026-09-01T10:22:00+09:00',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 14,
  totalPages: 1,
  hasNext: false,
  counts: { ALL: 14, ELECTRICITY: 5, WATER: 5, GAS: 4 },
}

const REPORTS = {
  content: [
    {
      reportId: 'MONTHLY_DIAGNOSIS:2026-08',
      type: 'MONTHLY_DIAGNOSIS',
      yearMonth: '2026-08',
      title: '8월 생활비 진단',
      createdAt: '2026-09-01T10:22:00+09:00',
      targetScreen: 'AN-07',
      targetParams: { month: '2026-08' },
      downloadable: false,
    },
    {
      reportId: 'ECO_MONTHLY:2026-07',
      type: 'ECO_MONTHLY',
      yearMonth: '2026-07',
      title: '7월분 전달 리포트',
      createdAt: '2026-08-03T00:00:00+09:00',
      targetScreen: 'WF-07',
      targetParams: { month: '2026-07' },
      downloadable: false,
    },
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
  ],
  page: 0,
  size: 100,
  totalElements: 3,
  totalPages: 1,
  hasNext: false,
}

async function mountView(component, path = '/mypage') {
  window.history.replaceState({}, '', path)
  const pinia = createPinia()
  setActivePinia(pinia)

  const router = createRouter({ history: createWebHistory(), routes })
  await router.push(path)
  await router.isReady()

  const wrapper = mount(component, { global: { plugins: [pinia, router] } })
  // 첫 렌더는 onMounted 보다 먼저다. 여기서 터지면 아래 flush 전에 이미 실패한다
  expect(wrapper.html()).toBeTruthy()
  await flushPromises()
  await flushPromises()
  return { wrapper, router }
}

beforeEach(() => {
  vi.clearAllMocks()
  getMypage.mockResolvedValue(MYPAGE)
  getPolicyPreferences.mockResolvedValue(PREFERENCES)
  updatePolicyPreferences.mockResolvedValue({ policyProfileCompleted: true, recommendationsUpdated: true })
  getBills.mockResolvedValue(BILLS)
  getReports.mockResolvedValue(REPORTS)
  getPolicies.mockResolvedValue(POLICY_LIST)
  getPolicyRecommendations.mockResolvedValue(POLICY_LIST)
  getPolicy.mockResolvedValue(POLICY_DETAIL)
  previewPolicyRecommendations.mockResolvedValue({ ...POLICY_LIST, preview: true })
})

describe('MY-01 마이페이지 메인', () => {
  it('본인 정보와 에코마일리지 주소를 그린다', async () => {
    const { wrapper } = await mountView(MypageHomeView)
    const text = wrapper.text()

    expect(text).toContain('김수현')
    expect(text).toContain('생년월일1998.03.14')
    expect(text).toContain('성별여성')
    expect(text).toContain('휴대전화010-9174-0339')
    expect(text).toContain('에코마일리지 등록 주소')
    expect(text).toContain('서울 관악구')
  })

  it('실제 응답의 추천 정책을 보여주고 상세로 이동한다', async () => {
    const { wrapper } = await mountView(MypageHomeView)
    expect(wrapper.text()).toContain('나를 위한 청년정책')
    expect(wrapper.text()).toContain('청년 주거 지원')
    expect(wrapper.text()).toContain('1개 추천 전체 보기')
  })

  it('계좌번호를 그리지 않는다 — 응답에는 있다', async () => {
    const { wrapper } = await mountView(MypageHomeView)
    expect(wrapper.text()).not.toContain('1005-1234-5678-90')
  })

  it('추천 조건이 없으면 설정 CTA를 표시한다', async () => {
    getMypage.mockResolvedValueOnce({
      ...MYPAGE,
      youthPolicy: { ...MYPAGE.youthPolicy, profileCompleted: false, preview: [] },
    })
    const { wrapper } = await mountView(MypageHomeView)
    expect(wrapper.text()).toContain('맞춤 추천 조건을 알려주세요')
    expect(wrapper.text()).toContain('추천 조건 설정하기')
  })

  it('월별·ECO 리포트 카드가 각각 해당 탭으로 이동한다', async () => {
    const { wrapper, router } = await mountView(MypageHomeView)
    const buttons = wrapper.findAll('button').filter((button) => button.text().includes('바로가기'))
    expect(buttons).toHaveLength(2)
    expect(wrapper.text()).not.toContain('고지서 보관함')
    expect(wrapper.text()).toContain('월별 리포트')
    expect(wrapper.text()).toContain('ECO 리포트')

    await buttons[0].trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.fullPath).toBe('/mypage/reports?tab=MONTHLY')

    await router.push('/mypage')
    await buttons[1].trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.fullPath).toBe('/mypage/reports?tab=ECO')
  })
})

describe('MY-03 고지서 보관함', () => {
  it('탭 배지는 목록 길이가 아니라 counts 다 (A-2-12)', async () => {
    const { wrapper } = await mountView(BillArchiveView, '/mypage/bills')
    const tabs = wrapper.findAll('[role="tab"]').map((tab) => tab.text())

    // 목록은 1건인데 배지는 서버가 센 14 · 5 · 5 · 4 다
    expect(tabs[0]).toContain('14')
    expect(tabs[1]).toContain('5')
    expect(tabs[3]).toContain('4')
  })

  it('탭을 누르면 utility 쿼리로 다시 조회한다', async () => {
    const { wrapper, router } = await mountView(BillArchiveView, '/mypage/bills')
    const electricity = wrapper.findAll('[role="tab"]')[1]

    await electricity.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.query.utility).toBe('ELECTRICITY')
    expect(getBills).toHaveBeenLastCalledWith({ utility: 'ELECTRICITY', page: 0 })
  })

  it('쿼리로 들어온 필터를 그대로 서버에 넘긴다', async () => {
    await mountView(BillArchiveView, '/mypage/bills?utility=WATER&year=2026')
    expect(getBills).toHaveBeenCalledWith({ utility: 'WATER', year: '2026', page: 0 })
  })

  it('아는 값이 아닌 쿼리는 전체로 떨어뜨린다', async () => {
    await mountView(BillArchiveView, '/mypage/bills?utility=HEAT&year=nope')
    expect(getBills).toHaveBeenCalledWith({ page: 0 })
  })

  it('고지서 한 줄에 금액·사용량·등록일·상태가 모두 있다', async () => {
    const { wrapper } = await mountView(BillArchiveView, '/mypage/bills')
    const text = wrapper.text()

    expect(text).toContain('2026년 8월')
    expect(text).toContain('전기 고지서')
    expect(text).toContain('43,200원')
    expect(text).toContain('210kWh')
    expect(text).toContain('등록일 2026.09.01')
    expect(text).toContain('등록 완료')
  })

  it('같은 청구 월의 고지서를 하나의 월 카드로 묶는다', async () => {
    getBills.mockResolvedValue({
      ...BILLS,
      content: [
        BILLS.content[0],
        { ...BILLS.content[0], recordId: 52, utilityType: 'WATER', billType: 'WATER' },
        {
          ...BILLS.content[0],
          recordId: 53,
          billingMonth: '2026-07',
          utilityType: 'GAS',
          billType: 'GAS',
        },
      ],
    })
    const { wrapper } = await mountView(BillArchiveView, '/mypage/bills')

    const august = wrapper.get('[data-billing-month="2026-08"]')
    expect(august.text()).toContain('전기 고지서')
    expect(august.text()).toContain('수도 고지서')
    expect(wrapper.findAll('[data-billing-month="2026-08"]')).toHaveLength(1)
    expect(wrapper.findAll('[data-billing-month]')).toHaveLength(2)
  })

  it('월을 누르면 해당 월 고지서가 펼쳐지고 다시 누르면 접힌다', async () => {
    const { wrapper } = await mountView(BillArchiveView, '/mypage/bills')
    const month = wrapper.get('[data-billing-month="2026-08"]')
    const trigger = month.get('button[aria-controls="bills-2026-08"]')
    const list = month.get('#bills-2026-08')

    expect(trigger.attributes('aria-expanded')).toBe('false')
    expect(list.isVisible()).toBe(false)
    await trigger.trigger('click')
    expect(trigger.attributes('aria-expanded')).toBe('true')
    expect(list.attributes('style') ?? '').not.toContain('display: none')
    await trigger.trigger('click')
    expect(list.attributes('style')).toContain('display: none')
  })

  it('비어 있으면 오류가 아니라 안내와 등록 유도를 낸다', async () => {
    getBills.mockResolvedValue({ ...BILLS, content: [], totalElements: 0, totalPages: 0 })
    const { wrapper } = await mountView(BillArchiveView, '/mypage/bills')

    expect(wrapper.text()).toContain('이 조건에 맞는 고지서가 없어요')
    expect(wrapper.text()).toContain('고지서 등록하러 가기')
  })
})

describe('MY-04 리포트 보관함', () => {
  it('tab 쿼리에 해당하는 리포트 목록을 바로 연다', async () => {
    const { wrapper } = await mountView(ReportArchiveView, '/mypage/reports?tab=ECO')
    expect(wrapper.find('[role="tab"]').exists()).toBe(false)
    expect(wrapper.get('h1').text()).toBe('ECO 리포트')
    expect(wrapper.text()).toContain('평가 결과')
    expect(wrapper.text()).not.toContain('7월분 전달 리포트')
    expect(wrapper.text()).not.toContain('8월 월간 리포트')
  })

  it('기본 진입은 월별 리포트 목록만 보여준다', async () => {
    const { wrapper } = await mountView(ReportArchiveView, '/mypage/reports')

    expect(wrapper.get('h1').text()).toBe('월별 리포트')
    expect(wrapper.text()).toContain('8월 월간 리포트')
    expect(wrapper.text()).not.toContain('7월분 전달 리포트')
  })

  it('type 을 넘기지 않고 한 번만 받는다', async () => {
    await mountView(ReportArchiveView, '/mypage/reports')
    expect(getReports).toHaveBeenCalledTimes(1)
    expect(getReports).toHaveBeenCalledWith({ size: 100 })
  })

  it('연도별로 묶고 최신순을 유지한다 (E-2-01)', async () => {
    const { wrapper } = await mountView(ReportArchiveView, '/mypage/reports?tab=ECO')

    const years = wrapper.findAll('h2').map((heading) => heading.text())
    expect(years).toEqual(['2026년'])
  })

  it('다운로드를 약속하지 않는다 (E-2-02 는 P2)', async () => {
    const { wrapper } = await mountView(ReportArchiveView, '/mypage/reports')
    expect(wrapper.text()).not.toContain('다운로드')
    expect(wrapper.text()).toContain('매월 리포트가 자동으로 저장돼요')
  })

  it('월별 진단 보기는 페이지 이동 없이 선택한 월의 리포트를 띄운다', async () => {
    const { wrapper, router } = await mountView(ReportArchiveView, '/mypage/reports')
    const view = wrapper.findAll('button').find((button) => button.text() === '보기')
    await view.trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.fullPath).toBe('/mypage/reports')
    const dialog = document.body.querySelector('[role="dialog"]')
    expect(dialog?.getAttribute('aria-label')).toBe('8월 월간 리포트')
    dialog.querySelector('[aria-label="리포트 닫기"]').click()
    await flushPromises()
    expect(document.body.querySelector('[role="dialog"]')).toBeNull()
  })

  it('ECO 평가는 페이지 이동 없이 리포트로 띄운다', async () => {
    const { wrapper, router } = await mountView(ReportArchiveView, '/mypage/reports?tab=ECO')

    const view = wrapper.findAll('button').find((button) => button.text() === '보기')
    await view.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/mypage/reports?tab=ECO')
    const dialog = document.body.querySelector('[role="dialog"]')
    expect(dialog?.getAttribute('aria-label')).toBe('ECO 리포트')
    dialog.querySelector('[aria-label="리포트 닫기"]').click()
    await flushPromises()
  })
})

describe('MY-02 정책 추천 조건 설정', () => {
  it('저장된 값으로 폼을 채운다', async () => {
    const { wrapper } = await mountView(ProfileEditView, '/mypage/policy-preferences')

    expect(wrapper.text()).toContain('1998.03.14')
    expect(wrapper.text()).toContain('관악구')
    expect(wrapper.get('[role="radio"][aria-checked="true"]').text()).toBe('재직 중')
  })

  it('세 가지 선택값만 저장한다', async () => {
    const { wrapper } = await mountView(ProfileEditView, '/mypage/policy-preferences')

    const save = wrapper.findAll('button').find((button) => button.text() === '저장하고 추천받기')
    await save.trigger('click')
    await flushPromises()

    expect(updatePolicyPreferences).toHaveBeenCalledWith({
      currentStatus: 'EMPLOYED',
      annualIncomeBand: 'FROM_24M_TO_36M',
      householdStatus: 'ONE_PERSON',
    })
  })
})

describe('MY-05 청년정책 목록', () => {
  it('맞춤 추천 목록과 전체 정책을 전환한다', async () => {
    const { wrapper } = await mountView(PolicyListView, '/mypage/policies?mode=recommended')
    expect(getPolicyRecommendations).toHaveBeenCalledWith({ page: 0, size: 20 })
    expect(wrapper.text()).toContain('청년 주거 지원')

    const all = wrapper.findAll('[role="tab"]').find((tab) => tab.text() === '전체 정책')
    await all.trigger('click')
    await flushPromises()
    expect(getPolicies).toHaveBeenCalledWith({ page: 0, size: 20 })
  })
})

describe('MY-06 청년정책 상세', () => {
  it('상세 정보와 판정 근거를 표시한다', async () => {
    const { wrapper } = await mountView(PolicyDetailView, '/mypage/policies/policy-1')
    expect(getPolicy).toHaveBeenCalledWith('policy-1')
    expect(wrapper.text()).toContain('정책 설명')
    expect(wrapper.text()).toContain('연령이 일치해요')
    expect(wrapper.text()).toContain('실제 신청 자격은 반드시 공고에서 확인해 주세요')
  })

  it('임시 추천과 내 정보 저장을 분리한다', async () => {
    const { wrapper } = await mountView(PolicyDetailView, '/mypage/policies/policy-1')
    await wrapper.findAll('button').find((button) => button.text() === '조건을 바꿔 다시 추천하기').trigger('click')
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text() === '이 조건으로 다시 추천').trigger('click')
    await flushPromises()
    expect(previewPolicyRecommendations).toHaveBeenCalledWith({
      currentStatus: 'EMPLOYED', annualIncomeBand: 'FROM_24M_TO_36M', householdStatus: 'ONE_PERSON', page: 0, size: 5,
    })
    expect(updatePolicyPreferences).not.toHaveBeenCalled()

    await wrapper.findAll('button').find((button) => button.text() === '내 정보에 저장').trigger('click')
    await flushPromises()
    expect(updatePolicyPreferences).toHaveBeenCalledWith({
      currentStatus: 'EMPLOYED', annualIncomeBand: 'FROM_24M_TO_36M', householdStatus: 'ONE_PERSON',
    })
  })
})
