/*
 * 회원가입·로그인 스토어 — ONB-01 · ONB-01a · ONB-01b (이슈 #121)
 *
 * ── 무엇을 들고 있고 무엇을 안 들고 있나 ────────────────────────────────
 * 문자인증의 만료 시간·데모 코드만 들고 있는다. **입력한 번호·인증번호·비밀번호는 담지 않는다** —
 * 화면에서만 살다 죽는 값이고, 서버로도 가지 않는다(`api/auth.js` 주석).
 *
 * ── 진입 플래그는 여기서 세운다 ─────────────────────────────────────────
 * 가입·로그인이 성공하면 `markLoggedIn()`, 프로필 저장은 `stores/onboarding.js` 의
 * `saveProfile()` 이 `markOnboarded()` 를 세운다. 가드는 그 둘만 본다(`router/guards.js`).
 *
 * ── 가입이 `POST /users` 를 부르는 이유 ─────────────────────────────────
 * BE 에 `POST /auth/signup` 이 아직 없다. 그 호출이 데모 키를 서버에 등록해 나머지 API 를
 * 여는 조건이라, 온보딩 스토어의 `startUser()` 를 그대로 빌려 쓴다 — 그쪽이 `user` 까지
 * 채워 주므로 ONB-02 가 바로 이어진다. 계약이 붙으면 `api/auth.js` 만 갈아끼운다.
 */

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  checkLoginId as checkLoginIdApi,
  login as loginApi,
  requestSmsCode as requestSmsCodeApi,
  verifySmsCode,
} from '@/api/auth'
import { clearLoggedIn, getLoginId, markLoggedIn } from '@/router/guards'
import { useOnboardingStore } from '@/stores/onboarding'

export const useAuthStore = defineStore('auth', () => {
  /*
   * ── 로딩은 **어떤 동작인지까지** 들고 있는다 ──────────────────────────
   * 회원가입 화면 하나에 버튼이 넷이다(인증번호 받기 · 확인 · 중복확인 · 가입).
   * 참/거짓 하나로 두면 **하나를 눌렀는데 넷이 모두 「확인 중」이 된다.**
   * 그래서 지금 무엇이 도는지를 담고, 화면은 자기 버튼에 해당하는 것만 본다.
   */
  const pending = ref('')
  const error = ref(null)

  /** 문자인증 상태. 만료 시간은 재전송하면 다시 내려온다 */
  const smsExpiresInSeconds = ref(0)

  /** 이 기기에 가입해 둔 아이디. 로그인 화면이 프리필에 쓴다 */
  const savedLoginId = computed(() => getLoginId())

  /** 아무거나 돌고 있나. 버튼이 하나뿐인 화면(ONB-01a)은 이것만 보면 된다 */
  const isLoading = computed(() => pending.value !== '')

  const isSendingSms = computed(() => pending.value === 'SMS')
  const isVerifyingCode = computed(() => pending.value === 'CODE')
  const isCheckingLoginId = computed(() => pending.value === 'LOGIN_ID')
  const isSigningUp = computed(() => pending.value === 'SIGNUP')

  async function run(task, kind = 'GENERAL') {
    pending.value = kind
    error.value = null
    try {
      return await task()
    } catch (nextError) {
      error.value = nextError
      return null
    } finally {
      pending.value = ''
    }
  }

  // ── 문자인증 ──────────────────────────────────────────────────────────

  async function requestSmsCode() {
    const data = await run(requestSmsCodeApi, 'SMS')
    if (data) smsExpiresInSeconds.value = data.expiresInSeconds ?? 0
    return data
  }

  /** 틀린 인증번호는 에러가 아니라 `verified: false` 다. 문구는 화면이 만든다 */
  async function verifyCode(code) {
    return run(() => verifySmsCode(code), 'CODE')
  }

  // ── 아이디 ────────────────────────────────────────────────────────────

  /** 중복확인. 「이미 쓴다」는 에러가 아니라 `available: false` 다. 문구는 화면이 만든다 */
  async function checkLoginId(loginId) {
    return run(() => checkLoginIdApi({ loginId, savedLoginId: savedLoginId.value }), 'LOGIN_ID')
  }

  // ── 가입 · 로그인 ─────────────────────────────────────────────────────

  /**
   * 회원가입. 성공하면 로그인 상태가 되고 ONB-02 로 이어진다.
   * 비밀번호는 받기만 하고 아무 데도 보내지 않는다(위 주석).
   */
  async function signup({ name, loginId }) {
    const onboarding = useOnboardingStore()
    const started = await run(() => onboarding.startUser(name), 'SIGNUP')
    if (!started) {
      // 온보딩 스토어가 자기 error 에 담는다. 화면이 둘 다 보지 않게 여기로 옮긴다
      error.value = onboarding.error
      return null
    }
    markLoggedIn(loginId)
    return started
  }

  /** 로그인. 이 기기에 가입한 아이디와 맞춰 본다 — 못 찾으면 에러가 아니라 안내다 */
  async function login({ loginId }) {
    const data = await run(() => loginApi({ loginId, savedLoginId: savedLoginId.value }))
    if (data?.found) markLoggedIn(data.loginId)
    return data
  }

  /** 로그아웃. 데모 키는 남긴다 — 같은 기기에서 다시 로그인하면 그대로 이어진다 */
  function logout() {
    clearLoggedIn()
  }

  return {
    isLoading,
    isSendingSms,
    isVerifyingCode,
    isCheckingLoginId,
    isSigningUp,
    error,
    smsExpiresInSeconds,
    savedLoginId,
    requestSmsCode,
    verifyCode,
    checkLoginId,
    signup,
    login,
    logout,
  }
})
