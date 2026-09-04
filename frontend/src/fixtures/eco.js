/*
 * 에코마일리지 화면 미리보기 데이터 — **API 연동 전까지만 쓴다.**
 *
 * 필드명·타입은 docs/api/api-spec.md 8절 응답 예시를 그대로 따른다.
 * 백엔드 DTO 와 1:1 대조해 일치를 확인했다(2026-09-04 기준).
 *   ECO_STATUS        ← EcoStatusResponse          GET  /eco/status
 *   ECO_LINK_UTILITIES ← EcoLinkProgressResponse.utilityStatus
 *                                                  GET  /eco/link/{linkJobId}
 *   ECO_CURRENT_ROUND ← EcoCurrentRoundResponse    GET  /eco/rounds/current
 *
 * 연동할 때는 이 파일을 지우고 stores/eco.js 의 응답을 넘기면 화면 코드는 그대로 둔다.
 * **값이나 필드를 임의로 만들지 않는다.** 새 필드가 필요하면 api-spec.md 를 먼저 고친다
 * (frontend/AGENTS.md 9절).
 *
 * 스펙 예시와 다른 값은 두 곳뿐이고, 둘 다 그리려는 화면 상태 때문이다.
 *   · ECO_STATUS      스펙 예시는 LINKED — WF-01 은 연동 전 화면이라 UNLINKED 로 둔다
 *   · ECO_CURRENT_ROUND  스펙 예시는 goalSet:true / GOAL_SET / nextScreen:'WF-06' —
 *                        WF-03 은 목표 미설정 화면이라 false / READY / 'WF-03' 으로 둔다
 */

/** GET /eco/status — WF-01 연동 전 (B-1-01 · B-1-09) */
export const ECO_STATUS = {
  linkStatus: 'UNLINKED',
  linkedAt: null,
  seoulResident: true,
  linkable: true,
  blockReason: null,
  registeredUtilities: [
    { utilityType: 'ELECTRICITY', registered: true, unregisteredReason: null },
    { utilityType: 'GAS', registered: true, unregisteredReason: null },
    { utilityType: 'WATER', registered: true, unregisteredReason: null },
  ],
  eligibleForRound: true,
  ecoAddress: null, // 미연동이면 null (api-spec.md 8.1)
  externalUrl: 'https://ecomileage.seoul.go.kr',
}

/*
 * GET /eco/link/{linkJobId} 의 utilityStatus 초기 상태 — WF-02 (B-1-03).
 * 항상 3건 고정이다(B-2-02). status 는 JobStatus enum: PENDING · RUNNING · SUCCEEDED.
 * 진행 순서와 타이밍은 데이터가 아니라 화면 동작이라 뷰가 들고 있다.
 */
export const ECO_LINK_UTILITIES = [
  { utilityType: 'ELECTRICITY', status: 'PENDING' },
  { utilityType: 'GAS', status: 'PENDING' },
  { utilityType: 'WATER', status: 'PENDING' },
]

/** GET /eco/rounds/current — WF-03 기준 사용량·비중 (B-1-05 · B-1-06 · B-1-07) */
export const ECO_CURRENT_ROUND = {
  roundId: 7,
  periodStart: '2026-04',
  periodEnd: '2026-09',
  remainingMonths: 2,
  roundStatus: 'READY',
  applicationStatus: 'NOT_APPLIED',
  goalSet: false,
  baselineQueriedAt: '2026-09-01T09:00:00+09:00',
  baselineDescription: '2024·2025년 4~9월 평균',

  baseline: {
    totalAmount: 420600,
    totalCarbonG: 831992.0,
    items: [
      {
        utilityType: 'ELECTRICITY',
        registered: true,
        amount: 268000,
        usage: 1340.0,
        usageUnit: 'kWh',
        carbonFactorG: 424.0,
        shareRate: 64.0,
      },
      {
        utilityType: 'GAS',
        registered: true,
        amount: 96600,
        usage: 108.0,
        usageUnit: 'm3',
        carbonFactorG: 2240.0,
        shareRate: 23.0,
      },
      {
        utilityType: 'WATER',
        registered: true,
        amount: 56000,
        usage: 66.0,
        usageUnit: 'm3',
        carbonFactorG: 332.0,
        shareRate: 13.0,
      },
    ],
    largestShareUtility: 'ELECTRICITY',
  },

  nextScreen: 'WF-03',
}
