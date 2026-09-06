/*
 * 뒤로가기 우선순위를 잠근다.
 *
 * **`back` prop 은 폴백이지 덮어쓰기가 아니다.** 왔던 곳이 있으면 그리로 돌아가고, `back` 은
 * 히스토리가 없을 때(새로고침 · 딥링크 · 데모 도구 바로가기)만 쓴다.
 *
 * 순서를 거꾸로 두면 **탭을 건너 들어온 화면이 남의 탭으로 나간다** — MY-04 리포트 보관함에서
 * 전달 리포트(WF-07)를 열면 그 화면의 `back="/whatif"` 때문에 뒤로가기가 보관함이 아니라
 * What-if 탭으로 갔다. 한 탭 안에서만 오갈 때는 두 값이 같아서 드러나지 않는다.
 *
 * 이 셸은 하위 화면 17개가 함께 쓴다. 그래서 동작을 코드로 잠가 둔다.
 */
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'

import AppSubLayout from '../AppSubLayout.vue'

import eco from '@/router/routes/eco'
import mypage from '@/router/routes/mypage'

function makeRouter() {
  return createRouter({ history: createMemoryHistory(), routes: [...eco, ...mypage] })
}

/**
 * `window.history.state?.back` 이 판정 근거다. jsdom 의 history 는 라우터를 거치지 않으므로
 * 실제 진입 경로를 흉내 내어 그 값만 세운다.
 */
function setHistoryBack(back) {
  window.history.replaceState(back === null ? {} : { back }, '')
}

async function mountLayout(props) {
  const router = makeRouter()
  await router.push('/whatif')
  await router.isReady()

  const push = vi.spyOn(router, 'push')
  const back = vi.spyOn(router, 'back')

  const wrapper = mount(AppSubLayout, { props, global: { plugins: [router] } })
  return { wrapper, push, back }
}

const clickBack = (wrapper) => wrapper.find('button').trigger('click')

beforeEach(() => {
  vi.restoreAllMocks()
  setHistoryBack(null)
})

describe('AppSubLayout 뒤로가기', () => {
  it('히스토리가 있으면 back prop 을 무시하고 왔던 곳으로 돌아간다', async () => {
    setHistoryBack('/mypage/reports')
    const { wrapper, push, back } = await mountLayout({ title: '전달 리포트', back: '/whatif' })

    await clickBack(wrapper)

    expect(back).toHaveBeenCalledTimes(1)
    expect(push).not.toHaveBeenCalled()
  })

  it('히스토리가 없으면 back prop 으로 간다 (새로고침 · 딥링크)', async () => {
    const { wrapper, push, back } = await mountLayout({ title: '전달 리포트', back: '/whatif' })

    await clickBack(wrapper)

    expect(push).toHaveBeenCalledWith('/whatif')
    expect(back).not.toHaveBeenCalled()
  })

  it('back prop 은 객체 경로도 받는다', async () => {
    const to = { path: '/benefit', query: { month: '2026-08' } }
    const { wrapper, push } = await mountLayout({ title: '실천항목 상세', back: to })

    await clickBack(wrapper)

    expect(push).toHaveBeenCalledWith(to)
  })

  it('둘 다 없으면 홈(What-if)으로 간다 (결정 C-1)', async () => {
    const { wrapper, push } = await mountLayout({ title: '어디선가' })

    await clickBack(wrapper)

    expect(push).toHaveBeenCalledWith('/whatif')
  })
})
