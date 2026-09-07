/*
 * 회원가입·로그인 (본인인증 포함) — 이슈 #121. 화면 ONB-01 · ONB-01a · ONB-01b.
 *
 * ⚠️ **여기 있는 것은 전부 `api-spec.md` 에 없는 FE 전용 모의다.** 결정 A-4(로그인 제외)를
 * 뒤집는 팀 결정에 따라 새로 만드는 흐름이고, 백엔드는 담당자가 별도로 진행한다.
 * 제안 계약은 이슈 #121 에 적어 두었다.
 *
 *   POST /auth/signup  { loginId, password, name }  →  { demoKey, nextScreen }
 *   POST /auth/login   { loginId, password }        →  { demoKey, nextScreen }
 *
 * ── 왜 `demoKey` 를 돌려받는 계약을 제안했나 ────────────────────────────
 * `demoKey` 는 이미 무기한 베어러 토큰이다(`api/client.js`). 로그인을 「인증 방식 교체」가
 * 아니라 **「그 키를 돌려주는 창구」**로 만들면 기존 API 60개와 인터셉터를 하나도 고치지
 * 않는다. 세션·JWT·만료 처리가 필요 없다.
 *
 * ── BE 가 붙기 전 (지금) ────────────────────────────────────────────────
 * 위 두 엔드포인트가 없다. 그래서 **가입은 기존 `POST /users`(4.1)로 사용자를 만들고**
 * (그 호출이 데모 키를 서버에 등록해 나머지 API 를 연다 — `api/onboarding.js` 주석),
 * 아이디·비밀번호는 문자인증과 같이 **화면에만 둔다.**
 *
 * ⚠️ **비밀번호를 검증하지도, 저장하지도 않는다.** 대조할 곳이 서버에 없고 평문을
 * `localStorage` 에 두는 쪽이 더 나쁘다. 로그인은 **이 기기에 가입한 아이디**와 맞춰 보는
 * 수준이며 화면이 그 사실을 그대로 밝힌다. 다른 기기 로그인은 BE 가 붙어야 된다.
 *
 * ⚠️ **실제 문자를 보내지 않는다.** 외부 SMS 는 MVP 제외 범위이고 `app_user` 에 전화번호
 * 컬럼도 없다. 데모 코드는 화면 캡션이 밝히되 **틀린 번호는 실제로 거부한다** —
 * 아무 여섯 자리나 통과하면 검증이 없는 것과 같다.
 *
 * 계약이 확정되면 **이 파일만 교체한다.** 스토어와 화면은 그대로 산다.
 * 이 파일은 `localStorage` 를 만지지 않는다 — 진입 판정 플래그는 `router/guards.js` 가 갖는다.
 */

/** 실제 호출처럼 지연을 준다 (`api/eco.js` 와 같은 이유 — 로딩 경로를 실제로 돌린다) */
const fake = async (value, ms = 400) => {
  await new Promise((resolve) => setTimeout(resolve, ms))
  return typeof value === 'function' ? value() : value
}

/** 데모 인증번호. 발표자가 맞힐 수 있어야 하고, 틀리면 거부돼야 한다 */
const DEMO_SMS_CODE = '000000'

/*
 * 인증번호 발송 요청. 한동안 `demoCode` 를 같이 내려 화면 캡션이 번호를 알려 줬는데,
 * **화면이 길어 캡션째 뺐다.** 입력칸 placeholder 가 `000000` 이라 발표에서는 그것으로 충분하다.
 */
export function requestSmsCode() {
  return fake({ expiresInSeconds: 180 }, 700)
}

/**
 * 인증번호 확인. **틀려도 예외를 던지지 않는다** — 공통 에러 코드를 새로 만들지 않기 위해
 * (AGENTS.md 3절) `verified: false` 로 돌려주고 문구는 화면이 만든다.
 */
export function verifySmsCode(code) {
  return fake(() => ({ verified: String(code ?? '').trim() === DEMO_SMS_CODE }), 900)
}

/*
 * 데모에서 「이미 쓰는 아이디」를 실제로 보여줄 수 있어야 한다. 서버에 사용자 목록이 없어
 * **화면용 고정 목록**을 둔다 — 스키마 값도 응답 필드도 아니다. 계약이 붙으면 이 목록과 함께
 * 사라진다. 이 기기에 가입해 둔 아이디도 「쓰는 중」으로 친다(로그아웃 후 재가입 경로).
 */
const TAKEN_LOGIN_IDS = ['admin', 'greenpocket', 'test']

/**
 * 아이디 중복확인. **「이미 쓴다」는 에러가 아니라 `available: false` 다** — 공통 에러 코드를
 * 새로 만들지 않는다(AGENTS.md 3절). 대조 대상을 인자로 받는 이유는 `login` 과 같다.
 */
export function checkLoginId({ loginId, savedLoginId }) {
  const input = String(loginId ?? '').trim()
  const saved = String(savedLoginId ?? '').trim()
  const taken =
    TAKEN_LOGIN_IDS.includes(input.toLowerCase()) ||
    (Boolean(saved) && saved.toLowerCase() === input.toLowerCase())
  return fake(() => ({ loginId: input, available: Boolean(input) && !taken }))
}

/**
 * 로그인. **아이디만 맞춰 본다**(위 주석 참고).
 *
 * 대조 대상(`savedLoginId`)을 인자로 받는다 — 이 계층이 저장소를 알면 계약 교체 때
 * 지울 곳이 늘어난다. 이 기기에 가입 기록이 없거나 아이디가 다르면 `found: false` 이고,
 * 그건 에러가 아니라 안내다.
 */
export function login({ loginId, savedLoginId }) {
  const input = String(loginId ?? '').trim()
  const saved = String(savedLoginId ?? '').trim()
  return fake(() => ({ found: Boolean(saved) && saved === input, loginId: input }))
}
