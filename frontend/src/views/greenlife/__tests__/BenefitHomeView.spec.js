/*
 * 혜택 탭 홈이 **첫 렌더에서 터지지 않는지**, 그리고 **두 상태를 서버 판정대로 그리는지** 본다.
 *
 * 월 이동(#110)도 여기서 지킨다 — 보고 있는 달은 URL 이 들고 있고, **현황과 목록을 같은 달로**
 * 함께 받아야 카드 합계와 목록 합계가 어긋나지 않는다(C-2-01).
 *
 * `fetchStatus()` 는 달을 지켜보는 watch 가 부르는데 첫 렌더는 그보다 먼저다. 그 한 틱 동안
 * `status` 가 null 인 채 본문이 그려지면 흰 화면이 된다(WhatIfHomeView 와 같은 함정).
 *
 * 이 도메인은 픽스처가 없어서(`api/__tests__/greenlife.spec.js`) API 모듈을 여기서 막는다.
 * **응답 모양은 api-spec.md 12.1 · 12.3 예시 그대로다** — 임의 필드를 만들면 계약이 갈라진다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'

import routes from '@/router/routes/greenlife'
import { currentMonth } from '@/utils/month'
import BenefitHomeView from '@/views/greenlife/BenefitHomeView.vue'

const { getGreenlifeStatus, getGreenlifeItems } = vi.hoisted(() => ({
  getGreenlifeStatus: vi.fn(),
  getGreenlifeItems: vi.fn(),
}))

vi.mock('@/api/greenlife', () => ({
  getGreenlifeStatus,
  getGreenlifeItems,
  getGreenlifeItemDetail: vi.fn(),
  linkGreenlife: vi.fn(),
}))

const PARTICIPATING = {
  participating: true,
  screen: 'BN-02',
  linkedAt: '2026-09-01T09:12:00+09:00',
  month: '2026-08',
  monthSummary: { activityCount: 44, pendingAmount: 5540, paidAmount: 3140, paidMonth: '2026-07' },
  annual: { year: 2026, paidAmount: 18600, limitAmount: 70000, progressPercent: 26.6 },
  delayNotice: '실적 반영까지 최소 3일~익월 말이 걸릴 수 있어요',
  standardYear: 2026,
}

const NOT_PARTICIPATING = {
  participating: false,
  screen: 'BN-01',
  linkedAt: null,
  programInfo: {
    name: '탄소중립포인트 녹색생활실천',
    itemCount: 17,
    annualLimit: 70000,
    standardYear: 2026,
    joinSteps: ['공식 누리집에서 회원가입해요', '참여기업 앱에서 실천 항목을 설정해요'],
    externalUrl: 'https://cpoint.or.kr',
  },
  featuredItems: [
    { itemId: 1, name: '전자영수증', unitPrice: 10, rewardUnit: '건', iconKey: 'receipt' },
  ],
}

const ITEMS = {
  month: '2026-08',
  standardYear: 2026,
  items: [
    // monthAmount = monthCount × unitPrice (상한 적용). 행에 뜨는 것은 이 값이다
    {
      itemId: 1,
      name: '전자영수증',
      unitPrice: 10,
      rewardUnit: '건',
      iconKey: 'receipt',
      monthCount: 24,
      monthAmount: 240,
    },
    {
      itemId: 7,
      name: '친환경제품 구매',
      unitPrice: 500,
      rewardUnit: '건',
      iconKey: 'eco',
      monthCount: 0,
      monthAmount: 0,
    },
  ],
  totalCount: 17,
  collapsedAfter: 6,
}

async function mountHome(path = '/benefit') {
  const router = createRouter({ history: createWebHistory(), routes })
  await router.push(path)
  await router.isReady()

  const errors = []
  const spy = vi
    .spyOn(console, 'error')
    .mockImplementation((...args) => errors.push(String(args[0])))
  const warn = vi
    .spyOn(console, 'warn')
    .mockImplementation((...args) => errors.push(String(args[0])))

  const wrapper = mount(BenefitHomeView, { global: { plugins: [createPinia(), router] } })
  await flushPromises()
  await flushPromises()

  spy.mockRestore()
  warn.mockRestore()
  return { wrapper, errors, router }
}

/** aria-label 로 찾는다. 화살표는 아이콘뿐이라 글자로는 못 짚는다 */
const arrow = (wrapper, label) =>
  wrapper.findAll('button').find((node) => node.attributes('aria-label') === label)

describe('BenefitHomeView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getGreenlifeItems.mockResolvedValue(ITEMS)
  })

  it('BN-02 — 월 현황과 항목 목록을 그린다', async () => {
    getGreenlifeStatus.mockResolvedValue(PARTICIPATING)
    const { wrapper, errors } = await mountHome()

    expect(errors).toEqual([])
    const text = wrapper.text()
    expect(text).toContain('8월 적립')
    expect(text).toContain('44건')
    // 적립 예정과 지급 완료는 색이 아니라 라벨로도 갈린다 (COM-06)
    expect(text).toContain('적립 예정')
    expect(text).toContain('5,540원')
    expect(text).toContain('지급 완료')
    expect(text).toContain('3,140원')
    expect(text).toContain('/ 70,000원')
    expect(text).toContain('전자영수증')
    // 행에 뜨는 것은 건수가 아니라 이번 달 금액이다 (핵심 규칙 1 — 원화 우선)
    // 행 오른쪽은 이번 달 건수다(C-2-03 · 결정 C-32). monthCount 24.000 → 24건
    expect(text).toContain('24')
    expect(text).toContain('10')
    // 실적이 없어도 17개를 전부 내려준다. 빈칸이 아니라 0원이다 (C-2-03)
    expect(text).toContain('친환경제품 구매')
    expect(text).toContain('0건')
    wrapper.unmount()
  })

  it('BN-01 — 참여 방법과 대표 항목을 그리고 목록 API 는 부르지 않는다', async () => {
    getGreenlifeStatus.mockResolvedValue(NOT_PARTICIPATING)
    const { wrapper, errors } = await mountHome()

    expect(errors).toEqual([])
    const text = wrapper.text()
    expect(text).toContain('17가지 실천으로')
    expect(text).toContain('70,000원')
    expect(text).toContain('공식 누리집에서 회원가입해요')
    expect(text).toContain('10원/건')
    // 미참여 화면은 대표 항목 4개로 충분하다. 17개는 「전체 보기」를 눌렀을 때만 받는다
    expect(getGreenlifeItems).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('첫 응답이 실패해도 흰 화면을 남기지 않는다', async () => {
    getGreenlifeStatus.mockRejectedValue(new Error('불러오지 못했어요'))
    const { wrapper, errors } = await mountHome()

    expect(errors).toEqual([])
    expect(wrapper.text()).toContain('다시 시도')
    wrapper.unmount()
  })
})

/*
 * 월 이동 (#110).
 *
 * ⚠️ 이건 **기능명세에 없는 기능이다.** C-2-01·C-2-03·C-2-04 가 전부 "이번 달" 고정으로
 * 적혀 있고 시안에도 컨트롤이 없다. 승인되면 엑셀 「결정 사항」 시트에 올려야 한다.
 */
describe('BenefitHomeView — 월 이동', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getGreenlifeItems.mockResolvedValue(ITEMS)
    getGreenlifeStatus.mockResolvedValue(PARTICIPATING)
  })

  /*
   * 둘이 갈리면 카드 합계와 목록 합계가 어긋난다 — C-2-01 완료 조건이다.
   * 쿼리가 없을 때 month 를 빼고 부르지 않는 이유는, 서버 기본값에 기대면 두 요청 사이에
   * 날짜가 바뀌는 경계에서 서로 다른 달을 받을 수 있어서다.
   */
  it('현황과 목록을 같은 달로 함께 받는다', async () => {
    const { wrapper } = await mountHome('/benefit?month=2026-08')

    expect(getGreenlifeStatus).toHaveBeenCalledWith({ month: '2026-08' })
    expect(getGreenlifeItems).toHaveBeenCalledWith({ month: '2026-08' })
    wrapper.unmount()
  })

  it('이전 달을 누르면 URL 이 바뀌고 그 달로 다시 받는다', async () => {
    const { wrapper, router } = await mountHome('/benefit?month=2026-08')

    await arrow(wrapper, '이전 달').trigger('click')
    await vi.waitFor(() => {
      expect(router.currentRoute.value.query.month).toBe('2026-07')
    })
    await flushPromises()

    expect(getGreenlifeStatus).toHaveBeenLastCalledWith({ month: '2026-07' })
    expect(getGreenlifeItems).toHaveBeenLastCalledWith({ month: '2026-07' })
    wrapper.unmount()
  })

  /*
   * 서버는 미래 달도 200 + 0건으로 준다. 막지 않으면 「11월 실천 현황 0건」을 무한히 넘긴다.
   * 아래쪽은 기준 연도 1월 — 연간 한도 카드가 같은 해 기준이라 한 화면 안에서 말이 맞는다.
   */
  it('이번 달에서는 다음 달로 못 간다', async () => {
    const { wrapper } = await mountHome(`/benefit?month=${currentMonth()}`)
    expect(arrow(wrapper, '다음 달').attributes('disabled')).toBeDefined()
    expect(arrow(wrapper, '이전 달').attributes('disabled')).toBeUndefined()
    wrapper.unmount()
  })

  it('기준 연도 1월에서는 이전 달로 못 간다', async () => {
    const { wrapper } = await mountHome('/benefit?month=2026-01')
    expect(arrow(wrapper, '이전 달').attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })

  // 쿼리는 밖에서 들어오는 값이다. 서버는 이런 값에 400 을 준다
  it.each([['bad'], ['2026-13'], ['2099-01']])('%s 는 이번 달로 떨어진다', async (bad) => {
    const { wrapper } = await mountHome(`/benefit?month=${bad}`)
    expect(getGreenlifeStatus).toHaveBeenCalledWith({ month: currentMonth() })
    wrapper.unmount()
  })

  /** 상세도 같은 달을 봐야 한다 — C-2-04 「상세 건수가 목록 건수와 일치한다」 */
  it('항목을 누르면 보던 달을 상세로 들고 간다', async () => {
    const { wrapper, router } = await mountHome('/benefit?month=2026-08')

    const row = wrapper.findAll('button').find((node) => node.text().includes('전자영수증'))
    await row.trigger('click')
    /*
     * 뷰가 `router.push` 를 await 하지 않으므로 내비게이션이 언제 끝나는지 테스트가 알 수 없다.
     * flushPromises 를 몇 번 부를지 세는 대신 조건이 참이 될 때까지 기다린다.
     */
    await vi.waitFor(() => {
      expect(router.currentRoute.value.path).toBe('/benefit/items/1')
    })
    expect(router.currentRoute.value.query.month).toBe('2026-08')
    wrapper.unmount()
  })
})
