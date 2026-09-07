/*
 * WF-07 · WF-10 · WF-11 의 시안 구조를 고정한다.
 *
 * 세 화면 다 「카드가 몇 장이냐」보다 **어떤 순서로 무엇을 말하느냐**가 명세다.
 *   WF-07 — B-4-07 이 ① 결과 → ② 원인 → ③ 처방 순서를 못 박는다. 그래프는 그 뒤다
 *   WF-10 — 확정 근거(핵심 규칙 7·10)와 돈의 3단계 라벨(핵심 규칙 2)이 빠지면 안 된다
 *   WF-11 — B-5-03 이 「확인 라벨」과 「아직 현금이 아니에요」 경고를 문장으로 못 박는다
 *
 * 클래스 이름이나 픽셀은 검사하지 않는다. 시안을 다시 손보다 **문서가 요구한 것**이
 * 조용히 빠지는 경우만 잡는다.
 *
 * ⚠️ **목데이터 모드를 세우고 돈다.** 기본값은 실 API 라 서버가 없는 이 환경에서는
 * 에러 화면만 그려져 검사할 것이 사라진다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHistory, RouterView } from 'vue-router'

import { getBills } from '@/api/mypage'

vi.mock('@/api/mypage', () => ({ getBills: vi.fn() }))
beforeEach(() => getBills.mockResolvedValue({ content: [], hasNext: false }))

import { DATA_SOURCE, setDataSource } from '@/api/dataSource'
import routes from '@/router/routes/eco'

const HARNESS_NOISE = /VUE_ROUTER_R0004|ECONNREFUSED|Cross origin|Error: connect|Network Error/

async function openPath(path) {
  window.history.replaceState({}, '', path)
  const pinia = createPinia()
  setActivePinia(pinia)

  const router = createRouter({ history: createWebHistory(), routes })
  await router.push(path)
  await router.isReady()

  const errors = []
  const collect = (...args) => errors.push(String(args[0]))
  const errorSpy = vi.spyOn(console, 'error').mockImplementation(collect)
  const warnSpy = vi.spyOn(console, 'warn').mockImplementation(collect)

  const wrapper = mount(RouterView, { global: { plugins: [pinia, router] } })
  await flushPromises()
  await new Promise((resolve) => setTimeout(resolve, 600))
  await flushPromises()

  errorSpy.mockRestore()
  warnSpy.mockRestore()
  return { wrapper, errors: errors.filter((message) => !HARNESS_NOISE.test(message)) }
}

/** 카드 제목(GpCard 의 h2)만 순서대로 뽑는다. 화면 제목은 h1 이라 섞이지 않는다 */
const cardTitles = (wrapper) => wrapper.findAll('h2').map((node) => node.text())

describe('WF-07 전달 리포트', () => {
  beforeEach(() => setDataSource(DATA_SOURCE.FIXTURE))
  afterEach(() => setDataSource(DATA_SOURCE.API))

  it('카드가 결과 → 원인 → 처방 → 그래프 순서다 (B-4-07)', async () => {
    const { wrapper, errors } = await openPath('/whatif/report')
    expect(errors).toEqual([])

    const titles = cardTitles(wrapper)
    expect(titles[0]).toContain('감축률')
    expect(titles[1]).toBe('어디가 발목을 잡았나')
    expect(titles[2]).toContain('이렇게 하면 돼요')
    expect(titles[3]).toBe('달마다 얼마나 줄였나')
    wrapper.unmount()
  })

  /*
   * 조정이 필요한 사람이 스크롤 끝까지 내려야 버튼을 찾는 일이 없어야 한다.
   * 문구의 요금 이름은 서버의 `adjustTargetUtility` 에서 온다 — 화면이 고르지 않는다.
   */
  it('실천 다시 고르기가 하단 고정 CTA 로 있다', async () => {
    const { wrapper } = await openPath('/whatif/report')
    const cta = wrapper.findAll('button').filter((node) => node.text().includes('다시 고르기'))
    expect(cta).toHaveLength(1)
    expect(cta[0].text()).toMatch(/^(전기|도시가스|수도) 실천 다시 고르기$/)
    wrapper.unmount()
  })

  it('비교값이 없어도 선택한 달의 요금과 사용량을 표시한다', async () => {
    getBills.mockResolvedValue({ content: [
      { recordId: 1, billingMonth: '2026-08', utilityType: 'ELECTRICITY', amount: 43200, usage: 210, usageUnit: 'kWh' },
      { recordId: 2, billingMonth: '2026-07', utilityType: 'GAS', amount: 99000, usage: 9, usageUnit: 'm3' },
    ], hasNext: false })
    const { wrapper } = await openPath('/whatif/report?month=2026-08')
    expect(wrapper.text()).toContain('43,200원')
    expect(wrapper.text()).toContain('210kWh')
    expect(wrapper.text()).not.toContain('99,000원')
    expect(wrapper.text()).toContain('감축률 —')
    expect(wrapper.text()).not.toContain('아직 올린 고지서가 없어요')
    wrapper.unmount()
  })

  it('고지서 조회 실패는 빈 달로 안내하지 않고 재시도한다', async () => {
    getBills.mockRejectedValueOnce({ message: '고지서 조회 실패' })
    const { wrapper } = await openPath('/whatif/report?month=2026-08')
    expect(wrapper.text()).toContain('고지서 조회 실패')
    expect(wrapper.text()).toContain('다시 시도')
    expect(wrapper.text()).not.toContain('아직 올린 고지서가 없어요')
    wrapper.unmount()
  })

  it('고지서가 없는 달은 에러가 아니라 안내다 (핵심 규칙 8)', async () => {
    const { wrapper, errors } = await openPath('/whatif/report?month=2026-08')
    expect(errors).toEqual([])
    expect(wrapper.text()).toContain('아직 올린 고지서가 없어요')
    wrapper.unmount()
  })
})

describe('WF-10 평가 결과', () => {
  beforeEach(() => setDataSource(DATA_SOURCE.FIXTURE))
  afterEach(() => setDataSource(DATA_SOURCE.API))

  /*
   * 최종 확정은 진단 탭 고지서가 아니라 누리집 기준이다(핵심 규칙 10).
   * 그 사실이 화면 어디에도 없으면 다른 화면 숫자와 다를 때 어느 쪽이 맞는지 알 수 없다.
   */
  it('헤더에 평가 기간·확정일·확정 기준이 함께 있다', async () => {
    const { wrapper, errors } = await openPath('/whatif/rounds/6/result')
    expect(errors).toEqual([])

    const subtitle = wrapper.get('h1').element.parentElement.textContent
    expect(subtitle).toMatch(/\d{4}-\d{2} ~ /)
    expect(subtitle).toContain('확정')
    expect(subtitle).toContain('누리집')
    wrapper.unmount()
  })

  /*
   * 돈의 3단계(핵심 규칙 2). 확정 마일리지에는 `확인` 라벨이 붙고, 아직 현금이 아니라는
   * 것을 문장으로 함께 말한다. 시안에는 라벨이 없어 그대로 옮기면 조용히 사라진다.
   */
  it('적립 마일리지에 확인 라벨과 「아직 현금이 아니에요」가 함께 있다', async () => {
    const { wrapper } = await openPath('/whatif/rounds/6/result')
    const text = wrapper.text()
    expect(text).toContain('확인')
    expect(text).toContain('아직 현금이 아니에요')
    wrapper.unmount()
  })

  it('리포트 화면에는 다음 회차 목표 CTA가 없다', async () => {
    const { wrapper } = await openPath('/whatif/rounds/6/result')
    const cta = wrapper.findAll('button').filter((node) => node.text().includes('목표 정하기'))
    expect(cta).toHaveLength(0)
    wrapper.unmount()
  })

  // 확정되지 않은 회차를 열면 픽스처도 서버와 같은 에러를 낸다 — 화면이 덮어 주지 않는다
  it('진행 중 회차를 열면 확정 전 안내가 뜬다', async () => {
    const { wrapper } = await openPath('/whatif/rounds/7/result')
    expect(wrapper.text()).toContain('아직 평가가 확정되지 않았어요')
    wrapper.unmount()
  })
})

describe('WF-11 마일리지 적립', () => {
  beforeEach(() => setDataSource(DATA_SOURCE.FIXTURE))
  afterEach(() => setDataSource(DATA_SOURCE.API))

  /*
   * B-5-03 이 "30,000M 적립됐어요 + 확인 라벨" 로 못 박는다. 초록 히어로 카드 위에서는
   * GpTag 를 못 써 배지를 손으로 그리는데, 그러다 조용히 빠지기 쉬운 자리다.
   */
  it('적립 금액에 확인 라벨이 함께 있다', async () => {
    const { wrapper, errors } = await openPath('/whatif/rounds/6/settlement')
    expect(errors).toEqual([])

    const text = wrapper.text()
    expect(text).toContain('적립됐어요')
    expect(text).toContain('확인')
    expect(text).toMatch(/[\d,]+M/)
    wrapper.unmount()
  })

  /*
   * 돈의 3단계 중 ②→③ 경계다(핵심 규칙 2). 이 경고가 빠지면 적립 = 입금으로 읽힌다.
   * 「줄인 금액」 옆에 두던 옛 문장을 지우면서 이 자리가 유일한 방어선이 됐다.
   */
  it('아직 현금이 아니라는 경고가 있다', async () => {
    const { wrapper } = await openPath('/whatif/rounds/6/settlement')
    const text = wrapper.text()
    expect(text).toContain('아직 현금이 아니에요')
    expect(text).toContain('그린포켓')
    wrapper.unmount()
  })

  it('계산 근거 세 줄과 비교 기준 문장이 있다', async () => {
    const { wrapper } = await openPath('/whatif/rounds/6/settlement')
    const text = wrapper.text()
    expect(text).toContain('기준 사용량 요금')
    expect(text).toContain('평가 기간 요금')
    expect(text).toContain('줄인 금액')
    // `calculation.note` — 무엇과 비교한 값인지 서버가 문장으로 준다 (핵심 규칙 7)
    expect(text).toContain('직전 2년')
    wrapper.unmount()
  })

  // 「나중에 할래요」를 골라도 포켓 탭에서 전환할 수 있다 (B-5-03 완료 조건)
  it('현금으로 바꾸기와 나중에 할래요가 함께 있다', async () => {
    const { wrapper } = await openPath('/whatif/rounds/6/settlement')
    const labels = wrapper.findAll('button').map((node) => node.text())
    expect(labels).toContain('현금으로 바꾸기')
    expect(labels).toContain('나중에 할래요')
    wrapper.unmount()
  })

  // 헤더가 뒤로가기(←)가 아니라 닫기(X)다 (시안 WF-11)
  it('헤더에 닫기만 있고 뒤로 가기는 없다', async () => {
    const { wrapper } = await openPath('/whatif/rounds/6/settlement')
    const labels = wrapper.findAll('button').map((node) => node.attributes('aria-label'))
    expect(labels).toContain('닫기')
    expect(labels).not.toContain('뒤로 가기')
    wrapper.unmount()
  })
})
