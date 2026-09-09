import axios from 'axios'

const DEMO_KEY_STORAGE = 'greenpocket.demoKey'

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

let accessToken = ''
let refreshPromise = null
let unauthorizedHandler = null

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

export function setAccessToken(token) {
  accessToken = String(token ?? '')
}

export function clearAccessToken() {
  accessToken = ''
}

export function hasAccessToken() {
  return Boolean(accessToken)
}

export function newIdempotencyKey() {
  return crypto.randomUUID()
}

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

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1'

const client = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
  withCredentials: true,
})

const refreshClient = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
  withCredentials: true,
})

function toApiError(error) {
  const status = error.response?.status
  const body = error.response?.data?.error
  if (body) return new ApiError({ ...body, status })
  return new ApiError({
    code: 'NETWORK_ERROR',
    message: '연결이 불안정해요. 잠시 후 다시 시도해 주세요.',
    status,
  })
}

export function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = refreshClient
      .post('/auth/refresh')
      .then((response) => {
        const data = response.data?.data
        setAccessToken(data?.accessToken)
        return data
      })
      .catch((error) => {
        clearAccessToken()
        throw toApiError(error)
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

async function redirectToStart() {
  await unauthorizedHandler?.()
}

client.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

client.interceptors.response.use(
  (response) => response.data?.data,
  async (error) => {
    const status = error.response?.status
    const code = error.response?.data?.error?.code
    const original = error.config ?? {}
    const isAuthEntry = /\/auth\/(login|signup)$/.test(original.url ?? '')

    if (
      status === 401 &&
      code === 'ACCESS_TOKEN_EXPIRED' &&
      !original._authRetried &&
      !original.skipAuthRefresh
    ) {
      original._authRetried = true
      try {
        const refreshed = await refreshAccessToken()
        original.headers = original.headers ?? {}
        original.headers.Authorization = `Bearer ${refreshed.accessToken}`
        return client(original)
      } catch (refreshError) {
        if (!original.skipAuthRedirect) await redirectToStart()
        return Promise.reject(refreshError)
      }
    }

    if (status === 401 && !isAuthEntry && !original.skipAuthRedirect) {
      await redirectToStart()
    }

    return Promise.reject(toApiError(error))
  },
)

export default client
