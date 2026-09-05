/*
 * 첫 진입 가드 (COM-02)
 *
 * 온보딩을 걷지 않으면 `POST /users` 가 불리지 않아 `X-Demo-Key` 가 서버에 등록되지 않고,
 * 그러면 나머지 API 가 전부 401 이다(`DemoKeyAuthenticationInterceptor`).
 * 그래서 온보딩 전에는 앱의 다른 화면을 열지 않는다.
 *
 * ⚠️ **판정 근거가 로컬 플래그다.** 원래는 `GET /users/me` 의 `entryScreen` 이어야 하지만,
 * 그 값의 재료인 `POST /profile` 이 백엔드에 없어 `onboardingCompleted` 가 영원히 false 다.
 * 서버 값을 그대로 믿으면 온보딩을 끝내도 다시 온보딩으로 밀려 **무한 리다이렉트**가 된다.
 * BE 가 붙으면 이 파일 하나만 `GET /users/me` 로 갈아끼운다 (이슈 #89).
 */

const ONBOARDED_STORAGE = 'greenpocket.onboarded'

/** 온보딩 안에서만 쓰는 경로. 여기서는 가드가 비켜선다 */
const ONBOARDING_PREFIX = '/onboarding'

export function isOnboarded() {
  return localStorage.getItem(ONBOARDED_STORAGE) === 'true'
}

export function markOnboarded() {
  localStorage.setItem(ONBOARDED_STORAGE, 'true')
}

/** 401 로 데모 키를 버릴 때 함께 지운다 — 키가 없으면 온보딩을 안 한 것과 같다 */
export function clearOnboarded() {
  localStorage.removeItem(ONBOARDED_STORAGE)
}

export function onboardingGuard(to) {
  const isOnboardingRoute = to.path.startsWith(ONBOARDING_PREFIX)

  // 아직 안 걸었다면 어디를 치든 첫 화면부터다
  if (!isOnboarded()) return isOnboardingRoute ? true : '/onboarding/start'

  // 이미 걸었다면 다시 들어갈 이유가 없다. 홈은 What-if 다(결정 C-1)
  return isOnboardingRoute ? '/whatif' : true
}
