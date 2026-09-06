/*
 * 녹색생활실천 스토어 — BN-01 · BN-02 · BN-03 세 화면이 함께 쓴다.
 *
 * ── 로딩 플래그를 나눈 이유 ──────────────────────────────────────────────
 * 「연동 상태 새로고침」과 「17개 전체 보기」는 화면 일부만 바꾸는 동작이다. 공용 `isLoading`
 * 을 같이 쓰면 버튼 하나 눌렀는데 화면 전체가 스켈레톤으로 덮여 방금 보던 것이 사라진다
 * (`stores/pocket.js` 의 `accountsLoading` 선례).
 *
 * ── 여기서 숫자를 만들지 않는다 ──────────────────────────────────────────
 * 적립 예정·지급 완료·연간 진행률은 전부 서버가 준 필드 그대로다. 합계를 여기서 다시 더하면
 * 서버와 두 벌이 되어 C-2-01 완료 조건("두 금액이 실적 내역 합계와 일치한다")이 조용히 깨진다.
 */

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  getGreenlifeItemDetail,
  getGreenlifeItems,
  getGreenlifeStatus,
  linkGreenlife,
} from '@/api/greenlife'

export const useGreenlifeStore = defineStore('greenlife', () => {
  // 서버 응답을 원형 그대로 담는다
  const status = ref(null)
  const items = ref(null)
  const itemDetail = ref(null)

  const isLoading = ref(false)
  const error = ref(null)

  const itemsLoading = ref(false)
  const itemsError = ref(null)

  const detailLoading = ref(false)
  const detailError = ref(null)

  const linkLoading = ref(false)
  const linkError = ref(null)
  /** 연동했는데 여전히 미참여일 때 띄울 안내. 에러가 아니다(핵심 규칙 8) */
  const linkNotice = ref('')

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

  // ── 고르기만 하는 computed ────────────────────────────────────────────

  /** 'BN-01' | 'BN-02'. **화면이 participating 을 보고 다시 판정하지 않는다** */
  const screen = computed(() => status.value?.screen ?? null)
  const participating = computed(() => status.value?.participating === true)

  // ── 조회 ──────────────────────────────────────────────────────────────

  /** GET /greenlife/status (C-1-01 · C-1-02 · C-2-01 · C-2-02) */
  async function fetchStatus(params) {
    const data = await run(() => getGreenlifeStatus(params))
    if (data) status.value = data
    return data
  }

  /**
   * GET /greenlife/items (C-2-03)
   * BN-02 는 진입할 때, BN-01 은 「17개 전체 보기」를 눌렀을 때 부른다.
   */
  async function fetchItems(params) {
    itemsLoading.value = true
    itemsError.value = null
    try {
      const data = await getGreenlifeItems(params)
      items.value = data
      return data
    } catch (nextError) {
      itemsError.value = nextError
      return null
    } finally {
      itemsLoading.value = false
    }
  }

  /**
   * GET /greenlife/items/{itemId} (C-2-04)
   * 다른 항목으로 넘어갈 때 이전 항목이 한 틱 남으면 안 되므로 먼저 비운다.
   */
  async function fetchItemDetail(itemId, params) {
    itemDetail.value = null
    detailLoading.value = true
    detailError.value = null
    try {
      const data = await getGreenlifeItemDetail(itemId, params)
      itemDetail.value = data
      return data
    } catch (nextError) {
      detailError.value = nextError
      return null
    } finally {
      detailLoading.value = false
    }
  }

  // ── 연동 새로고침 ─────────────────────────────────────────────────────

  /**
   * POST /greenlife/link (C-1-02)
   *
   * 링크 응답에는 월 현황·연간 한도가 없다. 참여로 바뀌었으면 상태를 다시 받아야
   * BN-02 를 그릴 수 있다 — C-1-01 완료 조건이 그 전환이다.
   * 여전히 미참여면 200 정상 응답이므로 에러가 아니라 안내로 남긴다.
   */
  async function refreshLink() {
    linkLoading.value = true
    linkError.value = null
    linkNotice.value = ''
    try {
      const linked = await linkGreenlife()
      if (linked?.participating) {
        await fetchStatus()
      } else {
        linkNotice.value = '아직 참여 상태가 아니에요. 공식 누리집에서 가입을 마친 뒤 다시 눌러 주세요.'
      }
      return linked
    } catch (nextError) {
      linkError.value = nextError
      return null
    } finally {
      linkLoading.value = false
    }
  }

  return {
    status,
    items,
    itemDetail,
    isLoading,
    error,
    itemsLoading,
    itemsError,
    detailLoading,
    detailError,
    linkLoading,
    linkError,
    linkNotice,
    screen,
    participating,
    fetchStatus,
    fetchItems,
    fetchItemDetail,
    refreshLink,
  }
})
