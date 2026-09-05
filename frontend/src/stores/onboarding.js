/*
 * 온보딩 스토어 — ONB-01 · ONB-02 두 화면이 함께 쓴다.
 *
 * ── 여기에 두지 않는 것 ──────────────────────────────────────────────────
 * 입력 중인 이름과 고른 칩은 서버 데이터가 아니라 한 화면에서만 살다 죽는 폼 상태다.
 * 뷰 로컬 `ref` 에 둔다(WF-04 선례). 화면을 건너는 것은 `user.name` 하나뿐이고,
 * 새로고침하면 `user` 가 null 이라 ONB-02 는 ONB-01 로 되돌려 보낸다.
 *
 * 시군구 조회만 로딩 플래그를 따로 둔다 — 시도 모달을 닫고 시군구를 불러오는 동안
 * 화면 전체가 로딩으로 덮이면 방금 고른 시도가 사라진 것처럼 보인다.
 */

import { ref } from 'vue'
import { defineStore } from 'pinia'

import {
  getRegions,
  saveProfile as saveProfileApi,
  startUser as startUserApi,
} from '@/api/onboarding'

export const useOnboardingStore = defineStore('onboarding', () => {
  const user = ref(null)
  const sidos = ref([])
  const sigungus = ref([])
  const profileResult = ref(null)
  const isLoading = ref(false)
  const error = ref(null)
  const sigungusLoading = ref(false)

  async function run(task) {
    isLoading.value = true
    error.value = null
    try {
      return await task()
    } catch (nextError) {
      error.value = nextError
      return null
    } finally {
      isLoading.value = false
    }
  }

  /** ONB-01 (COM-01). 성공해야 데모 키가 서버에 등록된다 */
  async function startUser(name) {
    const data = await run(() => startUserApi({ name }))
    if (data) user.value = data
    return data
  }

  async function fetchSidos() {
    const data = await run(() => getRegions())
    if (data) sidos.value = data.items ?? []
    return data
  }

  /** 시도를 바꾸면 시군구를 비우는 것은 뷰가 아니라 여기다 — 옛 목록이 한 틱 남으면 안 된다 */
  async function fetchSigungus(sidoCode) {
    sigungus.value = []
    sigungusLoading.value = true
    error.value = null
    try {
      const data = await getRegions({ sidoCode })
      sigungus.value = data.items ?? []
      return data
    } catch (nextError) {
      error.value = nextError
      return null
    } finally {
      sigungusLoading.value = false
    }
  }

  /** ONB-02 (A-1-05). 성공하면 `nextScreen: 'WF-06'` 이라 뷰가 /whatif 로 보낸다 */
  async function saveProfile(payload) {
    const data = await run(() => saveProfileApi(payload))
    if (data) profileResult.value = data
    return data
  }

  return {
    user,
    sidos,
    sigungus,
    profileResult,
    isLoading,
    error,
    sigungusLoading,
    startUser,
    fetchSidos,
    fetchSigungus,
    saveProfile,
  }
})
