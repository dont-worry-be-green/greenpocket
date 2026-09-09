/*
 * 온보딩 화면 (ONB) — 이슈 #121
 *   ONB-01  로그인
 *   ONB-01a 회원가입 (본인확인 + 계정 만들기 — 한 화면)
 *
 * 두 화면 모두 탭바가 없다. 첫 화면은 로그인이고 회원가입은 로그인 화면의 CTA로 진입한다.
 *
 * 별도 온보딩은 제거됐다(결정 C-26). 가입·로그인 후에는 진단 탭의
 * `/analysis/eco-link`에서 에코 연동과 기준 사용량 확인을 진행한다.
 */
export default [
  {
    path: '/onboarding/start',
    redirect: '/onboarding/login',
  },
  {
    path: '/onboarding/login',
    name: 'onb-01-login',
    component: () => import('@/views/onboarding/LoginView.vue'),
  },
  {
    path: '/onboarding/signup',
    name: 'onb-01a-signup',
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
    // 예전 ONB-02 주소로 들어와도 폐기된 주거 프로필을 다시 노출하지 않는다.
    path: '/onboarding/profile',
    redirect: '/analysis/eco-link',
  },
]
