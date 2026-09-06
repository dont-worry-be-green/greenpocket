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

import { login as loginApi, requestSmsCode as requestSmsCodeApi, verifySmsCode } from '@/api/auth'
import { clearLoggedIn, getLoginId, markLoggedIn } from '@/router/guards'
import { useOnboardingStore } from '@/stores/onboarding'

export const useAuthStore = defineStore('auth', () => {
  const isLoading = ref(false)
  const error = ref(null)

  /** 문자인증 상태. 만료 시간은 재전송하면 다시 내려온다 */
  const smsExpiresInSeconds = ref(0)
  const smsDemoCode = ref('')

  /** 이 기기에 가입해 둔 아이디. 로그인 화면이 프리필에 쓴다 */
  const savedLoginId = computed(() => getLoginId())

  async function run(task) {
    isLoading.value = true
    error.value = null
    try {
      return await task()
    } catch (nextError) {
      error.value = nextError
      return null
    } finally {
      isLoading.value = false
    }
  }

  // ── 문자인증 ──────────────────────────────────────────────────────────

  async function requestSmsCode() {
    const data = await run(requestSmsCodeApi)
    if (data) {
      smsExpiresInSeconds.value = data.expiresInSeconds ?? 0
      // 실제 엔드포인트가 생기면 이 필드가 사라지고 캡션도 함께 사라진다
      smsDemoCode.value = data.demoCode ?? ''
    }
    return data
  }

  /** 틀린 인증번호는 에러가 아니라 `verified: false` 다. 문구는 화면이 만든다 */
  async function verifyCode(code) {
    return run(() => verifySmsCode(code))
  }

  // ── 가입 · 로그인 ─────────────────────────────────────────────────────

  /**
   * 회원가입. 성공하면 로그인 상태가 되고 ONB-02 로 이어진다.
   * 비밀번호는 받기만 하고 아무 데도 보내지 않는다(위 주석).
   */
  async function signup({ name, loginId }) {
    const onboarding = useOnboardingStore()
    const started = await run(() => onboarding.startUser(name))
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
    error,
    smsExpiresInSeconds,
    smsDemoCode,
    savedLoginId,
    requestSmsCode,
    verifyCode,
    signup,
    login,
    logout,
  }
})
