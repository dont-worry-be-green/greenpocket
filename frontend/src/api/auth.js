import client, { clearAccessToken, refreshAccessToken, setAccessToken } from './client'

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

export async function signup({ email, password, name }) {
  const data = await client.post('/auth/signup', {
    email: String(email ?? '').trim(),
    password,
    name: String(name ?? '').trim(),
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

export function getCurrentUser() {
  return client.get('/users/me')
}

export function refreshSession() {
  return refreshAccessToken()
}

export async function logout() {
  try {
    await client.post('/auth/logout', null, { skipAuthRefresh: true, skipAuthRedirect: true })
  } finally {
    clearAccessToken()
  }
}
