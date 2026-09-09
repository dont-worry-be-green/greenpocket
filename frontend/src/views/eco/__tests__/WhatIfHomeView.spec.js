/*
 * 홈이 **첫 렌더에서 터지지 않는지** 본다.
 *
 * `fetchHome()` 은 `onMounted` 에서 부르는데 첫 렌더는 그보다 먼저다. 그 한 틱 동안
 * `home` 이 null 인 채로 본문이 그려지면 흰 화면이 된다. `?preview=` 로 들어오면 화면이
 * 이미 정해져 있어서 반드시 그 경로를 타므로, 로딩 가드가 무너지면 여기서 잡힌다.
 *
 * ⚠️ **목데이터 모드를 세우고 돈다.** 기본값은 실 API 라, 그대로 두면 서버가 없는 이 환경에서
 * 화면이 에러만 그려 로딩 가드를 검사할 수 없다. 모드 스위치는 `api/dataSource.js` 하나다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'

import { DATA_SOURCE, setDataSource } from '@/api/dataSource'
import ecoRoutes from '@/router/routes/eco'
import WhatIfHomeView from '@/views/eco/WhatIfHomeView.vue'

async function mountHome(search, { diagnosisEntry = false } = {}) {
  const path = diagnosisEntry ? '/analysis/eco-link' : '/whatif'
  window.history.replaceState({}, '', `${path}${search}`)
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      ...ecoRoutes,
      {
        path: '/analysis/eco-link',
        component: WhatIfHomeView,
        props: { diagnosisEntry: true },
      },
      { path: '/analysis', component: { template: '<div />' } },
    ],
  })
  await router.push(`${path}${search}`)
  await router.isReady()

  const errors = []
  const spy = vi
    .spyOn(console, 'error')
    .mockImplementation((...args) => errors.push(String(args[0])))
  const warn = vi
    .spyOn(console, 'warn')
    .mockImplementation((...args) => errors.push(String(args[0])))

  const wrapper = mount(WhatIfHomeView, {
    props: { diagnosisEntry },
    global: { plugins: [createPinia(), router], stubs: { Teleport: true } },
  })
  await flushPromises()
  await new Promise((resolve) => setTimeout(resolve, 500))
  await flushPromises()

  spy.mockRestore()
  warn.mockRestore()
  // eco 라우트만 등록해서 나는 하네스 경고다 (/mypage 링크)
  return { wrapper, router, errors: errors.filter((e) => !e.includes('VUE_ROUTER_R0004')) }
}

describe('WhatIfHomeView', () => {
  beforeEach(() => setDataSource(DATA_SOURCE.FIXTURE))
  afterEach(() => setDataSource(DATA_SOURCE.API))

  it('?preview=WF_09_RESULT_READY — 첫 렌더에서 터지지 않고 결산 모달이 뜬다', async () => {
    const { wrapper, errors } = await mountHome('?preview=WF_09_RESULT_READY')
    expect(errors).toEqual([])
    expect(wrapper.text()).toContain('평가 결과가 나왔어요')
    expect(wrapper.text()).toContain('30,000M')
    wrapper.unmount()
  })

  it('결과 보러 가기를 누르면 2026년 4~9월 평가 결과 화면으로 이동한다', async () => {
    const { wrapper, router } = await mountHome('?preview=WF_09_RESULT_READY')
    const modal = wrapper.findComponent({ name: 'EcoResultModal' })
    expect(modal.exists()).toBe(true)
    const push = vi.spyOn(router, 'push')

    modal.vm.$emit('view')
    await flushPromises()

    expect(push).toHaveBeenCalledWith({
      path: '/mypage/reports',
      query: { tab: 'ECO', report: 'DEMO_ECO_RESULT:2026-04-09' },
    })
    wrapper.unmount()
  })

  /*
   * WF-06 확정 시안 구조 고정 (design-system.md 9-2 · B-4-03).
   *
   * 구간 바는 지급 구간과 같은 4칸이고 현재 칸·목표 칸이 표시된다. 모양이 아니라
   * **명세와 시안이 못 박은 요소**만 짚는다 — 클래스 이름은 검사하지 않는다.
   */
  it('구간 바가 지급 구간 4칸이고 지금·목표가 표시된다', async () => {
    const { wrapper, errors } = await mountHome('?preview=WF_06_IN_PROGRESS')
    expect(errors).toEqual([])

    const cells = wrapper.findAll('[aria-label="마일리지 구간"] li')
    expect(cells).toHaveLength(4)
    expect(cells.map((cell) => cell.text().includes('M'))).toEqual([true, true, true, true])

    const text = wrapper.text()
    expect(text).toContain('지금')
    expect(text).toContain('목표')
    // 누적 감축률은 있는 값을 있는 이름으로 부른다 — 「평균」이 아니다
    expect(text).toContain('누적 감축률')
    // 시차 규칙(핵심 규칙 10) 캡션이 빠지면 화면 숫자와 누리집 값이 다른 이유가 사라진다
    expect(text).toContain('검침 확정분은 2~3개월 뒤에 반영돼요')
    wrapper.unmount()
  })

  it('헤더가 오늘 남은 실천 수를 서버 값으로 말한다', async () => {
    const { wrapper } = await mountHome('?preview=WF_06_IN_PROGRESS')
    // 픽스처 completedCount 3 · totalCount 5
    expect(wrapper.text()).toContain('2개가 남았어요')
    wrapper.unmount()
  })

  /*
   * 예상 마일리지는 **색만으로 구분하지 않는다**(COM-06 · 핵심 규칙 2).
   * 시안에는 이 라벨이 없어서 시안을 그대로 옮기면 조용히 사라진다.
   */
  it('마일리지가 보이는 카드에는 예상 라벨이 함께 있다', async () => {
    const { wrapper } = await mountHome('?preview=WF_06_IN_PROGRESS')
    expect(wrapper.text()).toContain('예상')
    wrapper.unmount()
  })

  it('preview 없이도 터지지 않는다', async () => {
    const { wrapper, errors } = await mountHome('')
    expect(errors).toEqual([])
    wrapper.unmount()
  })

  it.each(['WF_01_UNLINKED', 'WF_02_LINKING'])(
    'What-if에서 %s 상태는 진단 연동 화면으로 보낸다',
    async (preview) => {
      const { wrapper, router } = await mountHome(`?preview=${preview}`)
      expect(router.currentRoute.value.path).toBe('/analysis/eco-link')
      wrapper.unmount()
    },
  )

  it('진단 연동 화면에서 What-if 탭으로 이동해도 연동 전이면 진단으로 되돌린다', async () => {
    const { wrapper, router } = await mountHome('?preview=WF_01_UNLINKED', {
      diagnosisEntry: true,
    })

    await router.push('/whatif?preview=WF_01_UNLINKED')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/analysis/eco-link')
    wrapper.unmount()
  })

  it('진단의 연동 전 화면은 진단 제목과 세 가지 불러올 데이터를 보여준다', async () => {
    const { wrapper, errors } = await mountHome('?preview=WF_01_UNLINKED', {
      diagnosisEntry: true,
    })
    expect(errors).toEqual([])

    const text = wrapper.text()
    expect(text).toContain('진단')
    expect(text).toContain('진단을 위한 에코마일리지')
    expect(text).toContain('연동부터 진행할게요')
    expect(text).toContain('요금 종류')
    expect(text).toContain('2년 사용량')
    expect(text).toContain('등록 주소')
    expect(text).not.toContain('얼마나 줄일지 정해볼까요')
    wrapper.unmount()
  })

  it('진단 연동 흐름의 기준 사용량 화면에서 고지서 등록으로 이동한다', async () => {
    const { wrapper, router, errors } = await mountHome('?preview=WF_03_NO_GOAL', {
      diagnosisEntry: true,
    })
    expect(errors).toEqual([])

    const button = wrapper.findAll('button').find((item) => item.text().includes('고지서 등록하기'))
    expect(button).toBeTruthy()
    await button.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/analysis')
    wrapper.unmount()
  })
})
