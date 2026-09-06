/*
 * 녹색생활실천 API — api-spec.md 12절. 화면 BN-01 · BN-02 · BN-03.
 *
 * ── 픽스처가 없다 ────────────────────────────────────────────────────────
 * `api/eco.js` · `api/onboarding.js` 와 달리 **데이터 소스 스위치가 없다**(`api/dataSource.js`
 * 를 보지 않는다). 항상 실 호출이다. 백엔드 4개가 이미
 * 다 구현돼 있고(`GreenlifeController`) DTO 필드가 api-spec 12절과 1:1이며, 응답 수치까지
 * 스펙 예시와 일치한다 — 8월 44건 · 적립 예정 5,540원 · 7월 지급 3,140원 · 연간 18,600원.
 * 17개 항목 시드도 `GreenlifeItemSeedInitializer` 가 부팅마다 멱등하게 넣는다.
 *
 * 맞는 응답을 한 벌 더 베껴 두면 유지 대상만 늘고 통합 시점에 어긋날 자리가 생긴다.
 * 서버가 꺼져 있으면 화면이 에러로 뜨는 것이 정상이다(`api/pocket.js` 와 같다).
 *
 * `POST /greenlife/settlements`(12.5)는 여기 없다. 월 지급분을 포켓 거래로 만드는
 * **시스템 API** 라 15.3 화면 매핑표에 없고 화면이 부르지 않는다.
 */

import client from './client'

/**
 * GET /greenlife/status — 참여·연동 상태 + 월 현황 (C-1-01 · C-1-02 · C-2-01 · C-2-02)
 *
 * **BN-01 과 BN-02 를 이 하나로 분기한다.** 어느 화면인지는 응답의 `screen` 이 정하고
 * 화면이 `participating` 을 보고 다시 판정하지 않는다 — 두 벌이 되면 조용히 어긋난다.
 * 값은 `'BN-01'` · `'BN-02'` 이고, eco 의 `WF_01_UNLINKED` 같은 언더스코어 enum 이 아니다.
 */
export function getGreenlifeStatus(params = {}) {
  return client.get('/greenlife/status', { params })
}

/**
 * POST /greenlife/link — 연동 새로고침 (C-1-02)
 *
 * 본문이 없다. 여전히 미참여면 `participating: false` 를 **정상 200 으로** 돌려준다.
 * 에러가 아니므로 안내 문구로 그린다(C-1-01 예외 · 핵심 규칙 8).
 */
export function linkGreenlife() {
  return client.post('/greenlife/link')
}

/**
 * GET /greenlife/items — 실천 항목 17개 (C-1-03 · C-2-03)
 *
 * 실적이 없어도 17개를 전부 준다. `monthAmount: 0` 은 빈 목록이 아니라 아직 실천하지 않은 항목이다.
 * 정렬은 서버의 `displayOrder` 고정이라 화면에서 다시 정렬하지 않는다.
 */
export function getGreenlifeItems(params = {}) {
  return client.get('/greenlife/items', { params })
}

/** GET /greenlife/items/{itemId} — 실천항목 상세 (C-2-04 · BN-03) */
export function getGreenlifeItemDetail(itemId, params = {}) {
  return client.get(`/greenlife/items/${itemId}`, { params })
}
