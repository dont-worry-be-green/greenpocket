import { USER_ME } from '@/fixtures/onboarding'

import client, { clearAccessToken, refreshAccessToken, setAccessToken } from './client'
import { isFixtureMode } from './dataSource'

const fake = async (value, ms = 400) => {
  await new Promise((resolve) => setTimeout(resolve, ms))
  return typeof value === 'function' ? value() : value
}

const DEMO_SMS_CODE = '000000'

// 휴대폰 본인확인은 아직 MVP 데모 범위다. 계정 생성과 로그인만 실제 API를 사용한다.
export function requestSmsCode() {
  return fake({ expiresInSeconds: 180 }, 700)
}

export function verifySmsCode(code) {
  return fake(() => ({ verified: String(code ?? '').trim() === DEMO_SMS_CODE }), 900)
}

function rememberAccessToken(data) {
  setAccessToken(data?.accessToken)
  return data
}

export async function signup({ email, password, name, birthDate, gender, phoneNumber }) {
  const data = await client.post('/auth/signup', {
    email: String(email ?? '').trim(),
    password,
    name: String(name ?? '').trim(),
    birthDate,
    gender,
    phoneNumber: String(phoneNumber ?? '').replace(/\D/g, ''),
  })
  return rememberAccessToken(data)
}

export async function login({ email, password }) {
  const data = await client.post('/auth/login', {
    email: String(email ?? '').trim(),
    password,
  })
  return rememberAccessToken(data)
}

/*
 * 목데이터 모드(`api/dataSource.js`)에서는 세션 복구·부트스트랩을 픽스처로 답한다.
 * 그래야 서버 없이 온보딩 가드를 지나 데모 도구의 화면 바로가기가 열린다.
 * 회원가입·로그인은 목데이터가 없다 — 그 둘은 실 API 로만 확인한다.
 */
export function getCurrentUser() {
  if (isFixtureMode()) return fake(USER_ME, 120)
  return client.get('/users/me')
}

export function refreshSession() {
  if (isFixtureMode()) return fake(true, 60)
  return refreshAccessToken()
}

export async function logout() {
  if (isFixtureMode()) {
    clearAccessToken()
    return
  }
  try {
    await client.post('/auth/logout', null, { skipAuthRefresh: true, skipAuthRedirect: true })
  } finally {
    clearAccessToken()
  }
}
