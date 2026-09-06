/*
 * 녹색생활실천 화면 (BN) — api-spec.md 15.3
 *   BN-01 녹색생활실천 미참여
 *   BN-02 녹색생활실천 참여 메인
 *   BN-03 실천항목 상세
 *
 * 경로는 폴더명(greenlife)이 아니라 탭 이름(/benefit)을 따른다.
 * 탭 정의는 components/layout/tabs.js, meta 형태는 routes/eco.js 주석 참고.
 *
 * 추가 형태는 routes/onboarding.js 주석 참고.
 */
export default [
  {
    // BN-01·BN-02 는 라우트가 아니라 한 화면의 두 상태다. GET /greenlife/status 의 screen 이 정한다
    path: '/benefit',
    name: 'bn-home',
    component: () => import('@/views/greenlife/BenefitHomeView.vue'),
    meta: { tab: 'benefit', title: '혜택' },
  },
  {
    path: '/benefit/items/:itemId',
    name: 'bn-item-detail',
    component: () => import('@/views/greenlife/BenefitItemDetailView.vue'),
    meta: { tab: 'benefit', title: '실천항목 상세' },
  },
]
