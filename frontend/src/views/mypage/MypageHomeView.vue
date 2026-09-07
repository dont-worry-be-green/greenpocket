<script setup>
/*
 * 마이페이지 메인 — MY-01 (E-1-01 · E-1-02)
 *
 * 탭 최상위라 `AppTabLayout` 이다. `GET /mypage` 하나로 전부 그린다.
 *
 * ── 화면이 하지 않는 것 ─────────────────────────────────────────────────
 * **`GET /profile` 을 부르지 않는다.** `GET /mypage` 의 `profile` 에 표시할 값이 다 있다.
 * 지역 코드가 필요한 것은 수정 폼(MY-02)뿐이라 그 화면이 자기 것을 받는다.
 *
 * **요약 문장을 만들지 않는다.** `profileSummary` 는 서버가 조립한다(A-1-07).
 *
 * ── 응답에 있지만 그리지 않는 두 값 ─────────────────────────────────────
 * `pocketAccountNo` 와 `integration`(에코 연동 상태·녹색생활실천 참여·등록 요금)은
 * **시안에도 E-1-01 규칙에도 없다.** 명세에 없는 UI 를 만들지 않는 것이 규칙이고(AGENTS 3),
 * 계좌번호는 상시 노출할 이유가 없다. 필요해지면 그때 명세에 올리고 붙인다.
 *
 * ── 저장하고 돌아오면 다시 받는다 ───────────────────────────────────────
 * MY-02 가 저장에 성공하면 스토어가 `mypage` 를 비운다. `<KeepAlive>` 를 쓰지 않으므로
 * 돌아올 때 이 화면이 다시 마운트되고, 그때 비어 있는 것을 보고 새로 받는다.
 *
 * ⚠️ **첫 렌더는 `onMounted` 보다 먼저다.** 그 한 틱 동안 `mypage` 는 null 인데 `isLoading` 도
 * 아직 false 라, `isLoading` 을 로딩 조건에 그대로 쓰면 본문이 빈 채로 한 번 그려진다
 * (`WhatIfHomeView`·`BenefitHomeView` 와 같은 함정).
 */
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import AppTabLayout from '@/components/layout/AppTabLayout.vue'
import MypageArchiveLinks from '@/components/mypage/MypageArchiveLinks.vue'
import MypageInfoTable from '@/components/mypage/MypageInfoTable.vue'
import MypageProfileCard from '@/components/mypage/MypageProfileCard.vue'
import MypageState from '@/components/mypage/MypageState.vue'
import { useMypageStore } from '@/stores/mypage'

const router = useRouter()
const store = useMypageStore()

const bootstrapping = computed(() => !store.mypage && !store.error)

onMounted(() => {
  if (!store.mypage) store.fetchMypage()
})
</script>

<template>
  <AppTabLayout tab="mypage" title="마이페이지" subtitle="내 정보와 보관함을 확인해요">
    <MypageState
      :loading="bootstrapping"
      :error="store.mypage ? null : store.error"
      @retry="store.fetchMypage()"
    >
      <div v-if="store.mypage" class="space-y-5">
        <MypageProfileCard :profile="store.mypage.profile" />

        <MypageInfoTable :profile="store.mypage.profile" />

        <MypageArchiveLinks
          @monthly="router.push({ path: '/mypage/reports', query: { tab: 'MONTHLY' } })"
          @eco="router.push({ path: '/mypage/reports', query: { tab: 'ECO' } })"
        />
      </div>
    </MypageState>
  </AppTabLayout>
</template>
