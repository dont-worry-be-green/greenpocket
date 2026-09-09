// @vitest-environment jsdom
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useAuthStore } from '@/stores/auth'
import { onboardingGuard } from '../guards'

const at = (path) => onboardingGuard({ path })

beforeEach(() => {
  setActivePinia(createPinia())
})

describe('서버 세션 기반 진입 가드', () => {
  it('최초 진입에는 refresh와 users/me로 세션을 복구한다', async () => {
    const auth = useAuthStore()
    const restore = vi.spyOn(auth, 'restoreSession').mockImplementation(async () => {
      auth.sessionChecked = true
      auth.authenticated = true
      auth.onboardingCompleted = true
      return true
    })

    await expect(at('/whatif')).resolves.toBe(true)
    expect(restore).toHaveBeenCalledOnce()
  })

  it.each(['/whatif', '/mypage', '/onboarding/profile'])(
    '로그인 전 %s 접근은 로그인 화면으로 보낸다',
    async (path) => {
      const auth = useAuthStore()
      auth.sessionChecked = true
      await expect(at(path)).resolves.toBe('/onboarding/login')
    },
  )

  it.each(['/onboarding/login', '/onboarding/signup'])(
    '로그인 전 %s 접근은 허용한다',
    async (path) => {
      const auth = useAuthStore()
      auth.sessionChecked = true
      await expect(at(path)).resolves.toBe(true)
    },
  )

  it('예전 계정의 온보딩 플래그가 false여도 폐기된 프로필 화면으로 보내지 않는다', async () => {
    const auth = useAuthStore()
    auth.sessionChecked = true
    auth.authenticated = true
    auth.onboardingCompleted = false

    await expect(at('/whatif')).resolves.toBe(true)
  })

  it('온보딩까지 끝난 사용자가 인증 화면으로 가면 진단 연동 화면으로 보낸다', async () => {
    const auth = useAuthStore()
    auth.sessionChecked = true
    auth.authenticated = true
    auth.onboardingCompleted = true

    await expect(at('/onboarding/login')).resolves.toBe('/analysis/eco-link')
    await expect(at('/mypage')).resolves.toBe(true)
  })
})
