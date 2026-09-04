/*
 * 그린포켓 화면 (PK) — api-spec.md 15.3
 *   PK-01 계좌 미등록 메인   PK-05 적립 내역
 *   PK-02 계좌 등록 메인     PK-06 그린포켓 관리
 *   PK-03 출금 신청          PK-07 출금계좌 등록·변경
 *   PK-04 출금 완료          PK-08 출금 내역
 *
 * 탭바(COM-02)를 세우려면 탭 5개가 모두 갈 곳이 있어야 해서 포켓 탭 루트만 미리 물려 둔다.
 * **PK-01 을 붙일 때 아래 component 한 줄만 바꾸면 된다.** meta 형태는 routes/eco.js 주석 참고.
 *
 * 추가 형태는 routes/onboarding.js 주석 참고.
 */
export default [
  {
    path: '/pocket',
    name: 'pk-home',
    component: () => import('@/views/pocket/PocketHomeView.vue'),
    meta: { tab: 'pocket', title: '포켓' },
  },
  {
    path: '/pocket/transactions',
    name: 'pk-transactions',
    component: () => import('@/views/pocket/PocketTransactionsView.vue'),
  },
  {
    path: '/pocket/management',
    name: 'pk-management',
    component: () => import('@/views/pocket/PocketManagementView.vue'),
  },
  {
    path: '/pocket/withdraw',
    name: 'pk-withdraw',
    component: () => import('@/views/pocket/PocketWithdrawalView.vue'),
  },
  {
    path: '/pocket/withdraw/complete',
    name: 'pk-withdraw-complete',
    component: () => import('@/views/pocket/PocketWithdrawalCompleteView.vue'),
  },
]
