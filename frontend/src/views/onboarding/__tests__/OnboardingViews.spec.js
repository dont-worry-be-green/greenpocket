/*
 * 두 화면이 **첫 렌더에서 터지지 않는지** 본다.
 *
 * ONB-02 는 `fetchSidos()` 를 `onMounted` 에서 부르는데 첫 렌더는 그보다 먼저다.
 * 그 한 틱 동안 `sidos` 가 빈 배열인 채로 본문이 그려지므로, 목록이 있다고 가정한 템플릿은
 * 여기서 잡힌다(What-if 에서 흰 화면을 두 번 낸 원인).
 *
 * ⚠️ `api/onboarding.js` 의 `USE_FIXTURES` 가 true 인 것을 전제한다. 플래그를 끌 때 함께 손봐야 한다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'

import routes from '@/router/routes/onboarding'
import { useOnboardingStore } from '@/stores/onboarding'
import ProfileView from '@/views/onboarding/ProfileView.vue'
import StartView from '@/views/onboarding/StartView.vue'

/*
 * `startUser` 가 픽스처 모드에서도 `POST /users` 를 실제로 쏘는데 테스트에는 서버가 없다.
 * jsdom 이 그 실패를 콘솔로 흘리므로 화면이 낸 오류와 섞이지 않게 걸러 낸다.
 * (하네스가 온보딩 라우트만 등록해서 나는 링크 경고도 같이 거른다)
 */
const HARNESS_NOISE = /VUE_ROUTER_R0004|ECONNREFUSED|Cross origin|Error: connect|Network Error/

async function mountView(component, path, setup) {
  window.history.replaceState({}, '', path)
  const pinia = createPinia()
  setActivePinia(pinia)
  setup?.(useOnboardingStore())

  const router = createRouter({ history: createWebHistory(), routes })
  await router.push(path)
  await router.isReady()

  const errors = []
  const collect = (...args) => errors.push(String(args[0]))
  const errorSpy = vi.spyOn(console, 'error').mockImplementation(collect)
  const warnSpy = vi.spyOn(console, 'warn').mockImplementation(collect)

  const wrapper = mount(component, { global: { plugins: [pinia, router] } })
  await flushPromises()
  await new Promise((resolve) => setTimeout(resolve, 500))
  await flushPromises()

  errorSpy.mockRestore()
  warnSpy.mockRestore()
  return { wrapper, router, errors: errors.filter((message) => !HARNESS_NOISE.test(message)) }
}

describe('StartView (ONB-01)', () => {
  it('첫 렌더에서 터지지 않고 데모 안내와 CTA 가 보인다', async () => {
    const { wrapper, errors } = await mountView(StartView, '/onboarding/start')
    expect(errors).toEqual([])
    expect(wrapper.text()).toContain('발표용 데모예요')
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })

  it('이름을 넣으면 CTA 가 열리고 ONB-02 로 넘어간다', async () => {
    const { wrapper, router, errors } = await mountView(StartView, '/onboarding/start')
    expect(errors).toEqual([])

    await wrapper.find('input').setValue('김수현')
    await wrapper.find('button').trigger('click')
    await flushPromises()
    await new Promise((resolve) => setTimeout(resolve, 500))
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/onboarding/profile')
    wrapper.unmount()
  })
})

describe('ProfileView (ONB-02)', () => {
  const withUser = (store) => {
    store.user = { userId: 1, name: '김수현', onboardingCompleted: false, nextScreen: 'ONB-02' }
  }

  it('첫 렌더에서 터지지 않고 목록을 부르기 전에도 그려진다', async () => {
    const { wrapper, errors } = await mountView(ProfileView, '/onboarding/profile', withUser)
    expect(errors).toEqual([])
    expect(wrapper.text()).toContain('주거 형태')
    expect(wrapper.text()).toContain('10~20평')
    wrapper.unmount()
  })

  it('이름을 저장하지 않고 들어오면 ONB-01 로 돌려보낸다', async () => {
    const { wrapper, router, errors } = await mountView(ProfileView, '/onboarding/profile')
    expect(errors).toEqual([])
    expect(router.currentRoute.value.path).toBe('/onboarding/start')
    wrapper.unmount()
  })
})
