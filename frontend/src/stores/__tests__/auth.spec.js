// @vitest-environment jsdom
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/auth', () => ({
  getCurrentUser: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  refreshSession: vi.fn(),
  requestSmsCode: vi.fn(),
  signup: vi.fn(),
  verifySmsCode: vi.fn(),
}))

vi.mock('@/api/client', () => ({ clearAccessToken: vi.fn() }))

import * as authApi from '@/api/auth'
import { clearAccessToken } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

beforeEach(() => {
  vi.clearAllMocks()
  setActivePinia(createPinia())
})

describe('auth store', () => {
  it('로그인 응답으로 세션과 온보딩 상태를 정한다', async () => {
    authApi.login.mockResolvedValue({
      userId: 1,
      name: '이아영',
      onboardingCompleted: true,
      entryScreen: 'WF-06',
    })
    const store = useAuthStore()

    const result = await store.login({ email: 'user@example.com', password: 'password1234' })

    expect(authApi.login).toHaveBeenCalledWith({
      email: 'user@example.com',
      password: 'password1234',
    })
    expect(result.entryScreen).toBe('WF-06')
    expect(store.authenticated).toBe(true)
    expect(store.onboardingCompleted).toBe(true)
  })

  it('새로고침 시 refresh 후 users/me로 사용자 상태를 복구한다', async () => {
    authApi.refreshSession.mockResolvedValue({ accessToken: 'renewed' })
    authApi.getCurrentUser.mockResolvedValue({
      userId: 1,
      name: '이아영',
      onboardingCompleted: false,
      entryScreen: 'ONB-02',
    })
    const store = useAuthStore()

    await expect(store.restoreSession()).resolves.toBe(true)
    expect(authApi.refreshSession).toHaveBeenCalledOnce()
    expect(authApi.getCurrentUser).toHaveBeenCalledOnce()
    expect(store.authenticated).toBe(true)
    expect(store.onboardingCompleted).toBe(false)
  })

  it('refresh 실패는 비로그인 상태로 확정한다', async () => {
    authApi.refreshSession.mockRejectedValue(new Error('expired'))
    const store = useAuthStore()

    await expect(store.restoreSession()).resolves.toBe(false)
    expect(store.sessionChecked).toBe(true)
    expect(store.authenticated).toBe(false)
    expect(clearAccessToken).toHaveBeenCalled()
  })

  it('로그아웃 API가 실패해도 로컬 access token과 세션을 비운다', async () => {
    authApi.logout.mockRejectedValue(new Error('offline'))
    const store = useAuthStore()
    store.sessionChecked = true
    store.authenticated = true

    await store.logout()

    expect(store.authenticated).toBe(false)
    expect(clearAccessToken).toHaveBeenCalled()
  })
})
