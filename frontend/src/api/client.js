import axios from 'axios'

const DEMO_KEY_STORAGE = 'greenpocket.demoKey'

/**
 * 데모 키 (api-spec.md 1.2)
 * 로그인이 없다. FE가 UUID v4 를 만들어 보관하고 모든 요청에 `X-Demo-Key` 로 붙인다.
 * `POST /users` 로 이 키를 서버에 등록해야 나머지 API가 열린다.
 */
export function getDemoKey() {
  let key = localStorage.getItem(DEMO_KEY_STORAGE)
  if (!key) {
    key = crypto.randomUUID()
    localStorage.setItem(DEMO_KEY_STORAGE, key)
  }
  return key
}

export function clearDemoKey() {
  localStorage.removeItem(DEMO_KEY_STORAGE)
}

/**
 * 멱등키 (api-spec.md 1.6)
 * 출금·전환처럼 돈이 움직이는 요청에 붙인다.
 * **재시도할 때는 새로 만들지 말고 같은 키를 다시 보내야** 거래가 두 번 생기지 않는다.
 * 따라서 이 값은 호출하는 쪽(store)이 들고 있어야 한다.
 */
export function newIdempotencyKey() {
  return crypto.randomUUID()
}

/** 서버가 내려준 공통 에러(1.3)를 그대로 담는다. `message` 는 화면에 띄울 한국어 문장이다. */
export class ApiError extends Error {
  constructor({ code, message, field, details, status }) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.field = field
    this.details = details
    this.status = status
  }
}

const client = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
})

client.interceptors.request.use((config) => {
  config.headers['X-Demo-Key'] = getDemoKey()
  return config
})

/**
 * 401 뒤처리 — 온보딩 완료 플래그를 지우고 ONB-01 로 돌려보낸다.
 *
 * ⚠️ 라우터를 **지연 import 한다.** `router/index.js` → 라우트 → 뷰 → 스토어 → 이 파일로
 * 이어지는 순환이라 최상단에서 import 하면 로딩 순서에 따라 undefined 가 된다.
 * 이미 온보딩에 있으면 아무것도 하지 않는다 — 401 이 여러 번 나도 화면이 튀지 않게.
 */
async function redirectToOnboarding() {
  const { clearOnboarded } = await import('@/router/guards')
  clearOnboarded()

  const { default: router } = await import('@/router')
  if (!router.currentRoute.value.path.startsWith('/onboarding')) {
    router.replace('/onboarding/start')
  }
}

client.interceptors.response.use(
  // 공통 응답 래퍼(1.3)는 여기서 한 번만 벗긴다. 컴포넌트에서 res.data.data 를 파싱하지 않는다.
  (response) => response.data?.data,
  (error) => {
    const status = error.response?.status
    const body = error.response?.data?.error

    if (status === 401) {
      // 서버가 모르는 키다. 버리고 온보딩(ONB-01)부터 다시 시작해야 한다.
      clearDemoKey()
      redirectToOnboarding()
    }

    if (body) {
      return Promise.reject(new ApiError({ ...body, status }))
    }

    // 서버에 닿지 못한 경우다. 응답 래퍼가 없으므로 FE가 문구를 만든다.
    return Promise.reject(
      new ApiError({
        code: 'NETWORK_ERROR',
        message: '연결이 불안정해요. 잠시 후 다시 시도해 주세요.',
        status,
      }),
    )
  },
)

export default client
