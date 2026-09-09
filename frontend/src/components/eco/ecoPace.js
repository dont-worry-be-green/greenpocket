/*
 * WF-06 홈 감축률 카드의 「페이스」 — 누적 감축률이 목표 구간에 대해 어디 있나.
 *
 * 값은 전부 서버 응답에서 읽는다. 구간 위치는 `progress.currentTier · targetTier`(GET /eco/home),
 * 회복 가능 여부는 `prescription.achievable`(GET /eco/monthly-report)이다.
 * **감축률 숫자로 구간을 다시 계산하지 않는다** — 서버 판정과 두 벌이 된다.
 *
 * 구간 바는 지급 구간과 같은 4칸이다(AGENTS.md 고정 상수).
 *   0 = R<5 (0M) · 1 = TIER_5 (10,000M) · 2 = TIER_10 (30,000M) · 3 = TIER_15 (50,000M)
 *
 * 페이스는 **목표 대비 상대값**이라 같은 구간이라도 목표가 다르면 색이 뒤집힌다.
 *   ahead  현재 > 목표          목표 초과
 *   on     현재 = 목표          목표 달성 중
 *   near   현재 < 목표, 회복 가능 (achievable !== false)
 *   behind 현재 < 목표, achievable === false
 * 리포트가 아직 없으면 near 로 둔다 — 못 한다고 단정할 근거가 없다(핵심 규칙 8).
 */
export const SEGMENTS = [
  { key: 'NONE', label: '0~5%', mileage: 0 },
  { key: 'TIER_5', label: '5~10%', mileage: 10000 },
  { key: 'TIER_10', label: '10~15%', mileage: 30000 },
  { key: 'TIER_15', label: '15%+', mileage: 50000 },
]

const SEGMENT_INDEX = { TIER_5: 1, TIER_10: 2, TIER_15: 3 }

/** 구간 enum → 바의 칸 번호. 값이 없으면 0 (아직 5% 미만) */
export function segmentOf(tier) {
  return SEGMENT_INDEX[tier] ?? 0
}

export function derivePace(progress, prescription) {
  const current = segmentOf(progress?.currentTier)
  const target = segmentOf(progress?.targetTier)
  if (current > target) return 'ahead'
  if (current === target) return 'on'
  if (prescription?.achievable === false) return 'behind'
  return 'near'
}
