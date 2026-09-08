// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/client', () => ({
  default: { get: vi.fn(), post: vi.fn() },
  clearAccessToken: vi.fn(),
  refreshAccessToken: vi.fn(),
  setAccessToken: vi.fn(),
}))

import * as authApi from '@/api/auth'
import client, { clearAccessToken, refreshAccessToken, setAccessToken } from '@/api/client'

beforeEach(() => vi.clearAllMocks())

describe('실제 인증 API 계약', () => {
  it('회원가입은 이메일·비밀번호·이름을 보내고 access token을 메모리에 둔다', async () => {
    client.post.mockResolvedValue({ accessToken: 'signup-token', nextScreen: 'ONB-02' })

    await authApi.signup({
      email: ' user@example.com ',
      password: 'password1234',
      name: ' 이아영 ',
    })

    expect(client.post).toHaveBeenCalledWith('/auth/signup', {
      email: 'user@example.com',
      password: 'password1234',
      name: '이아영',
    })
    expect(setAccessToken).toHaveBeenCalledWith('signup-token')
  })

  it('로그인은 이메일·비밀번호를 보내고 access token을 메모리에 둔다', async () => {
    client.post.mockResolvedValue({ accessToken: 'login-token', entryScreen: 'WF-06' })

    await authApi.login({ email: 'user@example.com', password: 'password1234' })

    expect(client.post).toHaveBeenCalledWith('/auth/login', {
      email: 'user@example.com',
      password: 'password1234',
    })
    expect(setAccessToken).toHaveBeenCalledWith('login-token')
  })

  it('세션 복구는 refresh 쿠키 요청을 사용하고 users/me를 조회한다', async () => {
    await authApi.refreshSession()
    await authApi.getCurrentUser()

    expect(refreshAccessToken).toHaveBeenCalledOnce()
    expect(client.get).toHaveBeenCalledWith('/users/me')
  })

  it('로그아웃 성공 후 access token을 비운다', async () => {
    client.post.mockResolvedValue(undefined)

    await authApi.logout()

    expect(client.post).toHaveBeenCalledWith('/auth/logout', null, {
      skipAuthRefresh: true,
      skipAuthRedirect: true,
    })
    expect(clearAccessToken).toHaveBeenCalledOnce()
  })
})
