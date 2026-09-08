/*
 * 데이터 소스 스위치 — 목데이터(픽스처) / 실 API
 *
 * 화면을 실 서버에 붙인 뒤에도 **서버 없이 한 번 걸어 볼 길**을 남겨 둔다.
 * 시연 리허설과 백엔드가 내려간 순간이 겹치면 확인할 방법이 통째로 사라지기 때문이다.
 *
 * 값은 `localStorage` 에 있고 개발 빌드의 데모 도구(`components/layout/DemoToolsFab.vue`)가 바꾼다.
 *
 * 적용 범위는 `api/onboarding.js` · `api/eco.js` · `api/mypage.js` · `api/policy.js` 다.
 * 나머지(`greenlife` · `analysis` · `pocket`)는 픽스처가 없어 항상 실 호출이다.
 *
 * 목데이터 모드가 아직 필요한 이유는 `mission_catalog` 시드다(이슈 #75). 비어 있어서
 * 실 API 로는 WF-04 의 실천 미션 목록이 빈 카드로 뜬다 — 그 화면을 보려면 목데이터로 바꾼다.
 *
 * 기본값이 API 인 이유 — 연동을 끝낸 화면의 기본 동작은 실 호출이어야 한다.
 * 목데이터는 확인용으로 **일부러 고르는 것**이지 기본이 아니다.
 */

export const DATA_SOURCE = {
  FIXTURE: 'FIXTURE',
  API: 'API',
}

const STORAGE_KEY = 'greenpocket.dataSource'

/** 저장된 값이 FIXTURE 일 때만 목데이터다. 값이 없거나 깨졌으면 실 API */
export function getDataSource() {
  return localStorage.getItem(STORAGE_KEY) === DATA_SOURCE.FIXTURE
    ? DATA_SOURCE.FIXTURE
    : DATA_SOURCE.API
}

/** 기본값(API)일 때는 키를 지운다 — 남겨 두면 나중에 기본값을 바꿔도 옛 값이 이긴다 */
export function setDataSource(value) {
  if (value === DATA_SOURCE.FIXTURE) {
    localStorage.setItem(STORAGE_KEY, DATA_SOURCE.FIXTURE)
    return
  }
  localStorage.removeItem(STORAGE_KEY)
}

export function isFixtureMode() {
  return getDataSource() === DATA_SOURCE.FIXTURE
}
