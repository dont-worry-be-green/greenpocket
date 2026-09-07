/*
 * 첫 진입 가드 (COM-02) — **두 단계다.**
 *
 *   ① 로그인했나   아니면 ONB-01 랜딩으로
 *   ② 온보딩 마쳤나 아니면 ONB-02 프로필로
 *   둘 다 됐으면 앱. 홈은 What-if 탭이다(결정 C-1).
 *
 * 온보딩을 걷지 않으면 `POST /users` 가 불리지 않아 `X-Demo-Key` 가 서버에 등록되지 않고,
 * 그러면 나머지 API 가 전부 401 이다(`DemoKeyAuthenticationInterceptor`). 그래서 가입 전에는
 * 앱의 다른 화면을 열지 않는다.
 *
 * ── ⚠️ 판정 근거가 로컬 플래그다 ────────────────────────────────────────
 * 원래는 `GET /users/me` 의 `entryScreen` 이어야 한다(api-spec 4.2). 지금은 두 값 다
 * `localStorage` 이고, 그래서 **기기를 옮기면 이어지지 않는다.** 로그인이 아이디만 맞춰 보는
 * 것도 같은 한계다(`api/auth.js` 주석). BE 에 로그인이 붙으면 이 파일과 그 파일만 고친다
 * (이슈 #89 · #121).
 */

const LOGIN_STORAGE = 'greenpocket.loginId'
const ONBOARDED_STORAGE = 'greenpocket.onboarded'

/** 로그인 없이 들어갈 수 있는 화면. 여기서는 가드가 비켜선다 */
const AUTH_PATHS = ['/onboarding/start', '/onboarding/login', '/onboarding/signup']
const PROFILE_PATH = '/onboarding/profile'

// ── 로그인 (ONB-01a · ONB-01b) ────────────────────────────────────────

export function isLoggedIn() {
  return Boolean(getLoginId())
}

/** 이 기기에 가입해 둔 아이디. **비밀번호는 어디에도 담지 않는다** */
export function getLoginId() {
  return localStorage.getItem(LOGIN_STORAGE) ?? ''
}

export function markLoggedIn(loginId) {
  const value = String(loginId ?? '').trim()
  if (value) localStorage.setItem(LOGIN_STORAGE, value)
}

export function clearLoggedIn() {
  localStorage.removeItem(LOGIN_STORAGE)
}

// ── 온보딩 (ONB-02) ───────────────────────────────────────────────────

export function isOnboarded() {
  return localStorage.getItem(ONBOARDED_STORAGE) === 'true'
}

export function markOnboarded() {
  localStorage.setItem(ONBOARDED_STORAGE, 'true')
}

/** 401 로 데모 키를 버릴 때 함께 지운다 — 키가 없으면 가입을 안 한 것과 같다 */
export function clearOnboarded() {
  localStorage.removeItem(ONBOARDED_STORAGE)
  clearLoggedIn()
}

export function onboardingGuard(to) {
  const isAuthRoute = AUTH_PATHS.includes(to.path)
  const isProfileRoute = to.path === PROFILE_PATH

  // ① 로그인 전. 어디를 치든 랜딩·로그인·회원가입 셋 안에서만 움직인다
  if (!isLoggedIn()) return isAuthRoute ? true : '/onboarding/start'

  // ② 로그인은 했지만 프로필이 없다. 프로필 화면 하나만 열어 준다
  if (!isOnboarded()) return isProfileRoute ? true : PROFILE_PATH

  // 둘 다 끝났다. 온보딩으로 되돌아갈 이유가 없다
  return isAuthRoute || isProfileRoute ? '/whatif' : true
}
