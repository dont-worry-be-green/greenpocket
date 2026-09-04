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
     * 온보딩 미완료 시 ONB-01 로 보내는 가드(COM-02)는 ONB 라우트가 생긴 뒤에 붙인다.
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
     * WF-07. 화면은 아직 없다 — WF-06 의 「자세히」가 갈 곳을 먼저 만들어 둔다.
     * 라우트가 없으면 router.push 가 빈 화면으로 떨어져 무반응이 된다(COM-08).
     * **실제 화면이 붙으면 component 한 줄만 바꾼다.**
     */
    component: () => import('@/views/ComingSoonView.vue'),
    meta: { tab: 'whatif', title: '전달 리포트' },
  },
]
