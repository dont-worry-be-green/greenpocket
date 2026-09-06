/*
 * 온보딩 네 화면이 **첫 렌더에서 터지지 않는지**, 그리고 시안의 뼈대가 실제로 그려지는지 본다.
 * ONB-01 랜딩 · ONB-01a 로그인 · ONB-01b 회원가입 · ONB-02 프로필 (이슈 #121).
 *
 * ONB-02 는 `fetchSidos()` 를 `onMounted` 에서 부르는데 첫 렌더는 그보다 먼저다.
 * 그 한 틱 동안 `sidos` 가 빈 배열인 채로 본문이 그려지므로, 목록이 있다고 가정한 템플릿은
 * 여기서 잡힌다(What-if 에서 흰 화면을 두 번 낸 원인).
 *
 * ⚠️ **목데이터 모드를 명시적으로 세우고 돈다.** 기본값은 실 API 라 그대로 두면 서버가 없는
 * 이 환경에서 화면이 에러만 그린다. 모드 스위치는 `api/dataSource.js` 하나다.
 */
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'

import { DATA_SOURCE, setDataSource } from '@/api/dataSource'
import { getLoginId, isLoggedIn, isOnboarded, markLoggedIn } from '@/router/guards'
import routes from '@/router/routes/onboarding'
import { useOnboardingStore } from '@/stores/onboarding'
import LoginView from '@/views/onboarding/LoginView.vue'
import ProfileView from '@/views/onboarding/ProfileView.vue'
import SignupView from '@/views/onboarding/SignupView.vue'
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

/** 시·군·구 모달은 `<Teleport to="body">` 라 wrapper 밖에 그려진다 */
async function pickFromModal(name) {
  const option = [...document.querySelectorAll('[role="option"]')].find(
    (element) => element.textContent.trim() === name,
  )
  expect(option, `모달에 ${name} 가 없다`).toBeTruthy()
  option.click()
  await settle()
}

/** 라벨이 아니라 보이는 글자로 찾는다 — 사용자가 누르는 것과 같은 기준이다 */
function buttonWith(wrapper, text) {
  return wrapper.findAll('button').find((button) => button.text().includes(text))
}

/** `localStorage.clear()` 가 데이터 소스 키까지 지우므로 지운 **뒤에** 세운다 */
function resetStorage() {
  localStorage.clear()
  setDataSource(DATA_SOURCE.FIXTURE)
}

describe('StartView (ONB-01 랜딩)', () => {
  beforeEach(resetStorage)

  it('첫 렌더에서 터지지 않고 두 갈래를 보여준다', async () => {
    const { wrapper, errors } = await mountView(StartView, '/onboarding/start')
    expect(errors).toEqual([])
    expect(wrapper.text()).toContain('얼마나 아낄 수 있는지 알려드려요')
    expect(wrapper.text()).toContain('회원가입')
    expect(wrapper.text()).toContain('이미 계정이 있어요')
    wrapper.unmount()
  })

  /*
   * 이름은 회원가입으로 옮겼다(이슈 #121). 이 화면이 다시 이름을 받으면 `POST /users` 를
   * 부르는 곳이 둘이 되어 어느 쪽이 계정을 만드는지 알 수 없게 된다.
   */
  it('이름을 받지 않는다 — 회원가입으로 옮겼다', async () => {
    const { wrapper } = await mountView(StartView, '/onboarding/start')
    expect(wrapper.find('input').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('어떻게 부를까요?')
    wrapper.unmount()
  })

  it('이 기기에 가입한 아이디가 있으면 로그인 버튼에 그 아이디를 보여준다', async () => {
    markLoggedIn('suhyeon')
    const { wrapper } = await mountView(StartView, '/onboarding/start')
    expect(wrapper.text()).toContain('suhyeon 로 로그인')
    wrapper.unmount()
  })
})

describe('LoginView (ONB-01a)', () => {
  beforeEach(resetStorage)

  it('아이디와 비밀번호를 다 넣어야 CTA 가 열린다', async () => {
    const { wrapper, errors } = await mountView(LoginView, '/onboarding/login')
    expect(errors).toEqual([])

    const cta = buttonWith(wrapper, '로그인')
    expect(cta.attributes('disabled')).toBeDefined()

    await wrapper.find('input[autocomplete="username"]').setValue('suhyeon')
    expect(buttonWith(wrapper, '로그인').attributes('disabled')).toBeDefined()

    await wrapper.find('input[type="password"]').setValue('password1234')
    expect(buttonWith(wrapper, '로그인').attributes('disabled')).toBeUndefined()
    wrapper.unmount()
  })

  /*
   * ⚠️ 서버가 아니라고 한 것이 아니라 **이 기기에 기록이 없는 것**이다. 그래서 에러 화면이
   * 아니라 안내이고, 문구도 그렇게 적는다(`api/auth.js` 주석).
   */
  it('이 기기에 없는 아이디는 에러가 아니라 안내로 알린다', async () => {
    const { wrapper, router } = await mountView(LoginView, '/onboarding/login')

    await wrapper.find('input[autocomplete="username"]').setValue('nobody')
    await wrapper.find('input[type="password"]').setValue('password1234')
    await buttonWith(wrapper, '로그인').trigger('click')
    await settle()

    expect(wrapper.text()).toContain('가입한 기록이 없어요')
    expect(router.currentRoute.value.path).toBe('/onboarding/login')
    wrapper.unmount()
  })

  it('가입해 둔 아이디로는 들어가고, 프로필이 없으면 ONB-02 로 이어진다', async () => {
    markLoggedIn('suhyeon')
    const { wrapper, router } = await mountView(LoginView, '/onboarding/login')

    // 아이디는 프리필된다. 비밀번호만 넣으면 된다
    expect(wrapper.find('input[autocomplete="username"]').element.value).toBe('suhyeon')
    await wrapper.find('input[type="password"]').setValue('password1234')
    await buttonWith(wrapper, '로그인').trigger('click')
    await settle()

    expect(router.currentRoute.value.path).toBe('/onboarding/profile')
    wrapper.unmount()
  })

  it('비밀번호를 어디에도 저장하지 않는다', async () => {
    markLoggedIn('suhyeon')
    const { wrapper } = await mountView(LoginView, '/onboarding/login')

    await wrapper.find('input[type="password"]').setValue('sup3rs3cret')
    await buttonWith(wrapper, '로그인').trigger('click')
    await settle()

    expect(JSON.stringify(localStorage)).not.toContain('sup3rs3cret')
    wrapper.unmount()
  })
})

describe('SignupView (ONB-01b)', () => {
  beforeEach(resetStorage)

  /*
   * 본인확인 → 인증번호 → 계정.
   * ⚠️ 문자인증 모의는 발송 0.7초 · 확인 0.9초라 공용 `settle()`(0.5초)보다 길다.
   * 여기서만 더 기다린다 — 짧게 두면 다음 단계가 아직 안 그려져 버튼을 못 찾는다.
   */
  const settleSms = async () => {
    await flushPromises()
    await new Promise((resolve) => setTimeout(resolve, 1200))
    await flushPromises()
  }

  async function verify(wrapper, code = '000000') {
    await buttonWith(wrapper, 'SKT').trigger('click')
    await wrapper.find('input[type="tel"]').setValue('01012345678')
    await wrapper.find('input[type="checkbox"]').setValue(true)
    await buttonWith(wrapper, '인증번호 받기').trigger('click')
    await settleSms()

    await wrapper.find('input[inputmode="numeric"]').setValue(code)
    await buttonWith(wrapper, '확인').trigger('click')
    await settleSms()
  }

  it('본인확인부터 시작하고 동의 없이는 인증번호를 보낼 수 없다', async () => {
    const { wrapper, errors } = await mountView(SignupView, '/onboarding/signup')
    expect(errors).toEqual([])
    expect(wrapper.text()).toContain('본인확인을 해주세요')

    await buttonWith(wrapper, 'SKT').trigger('click')
    await wrapper.find('input[type="tel"]').setValue('01012345678')
    expect(buttonWith(wrapper, '인증번호 받기').attributes('disabled')).toBeDefined()

    await wrapper.find('input[type="checkbox"]').setValue(true)
    expect(buttonWith(wrapper, '인증번호 받기').attributes('disabled')).toBeUndefined()
    wrapper.unmount()
  })

  // 아무 여섯 자리나 통과하면 검증이 없는 것과 같다
  it('틀린 인증번호는 통과하지 않는다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')

    await verify(wrapper, '123456')
    expect(wrapper.text()).toContain('인증번호가 맞지 않아요')
    expect(wrapper.text()).not.toContain('계정을 만들어요')
    wrapper.unmount()
  })

  it('본인확인을 마쳐야 계정 만들기로 넘어간다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')

    // 인증 전에는 계정 입력칸이 없다
    expect(wrapper.text()).not.toContain('계정을 만들어요')

    await verify(wrapper)
    expect(wrapper.text()).toContain('계정을 만들어요')
    wrapper.unmount()
  })

  it('비밀번호가 서로 다르면 가입되지 않는다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')
    await verify(wrapper)

    await wrapper.find('input[autocomplete="name"]').setValue('김수현')
    await wrapper.find('input[autocomplete="username"]').setValue('suhyeon')
    const passwords = wrapper.findAll('input[type="password"]')
    await passwords[0].setValue('password1234')
    await passwords[1].setValue('password9999')
    await buttonWith(wrapper, '가입하고 시작하기').trigger('click')
    await settleSms()

    expect(wrapper.text()).toContain('비밀번호가 서로 달라요')
    wrapper.unmount()
  })

  it('가입하면 로그인 상태가 되고 ONB-02 로 넘어간다', async () => {
    const { wrapper, router } = await mountView(SignupView, '/onboarding/signup')
    await verify(wrapper)

    await wrapper.find('input[autocomplete="name"]').setValue('김수현')
    await wrapper.find('input[autocomplete="username"]').setValue('suhyeon')
    const passwords = wrapper.findAll('input[type="password"]')
    await passwords[0].setValue('password1234')
    await passwords[1].setValue('password1234')
    await buttonWith(wrapper, '가입하고 시작하기').trigger('click')
    await settleSms()

    expect(isLoggedIn()).toBe(true)
    expect(getLoginId()).toBe('suhyeon')
    // 비밀번호는 어디에도 남지 않는다
    expect(JSON.stringify(localStorage)).not.toContain('password1234')
    expect(router.currentRoute.value.path).toBe('/onboarding/profile')
    wrapper.unmount()
  })
})

describe('ProfileView (ONB-02)', () => {
  /*
   * 예전에는 `store.user` 가 없으면 뷰가 ONB-01 로 되돌려 보냈다. 그 판정은 가드로 옮겼고
   * (이슈 #121), 그래서 여기서 스토어를 미리 채우지 않아도 화면이 그려져야 한다.
   */
  const withUser = (store) => {
    store.user = { userId: 1, name: '김수현', onboardingCompleted: false, nextScreen: 'ONB-02' }
  }

  beforeEach(resetStorage)

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

  /*
   * 스토어가 비어 있어도 이 화면은 그려진다. 진입을 막는 것은 가드의 일이다(이슈 #121) —
   * 예전에는 뷰가 `store.user` 를 보고 되돌려 보냈는데, 새로고침하면 그 값이 사라져서
   * 로그인을 마친 사람까지 밀려났다.
   */
  it('스토어가 비어 있어도 되돌려 보내지 않는다 — 진입 판정은 가드가 한다', async () => {
    const { wrapper, router, errors } = await mountView(ProfileView, '/onboarding/profile')
    expect(errors).toEqual([])
    expect(router.currentRoute.value.path).toBe('/onboarding/profile')
    expect(wrapper.text()).toContain('어디에 살고 계세요?')
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

    // 시·도는 고르지 않는다 — 서버가 1건만 주고 뷰가 자동으로 세운다(결정 C-15)
    expect(wrapper.text()).toContain('서울특별시')

    await wrapper.get('[aria-haspopup="listbox"]').trigger('click')
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
