/*
 * WF-01a 본인확인 — **기능명세서에 없는 화면이다.** 사정은 `api/eco.js` 의
 * `verifyEcoIdentity` 주석에 적어 두었다.
 *
 * 여기서 지키려는 것은 셋이다.
 *   1. 동의 없이는 조회가 시작되지 않는다 (핵심 비즈니스 규칙 4)
 *   2. 인증이 끝나면 연동까지 걸고 홈으로 넘긴다 — 폴링은 홈이 받는다
 *   3. 미가입 분기는 서버 판정이 아니라 사용자가 누른 결과다
 *
 * ⚠️ `api/eco.js` 의 `USE_FIXTURES` 가 true 인 것을 전제한다. 인증 모의가 1.5초라
 * 실제 타이머로 돌린다 — 여기서 시간을 가짜로 만들면 검사할 것이 사라진다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'

import routes from '@/router/routes/eco'
import EcoLinkVerifyView from '@/views/eco/EcoLinkVerifyView.vue'

async function mountView() {
  window.history.replaceState({}, '', '/whatif/link')
  const router = createRouter({ history: createWebHistory(), routes })
  await router.push('/whatif/link')
  await router.isReady()

  const wrapper = mount(EcoLinkVerifyView, { global: { plugins: [createPinia(), router] } })
  await flushPromises()
  // GET /eco/status 를 기다린다. externalUrl 이 여기서 온다
  await new Promise((resolve) => setTimeout(resolve, 400))
  await flushPromises()

  return { wrapper, router }
}

/** 라벨이 아니라 보이는 글자로 찾는다 — 사용자가 누르는 것과 같은 기준이다 */
function buttonWith(wrapper, text) {
  return wrapper.findAll('button').find((button) => button.text().includes(text))
}

async function fillForm(wrapper, { agree = true } = {}) {
  await wrapper.find('input[type="text"]').setValue('김수현')
  await buttonWith(wrapper, 'SKT').trigger('click')
  await wrapper.find('input[type="tel"]').setValue('01012345678')
  if (agree) await wrapper.find('input[type="checkbox"]').setValue(true)
}

describe('EcoLinkVerifyView', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('동의하지 않으면 CTA 가 열리지 않는다 — 입력을 다 채워도 마찬가지다', async () => {
    const { wrapper } = await mountView()

    await fillForm(wrapper, { agree: false })
    expect(buttonWith(wrapper, '인증하고 사용량 불러오기').attributes('disabled')).toBeDefined()

    await wrapper.find('input[type="checkbox"]').setValue(true)
    expect(buttonWith(wrapper, '인증하고 사용량 불러오기').attributes('disabled')).toBeUndefined()

    wrapper.unmount()
  })

  it('인증하면 연동을 걸고 홈으로 넘긴다 — 폴링은 홈이 받는다', async () => {
    const { wrapper, router } = await mountView()

    await fillForm(wrapper)
    await buttonWith(wrapper, '인증하고 사용량 불러오기').trigger('click')

    // 인증 1.5초 + 연동 시작
    await new Promise((resolve) => setTimeout(resolve, 2200))
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/whatif')
    wrapper.unmount()
  })

  it('「회원이 아니에요」는 서버 판정이 아니라 사용자가 누른 결과다', async () => {
    const { wrapper } = await mountView()

    expect(wrapper.text()).toContain('본인확인이 필요해요')

    await buttonWith(wrapper, '아직 에코마일리지 회원이 아니에요').trigger('click')

    expect(wrapper.text()).toContain('먼저 가입이 필요해요')
    expect(wrapper.text()).not.toContain('본인확인이 필요해요')
    wrapper.unmount()
  })

  it('가입은 누리집에서 한다 — 주소는 GET /eco/status 가 준다', async () => {
    const { wrapper } = await mountView()
    const open = vi.spyOn(window, 'open').mockImplementation(() => null)

    await buttonWith(wrapper, '아직 에코마일리지 회원이 아니에요').trigger('click')
    await buttonWith(wrapper, '누리집에서 가입하기').trigger('click')

    expect(open).toHaveBeenCalledTimes(1)
    expect(open.mock.calls[0][0]).toMatch(/^https?:\/\//)
    expect(open.mock.calls[0][2]).toBe('noopener')

    open.mockRestore()
    wrapper.unmount()
  })
})
