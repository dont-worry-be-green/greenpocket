/*
 * 에코마일리지 화면 (WF) — api-spec.md 15.3
 *   WF-01 에코마일리지 미연동      WF-07 전달 리포트 상세
 *   WF-02 사용량 불러오는 중       WF-08 실천 다시 고르기
 *   WF-03 목표 미설정 메인         WF-09 평가 종료 팝업
 *   WF-04 평가 기간 목표 정하기    WF-10 평가 결과 상세
 *   WF-05 미등록 요금 목표 상태    WF-11 마일리지 적립·현금 전환
 *   WF-06 목표 설정 후 메인 (홈)
 *
 * 경로는 폴더명(eco)이 아니라 탭 이름(/whatif)을 따른다. 탭 정의는 components/layout/tabs.js.
 *
 * meta 는 AppTabLayout 이 헤더를 그리는 데 쓴다.
 *   tab      탭바에서 켜질 키 (tabs.js 의 key)
 *   title    화면 제목
 *   subtitle 제목 밑 한 줄 설명 (선택)
 *
 * 추가 형태는 routes/onboarding.js 주석 참고.
 */
export default [
  {
    /*
     * 홈. 온보딩을 마치면 항상 What-if 탭으로 진입한다 — 마지막 방문 탭 복원은 만들지 않는다(결정 C-1).
     * 온보딩 미완료 시 ONB-01 로 보내는 가드(COM-02)는 `router/guards.js` 에 있다.
     */
    path: '/',
    redirect: '/whatif',
  },
  {
    path: '/whatif',
    name: 'wf-home',
    /*
     * WF-01 · WF-02 · WF-03 은 라우트가 아니라 한 홈의 세 상태다(api-spec.md 10.1).
     * 뷰가 GET /eco/home 의 screen 값으로 분기한다. WF-06 도 여기에 붙는다.
     */
    component: () => import('@/views/eco/WhatIfHomeView.vue'),
    // 제목·부제가 화면 상태마다 달라 subtitle 은 뷰가 AppTabLayout 에 직접 넘긴다
    meta: { tab: 'whatif', title: 'Green What-if' },
  },
  {
    path: '/whatif/link',
    name: 'wf-01a-verify',
    /*
     * WF-01a 본인확인. ⚠️ **기능명세서에 없는 화면이다.**
     *
     * `POST /eco/link` 는 `X-Demo-Key` 밖에 모르는데 「작년 우리 집 사용량」을 내려준다.
     * 신원을 잇는 단계가 흐름에 빠져 있어 그 자리를 화면으로 채운다. 서버 계약은 그대로다.
     *
     * 미가입 안내(WF-01b)는 라우트가 아니라 이 화면의 두 번째 상태다.
     */
    component: () => import('@/views/eco/EcoLinkVerifyView.vue'),
    meta: { tab: 'whatif', title: '에코마일리지 본인확인' },
  },
  {
    path: '/whatif/goal',
    name: 'wf-goal',
    /*
     * WF-04(등록)·WF-05(일부 미등록)도 한 화면의 두 상태다. goal-form 의
     * segments[].registered 로 갈리므로 경로를 나누지 않는다.
     *
     * roundId 를 경로에 두지 않는다 — 목표는 **현재 회차**에만 정할 수 있어서
     * 지난 회차 번호가 URL 에 들어오면 서버가 거절한다. 뷰가 스토어에서 가져온다.
     */
    component: () => import('@/views/eco/GoalSettingView.vue'),
    meta: { tab: 'whatif', title: '평가 기간 목표 정하기' },
  },
  {
    path: '/whatif/report',
    name: 'wf-report',
    /*
     * WF-07. `?month=` 는 선택이다 — 없으면 서버가 가장 최근에 채점 가능한 달을 고른다.
     * 쿼리는 라우트에 선언하지 않는다(뷰가 route.query 로 읽는다).
     */
    component: () => import('@/views/eco/MonthlyReportView.vue'),
    meta: { tab: 'whatif', title: '전달 리포트' },
  },
  {
    path: '/whatif/missions',
    name: 'wf-mission-adjust',
    /*
     * WF-08. `?utility=ELECTRICITY&month=2026-07` 로 들어온다.
     * ⚠️ **쿼리 키는 `utility`, 응답 필드는 `utilityType` 이다.** 이름이 다르다.
     *
     * roundId 를 경로에 두지 않는다 — 미션 교체는 **현재 회차**에만 가능하다.
     */
    component: () => import('@/views/eco/MissionAdjustView.vue'),
    meta: { tab: 'whatif', title: '실천 다시 고르기' },
  },
  {
    path: '/whatif/rounds/:roundId/result',
    name: 'wf-round-result',
    /*
     * WF-10. ⚠️ **여기의 roundId 는 지난 회차다.** 위 세 화면과 반대로 경로에 회차를 둔다 —
     * 확정된 회차만 결과가 있고 그건 현재 회차가 아니다. 뷰가 `store.roundId`(현재 회차)를
     * 쓰면 아직 확정 전이라 409 가 난다.
     *
     * WF-09 결산 모달이 `home.resultModal.roundId` 로, 진단 없이도 링크로 들어올 수 있다.
     */
    component: () => import('@/views/eco/RoundResultView.vue'),
    meta: { tab: 'whatif', title: '평가 결과' },
  },
  {
    path: '/whatif/rounds/:roundId/settlement',
    name: 'wf-settlement',
    /*
     * WF-11. 결과와 같은 지난 회차다.
     * 「현금으로 바꾸기」는 `POST /pocket/conversions` 라 **포켓 도메인**이고,
     * 이 화면은 `/pocket` 으로 보내기만 한다.
     */
    component: () => import('@/views/eco/SettlementView.vue'),
    meta: { tab: 'whatif', title: '마일리지 적립' },
  },
]
