/*
 * 마이페이지 스토어 — MY-01 · MY-02 · MY-03 · MY-04 네 화면이 함께 쓴다.
 *
 * ── 로딩·에러 플래그를 화면별로 나눈다 ───────────────────────────────────
 * 보관함에서 탭·연도를 바꾸면 목록만 다시 받는다. 공용 `isLoading` 을 같이 쓰면 탭 하나
 * 눌렀는데 프로필 카드까지 스켈레톤으로 덮인다(`stores/greenlife.js` 와 같은 이유).
 *
 * ── 여기서 숫자를 만들지 않는다 ──────────────────────────────────────────
 * 탭 배지 건수(`counts`)·총 건수·요약 문장은 전부 서버가 준 필드 그대로다. 화면에서 다시
 * 세면 A-2-12 완료 조건("필터 적용 시 건수가 실제 데이터와 일치한다")이 조용히 깨진다.
 *
 * ── 목록을 이어 붙이는 곳은 여기다 ───────────────────────────────────────
 * 고지서 보관함은 `hasNext` 를 따라 다음 페이지를 **덧붙인다**(`appendBills`). 뷰가 배열을
 * 합치면 필터를 바꿀 때 지난 페이지가 남는다 — 필터 조회는 항상 `fetchBills` 로 새로 시작한다.
 */

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { getBills, getMypage, getProfile, getReports, updateProfile } from '@/api/mypage'

/** 리포트는 한 번에 받아 화면이 두 탭·연도 그룹으로 가른다. 서버 상한이 100 이다(14.2) */
const REPORT_PAGE_SIZE = 100

export const useMypageStore = defineStore('mypage', () => {
  // 서버 응답을 원형 그대로 담는다
  const mypage = ref(null)
  const profile = ref(null)
  const bills = ref(null)
  const reports = ref(null)

  const isLoading = ref(false)
  const error = ref(null)

  const profileLoading = ref(false)
  const profileError = ref(null)

  const saveLoading = ref(false)
  const saveError = ref(null)
  /**
   * 지역 변경 경고 (A-1-06 예외). 서버가 `409` + `details.warning` 으로 준 문구다.
   * **에러 자리에 두지 않는다** — 실패가 아니라 확인을 받아야 하는 상태다.
   */
  const baselineWarning = ref(null)

  const billsLoading = ref(false)
  const billsError = ref(null)

  const reportsLoading = ref(false)
  const reportsError = ref(null)

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

  /** 미연동이면 `ecoAddress` 가 null 이다. 그때는 카드를 숨긴다 — 없는 주소를 지어내지 않는다 */
  const ecoAddress = computed(() => mypage.value?.ecoAddress ?? null)

  /** 프로필 주소와 누리집 등록 주소의 시군구가 다른 상태 (B-1-08 이사 안내) */
  const needsMovingNotice = computed(
    () => Boolean(ecoAddress.value) && ecoAddress.value.matchesProfile === false,
  )

  const billCounts = computed(() => bills.value?.counts ?? null)

  // ── MY-01 ─────────────────────────────────────────────────────────────

  async function fetchMypage() {
    const data = await run(() => getMypage())
    if (data) mypage.value = data
    return data
  }

  // ── MY-02 ─────────────────────────────────────────────────────────────

  /** 수정 폼 프리필. `GET /mypage` 에는 지역 **코드**가 없어 이걸 따로 부른다 */
  async function fetchProfile() {
    profileLoading.value = true
    profileError.value = null
    try {
      const data = await getProfile()
      profile.value = data
      return data
    } catch (nextError) {
      profileError.value = nextError
      return null
    } finally {
      profileLoading.value = false
    }
  }

  /**
   * 프로필 저장 (A-1-06).
   *
   * 진행 중 회차가 있는데 지역이 바뀌면 서버가 `409 CONFLICT` +
   * `field: 'confirmBaselineChange'` 로 막는다. 그 경우만 `baselineWarning` 에 담아
   * **뷰가 확인 다이얼로그를 띄우고 같은 payload + `confirmBaselineChange: true` 로 다시 부른다.**
   * 사용자 확인 없이 여기서 플래그를 붙여 재시도하지 않는다(핵심 규칙 9).
   */
  async function saveProfile(payload) {
    saveLoading.value = true
    saveError.value = null
    baselineWarning.value = null
    try {
      const data = await updateProfile(payload)
      // 저장이 성공하면 MY-01 이 들고 있던 값은 낡았다. 다음 진입에서 다시 받게 비운다
      mypage.value = null
      return data
    } catch (nextError) {
      if (nextError.field === 'confirmBaselineChange') {
        baselineWarning.value =
          nextError.details?.warning ?? '지역을 바꾸면 비교 기준이 다시 계산돼요.'
      } else {
        saveError.value = nextError
      }
      return null
    } finally {
      saveLoading.value = false
    }
  }

  function dismissBaselineWarning() {
    baselineWarning.value = null
  }

  // ── MY-03 ─────────────────────────────────────────────────────────────

  /** 탭·연도 조회. 항상 첫 페이지부터 새로 받는다 */
  async function fetchBills(params = {}) {
    billsLoading.value = true
    billsError.value = null
    try {
      const data = await getBills({ ...params, page: 0 })
      bills.value = data
      return data
    } catch (nextError) {
      billsError.value = nextError
      return null
    } finally {
      billsLoading.value = false
    }
  }

  /** 「더 보기」. 받은 페이지를 **덧붙인다.** 필터가 같을 때만 부른다 */
  async function appendBills(params = {}) {
    const current = bills.value
    if (!current?.hasNext || billsLoading.value) return null

    billsLoading.value = true
    billsError.value = null
    try {
      const data = await getBills({ ...params, page: current.page + 1 })
      bills.value = { ...data, content: [...current.content, ...data.content] }
      return data
    } catch (nextError) {
      billsError.value = nextError
      return null
    } finally {
      billsLoading.value = false
    }
  }

  // ── MY-04 ─────────────────────────────────────────────────────────────

  /**
   * 리포트 전량 조회. `type` 을 넘기지 않는다 — 시안의 탭 두 개가 타입 셋을 나눠 담고,
   * 서버가 이미 세 타입을 최신순으로 합쳐 준다(14.2).
   */
  async function fetchReports() {
    reportsLoading.value = true
    reportsError.value = null
    try {
      const data = await getReports({ size: REPORT_PAGE_SIZE })
      reports.value = data
      return data
    } catch (nextError) {
      reportsError.value = nextError
      return null
    } finally {
      reportsLoading.value = false
    }
  }

  return {
    mypage,
    profile,
    bills,
    reports,
    isLoading,
    error,
    profileLoading,
    profileError,
    saveLoading,
    saveError,
    baselineWarning,
    billsLoading,
    billsError,
    reportsLoading,
    reportsError,
    ecoAddress,
    needsMovingNotice,
    billCounts,
    fetchMypage,
    fetchProfile,
    saveProfile,
    dismissBaselineWarning,
    fetchBills,
    appendBills,
    fetchReports,
  }
})
