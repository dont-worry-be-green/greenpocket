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
 * ⚠️ `roundId 6` 은 **확정된 지난 회차**다. 진행 중인 7 을 넣으면 픽스처가 일부러
 * `ECO_RESULT_NOT_CONFIRMED` 를 낸다(`api/eco.js` 의 `getRoundResult` 주석).
 */
export const DEMO_SHORTCUTS = [
  {
    group: 'What-if 홈 (한 화면의 다섯 상태)',
    items: [
      { label: 'WF-01 연동 전', to: '/whatif?preview=WF_01_UNLINKED' },
      { label: 'WF-02 불러오는 중', to: '/whatif?preview=WF_02_LINKING' },
      { label: 'WF-03 목표 미설정', to: '/whatif?preview=WF_03_NO_GOAL' },
      { label: 'WF-06 목표 설정 후', to: '/whatif?preview=WF_06_IN_PROGRESS' },
      { label: 'WF-09 결산 모달', to: '/whatif?preview=WF_09_RESULT_READY' },
    ],
  },
  {
    group: '목표 · 실천',
    items: [
      { label: 'WF-01a 본인확인', to: '/whatif/link' },
      { label: 'WF-04 목표 정하기', to: '/whatif/goal' },
      { label: 'WF-05 미등록 요금', to: '/whatif/goal?preview=WF-05' },
      { label: 'WF-08 실천 다시 고르기', to: '/whatif/missions?utility=ELECTRICITY&month=2026-07' },
    ],
  },
  {
    group: '리포트 · 결과',
    items: [
      { label: 'WF-07 전달 리포트', to: '/whatif/report' },
      { label: 'WF-07 고지서 없는 달', to: '/whatif/report?month=2026-08' },
      { label: 'WF-10 평가 결과', to: '/whatif/rounds/6/result' },
      { label: 'WF-11 마일리지 적립', to: '/whatif/rounds/6/settlement' },
    ],
  },
]
