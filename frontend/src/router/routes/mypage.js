/*
 * 마이페이지 화면 (MY) — api-spec.md 15.3
 *   MY-01 마이 메인            MY-03 고지서 보관함
 *   MY-02 정책 추천 조건       MY-04 리포트 보관함
 *   MY-05 청년정책 목록        MY-06 청년정책 상세
 *
 * meta 형태는 routes/eco.js 주석 참고. 추가 형태는 routes/onboarding.js 주석 참고.
 */
export default [
  {
    path: '/mypage',
    name: 'my-home',
    component: () => import('@/views/mypage/MypageHomeView.vue'),
    meta: { tab: 'mypage', title: '마이' },
  },
  {
    path: '/mypage/policy-preferences',
    name: 'my-02-policy-preferences',
    component: () => import('@/views/mypage/ProfileEditView.vue'),
    meta: { title: '추천 조건 설정' },
  },
  {
    path: '/mypage/bills',
    name: 'my-03-bill-archive',
    /*
     * MY-03. 필터는 `?utility=ELECTRICITY&year=2026` 로 URL 이 들고 있다.
     * ⚠️ **쿼리 키는 `utility`, 응답 필드는 `utilityType` 이다**(WF-08 과 같은 함정).
     * 쿼리는 라우트에 선언하지 않는다 — 뷰가 `route.query` 로 읽는다.
     *
     * 기능 ID 는 A-2-12(고지서 도메인)이지만 **화면은 마이페이지 것**이라 여기에 둔다.
     */
    component: () => import('@/views/mypage/BillArchiveView.vue'),
    meta: { title: '고지서 보관함' },
  },
  {
    path: '/mypage/reports',
    name: 'my-04-report-archive',
    /*
     * MY-04. 탭(월별·ECO)은 URL 이 아니라 화면 상태다 — 보관함은 한 번에 훑는 화면이고
     * 탭까지 URL 에 올리면 뒤로가기가 탭 전환을 되짚는다(MY-03 의 필터와 다르다).
     */
    component: () => import('@/views/mypage/ReportArchiveView.vue'),
    meta: { title: '리포트 보관함' },
  },
  {
    path: '/mypage/policies',
    name: 'my-05-policy-list',
    component: () => import('@/views/mypage/PolicyListView.vue'),
    meta: { title: '청년정책' },
  },
  {
    path: '/mypage/policies/:policyId',
    name: 'my-06-policy-detail',
    component: () => import('@/views/mypage/PolicyDetailView.vue'),
    meta: { title: '청년정책 상세' },
  },
]
