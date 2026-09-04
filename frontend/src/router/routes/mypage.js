/*
 * 마이페이지 화면 (MY) — api-spec.md 15.3
 *   MY-01 마이페이지 메인
 *   MY-02 기본 정보 수정
 *   MY-03 고지서 보관함
 *   MY-04 리포트 보관함
 *
 * 탭바(COM-02)를 세우려면 탭 5개가 모두 갈 곳이 있어야 해서 마이페이지 탭 루트만 미리 물려 둔다.
 * **MY-01 을 붙일 때 아래 component 한 줄만 바꾸면 된다.** meta 형태는 routes/eco.js 주석 참고.
 *
 * 추가 형태는 routes/onboarding.js 주석 참고.
 */
export default [
  {
    path: '/mypage',
    name: 'my-home',
    // TODO(마이페이지 담당): MY-01 마이페이지 메인으로 교체
    component: () => import('@/views/ComingSoonView.vue'),
    meta: { tab: 'mypage', title: '마이페이지' },
  },
]
