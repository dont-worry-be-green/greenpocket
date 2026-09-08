/*
 * 마이페이지 API — api-spec.md 5.2 · 5.3 · 6.6 · 14.1 · 14.2. 화면 MY-01 ~ MY-04.
 *
 * ── 이 파일이 도메인을 두 개 걸치는 이유 ─────────────────────────────────
 * `GET /bills`(6.6)는 고지서 도메인 API 지만 **부르는 화면이 MY-03 하나뿐**이다(15.3 매핑표).
 * `api/analysis.js` 는 OCR·등록 흐름을 담고 있고 픽스처 스위치가 없다 — 거기에 목데이터
 * 분기를 끼우면 진단 탭 등록 흐름까지 모드에 얽힌다. 진단 탭이 나중에 목록을 쓰게 되면
 * 그때 옮긴다.
 *
 * 청년정책 추천 조건은 `/profile/policy-preferences` 에서 별도로 조회·저장한다.
 * 이름·생년월일·성별·전화번호는 회원가입 본인확인 정보이며 이 화면에서 수정하지 않는다.
 *
 * ── 데이터 소스 ──────────────────────────────────────────────────────────
 * 기본값은 실 호출이다. 스위치는 `api/dataSource.js` 하나이고 onboarding·eco 와 공유한다.
 *
 * 목데이터가 필요한 이유는 **보관함 두 화면이 실 서버에서 비어 있다**는 것이다 —
 * 데모 시드에 고지서가 없고, 리포트는 그 고지서와 확정 회차에서 나온다. 자세한 사정은
 * `fixtures/mypage.js` 머리말에 적어 두었다.
 *
 * `fake()` 를 async + 지연으로 둔 이유는 `api/eco.js` 와 같다 — 로딩 경로를 실제로 돌린다.
 */

import {
  buildBillArchive,
  buildReportArchive,
  MYPAGE,
} from '@/fixtures/mypage'
import { POLICY_PREFERENCES } from '@/fixtures/policy'

import client from './client'
import { isFixtureMode } from './dataSource'

const fake = async (value, ms = 220) => {
  await new Promise((resolve) => setTimeout(resolve, ms))
  return typeof value === 'function' ? value() : value
}

/*
 * 목데이터 모드에서 수정한 정책 추천 조건. 새로고침하면 사라진다.
 * 저장한 값이 되돌아오지 않으면 MY-02 가 제대로 저장했는지 화면에서 확인할 수 없다.
 */
let editedPolicyPreferences = null

/** GET /mypage — 마이페이지 메인 (E-1-01 · E-1-02 · MY-01) */
export function getMypage() {
  if (isFixtureMode()) return fake(MYPAGE)
  return client.get('/mypage')
}

/** GET /profile/policy-preferences — 정책 추천 조건 프리필 (MY-02) */
export function getPolicyPreferences() {
  if (isFixtureMode()) {
    return fake(() => ({ ...POLICY_PREFERENCES, ...editedPolicyPreferences }))
  }
  return client.get('/profile/policy-preferences')
}

/** PUT /profile/policy-preferences — 현재 상태·연소득·가구 상태 저장 (MY-02·MY-06) */
export function updatePolicyPreferences(payload) {
  if (isFixtureMode()) {
    return fake(() => {
      editedPolicyPreferences = { ...payload, completed: true }
      return { policyProfileCompleted: true, recommendationsUpdated: true }
    }, 400)
  }
  return client.put('/profile/policy-preferences', payload)
}

/**
 * GET /bills — 고지서 보관함 목록 (A-2-12 · MY-03)
 *
 * `utility` 없으면 전체 탭이다. `counts` 는 **필터와 무관한 전체 집계**라 탭 배지에 쓴다
 * (api-spec 6.6). 정렬은 최신 월 우선으로 서버가 하고 화면은 다시 정렬하지 않는다.
 */
export function getBills(params = {}) {
  if (isFixtureMode()) return fake(() => buildBillArchive(params))
  return client.get('/bills', { params })
}

/**
 * GET /reports — 리포트 보관함 (E-2-01 · MY-04)
 *
 * `type` 은 단일값이다. 시안의 탭은 두 개인데 타입은 셋(`MONTHLY_DIAGNOSIS` ·
 * `ECO_MONTHLY` · `ECO_RESULT`)이라, 화면은 **`type` 없이 한 번 받아** 두 탭으로 가른다.
 * 서버가 이미 세 타입을 합쳐 최신순으로 정렬해 준다(`ReportService`).
 */
export function getReports(params = {}) {
  if (isFixtureMode()) return fake(() => buildReportArchive(params))
  return client.get('/reports', { params })
}
