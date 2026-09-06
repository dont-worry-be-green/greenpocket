/*
 * 마이페이지 네 화면이 **첫 렌더에서 터지지 않는지**, 그리고 명세가 못 박은 것들이 실제로
 * 지켜지는지 본다. API 모듈을 여기서 막는다 — 응답 모양은 **api-spec.md 5.2 · 6.6 · 14.1 · 14.2
 * 예시 그대로**다. 임의 필드를 만들면 계약이 갈라진다.
 *
 * 특히 지키려는 것 넷:
 *   ① MY-01 에 나이·소득 구간·취업 상태가 **없다**(결정 B-1). 시안에는 있어서 되살아나기 쉽다
 *   ② 계좌번호를 그리지 않는다 — 응답에는 있다
 *   ③ MY-03 탭 배지가 목록 길이가 아니라 `counts` 다(A-2-12 완료 조건)
 *   ④ MY-02 는 지역 변경 경고를 확인받고 나서야 `confirmBaselineChange` 를 붙인다(핵심 규칙 9)
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'

import { ApiError } from '@/api/client'
import routes from '@/router/routes/mypage'
import BillArchiveView from '@/views/mypage/BillArchiveView.vue'
import MypageHomeView from '@/views/mypage/MypageHomeView.vue'
import ProfileEditView from '@/views/mypage/ProfileEditView.vue'
import ReportArchiveView from '@/views/mypage/ReportArchiveView.vue'

const { getMypage, getProfile, updateProfile, getBills, getReports } = vi.hoisted(() => ({
  getMypage: vi.fn(),
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
  getBills: vi.fn(),
  getReports: vi.fn(),
}))

vi.mock('@/api/mypage', () => ({ getMypage, getProfile, updateProfile, getBills, getReports }))

/** MY-02 는 지역 목록을 온보딩 스토어에서 가져온다(A-1-06 "ONB-02 폼 재사용") */
const { getRegions } = vi.hoisted(() => ({ getRegions: vi.fn() }))
vi.mock('@/api/onboarding', () => ({
  getRegions,
  startUser: vi.fn(),
  saveProfile: vi.fn(),
}))

const MYPAGE = {
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
    reportArchive: { count: 9, screen: 'MY-04' },
  },
  ecoAddress: {
    label: '서울 관악구',
    registeredAt: '2026-03',
    matchesProfile: true,
    notice: '이사했다면 꼭 바꿔주세요.',
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

const PROFILE = {
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
  getProfile.mockResolvedValue(PROFILE)
  getBills.mockResolvedValue(BILLS)
  getReports.mockResolvedValue(REPORTS)
  getRegions.mockImplementation(({ sidoCode } = {}) =>
    Promise.resolve(
      sidoCode
        ? { level: 'SIGUNGU', items: [{ code: '11620', name: '관악구', sidoCode: '11' }] }
        : { level: 'SIDO', items: [{ code: '11', name: '서울특별시' }] },
    ),
  )
})

describe('MY-01 마이페이지 메인', () => {
  it('프로필 요약과 기본 정보를 그린다', async () => {
    const { wrapper } = await mountView(MypageHomeView)
    const text = wrapper.text()

    expect(text).toContain('김수현')
    expect(text).toContain('서울 관악구 · 원룸 · 10평 이하')
    expect(text).toContain('서울특별시 관악구')
    // 주거 형태와 평수는 시안대로 한 행이다
    expect(text).toContain('원룸 · 10평 이하')
  })

  it('나이·소득 구간·취업 상태 행이 없다 (결정 B-1)', async () => {
    const { wrapper } = await mountView(MypageHomeView)
    const text = wrapper.text()

    expect(text).not.toContain('나이')
    expect(text).not.toContain('소득')
    expect(text).not.toContain('취업')
  })

  it('계좌번호를 그리지 않는다 — 응답에는 있다', async () => {
    const { wrapper } = await mountView(MypageHomeView)
    expect(wrapper.text()).not.toContain('1005-1234-5678-90')
  })

  it('에코마일리지 등록 주소를 프로필 주소와 구분해 보여준다 (E-1-01 완료 조건)', async () => {
    const { wrapper } = await mountView(MypageHomeView)
    const text = wrapper.text()

    expect(text).toContain('에코마일리지 등록 주소')
    expect(text).toContain('2026년 3월 등록')
    // 일치하는 상태에서는 이사 안내를 띄우지 않는다
    expect(text).not.toContain('이사했다면 꼭 바꿔주세요')
  })

  it('프로필 주소와 시군구가 다르면 이사 안내를 띄운다 (B-1-08)', async () => {
    getMypage.mockResolvedValue({
      ...MYPAGE,
      ecoAddress: { ...MYPAGE.ecoAddress, matchesProfile: false },
    })
    const { wrapper } = await mountView(MypageHomeView)
    expect(wrapper.text()).toContain('이사했다면 꼭 바꿔주세요')
  })

  it('미연동이면 등록 주소 카드를 그리지 않는다', async () => {
    getMypage.mockResolvedValue({ ...MYPAGE, ecoAddress: null })
    const { wrapper } = await mountView(MypageHomeView)
    expect(wrapper.text()).not.toContain('에코마일리지 등록 주소')
  })

  it('보관함 카드에서 두 화면으로 이동한다 (E-1-02)', async () => {
    const { wrapper, router } = await mountView(MypageHomeView)
    const buttons = wrapper.findAll('button').filter((button) => button.text().includes('바로가기'))
    expect(buttons).toHaveLength(2)

    await buttons[0].trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/mypage/bills')
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

    expect(text).toContain('2026년 8월 · 전기 고지서')
    expect(text).toContain('43,200원')
    expect(text).toContain('210kWh')
    expect(text).toContain('등록일 2026.09.01')
    expect(text).toContain('등록 완료')
  })

  it('비어 있으면 오류가 아니라 안내와 등록 유도를 낸다', async () => {
    getBills.mockResolvedValue({ ...BILLS, content: [], totalElements: 0, totalPages: 0 })
    const { wrapper } = await mountView(BillArchiveView, '/mypage/bills')

    expect(wrapper.text()).toContain('이 조건에 맞는 고지서가 없어요')
    expect(wrapper.text()).toContain('고지서 등록하러 가기')
  })
})

describe('MY-04 리포트 보관함', () => {
  it('탭 두 개가 타입 셋을 나눠 담는다', async () => {
    const { wrapper } = await mountView(ReportArchiveView, '/mypage/reports')

    // 기본은 월별 리포트 탭
    expect(wrapper.text()).toContain('8월 생활비 진단')
    expect(wrapper.text()).not.toContain('7월분 전달 리포트')

    await wrapper.findAll('[role="tab"]')[1].trigger('click')
    await flushPromises()

    // ECO 탭은 ECO_MONTHLY 와 ECO_RESULT 를 함께 담는다
    expect(wrapper.text()).toContain('7월분 전달 리포트')
    expect(wrapper.text()).toContain('평가 결과')
    expect(wrapper.text()).not.toContain('8월 생활비 진단')
  })

  it('type 을 넘기지 않고 한 번만 받는다', async () => {
    await mountView(ReportArchiveView, '/mypage/reports')
    expect(getReports).toHaveBeenCalledTimes(1)
    expect(getReports).toHaveBeenCalledWith({ size: 100 })
  })

  it('연도별로 묶고 최신순을 유지한다 (E-2-01)', async () => {
    const { wrapper } = await mountView(ReportArchiveView, '/mypage/reports')
    await wrapper.findAll('[role="tab"]')[1].trigger('click')
    await flushPromises()

    const years = wrapper.findAll('h2').map((heading) => heading.text())
    expect(years).toEqual(['2026년'])
  })

  it('다운로드를 약속하지 않는다 (E-2-02 는 P2)', async () => {
    const { wrapper } = await mountView(ReportArchiveView, '/mypage/reports')
    expect(wrapper.text()).not.toContain('다운로드')
    expect(wrapper.text()).toContain('매월 리포트가 자동으로 저장돼요')
  })

  it('targetScreen 이 가리키는 화면으로 이동한다', async () => {
    const { wrapper, router } = await mountView(ReportArchiveView, '/mypage/reports')
    await wrapper.findAll('[role="tab"]')[1].trigger('click')
    await flushPromises()

    const view = wrapper.findAll('button').find((button) => button.text() === '보기')
    await view.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/whatif/report')
    expect(router.currentRoute.value.query.month).toBe('2026-07')
  })
})

describe('MY-02 기본 정보 수정', () => {
  it('저장된 값으로 폼을 채운다', async () => {
    const { wrapper } = await mountView(ProfileEditView, '/mypage/profile')

    expect(wrapper.find('#my-profile-name').element.value).toBe('김수현')
    expect(wrapper.text()).toContain('관악구')
  })

  it('저장하면 name 까지 함께 보낸다 (api-spec 5.3)', async () => {
    updateProfile.mockResolvedValue({
      profileSummary: '서울 관악구 · 원룸 10평 이하',
      baselineRecalculated: false,
      affectedRoundId: null,
    })
    const { wrapper } = await mountView(ProfileEditView, '/mypage/profile')

    const save = wrapper.findAll('button').find((button) => button.text() === '저장하기')
    await save.trigger('click')
    await flushPromises()

    expect(updateProfile).toHaveBeenCalledWith({
      name: '김수현',
      sidoCode: '11',
      sidoName: '서울특별시',
      sigunguCode: '11620',
      sigunguName: '관악구',
      housingType: 'ONE_ROOM',
      areaBand: 'UNDER_10',
    })
  })

  it('지역 변경 경고를 확인받고 나서야 confirmBaselineChange 를 붙인다 (A-1-06 · 핵심 규칙 9)', async () => {
    const warning = '지역을 바꾸면 진행 중 평가의 비교 기준이 바뀌어요'
    updateProfile.mockRejectedValueOnce(
      new ApiError({
        code: 'CONFLICT',
        message: '확인이 필요해요.',
        field: 'confirmBaselineChange',
        details: { warning, affectedRoundId: 7 },
        status: 409,
      }),
    )
    const { wrapper } = await mountView(ProfileEditView, '/mypage/profile')

    const save = wrapper.findAll('button').find((button) => button.text() === '저장하기')
    await save.trigger('click')
    await flushPromises()

    // 첫 요청에는 플래그가 없다 — 앱이 알아서 붙이지 않는다
    expect(updateProfile.mock.calls[0][0].confirmBaselineChange).toBeUndefined()
    expect(document.body.textContent).toContain(warning)

    updateProfile.mockResolvedValue({
      profileSummary: '서울 관악구 · 원룸 10평 이하',
      baselineRecalculated: true,
      affectedRoundId: 7,
    })
    const confirm = [...document.body.querySelectorAll('button')].find(
      (button) => button.textContent.trim() === '그대로 바꾸기',
    )
    confirm.click()
    await flushPromises()

    expect(updateProfile.mock.calls[1][0].confirmBaselineChange).toBe(true)
  })
})
