/*
 * 온보딩 화면 (ONB) — 이슈 #121
 *   ONB-01  랜딩 (로그인 / 회원가입 두 갈래)
 *   ONB-01a 로그인
 *   ONB-01b 회원가입 (본인확인 → 계정 만들기)
 *   ONB-02  주거 프로필
 *
 * 네 화면 모두 탭바가 없다. ONB-01 은 첫 화면이라 뒤로가기도 없어서 셸을 쓰지 않고,
 * 나머지는 `AppSubLayout` 으로 앞 화면으로 돌아간다.
 *
 * 진입 가드(COM-02)는 `router/guards.js` 에 있고 **두 단계다** — 로그인했나 · 온보딩 마쳤나.
 * 그 판정이 서버가 아니라 로컬 플래그인 이유도 거기 적어 두었다.
 */
export default [
  {
    path: '/onboarding/start',
    name: 'onb-01-start',
    component: () => import('@/views/onboarding/StartView.vue'),
  },
  {
    path: '/onboarding/login',
    name: 'onb-01a-login',
    /*
     * ⚠️ **비밀번호를 검증하지 않는다.** BE 에 `POST /auth/login` 이 아직 없어 대조할 곳이
     * 없다. 이 기기에 가입한 아이디와 맞춰 보는 수준이고 화면이 그 사실을 밝힌다.
     */
    component: () => import('@/views/onboarding/LoginView.vue'),
  },
  {
    path: '/onboarding/signup',
    name: 'onb-01b-signup',
    /*
     * 본인확인(번호 → 인증번호) → 계정 만들기. 세 단계가 라우트가 아니라 한 화면의 상태다 —
     * 히스토리가 늘면 뒤로가기를 세 번 눌러야 랜딩을 벗어난다.
     */
    component: () => import('@/views/onboarding/SignupView.vue'),
  },
  {
    path: '/onboarding/profile',
    name: 'onb-02-profile',
    /*
     * 로그인해야 들어올 수 있다. 그 판정은 뷰가 아니라 가드가 한다 —
     * 예전에는 뷰가 스토어의 `user` 를 보고 되돌려 보냈는데, 새로고침하면 그 값이 사라져
     * 정상 진입까지 막혔다.
     */
    component: () => import('@/views/onboarding/ProfileView.vue'),
  },
]
