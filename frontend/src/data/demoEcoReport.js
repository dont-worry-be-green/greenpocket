/*
 * 배포 시연용 ECO 리포트.
 *
 * 현재 평가 확정·리포트 생성 백엔드 흐름이 준비되지 않아, MY-04 보관함에서만
 * 고정으로 보여 준다. 서버가 동일 회차의 ECO_RESULT를 반환하면 목록에서는
 * 이 항목 대신 서버 항목을 사용한다.
 */
import { ECO_RESULT } from '@/fixtures/ecoResult'

export const DEMO_ECO_REPORT_ID = 'DEMO_ECO_RESULT:2026-04-09'

export const DEMO_ECO_REPORT = {
  reportId: DEMO_ECO_REPORT_ID,
  type: 'ECO_RESULT',
  yearMonth: '2026-09',
  title: '2026-04 ~ 09 평가 결과',
  createdAt: '2026-10-05T00:00:00+09:00',
  targetScreen: 'WF-10',
  targetParams: { roundId: 7 },
  downloadable: false,
  result: ECO_RESULT,
}
