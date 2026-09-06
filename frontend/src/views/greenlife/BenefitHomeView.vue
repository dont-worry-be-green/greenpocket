<script setup>
/*
 * 혜택 탭 홈 — BN-01(미참여) · BN-02(참여 중)
 *
 * 둘은 라우트가 아니라 **한 탭 홈의 두 상태**다(api-spec.md 12.1). 경로는 /benefit 하나이고
 * `GET /greenlife/status` 의 `screen` 이 무엇을 그릴지 정한다.
 *
 * ── 화면이 하지 않는 것 ─────────────────────────────────────────────────
 * **`participating` 을 보고 여기서 다시 판정하지 않는다.** 서버 판정과 두 벌이 되어 조용히
 * 어긋난다(`views/eco/WhatIfHomeView.vue` 와 같은 규칙).
 *
 * **숫자를 만들지 않는다.** 적립 예정·지급 완료·연간 진행률·건수는 전부 응답 필드 그대로다.
 *
 * ── 복귀 시 자동 재조회를 넣지 않았다 ───────────────────────────────────
 * C-1-01 은 "누리집 복귀 시 상태를 갱신한다" 지만 `focus`·`visibilitychange` 는 탭 전환·알림·
 * 화면잠금에도 걸린다. 시연 중 화면이 제멋대로 다시 로딩되는 쪽이 위험이 크다. 시안에 있는
 * 「연동 상태 새로고침」 버튼이 그 조건을 이미 충족한다.
 */
import { computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'

import GreenlifeItemList from '@/components/greenlife/GreenlifeItemList.vue'
import GreenlifeItemRow from '@/components/greenlife/GreenlifeItemRow.vue'
import GreenlifeProgramCard from '@/components/greenlife/GreenlifeProgramCard.vue'
import GreenlifeState from '@/components/greenlife/GreenlifeState.vue'
import GreenlifeStepList from '@/components/greenlife/GreenlifeStepList.vue'
import GreenlifeSummaryCard from '@/components/greenlife/GreenlifeSummaryCard.vue'
import AppTabLayout from '@/components/layout/AppTabLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconChevronDown from '@/components/ui/icons/IconChevronDown.vue'
import IconExternalLink from '@/components/ui/icons/IconExternalLink.vue'
import IconRefresh from '@/components/ui/icons/IconRefresh.vue'
import { useGreenlifeStore } from '@/stores/greenlife'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const store = useGreenlifeStore()

const participating = computed(() => store.screen === 'BN-02')

/*
 * 첫 응답 전. `isLoading` 을 조건에 넣지 않는다 — 첫 렌더는 onMounted 보다 먼저라
 * 그 한 틱 동안 status 도 null, isLoading 도 아직 false 다.
 */
const bootstrapping = computed(() => !store.status && !store.error)

const programInfo = computed(() => store.status?.programInfo ?? null)

/** BN-01 은 `featuredItems`, 「전체 보기」를 누르면 12.3 의 17개로 바뀐다 */
const featuredItems = computed(() => store.items?.items ?? store.status?.featuredItems ?? [])
const featuredExpanded = computed(() => Boolean(store.items))

onMounted(() => {
  store.fetchStatus()
})

/*
 * 목록은 참여 중일 때만 자동으로 받는다. 미참여 화면은 대표 항목 4개로 충분하고,
 * 「전체 보기」를 누른 사람에게만 17개를 부른다.
 */
watch(
  participating,
  (value) => {
    if (value && !store.items) store.fetchItems()
  },
  { immediate: true },
)

function goToItem(itemId) {
  router.push(`/benefit/items/${itemId}`)
}

/** 실제 가입은 누리집에서 한다(C-1-01). 여기서는 새 탭을 열어줄 뿐이다 */
function openExternal() {
  const url = programInfo.value?.externalUrl
  if (url) window.open(url, '_blank', 'noopener')
}
</script>

<template>
  <AppTabLayout
    tab="benefit"
    title="혜택"
    :subtitle="participating ? '탄소중립포인트 녹색생활실천' : '친환경 실천으로 받는 혜택을 확인해요'"
  >
    <template v-if="participating" #headerAction>
      <GpTag tone="primary">누리집 연동</GpTag>
    </template>

    <GreenlifeState
      :loading="bootstrapping"
      :error="store.status ? null : store.error"
      @retry="store.fetchStatus()"
    >
      <!-- ── BN-02 참여 중 ─────────────────────────────────────────── -->
      <div v-if="participating" class="space-y-5">
        <GreenlifeSummaryCard
          :month="store.status.month"
          :month-summary="store.status.monthSummary"
          :annual="store.status.annual"
        />

        <p v-if="store.status.delayNotice" class="text-caption text-muted m-0 px-1">
          {{ store.status.delayNotice }}
        </p>

        <GreenlifeState
          :loading="store.itemsLoading && !store.items"
          :error="store.itemsError"
          @retry="store.fetchItems()"
        >
          <GreenlifeItemList
            v-if="store.items"
            :items="store.items.items"
            :total-count="store.items.totalCount"
            :collapsed-after="store.items.collapsedAfter"
            @select="goToItem"
          />
        </GreenlifeState>

        <div class="text-caption text-muted space-y-1 pt-1 text-center">
          <p class="m-0">{{ store.status.standardYear }}년 공식 적립 기준</p>
          <button
            type="button"
            class="mx-auto flex items-center gap-1.5"
            :disabled="store.linkLoading"
            @click="store.refreshLink()"
          >
            <span>최근 연동 {{ formatDateTime(store.status.linkedAt) }}</span>
            <IconRefresh :size="14" :class="store.linkLoading && 'animate-spin'" />
          </button>
        </div>
      </div>

      <!-- ── BN-01 미참여 ──────────────────────────────────────────── -->
      <div v-else-if="programInfo" class="space-y-4">
        <GreenlifeProgramCard :program-info="programInfo" />

        <GpCard title="참여 방법">
          <GreenlifeStepList :steps="programInfo.joinSteps" />
        </GpCard>

        <GpCard title="대표 실천 항목">
          <div class="space-y-2">
            <GreenlifeItemRow v-for="item in featuredItems" :key="item.itemId" :item="item" compact />
          </div>

          <div v-if="!featuredExpanded" class="mt-3 flex justify-center">
            <GpButton
              variant="pill"
              size="pill"
              :disabled="store.itemsLoading"
              @click="store.fetchItems()"
            >
              {{ programInfo.itemCount }}개 전체 보기
              <IconChevronDown :size="12" class="ml-1" />
            </GpButton>
          </div>
        </GpCard>

        <GpButton @click="openExternal">
          공식 누리집에서 참여하기
          <IconExternalLink :size="16" class="ml-1.5" />
        </GpButton>

        <div class="space-y-1 pt-1 text-center">
          <p class="text-caption text-muted m-0">이미 참여하고 있다면</p>
          <button
            type="button"
            class="text-body-strong text-primary-on-soft mx-auto flex items-center gap-1.5"
            :disabled="store.linkLoading"
            @click="store.refreshLink()"
          >
            <IconRefresh :size="16" :class="store.linkLoading && 'animate-spin'" />
            연동 상태 새로고침
          </button>
        </div>
      </div>

      <!--
        새로고침했는데 여전히 미참여. 오류가 아니라 정상 응답이라(12.2 · 핵심 규칙 8)
        에러 화면이 아니라 안내로 남긴다.
      -->
      <p v-if="store.linkNotice" class="text-caption text-ink-soft mt-3 mb-0 text-center">
        {{ store.linkNotice }}
      </p>
      <p v-else-if="store.linkError" class="text-caption text-negative mt-3 mb-0 text-center">
        {{ store.linkError.message }}
      </p>
    </GreenlifeState>
  </AppTabLayout>
</template>
