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

/*
 * ⚠️ **기본 타임아웃(5초)으로는 모자란다.** 이 화면 하나에서 문자 발송(0.7초) · 확인(0.9초) ·
 * 중복확인(0.4초) · 가입(`POST /users`)을 차례로 기다리는 시나리오가 있다. 모의 지연을 줄이면
 * 로딩 경로가 실제로 안 돌아가므로(`api/auth.js` 주석) 지연이 아니라 타임아웃을 늘린다.
 */
describe('SignupView (ONB-01b)', { timeout: 20000 }, () => {
  beforeEach(resetStorage)

  /*
   * **한 화면이다.** 본인확인(이름·번호 → 인증번호)이 위, 계정(아이디·비밀번호)이 아래고
   * 잠기는 것은 가입 CTA 하나다.
   *
   * ⚠️ 문자인증 모의는 발송 0.7초 · 확인 0.9초라 공용 `settle()`(0.5초)보다 길다.
   * 여기서만 더 기다린다 — 짧게 두면 다음 블록이 아직 안 그려져 버튼을 못 찾는다.
   */
  const settleSms = async () => {
    await flushPromises()
    await new Promise((resolve) => setTimeout(resolve, 1200))
    await flushPromises()
  }

  /** 본인확인 블록. 이름도 여기 있다 — 인증의 대상이고 예금주가 되는 값이다 */
  async function verify(wrapper, code = '000000') {
    await wrapper.find('input[autocomplete="name"]').setValue('김수현')
    await buttonWith(wrapper, 'SKT').trigger('click')
    await wrapper.find('input[type="tel"]').setValue('01012345678')
    await buttonWith(wrapper, '인증번호 받기').trigger('click')
    await settleSms()

    // 번호 칸도 inputmode="numeric" 이라 자릿수가 아니라 용도로 찾는다
    await wrapper.find('input[autocomplete="one-time-code"]').setValue(code)
    await buttonWith(wrapper, '확인').trigger('click')
    await settleSms()
  }

  /** `check` 를 끄면 중복확인을 누르지 않는다 — 그 자체를 보는 테스트가 있다 */
  async function fillAccount(
    wrapper,
    { id = 'suhyeon', confirm = 'password1234', check = true } = {},
  ) {
    await wrapper.find('input[autocomplete="username"]').setValue(id)
    if (check) {
      // 중복확인 모의는 0.4초라 공용 `settle()`(0.5초)로 충분하다
      await buttonWith(wrapper, '중복확인').trigger('click')
      await settle()
    }
    const passwords = wrapper.findAll('input[type="password"]')
    await passwords[0].setValue('password1234')
    await passwords[1].setValue(confirm)
  }

  it('본인확인과 계정 입력이 한 화면에 같이 있다', async () => {
    const { wrapper, errors } = await mountView(SignupView, '/onboarding/signup')
    expect(errors).toEqual([])

    // 제목은 헤더가 든다 — 본문 위 큰 제목을 두면 화면이 그만큼 길어진다
    expect(wrapper.text()).toContain('회원가입')
    expect(wrapper.find('input[autocomplete="tel"]').exists()).toBe(true)
    // 인증 전에도 계정 입력칸이 보인다. 잠기는 것은 CTA 하나다
    expect(wrapper.find('input[autocomplete="username"]').exists()).toBe(true)
    wrapper.unmount()
  })

  // 이름은 본인확인의 대상이다. 번호만 있고 이름이 비면 보낼 것이 못 된다
  it('이름·통신사·번호가 다 있어야 인증번호를 보낼 수 있다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')

    await buttonWith(wrapper, 'SKT').trigger('click')
    await wrapper.find('input[type="tel"]').setValue('01012345678')
    expect(buttonWith(wrapper, '인증번호 받기').attributes('disabled')).toBeDefined()

    await wrapper.find('input[autocomplete="name"]').setValue('김수현')
    expect(buttonWith(wrapper, '인증번호 받기').attributes('disabled')).toBeUndefined()
    wrapper.unmount()
  })

  // 한 화면이 되면서 이름칸 blur 만으로 아직 손대지 않은 아이디 오류가 뜬 적이 있다
  it('이름 오류는 이름칸 아래에만 뜨고 계정 오류를 끌어오지 않는다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')

    await wrapper.find('input[autocomplete="name"]').trigger('blur')
    expect(wrapper.text()).toContain('이름을 입력해 주세요.')
    expect(wrapper.text()).not.toContain('아이디는 4자 이상')
    wrapper.unmount()
  })

  // 아무 여섯 자리나 통과하면 검증이 없는 것과 같다
  it('틀린 인증번호는 통과하지 않는다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')

    await verify(wrapper, '123456')
    expect(wrapper.text()).toContain('인증번호가 맞지 않아요')
    expect(wrapper.text()).not.toContain('본인인증 완료')
    wrapper.unmount()
  })

  // 한 화면이 되면서 「가입만 눌러 인증을 건너뛰는」 길이 생겼다. 그 길을 막는 계약이다
  it('계정을 다 채워도 본인확인 전에는 가입할 수 없다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')

    await wrapper.find('input[autocomplete="name"]').setValue('김수현')
    await fillAccount(wrapper)

    expect(buttonWith(wrapper, '가입하고 시작하기').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('휴대폰 본인확인을 마치면 가입할 수 있어요')
    expect(isLoggedIn()).toBe(false)
    wrapper.unmount()
  })

  // 중복확인을 안 눌러도 가입되면 그 버튼은 장식이다
  it('중복확인을 누르지 않으면 가입할 수 없다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')
    await verify(wrapper)
    await fillAccount(wrapper, { check: false })

    expect(buttonWith(wrapper, '가입하고 시작하기').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('아이디 중복확인을 눌러 주세요')
    wrapper.unmount()
  })

  // 「중복확인」을 누르면 아이디 칸이 blur 된다. 그것이 비밀번호 오류를 켜면 안 된다
  it('중복확인을 눌러도 아직 손대지 않은 비밀번호 오류가 뜨지 않는다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')

    await wrapper.find('input[autocomplete="username"]').setValue('suhyeon')
    await buttonWith(wrapper, '중복확인').trigger('click')
    await settle()

    expect(wrapper.text()).toContain('사용할 수 있는 아이디예요')
    expect(wrapper.text()).not.toContain('비밀번호는 8자 이상')
    wrapper.unmount()
  })

  it('이미 쓰는 아이디는 통과하지 않는다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')
    await verify(wrapper)
    await fillAccount(wrapper, { id: 'admin' })

    expect(wrapper.text()).toContain('이미 사용 중인 아이디예요')
    expect(buttonWith(wrapper, '가입하고 시작하기').attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })

  // 확인한 아이디와 제출되는 아이디가 어긋나면 안 된다 — 번호와 같은 이유다
  it('중복확인 뒤 아이디를 고치면 확인이 풀린다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')
    await verify(wrapper)
    await fillAccount(wrapper)
    expect(wrapper.text()).toContain('사용할 수 있는 아이디예요')

    await wrapper.find('input[autocomplete="username"]').setValue('suhyeon2')
    expect(wrapper.text()).not.toContain('사용할 수 있는 아이디예요')
    expect(buttonWith(wrapper, '가입하고 시작하기').attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })

  // 끝난 일이 화면의 절반을 계속 차지하지 않도록 입력칸을 접는다
  it('인증을 마치면 입력칸이 접히고 완료 카드만 남는다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')
    await verify(wrapper)

    expect(wrapper.text()).toContain('본인인증 완료')
    expect(wrapper.text()).toContain('010-1234-5678')
    expect(wrapper.find('input[type="tel"]').exists()).toBe(false)
    expect(wrapper.find('input[autocomplete="name"]').exists()).toBe(false)
    wrapper.unmount()
  })

  // 인증한 번호와 제출되는 번호가 어긋나면 안 된다 — 합치면서 새로 생긴 경로다
  it('인증을 마친 뒤 번호를 바꾸면 인증이 풀린다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')
    await verify(wrapper)
    expect(wrapper.text()).toContain('본인인증 완료')

    await buttonWith(wrapper, '번호 변경').trigger('click')
    await settleSms()

    expect(wrapper.text()).not.toContain('본인인증 완료')
    expect(buttonWith(wrapper, '인증번호 받기')).toBeTruthy()
    expect(buttonWith(wrapper, '가입하고 시작하기').attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })

  /*
   * 버튼 넷이 한 로딩 플래그를 나눠 쓰면 **하나를 눌렀을 때 넷이 다 「확인 중」이 된다.**
   * 중복확인이 도는 동안 가입 CTA 가 「가입하는 중...」이 되면 안 된다.
   */
  it('버튼마다 로딩이 따로 돈다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')
    await verify(wrapper)

    await wrapper.find('input[autocomplete="username"]').setValue('suhyeon')
    await buttonWith(wrapper, '중복확인').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('확인 중...')
    expect(wrapper.text()).not.toContain('가입하는 중...')

    await settle()
    wrapper.unmount()
  })

  it('비밀번호가 서로 다르면 가입되지 않는다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')
    await verify(wrapper)

    await fillAccount(wrapper, { confirm: 'password9999' })
    await buttonWith(wrapper, '가입하고 시작하기').trigger('click')
    await settleSms()

    expect(wrapper.text()).toContain('비밀번호가 서로 달라요')
    expect(isLoggedIn()).toBe(false)
    wrapper.unmount()
  })

  it('가입하면 로그인 상태가 되고 ONB-02 로 넘어간다', async () => {
    const { wrapper, router } = await mountView(SignupView, '/onboarding/signup')
    await verify(wrapper)

    await fillAccount(wrapper)
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
