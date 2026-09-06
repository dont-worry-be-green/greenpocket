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
import routes from '@/router/routes/eco'
import WhatIfHomeView from '@/views/eco/WhatIfHomeView.vue'

async function mountHome(search) {
  window.history.replaceState({}, '', `/whatif${search}`)
  const router = createRouter({ history: createWebHistory(), routes })
  await router.push(`/whatif${search}`)
  await router.isReady()

  const errors = []
  const spy = vi.spyOn(console, 'error').mockImplementation((...args) => errors.push(String(args[0])))
  const warn = vi.spyOn(console, 'warn').mockImplementation((...args) => errors.push(String(args[0])))

  const wrapper = mount(WhatIfHomeView, { global: { plugins: [createPinia(), router] } })
  await flushPromises()
  await new Promise((resolve) => setTimeout(resolve, 500))
  await flushPromises()

  spy.mockRestore()
  warn.mockRestore()
  // eco 라우트만 등록해서 나는 하네스 경고다 (/mypage 링크)
  return { wrapper, errors: errors.filter((e) => !e.includes('VUE_ROUTER_R0004')) }
}

describe('WhatIfHomeView', () => {
  beforeEach(() => setDataSource(DATA_SOURCE.FIXTURE))
  afterEach(() => setDataSource(DATA_SOURCE.API))

  it('?preview=WF_09_RESULT_READY — 첫 렌더에서 터지지 않고 결산 모달이 뜬다', async () => {
    const { wrapper, errors } = await mountHome('?preview=WF_09_RESULT_READY')
    expect(errors).toEqual([])
    expect(document.body.textContent).toContain('평가 결과가 나왔어요')
    expect(document.body.textContent).toContain('30,000M')
    wrapper.unmount()
  })

  /*
   * WF-06 시안 구조 고정 (B-4-03 · B-4-04 · B-4-06).
   *
   * 앞서 진행 카드를 세로 목록으로 그려 두었다가 명세의 「구간 계단 3칸」과 어긋난 적이 있다.
   * 모양이 아니라 **명세가 못 박은 요소**만 짚는다 — 클래스 이름은 검사하지 않는다.
   */
  it('구간 계단이 3칸이고 지금·목표가 한 칸씩 붙는다', async () => {
    const { wrapper, errors } = await mountHome('?preview=WF_06_IN_PROGRESS')
    expect(errors).toEqual([])

    const cells = wrapper.findAll('[aria-label="마일리지 구간"] li')
    expect(cells).toHaveLength(3)
    expect(cells.map((cell) => cell.text().includes('M'))).toEqual([true, true, true])

    const text = wrapper.text()
    expect(text).toContain('지금')
    expect(text).toContain('목표')
    // 시차 규칙(핵심 규칙 10) 캡션이 빠지면 화면 숫자와 누리집 값이 다른 이유가 사라진다
    expect(text).toContain('검침 확정분은 2~3개월 뒤에 반영돼요')
    wrapper.unmount()
  })

  it('전달 리포트의 목표 대비는 문장이 아니라 배지다', async () => {
    const { wrapper } = await mountHome('?preview=WF_06_IN_PROGRESS')
    const text = wrapper.text()
    expect(text).toMatch(/목표 [\d.]+% 미달|목표 달성/)
    // 배지로 바꾸기 전의 긴 문장이 되살아나면 여기서 걸린다
    expect(text).not.toContain('이 달만 보면')
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
})
