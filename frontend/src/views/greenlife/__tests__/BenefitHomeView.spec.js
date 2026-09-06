/*
 * 혜택 탭 홈이 **첫 렌더에서 터지지 않는지**, 그리고 **두 상태를 서버 판정대로 그리는지** 본다.
 *
 * `fetchStatus()` 는 `onMounted` 에서 부르는데 첫 렌더는 그보다 먼저다. 그 한 틱 동안
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
  featuredItems: [{ itemId: 1, name: '전자영수증', unitPrice: 10, rewardUnit: '건', iconKey: 'receipt' }],
}

const ITEMS = {
  month: '2026-08',
  standardYear: 2026,
  items: [
    // monthAmount = monthCount × unitPrice (상한 적용). 행에 뜨는 것은 이 값이다
    { itemId: 1, name: '전자영수증', unitPrice: 10, rewardUnit: '건', iconKey: 'receipt', monthCount: 24, monthAmount: 240 },
    { itemId: 7, name: '친환경제품 구매', unitPrice: 500, rewardUnit: '건', iconKey: 'eco', monthCount: 0, monthAmount: 0 },
  ],
  totalCount: 17,
  collapsedAfter: 6,
}

async function mountHome() {
  const router = createRouter({ history: createWebHistory(), routes })
  await router.push('/benefit')
  await router.isReady()

  const errors = []
  const spy = vi.spyOn(console, 'error').mockImplementation((...args) => errors.push(String(args[0])))
  const warn = vi.spyOn(console, 'warn').mockImplementation((...args) => errors.push(String(args[0])))

  const wrapper = mount(BenefitHomeView, { global: { plugins: [createPinia(), router] } })
  await flushPromises()
  await flushPromises()

  spy.mockRestore()
  warn.mockRestore()
  return { wrapper, errors }
}

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
    expect(text).toContain('2026년 8월 실천 현황')
    // 적립 예정과 지급 완료는 색이 아니라 라벨로도 갈린다 (COM-06)
    expect(text).toContain('적립 예정')
    expect(text).toContain('5,540원')
    expect(text).toContain('지급 완료')
    expect(text).toContain('3,140원')
    expect(text).toContain('18,600원 / 70,000원')
    expect(text).toContain('전자영수증')
    // 행에 뜨는 것은 건수가 아니라 이번 달 금액이다 (핵심 규칙 1 — 원화 우선)
    expect(text).toContain('240원')
    // 실적이 없어도 17개를 전부 내려준다. 빈칸이 아니라 0원이다 (C-2-03)
    expect(text).toContain('친환경제품 구매')
    expect(text).toContain('0원')
    wrapper.unmount()
  })

  it('BN-01 — 참여 방법과 대표 항목을 그리고 목록 API 는 부르지 않는다', async () => {
    getGreenlifeStatus.mockResolvedValue(NOT_PARTICIPATING)
    const { wrapper, errors } = await mountHome()

    expect(errors).toEqual([])
    const text = wrapper.text()
    expect(text).toContain('탄소중립포인트 녹색생활실천')
    expect(text).toContain('17가지 실천 · 연간 최대')
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
