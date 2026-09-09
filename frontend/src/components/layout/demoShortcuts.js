/*
 * 데모 도구 · 화면 바로가기 목록 (개발 빌드 전용)
 *
 * `components/layout/tabs.js` 와 같은 자리다 — 목록이 컴포넌트 안에 있으면 테스트가 읽을 수 없다.
 * `views/eco/__tests__/DemoShortcuts.spec.js` 가 이 목록을 그대로 걸어 보며
 * **죽은 바로가기가 생기지 않는지** 확인한다.
 *
 * **여기서 상태를 만들지 않는다.** 전부 뷰가 이미 읽고 있는 쿼리·경로일 뿐이라,
 * 이 파일을 지워도 화면은 그대로 동작한다(`api/eco.js` 의 `previewParam` · 각 뷰의 `route.query`).
 *
 * ⚠️ `roundId 7` 은 **확정된 지난 회차**다. 진행 중인 8 을 넣으면 픽스처가 일부러
 * `ECO_RESULT_NOT_CONFIRMED` 를 낸다(`api/eco.js` 의 `getRoundResult` 주석).
 */
export const DEMO_SHORTCUTS = [
  {
    group: '에코 연동 · What-if 홈',
    items: [
      { label: 'WF-01 연동 전', to: '/analysis/eco-link?preview=WF_01_UNLINKED' },
      { label: 'WF-02 불러오는 중', to: '/analysis/eco-link?preview=WF_02_LINKING' },
      { label: 'WF-03 기준 사용량', to: '/analysis/eco-link?preview=WF_03_NO_GOAL' },
      { label: 'WF-06 목표 설정 후', to: '/whatif?preview=WF_06_IN_PROGRESS' },
      { label: 'WF-09 결산 모달', to: '/whatif?preview=WF_09_RESULT_READY' },
    ],
  },
  {
    group: '목표 · 실천',
    items: [
      { label: 'WF-04 목표 정하기', to: '/whatif/goal' },
      { label: 'WF-05 미등록 요금', to: '/whatif/goal?preview=WF-05' },
    ],
  },
  {
    group: '마이 · 보관함',
    items: [
      { label: 'MY-01 마이', to: '/mypage' },
      { label: 'MY-02 추천 조건 설정', to: '/mypage/policy-preferences' },
      { label: 'MY-05 청년정책 목록', to: '/mypage/policies?mode=recommended' },
      { label: 'MY-03 고지서 보관함', to: '/mypage/bills' },
      { label: 'MY-03 전기 탭', to: '/mypage/bills?utility=ELECTRICITY' },
      { label: 'MY-04 리포트 보관함', to: '/mypage/reports' },
    ],
  },
  {
    group: '리포트 · 결과',
    items: [
      { label: 'WF-07 전달 리포트', to: '/whatif/report' },
      { label: 'WF-07 고지서 없는 달', to: '/whatif/report?month=2026-08' },
      { label: 'WF-10 평가 결과', to: '/whatif/rounds/7/result' },
      { label: 'WF-11 마일리지 적립', to: '/whatif/rounds/7/settlement' },
    ],
  },
]
