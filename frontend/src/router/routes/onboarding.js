/*
 * 온보딩 화면 (ONB) — api-spec.md 15.3
 *   ONB-01 시작·이름 등록
 *   ONB-02 주거 프로필
 *
 * 두 화면 모두 탭바가 없다. ONB-01 은 첫 화면이라 뒤로가기도 없어서 셸을 쓰지 않고,
 * ONB-02 는 `AppSubLayout` 으로 ONB-01 로 돌아간다.
 *
 * **온보딩 미완료 진입 가드(COM-02)와 401 리다이렉트는 아직 없다.** 지금은 주소창으로만
 * 들어온다 — 가드는 `GET /users/me` 의 `entryScreen` 을 근거로 삼아야 하는데
 * 그 판정에 필요한 `POST /profile` 이 백엔드에 없어 `onboardingCompleted` 가 늘 false 다.
 */
export default [
  {
    path: '/onboarding/start',
    name: 'onb-01-start',
    component: () => import('@/views/onboarding/StartView.vue'),
  },
  {
    path: '/onboarding/profile',
    name: 'onb-02-profile',
    /*
     * 이름을 저장해야 들어올 수 있다. 새로고침하면 스토어의 `user` 가 사라지므로
     * 뷰가 `onMounted` 에서 ONB-01 로 되돌려 보낸다.
     */
    component: () => import('@/views/onboarding/ProfileView.vue'),
  },
]
