// @vitest-environment jsdom
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'

vi.mock('@/api/auth', () => ({
  getCurrentUser: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  refreshSession: vi.fn(),
  requestSmsCode: vi.fn().mockResolvedValue({ expiresInSeconds: 180 }),
  signup: vi.fn(),
  verifySmsCode: vi.fn((code) => Promise.resolve({ verified: code === '000000' })),
}))

import * as authApi from '@/api/auth'
import { DATA_SOURCE, setDataSource } from '@/api/dataSource'
import routes from '@/router/routes/onboarding'
import LoginView from '@/views/onboarding/LoginView.vue'
import SignupView from '@/views/onboarding/SignupView.vue'
import StartView from '@/views/onboarding/StartView.vue'

async function mountView(component, path) {
  window.history.replaceState({}, '', path)
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createWebHistory(),
    routes: [...routes, { path: '/analysis/eco-link', component: { template: '<div />' } }],
  })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(component, { global: { plugins: [pinia, router] } })
  await flushPromises()
  return { wrapper, router }
}

function buttonWith(wrapper, text) {
  return wrapper.findAll('button').find((button) => button.text().includes(text))
}

async function verifyPhone(wrapper) {
  await wrapper.find('input[autocomplete="name"]').setValue('이아영')
  await wrapper.find('input[autocomplete="bday"]').setValue('1998-03-15')
  await buttonWith(wrapper, '여성').trigger('click')
  await buttonWith(wrapper, 'SKT').trigger('click')
  await wrapper.find('input[type="tel"]').setValue('01011111111')
  await buttonWith(wrapper, '인증번호 받기').trigger('click')
  await flushPromises()
  await wrapper.find('input[autocomplete="one-time-code"]').setValue('000000')
  await buttonWith(wrapper, '확인').trigger('click')
  await flushPromises()
}

beforeEach(() => {
  vi.clearAllMocks()
  setDataSource(DATA_SOURCE.FIXTURE)
  authApi.requestSmsCode.mockResolvedValue({ expiresInSeconds: 180 })
  authApi.verifySmsCode.mockImplementation((code) =>
    Promise.resolve({ verified: code === '000000' }),
  )
})

describe('StartView', () => {
  it('회원가입과 기존 계정 로그인의 두 진입을 보여준다', async () => {
    const { wrapper } = await mountView(StartView, '/onboarding/start')
    expect(wrapper.text()).toContain('회원가입')
    expect(wrapper.text()).toContain('이미 계정이 있어요')
    expect(wrapper.text()).not.toContain('이 기기에만 저장')
  })
})

describe('LoginView', () => {
  it('이메일과 비밀번호를 모두 입력해야 로그인할 수 있다', async () => {
    const { wrapper } = await mountView(LoginView, '/onboarding/login')
    const cta = () => buttonWith(wrapper, '로그인')
    expect(cta().attributes('disabled')).toBeDefined()

    await wrapper.find('input[autocomplete="email"]').setValue('user@example.com')
    await wrapper.find('input[autocomplete="current-password"]').setValue('password1234')
    expect(cta().attributes('disabled')).toBeUndefined()
  })

  it('실제 로그인 응답을 받은 뒤 진단 탭의 에코 연동 화면으로 이동한다', async () => {
    authApi.login.mockResolvedValue({
      userId: 1,
      name: '이아영',
      onboardingCompleted: true,
      entryScreen: 'WF-01',
    })
    const { wrapper, router } = await mountView(LoginView, '/onboarding/login')
    await wrapper.find('input[autocomplete="email"]').setValue('user@example.com')
    await wrapper.find('input[autocomplete="current-password"]').setValue('password1234')
    await buttonWith(wrapper, '로그인').trigger('click')
    await flushPromises()

    expect(authApi.login).toHaveBeenCalledWith({
      email: 'user@example.com',
      password: 'password1234',
    })
    expect(router.currentRoute.value.path).toBe('/analysis/eco-link')
  })

  it('서버 인증 오류 문구를 로그인 화면에 표시한다', async () => {
    authApi.login.mockRejectedValue(new Error('이메일 또는 비밀번호가 올바르지 않아요.'))
    const { wrapper, router } = await mountView(LoginView, '/onboarding/login')
    await wrapper.find('input[autocomplete="email"]').setValue('user@example.com')
    await wrapper.find('input[autocomplete="current-password"]').setValue('wrong-password')
    await buttonWith(wrapper, '로그인').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('이메일 또는 비밀번호가 올바르지 않아요.')
    expect(router.currentRoute.value.path).toBe('/onboarding/login')
  })
})

describe('SignupView', () => {
  it('아이디 중복확인 없이 이메일과 비밀번호를 받는다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')
    expect(wrapper.find('input[autocomplete="email"]').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('중복확인')
    expect(wrapper.text()).not.toContain('아이디')
    expect(wrapper.find('input[autocomplete="bday"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('성별')
  })

  it('본인정보를 모두 입력하기 전에는 인증번호를 요청할 수 없다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')
    await wrapper.find('input[autocomplete="name"]').setValue('이아영')
    await buttonWith(wrapper, 'SKT').trigger('click')
    await wrapper.find('input[type="tel"]').setValue('01011111111')

    expect(buttonWith(wrapper, '인증번호 받기').attributes('disabled')).toBeDefined()

    await wrapper.find('input[autocomplete="bday"]').setValue('1998-03-15')
    await buttonWith(wrapper, '여성').trigger('click')
    expect(buttonWith(wrapper, '인증번호 받기').attributes('disabled')).toBeUndefined()
  })

  it('인증번호 발송 후에는 인증 대상 정보를 잠근다', async () => {
    const { wrapper } = await mountView(SignupView, '/onboarding/signup')
    await wrapper.find('input[autocomplete="name"]').setValue('이아영')
    await wrapper.find('input[autocomplete="bday"]').setValue('1998-03-15')
    await buttonWith(wrapper, '여성').trigger('click')
    await buttonWith(wrapper, 'SKT').trigger('click')
    await wrapper.find('input[type="tel"]').setValue('01011111111')
    await buttonWith(wrapper, '인증번호 받기').trigger('click')
    await flushPromises()

    expect(wrapper.find('input[autocomplete="name"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('input[autocomplete="bday"]').attributes('disabled')).toBeDefined()
    expect(wrapper.find('input[type="tel"]').attributes('disabled')).toBeDefined()
  })

  it('본인확인 뒤 실제 signup 계약으로 가입한다', async () => {
    authApi.signup.mockResolvedValue({
      userId: 1,
      email: 'user@example.com',
      name: '이아영',
      onboardingCompleted: true,
      nextScreen: 'WF-01',
    })
    const { wrapper, router } = await mountView(SignupView, '/onboarding/signup')
    await verifyPhone(wrapper)
    await wrapper.find('input[autocomplete="email"]').setValue('user@example.com')
    const passwords = wrapper.findAll('input[type="password"]')
    await passwords[0].setValue('password1234')
    await passwords[1].setValue('password1234')
    await buttonWith(wrapper, '가입하고 시작하기').trigger('click')
    await flushPromises()

    expect(authApi.signup).toHaveBeenCalledWith({
      name: '이아영',
      birthDate: '1998-03-15',
      gender: 'FEMALE',
      phoneNumber: '01011111111',
      email: 'user@example.com',
      password: 'password1234',
    })
    expect(router.currentRoute.value.path).toBe('/analysis/eco-link')
    expect(JSON.stringify(localStorage)).not.toContain('password1234')
  })
})
