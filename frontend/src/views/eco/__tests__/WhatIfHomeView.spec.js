/*
 * 홈이 **첫 렌더에서 터지지 않는지** 본다.
 *
 * `fetchHome()` 은 `onMounted` 에서 부르는데 첫 렌더는 그보다 먼저다. 그 한 틱 동안
 * `home` 이 null 인 채로 본문이 그려지면 흰 화면이 된다. `?preview=` 로 들어오면 화면이
 * 이미 정해져 있어서 반드시 그 경로를 타므로, 로딩 가드가 무너지면 여기서 잡힌다.
 *
 * ⚠️ `api/eco.js` 의 `USE_FIXTURES` 가 true 인 것을 전제한다. 플래그를 끌 때 함께 손봐야 한다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'

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
  it('?preview=WF_09_RESULT_READY — 첫 렌더에서 터지지 않고 결산 모달이 뜬다', async () => {
    const { wrapper, errors } = await mountHome('?preview=WF_09_RESULT_READY')
    expect(errors).toEqual([])
    expect(document.body.textContent).toContain('평가 결과가 나왔어요')
    expect(document.body.textContent).toContain('30,000M')
    wrapper.unmount()
  })

  it('preview 없이도 터지지 않는다', async () => {
    const { wrapper, errors } = await mountHome('')
    expect(errors).toEqual([])
    wrapper.unmount()
  })
})
