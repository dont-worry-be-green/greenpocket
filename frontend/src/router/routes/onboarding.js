/*
 * 온보딩 화면 (ONB) — 이슈 #121
 *   ONB-01  랜딩 (로그인 / 회원가입 두 갈래)
 *   ONB-01a 로그인
 *   ONB-01b 회원가입 (본인확인 + 계정 만들기 — 한 화면)
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
     * **한 화면이다.** 본인확인(이름·번호 → 인증번호)이 위, 계정(아이디·비밀번호)이 아래고
     * 잠기는 것은 가입 CTA 하나다. 화면을 쪼개지 않는 이유는 히스토리이기도 하다 —
     * 라우트로 나누면 뒤로가기를 세 번 눌러야 랜딩을 벗어난다.
     *
     * 합친 것은 **화면 수이지 누름 수가 아니다.** 인증번호 받기·확인·가입 세 버튼은 그대로
     * 남는다. 그 근거는 `views/onboarding/SignupView.vue` 주석에 있다.
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
