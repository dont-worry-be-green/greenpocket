import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'

import PocketHomeView from '@/views/pocket/PocketHomeView.vue'
import PocketProductDetailView from '@/views/pocket/PocketProductDetailView.vue'

const pocketApi = vi.hoisted(() => ({
  completeMileageConversion: vi.fn(),
  createWithdrawalAccount: vi.fn(),
  getConvertibleMileage: vi.fn(),
  getPocketBalance: vi.fn(),
  getPocketHome: vi.fn(),
  getPocketManagement: vi.fn(),
  getRecommendedPocketProduct: vi.fn(),
  getPocketTransactions: vi.fn(),
  getWithdrawalAccounts: vi.fn(),
  getWithdrawals: vi.fn(),
  requestWithdrawal: vi.fn(),
  setDefaultWithdrawalAccount: vi.fn(),
  startMileageConversion: vi.fn(),
}))

vi.mock('@/api/pocket', () => pocketApi)

const PRODUCT = {
  productCode: 'DP01000942',
  name: 'KB맑은하늘적금',
  tagline: '맑은하늘 만들고 금리도 Up',
  recommendation: {
    badge: '그린포켓 추천',
    title: '친환경 실천과 가장 잘 어울리는 적금',
    description: '맑은하늘을 위한 생활 속 작은 실천에 우대금리를 제공해요.',
  },
  productType: '자유적립식',
  monthlyDeposit: { minimumAmount: 10000, maximumAmount: 1000000 },
  contractTermsMonths: [12, 24, 36],
  preferentialMissions: ['종이통장 줄이기', '비대면 가입', '대중교통 이용', '미세먼지 퀴즈'],
  informationBaseDate: '2026-08-26',
  applicationUrl: 'https://obank.kbstar.com/product',
  notice: '금리와 우대 조건은 가입 시점에 KB국민은행에서 확인해 주세요.',
}

async function mountView(component, path) {
  window.history.replaceState({}, '', path)
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/pocket', component: PocketHomeView },
      { path: '/pocket/recommended-product', component: PocketProductDetailView },
      { path: '/pocket/transactions', component: { template: '<div />' } },
      { path: '/pocket/management', component: { template: '<div />' } },
      { path: '/pocket/withdraw', component: { template: '<div />' } },
    ],
  })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(component, { global: { plugins: [pinia, router] } })
  await flushPromises()
  await flushPromises()
  return { wrapper, router }
}

beforeEach(() => {
  vi.clearAllMocks()
  pocketApi.getPocketHome.mockResolvedValue({ balance: 64000, convertibleMileage: 0 })
  pocketApi.getPocketTransactions.mockResolvedValue({ groups: [] })
  pocketApi.getWithdrawals.mockResolvedValue({ content: [] })
  pocketApi.getRecommendedPocketProduct.mockResolvedValue(PRODUCT)
})

describe('D-4-01 KB 금융상품 추천', () => {
  it('포켓 홈에서 추천 상품을 보여주고 상품 상세로 이동한다', async () => {
    const { wrapper, router } = await mountView(PocketHomeView, '/pocket')

    expect(wrapper.text()).not.toContain('친환경 실천과 가장 잘 어울리는 적금')
    expect(wrapper.find('[aria-label="추천 금융 상품"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('KB맑은하늘적금')
    const viewButton = wrapper.findAll('button').find((button) => button.text() === '상품 보기')
    expect(viewButton).toBeTruthy()
    await viewButton.trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/pocket/recommended-product')
  })

  it('상품 상세에 API 조건을 표시하고 신청 버튼으로 외부 페이지를 연다', async () => {
    const open = vi.spyOn(window, 'open').mockImplementation(() => null)
    const { wrapper } = await mountView(
      PocketProductDetailView,
      '/pocket/recommended-product',
    )
    const text = wrapper.text()

    expect(text).toContain('KB맑은하늘적금')
    expect(text).not.toContain('그린포켓 추천')
    expect(text).toContain('자유적립식')
    expect(text).toContain('월 1만원 ~ 1백만원 (원단위)')
    expect(text).toContain('1년 · 2년 · 3년')
    expect(text).toContain('종이통장 줄이기')
    expect(text).not.toContain('친환경 실천과 가장 잘 어울리는 적금')
    expect(text).toContain('금리 및 우대 이율은 가입 시점에 KB국민은행에서 확인해 주세요.')
    expect(text).toContain('정보 기준일 2026-08-26')

    const applyButton = wrapper
      .findAll('button')
      .find((button) => button.text() === 'KB 상품 신청 페이지로 이동')
    expect(applyButton).toBeTruthy()
    await applyButton.trigger('click')
    expect(open).toHaveBeenCalledWith(PRODUCT.applicationUrl, '_blank', 'noopener')
  })
})
