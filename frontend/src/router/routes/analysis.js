/*
 * 생활비 분석 화면 (AN) — api-spec.md 15.3
 *   AN-01 고지서 미등록 메인      AN-05 인식 내용 수정·직접 입력
 *   AN-02 사진·직접 입력 선택      AN-06 생활요금 최종 확인
 *   AN-03 OCR 분석 중             AN-07 생활비 분석 메인
 *   AN-04 OCR 결과 확인           AN-08 고지서 상세·수정
 *
 * 탭바(COM-02)를 세우려면 탭 5개가 모두 갈 곳이 있어야 해서 진단 탭 루트만 미리 물려 둔다.
 * **AN-01 을 붙일 때 아래 component 한 줄만 바꾸면 된다.** meta 형태는 routes/eco.js 주석 참고.
 *
 * 추가 형태는 routes/onboarding.js 주석 참고.
 */
export default [
  {
    path: '/analysis',
    name: 'an-home',
    component: () => import('@/views/analysis/AnalysisHomeView.vue'),
    meta: { tab: 'analysis', title: '진단' },
  },
  {
    path: '/analysis/bills/new',
    name: 'an-bill-create',
    component: () => import('@/views/analysis/BillRegistrationView.vue'),
    meta: { title: '고지서 등록' },
  },
  {
    path: '/analysis/bills/analyzing',
    name: 'an-bill-analyzing',
    component: () => import('@/views/ComingSoonView.vue'),
    meta: { title: '고지서 분석' },
  },
  {
    path: '/analysis/bills/edit',
    name: 'an-bill-edit',
    redirect: (to) => ({
      path: '/analysis/bills/new',
      query: { ...to.query, mode: 'manual' },
    }),
    meta: { title: '인식 내용 수정' },
  },
  {
    path: '/analysis/bills/confirm',
    name: 'an-bill-confirm',
    component: () => import('@/views/ComingSoonView.vue'),
    meta: { title: '고지서 최종 확인' },
  },
]
