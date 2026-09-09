<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import MypageState from '@/components/mypage/MypageState.vue'
import PolicyCard from '@/components/mypage/PolicyCard.vue'
import { useMypageStore } from '@/stores/mypage'
import { usePolicyStore } from '@/stores/policy'
import { POLICY_CATEGORY_OPTIONS } from '@/utils/policy'

const route = useRoute()
const router = useRouter()
const store = usePolicyStore()
const mypage = useMypageStore()

const mode = ref(route.query.mode === 'recommended' ? 'recommended' : 'all')
const keyword = ref('')
const category = ref('')
const regionCode = ref('')

const bootstrapping = computed(() => !store.list && !store.error)
const isRecommended = computed(() => mode.value === 'recommended')
const pageNumbers = computed(() => {
  if (isRecommended.value || !store.list?.totalPages) return []
  const visibleCount = Math.min(5, store.list.totalPages)
  const maximumStart = Math.max(0, store.list.totalPages - visibleCount)
  const start = Math.min(Math.max(0, store.list.page - 2), maximumStart)
  return Array.from({ length: visibleCount }, (_, index) => start + index)
})

function params(page = 0) {
  const values = { page, size: isRecommended.value ? 5 : 6 }
  if (!isRecommended.value && keyword.value.trim()) values.keyword = keyword.value.trim()
  if (!isRecommended.value && category.value) values.category = category.value
  if (!isRecommended.value && regionCode.value) values.regionCode = regionCode.value
  return values
}

async function load() {
  await store.fetchList(params(), { recommended: isRecommended.value })
}

async function changeMode(nextMode) {
  if (mode.value === nextMode) return
  mode.value = nextMode
  await router.replace({ query: nextMode === 'recommended' ? { mode: 'recommended' } : {} })
  await load()
}

function resetFilters() {
  keyword.value = ''
  category.value = ''
  regionCode.value = ''
  load()
}

async function goToPage(page) {
  if (isRecommended.value || store.loading || page === store.list?.page) return
  if (page < 0 || page >= (store.list?.totalPages ?? 0)) return
  await store.fetchList(params(page), { recommended: false })
}

onMounted(async () => {
  await Promise.all([load(), mypage.fetchPolicyPreferences()])
})
</script>

<template>
  <AppSubLayout title="청년정책" subtitle="내 조건에 맞는 정책부터 살펴보세요" back="/mypage">
    <div
      class="bg-surface mb-4 grid grid-cols-2 rounded-md p-1"
      role="tablist"
      aria-label="정책 목록 구분"
    >
      <button
        v-for="item in [
          { value: 'recommended', label: '맞춤 추천' },
          { value: 'all', label: '전체 정책' },
        ]"
        :key="item.value"
        type="button"
        role="tab"
        :aria-selected="mode === item.value"
        class="text-body-strong min-h-11 cursor-pointer rounded-sm border-0 transition-colors"
        :class="mode === item.value ? 'bg-primary text-on-primary' : 'bg-transparent text-muted'"
        @click="changeMode(item.value)"
      >
        {{ item.label }}
      </button>
    </div>

    <form
      v-if="!isRecommended"
      class="bg-surface mb-4 space-y-3 rounded-lg p-4"
      @submit.prevent="load"
    >
      <label for="policy-keyword" class="text-label text-muted block">정책 검색</label>
      <div class="flex gap-2">
        <input
          id="policy-keyword"
          v-model="keyword"
          type="search"
          class="border-border text-body text-ink min-h-11 min-w-0 flex-1 rounded-md border px-3"
          placeholder="정책명이나 지원 내용을 검색해 보세요"
        />
        <button
          type="submit"
          class="bg-primary text-label text-on-primary min-h-11 cursor-pointer rounded-md border-0 px-4 font-semibold"
        >
          검색
        </button>
      </div>
      <div>
        <label for="policy-category" class="text-label text-muted mb-1 block"
          >정책 분야로 찾기</label
        >
        <select
          id="policy-category"
          v-model="category"
          aria-label="정책 분야"
          class="border-border bg-surface text-body text-ink min-h-11 rounded-md border px-3"
          @change="load"
        >
          <option
            v-for="option in POLICY_CATEGORY_OPTIONS"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </option>
        </select>
        <p class="text-caption-sm text-muted mt-1.5 mb-0">
          목록만 필터링하며 저장된 내 관심 분야는 바뀌지 않아요.
        </p>
      </div>
      <div class="flex items-center justify-between gap-3">
        <label class="text-body-sm text-ink-soft flex min-h-11 items-center gap-2">
          <input
            v-model="regionCode"
            type="checkbox"
            :true-value="mypage.policyPreferences?.ecoAddress?.sigunguCode ?? ''"
            false-value=""
            :disabled="!mypage.policyPreferences?.ecoAddress"
            @change="load"
          />
          내 지역만 보기
        </label>
        <button
          type="button"
          class="text-label text-muted min-h-11 cursor-pointer border-0 bg-transparent px-1 underline"
          @click="resetFilters"
        >
          필터 초기화
        </button>
      </div>
    </form>

    <MypageState
      :loading="bootstrapping"
      :error="store.error"
      :empty="Boolean(store.list) && store.list.content.length === 0"
      empty-message="조건에 맞는 청년정책이 없어요."
      @retry="load"
    >
      <template v-if="store.list">
        <div class="mb-3 flex items-center justify-between px-1">
          <p class="text-body-sm text-muted m-0">총 {{ store.list.totalElements }}개</p>
          <p v-if="store.list.lastSyncedAt" class="text-caption-sm text-muted m-0">
            온통청년 최신 데이터
          </p>
        </div>
        <div class="space-y-3">
          <PolicyCard
            v-for="policy in store.list.content"
            :key="policy.policyId"
            :policy="policy"
            @select="router.push(`/mypage/policies/${$event.policyId}`)"
          />
        </div>
        <nav
          v-if="!isRecommended && store.list.totalPages > 1"
          class="mt-5 flex items-center justify-center gap-1"
          aria-label="청년정책 페이지"
        >
          <button
            type="button"
            aria-label="이전 페이지"
            :disabled="store.list.page === 0 || store.loading"
            class="border-border text-body-sm text-ink min-h-11 min-w-11 cursor-pointer rounded-md border bg-white disabled:cursor-not-allowed disabled:opacity-40"
            @click="goToPage(store.list.page - 1)"
          >
            이전
          </button>
          <button
            v-for="page in pageNumbers"
            :key="page"
            type="button"
            :aria-label="`${page + 1}페이지`"
            :aria-current="store.list.page === page ? 'page' : undefined"
            :disabled="store.loading"
            class="text-body-strong min-h-11 min-w-11 cursor-pointer rounded-md border disabled:cursor-not-allowed disabled:opacity-40"
            :class="
              store.list.page === page
                ? 'border-primary bg-primary text-on-primary'
                : 'border-border bg-white text-ink'
            "
            @click="goToPage(page)"
          >
            {{ page + 1 }}
          </button>
          <button
            type="button"
            aria-label="다음 페이지"
            :disabled="!store.list.hasNext || store.loading"
            class="border-border text-body-sm text-ink min-h-11 min-w-11 cursor-pointer rounded-md border bg-white disabled:cursor-not-allowed disabled:opacity-40"
            @click="goToPage(store.list.page + 1)"
          >
            다음
          </button>
        </nav>
      </template>
    </MypageState>
  </AppSubLayout>
</template>
