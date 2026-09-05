/*
 * 온보딩 API — api-spec.md 4·5절. 화면 ONB-01 · ONB-02.
 *
 * ── 픽스처 shim ──────────────────────────────────────────────────────────
 * 백엔드에 `POST /users` 는 있지만 `GET /meta/regions` 는 컨트롤러가 없고
 * `POST /profile` 은 패키지 자체가 없다(origin/main 8654695 기준). 그동안 `src/fixtures/` 로 대신한다.
 *
 * **연동할 때 고칠 파일은 여기 하나다.** `USE_FIXTURES` 를 false 로 두면 스토어·뷰는 그대로 산다.
 *   grep -n USE_FIXTURES src/api/onboarding.js
 *
 * `fake()` 를 async + 지연으로 둔 이유는 로딩 스피너와 await 순서를 **실제로 돌리기** 위해서다.
 * 스토어에 분기를 두면 즉시 return 이라 로딩 경로가 한 번도 실행되지 않는다.
 *
 * **`src/fixtures/` 를 import 할 수 있는 파일은 `src/api/` 아래뿐이다.**
 * (`api/__tests__/eco.spec.js` 가 `src/` 전체를 훑어 확인한다)
 */

import { buildProfileResult, buildUserStart, SEOUL_SIGUNGUS, SIDOS } from '@/fixtures/onboarding'

import client, { ApiError, getDemoKey } from './client'

const USE_FIXTURES = true

/** 실제 호출처럼 지연을 준다. 값 대신 함수를 넘기면 호출 시점에 계산한다 */
const fake = async (value, ms = 220) => {
  await new Promise((resolve) => setTimeout(resolve, ms))
  return typeof value === 'function' ? value() : value
}

/**
 * POST /users — 데모 사용자 시작 (COM-01 · ONB-01).
 *
 * ⚠️ **픽스처 모드에서도 실제로 서버를 부르는 유일한 함수다.**
 * `X-Demo-Key` 는 FE 가 만들어 `localStorage` 에 넣지만, 이 호출로 서버에 등록하지 않으면
 * `DemoKeyAuthenticationInterceptor` 가 나머지 API 를 전부 401 로 막는다. 온보딩을 한 번 걸으면
 * 포켓·What-if 가 401 없이 뜨는 것이 이 한 줄 때문이다.
 *
 * 서버가 꺼져 있으면 실패를 **삼키고** 픽스처로 넘어간다 — 실패를 삼키는 곳은 앱 전체에서
 * 여기 하나뿐이다. 나머지 실패는 전부 화면에 뜬다(COM-08).
 */
export async function startUser({ name }) {
  const payload = { demoKey: getDemoKey(), name: String(name ?? '').trim() }
  if (!USE_FIXTURES) return client.post('/users', payload)

  assertNameValid(payload.name)
  const registered = await registerQuietly(payload)
  return fake(() => registered ?? buildUserStart(payload))
}

/**
 * GET /meta/regions — 행정구역 목록 (A-1-01 · ONB-02).
 * `sidoCode` 가 없으면 시도, 있으면 그 시도의 시군구다.
 *
 * 서울 밖 시도는 **빈 배열**이다. 시군구 목록을 상상해서 만들면 그 코드가 그대로 진단
 * 기준선 조회 키가 되어 없는 지역을 가리키게 된다. 빈 배열은 에러가 아니라 안내다(핵심 규칙 8).
 */
export function getRegions({ sidoCode } = {}) {
  if (USE_FIXTURES) {
    if (!sidoCode) return fake({ level: 'SIDO', items: SIDOS })
    return fake({ level: 'SIGUNGU', items: sidoCode === '11' ? SEOUL_SIGUNGUS : [] })
  }
  return client.get('/meta/regions', { params: sidoCode ? { sidoCode } : {} })
}

/** POST /profile — 프로필 저장·온보딩 완료 (A-1-05 · ONB-02) */
export function saveProfile(payload) {
  if (USE_FIXTURES) return fake(() => buildProfileResult(payload), 400)
  return client.post('/profile', payload)
}

// ── 픽스처 전용 헬퍼. USE_FIXTURES 를 끄면 아래는 아무도 부르지 않는다 ──

/**
 * 이름 검증을 shim 이 **실제로 던진다**(api-spec.md 4.1 `NAME_INVALID`).
 * 뷰도 같은 조건으로 CTA 를 막지만, 여기서 통과시켜 버리면 연동 후 처음 보는 에러가 된다.
 * shim 이 도메인 에러를 던지는 것은 `api/eco.js` 의 `getRoundResult` 선례가 있다.
 */
function assertNameValid(name) {
  if (name.length >= 1 && name.length <= 20 && /[\p{L}\p{N}]/u.test(name)) return
  throw new ApiError({
    code: 'NAME_INVALID',
    message: '이름을 1~20자로 입력해 주세요.',
    field: 'name',
    status: 400,
  })
}

/** 서버가 떠 있으면 데모 키를 등록하고, 꺼져 있으면 null 을 돌려준다 */
async function registerQuietly(payload) {
  try {
    return await client.post('/users', payload)
  } catch {
    return null
  }
}
