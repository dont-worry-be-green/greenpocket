/*
 * 첫 진입 가드 (COM-02).
 *
 * 이 가드가 없으면 `/` → `/whatif` 로 바로 새어 `POST /users` 가 한 번도 불리지 않는다.
 * 그러면 `X-Demo-Key` 가 서버에 등록되지 않아 실서버에서 전 API 가 401 이다.
 *
 * **반대 방향도 함께 본다.** 이미 걸은 사람을 온보딩으로 되돌리면 그 화면에서 다시
 * `/whatif` 로 밀려나 무한 리다이렉트가 된다.
 */
import { beforeEach, describe, expect, it } from 'vitest'

import { clearOnboarded, isOnboarded, markOnboarded, onboardingGuard } from '../guards'

const at = (path) => onboardingGuard({ path })

describe('onboardingGuard — 온보딩 전', () => {
  beforeEach(() => localStorage.clear())

  it.each(['/', '/whatif', '/pocket', '/mypage', '/analysis'])(
    '%s 로 들어와도 ONB-01 로 보낸다',
    (path) => {
      expect(at(path)).toBe('/onboarding/start')
    },
  )

  it.each(['/onboarding/start', '/onboarding/profile'])('%s 는 그대로 통과시킨다', (path) => {
    expect(at(path)).toBe(true)
  })
})

describe('onboardingGuard — 온보딩 후', () => {
  beforeEach(() => {
    localStorage.clear()
    markOnboarded()
  })

  it.each(['/whatif', '/pocket', '/mypage'])('%s 를 그대로 통과시킨다', (path) => {
    expect(at(path)).toBe(true)
  })

  // 홈은 What-if 다(결정 C-1). 되돌아가면 프로필을 두 번 만들게 된다
  it.each(['/onboarding/start', '/onboarding/profile'])('%s 재진입은 홈으로 막는다', (path) => {
    expect(at(path)).toBe('/whatif')
  })
})

describe('플래그', () => {
  beforeEach(() => localStorage.clear())

  it('저장 전에는 false 이고, markOnboarded 뒤에 true 다', () => {
    expect(isOnboarded()).toBe(false)
    markOnboarded()
    expect(isOnboarded()).toBe(true)
  })

  // 401 이면 데모 키와 함께 지운다 — 키가 없으면 온보딩을 안 한 것과 같다
  it('clearOnboarded 뒤에는 다시 온보딩으로 보낸다', () => {
    markOnboarded()
    clearOnboarded()
    expect(isOnboarded()).toBe(false)
    expect(at('/whatif')).toBe('/onboarding/start')
  })
})
