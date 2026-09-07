/*
 * 첫 진입 가드 (COM-02) — **두 단계다** (이슈 #121).
 *
 *   ① 로그인했나   아니면 랜딩·로그인·회원가입 셋 안에서만
 *   ② 온보딩 마쳤나 아니면 프로필 화면 하나만
 *
 * 이 가드가 없으면 `/` → `/whatif` 로 바로 새어 `POST /users` 가 한 번도 불리지 않는다.
 * 그러면 `X-Demo-Key` 가 서버에 등록되지 않아 실서버에서 전 API 가 401 이다.
 *
 * **반대 방향도 함께 본다.** 이미 걸은 사람을 온보딩으로 되돌리면 그 화면에서 다시
 * `/whatif` 로 밀려나 무한 리다이렉트가 된다.
 *
 * 그리고 **두 단계가 서로를 건너뛰지 않는지**도 본다 — 로그인만 하고 프로필을 건너뛰면
 * 서버에 프로필이 없는 채로 홈이 열려 진단·What-if 가 빈 화면이 된다.
 */
import { beforeEach, describe, expect, it } from 'vitest'

import {
  clearLoggedIn,
  clearOnboarded,
  getLoginId,
  isLoggedIn,
  isOnboarded,
  markLoggedIn,
  markOnboarded,
  onboardingGuard,
} from '../guards'

const at = (path) => onboardingGuard({ path })

const AUTH_PATHS = ['/onboarding/start', '/onboarding/login', '/onboarding/signup']
const APP_PATHS = ['/whatif', '/pocket', '/mypage', '/analysis', '/benefit']

describe('① 로그인 전', () => {
  beforeEach(() => localStorage.clear())

  it.each(['/', ...APP_PATHS, '/onboarding/profile'])('%s 로 들어와도 랜딩으로 보낸다', (path) => {
    expect(at(path)).toBe('/onboarding/start')
  })

  it.each(AUTH_PATHS)('%s 는 그대로 통과시킨다', (path) => {
    expect(at(path)).toBe(true)
  })
})

describe('② 로그인했지만 프로필 전', () => {
  beforeEach(() => {
    localStorage.clear()
    markLoggedIn('suhyeon')
  })

  // 프로필을 건너뛰고 홈이 열리면 서버에 프로필이 없는 채로 앱을 걷게 된다
  it.each(['/', ...APP_PATHS])('%s 로 들어와도 프로필로 보낸다', (path) => {
    expect(at(path)).toBe('/onboarding/profile')
  })

  it('프로필 화면은 통과시킨다', () => {
    expect(at('/onboarding/profile')).toBe(true)
  })

  // 로그인은 이미 했으니 가입·로그인 화면으로 돌아갈 이유가 없다
  it.each(AUTH_PATHS)('%s 재진입도 프로필로 보낸다', (path) => {
    expect(at(path)).toBe('/onboarding/profile')
  })
})

describe('③ 둘 다 끝난 뒤', () => {
  beforeEach(() => {
    localStorage.clear()
    markLoggedIn('suhyeon')
    markOnboarded()
  })

  it.each(APP_PATHS)('%s 를 그대로 통과시킨다', (path) => {
    expect(at(path)).toBe(true)
  })

  // 홈은 What-if 다(결정 C-1). 되돌아가면 프로필을 두 번 만들게 된다
  it.each([...AUTH_PATHS, '/onboarding/profile'])('%s 재진입은 홈으로 막는다', (path) => {
    expect(at(path)).toBe('/whatif')
  })
})

describe('플래그', () => {
  beforeEach(() => localStorage.clear())

  it('저장 전에는 둘 다 false 다', () => {
    expect(isLoggedIn()).toBe(false)
    expect(isOnboarded()).toBe(false)
  })

  it('markLoggedIn 은 아이디를 남기고 비밀번호는 담지 않는다', () => {
    markLoggedIn('  suhyeon  ')
    expect(isLoggedIn()).toBe(true)
    // 앞뒤 공백은 지운다 — 로그인 화면의 입력과 그대로 비교해야 한다
    expect(getLoginId()).toBe('suhyeon')
    expect(JSON.stringify(localStorage)).not.toContain('password')
  })

  it('빈 아이디로는 로그인 상태가 되지 않는다', () => {
    markLoggedIn('   ')
    expect(isLoggedIn()).toBe(false)
  })

  it('로그아웃은 로그인만 지운다 — 프로필은 남는다', () => {
    markLoggedIn('suhyeon')
    markOnboarded()
    clearLoggedIn()

    expect(isLoggedIn()).toBe(false)
    expect(isOnboarded()).toBe(true)
    expect(at('/whatif')).toBe('/onboarding/start')
  })

  /*
   * 401 이면 데모 키와 함께 둘 다 지운다 — 서버가 모르는 키라 가입을 안 한 것과 같다.
   * 로그인만 남겨 두면 프로필 화면에서 다시 401 이 나 제자리를 돈다.
   */
  it('clearOnboarded 는 로그인까지 함께 지운다', () => {
    markLoggedIn('suhyeon')
    markOnboarded()
    clearOnboarded()

    expect(isOnboarded()).toBe(false)
    expect(isLoggedIn()).toBe(false)
    expect(at('/whatif')).toBe('/onboarding/start')
  })
})
