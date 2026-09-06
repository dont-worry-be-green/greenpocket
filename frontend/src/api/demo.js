/*
 * 데모 초기화 API — api-spec.md 4.4 (COM-10). 화면 MY-01.
 *
 * 인증 예외라 `X-Demo-Key` 없이도 통하지만 **대상은 본문의 `demoKey` 다.**
 * 서버는 `DELETE FROM app_user` 한 줄이고 FK CASCADE 로 사용자 데이터 8개 테이블이 정리된다.
 * 마스터(`mission_catalog` · `greenlife_item` · `region_utility_snapshot`)는 남는다.
 * 대상이 이미 없어도 `200` 이다.
 *
 * **목데이터 모드에서도 실제로 쏜다.** 지우는 대상이 서버 데이터라 흉내 낼 것이 없다.
 */
import client, { getDemoKey } from './client'

/** @returns {Promise<{ resetAt: string, nextScreen: string }>} */
export function resetDemo() {
  return client.post('/demo/reset', { demoKey: getDemoKey() })
}
