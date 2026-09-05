/*
 * 두 화면이 **첫 렌더에서 터지지 않는지**, 그리고 시안의 뼈대가 실제로 그려지는지 본다.
 *
 * ONB-02 는 `fetchSidos()` 를 `onMounted` 에서 부르는데 첫 렌더는 그보다 먼저다.
 * 그 한 틱 동안 `sidos` 가 빈 배열인 채로 본문이 그려지므로, 목록이 있다고 가정한 템플릿은
 * 여기서 잡힌다(What-if 에서 흰 화면을 두 번 낸 원인).
 *
 * ⚠️ `api/onboarding.js` 의 `USE_FIXTURES` 가 true 인 것을 전제한다. 플래그를 끌 때 함께 손봐야 한다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'

import { isOnboarded } from '@/router/guards'
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

/** 픽스처 지연(220~400ms)이 끝날 때까지 기다린다 */
async function settle() {
  await flushPromises()
  await new Promise((resolve) => setTimeout(resolve, 500))
  await flushPromises()
}

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
  await settle()

  errorSpy.mockRestore()
  warnSpy.mockRestore()
  return { wrapper, router, errors: errors.filter((message) => !HARNESS_NOISE.test(message)) }
}

/** 시·도 / 시·군·구 모달은 `<Teleport to="body">` 라 wrapper 밖에 그려진다 */
async function pickFromModal(name) {
  const option = [...document.querySelectorAll('[role="option"]')].find(
    (element) => element.textContent.trim() === name,
  )
  expect(option, `모달에 ${name} 가 없다`).toBeTruthy()
  option.click()
  await settle()
}

describe('StartView (ONB-01)', () => {
  beforeEach(() => localStorage.clear())

  it('첫 렌더에서 터지지 않고 시안 문구와 CTA 가 보인다', async () => {
    const { wrapper, errors } = await mountView(StartView, '/onboarding/start')
    expect(errors).toEqual([])
    expect(wrapper.text()).toContain('얼마나 아낄 수 있는지 알려드려요')
    expect(wrapper.text()).toContain('발표용 데모예요')
    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })

  /*
   * 시안은 라벨 옆에 「선택」 배지를 달아 두었지만 name 은 api-spec.md 4.1 에서 필수이고
   * 그 값이 그린포켓 예금주가 된다(결정 C-14). 배지가 다시 들어오면 여기서 잡힌다.
   */
  it('이름 라벨은 시안 문구를 쓰되 「선택」으로 안내하지 않는다', async () => {
    const { wrapper } = await mountView(StartView, '/onboarding/start')
    expect(wrapper.text()).toContain('어떻게 부를까요?')
    expect(wrapper.text()).not.toContain('선택')
    wrapper.unmount()
  })

  it('이름을 넣으면 CTA 가 열리고 ONB-02 로 넘어간다', async () => {
    const { wrapper, router, errors } = await mountView(StartView, '/onboarding/start')
    expect(errors).toEqual([])

    await wrapper.find('input').setValue('김수현')
    await wrapper.find('button').trigger('click')
    await settle()

    expect(router.currentRoute.value.path).toBe('/onboarding/profile')
    wrapper.unmount()
  })
})

describe('ProfileView (ONB-02)', () => {
  const withUser = (store) => {
    store.user = { userId: 1, name: '김수현', onboardingCompleted: false, nextScreen: 'ONB-02' }
  }

  beforeEach(() => localStorage.clear())

  it('첫 렌더에서 터지지 않고 목록을 부르기 전에도 그려진다', async () => {
    const { wrapper, errors } = await mountView(ProfileView, '/onboarding/profile', withUser)
    expect(errors).toEqual([])
    expect(wrapper.text()).toContain('어디에 살고 계세요?')
    expect(wrapper.text()).toContain('주거 형태')
    expect(wrapper.text()).toContain('10~20평')
    wrapper.unmount()
  })

  it('진행 바가 2칸 모두 찬 상태로 그려진다', async () => {
    const { wrapper } = await mountView(ProfileView, '/onboarding/profile', withUser)
    const progress = wrapper.get('[role="progressbar"]')
    expect(progress.attributes('aria-valuenow')).toBe('2')
    expect(progress.attributes('aria-valuemax')).toBe('2')
    wrapper.unmount()
  })

  // 시안이 주거 형태만 칩이고 평수는 라디오 리스트 3줄이다
  it('평수는 라디오 3줄이고 주거 형태는 칩 4개다', async () => {
    const { wrapper } = await mountView(ProfileView, '/onboarding/profile', withUser)
    expect(wrapper.findAll('[role="radiogroup"][aria-label="평수"] [role="radio"]')).toHaveLength(3)
    expect(
      wrapper.findAll('[role="radiogroup"][aria-label="주거 형태"] [role="radio"]'),
    ).toHaveLength(4)
    wrapper.unmount()
  })

  it('이름을 저장하지 않고 들어오면 ONB-01 로 돌려보낸다', async () => {
    const { wrapper, router, errors } = await mountView(ProfileView, '/onboarding/profile')
    expect(errors).toEqual([])
    expect(router.currentRoute.value.path).toBe('/onboarding/start')
    wrapper.unmount()
  })

  /*
   * 「다음」이 열리는 조건은 넷을 다 고른 것이다. 저장에 성공해야 진입 가드가 볼
   * 온보딩 완료 플래그가 남는다 — 이게 없으면 홈으로 가도 곧장 ONB-01 로 되돌아온다.
   */
  it('넷을 다 고르면 「다음」이 열리고, 저장하면 온보딩 완료로 표시된다', async () => {
    const { wrapper } = await mountView(ProfileView, '/onboarding/profile', withUser)
    const cta = () => wrapper.findAll('button').at(-1)
    expect(cta().text()).toBe('다음')
    expect(cta().attributes('disabled')).toBeDefined()

    const [sidoButton, sigunguButton] = wrapper.findAll('[aria-haspopup="listbox"]')
    await sidoButton.trigger('click')
    await pickFromModal('서울특별시')
    await sigunguButton.trigger('click')
    await pickFromModal('관악구')

    await wrapper.get('[aria-label="주거 형태"] [role="radio"]').trigger('click')
    await wrapper.get('[aria-label="평수"] [role="radio"]').trigger('click')

    expect(cta().attributes('disabled')).toBeUndefined()
    expect(isOnboarded()).toBe(false)

    await cta().trigger('click')
    await settle()

    expect(isOnboarded()).toBe(true)
    wrapper.unmount()
  })
})
