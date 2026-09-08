import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  getCurrentUser,
  login as loginApi,
  logout as logoutApi,
  refreshSession,
  requestSmsCode as requestSmsCodeApi,
  signup as signupApi,
  verifySmsCode,
} from '@/api/auth'
import { clearAccessToken } from '@/api/client'

export const useAuthStore = defineStore('auth', () => {
  const pending = ref('')
  const error = ref(null)
  const smsExpiresInSeconds = ref(0)
  const authenticated = ref(false)
  const onboardingCompleted = ref(false)
  const sessionChecked = ref(false)
  const user = ref(null)
  let restorePromise = null

  const isLoading = computed(() => pending.value !== '')
  const isSendingSms = computed(() => pending.value === 'SMS')
  const isVerifyingCode = computed(() => pending.value === 'CODE')
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

  function applySession(data) {
    authenticated.value = true
    onboardingCompleted.value = Boolean(data?.onboardingCompleted)
    sessionChecked.value = true
    user.value = data ?? null
  }

  function clearSession() {
    clearAccessToken()
    authenticated.value = false
    onboardingCompleted.value = false
    sessionChecked.value = true
    user.value = null
  }

  async function restoreSession() {
    if (sessionChecked.value) return authenticated.value
    if (!restorePromise) {
      restorePromise = (async () => {
        try {
          await refreshSession()
          const bootstrap = await getCurrentUser()
          applySession(bootstrap)
          return true
        } catch {
          clearSession()
          return false
        } finally {
          restorePromise = null
        }
      })()
    }
    return restorePromise
  }

  async function requestSmsCode() {
    const data = await run(requestSmsCodeApi, 'SMS')
    if (data) smsExpiresInSeconds.value = data.expiresInSeconds ?? 0
    return data
  }

  function verifyCode(code) {
    return run(() => verifySmsCode(code), 'CODE')
  }

  async function signup(payload) {
    const data = await run(() => signupApi(payload), 'SIGNUP')
    if (data) applySession(data)
    return data
  }

  async function login(payload) {
    const data = await run(() => loginApi(payload), 'LOGIN')
    if (data) applySession(data)
    return data
  }

  function completeOnboarding() {
    onboardingCompleted.value = true
    if (user.value) user.value = { ...user.value, onboardingCompleted: true, entryScreen: 'WF-06' }
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 서버 세션이 이미 끝났어도 이 기기의 로그인 상태는 반드시 끝낸다.
    } finally {
      clearSession()
    }
  }

  return {
    authenticated,
    onboardingCompleted,
    sessionChecked,
    user,
    isLoading,
    isSendingSms,
    isVerifyingCode,
    isSigningUp,
    error,
    smsExpiresInSeconds,
    requestSmsCode,
    verifyCode,
    signup,
    login,
    restoreSession,
    completeOnboarding,
    clearSession,
    logout,
  }
})
