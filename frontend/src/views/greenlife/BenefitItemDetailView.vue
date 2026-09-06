<script setup>
/*
 * BN-03 실천항목 상세 (C-2-04) — `GET /greenlife/items/{itemId}`
 *
 * ── 두 금액을 색으로만 나누지 않는다 ────────────────────────────────────
 * 내역의 `PENDING` 은 아직 현금이 아니고 포켓 잔액에도 들어가지 않는다(C-2-05 · 핵심 규칙 3).
 * `formatRewardStatus` 가 주는 '적립 예정' / '지급 완료' 라벨을 반드시 함께 단다(COM-06).
 *
 * ── 건수를 여기서 세지 않는다 ───────────────────────────────────────────
 * `validCount` 는 서버가 준다. `history.length` 로 다시 세면 목록(12.3)의 건수와 어긋나
 * C-2-04 완료 조건("상세 건수가 목록 건수와 일치한다")이 조용히 깨진다. 내역은 최근 몇 건만
 * 내려올 수 있고 상한이 걸린 항목은 유효 건수와 원시 건수가 다르다.
 *
 * `monthlyCapAmount` 가 null 이면 상한이 아직 확정되지 않은 항목이라 표시하지 않는다
 * (api-spec 12.3 · 결정 10).
 *
 * ── 아이콘은 상세 응답에 없다 ───────────────────────────────────────────
 * `iconKey` 는 12.3 목록에만 있고 12.4 상세에는 없다. 명세에 없는 필드를 서버에 요구하는 대신
 * 이미 받아 둔 목록에서 같은 `itemId` 를 찾아 쓴다. 목록 없이 URL 로 바로 들어오면 못 찾는데,
 * 그때는 `GreenlifeItemIcon` 의 폴백(새싹)으로 떨어진다 — 화면이 비지 않는 쪽을 택한다.
 */
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'

import GreenlifeItemIcon from '@/components/greenlife/GreenlifeItemIcon.vue'
import GreenlifeState from '@/components/greenlife/GreenlifeState.vue'
import GreenlifeStepList from '@/components/greenlife/GreenlifeStepList.vue'
import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconExternalLink from '@/components/ui/icons/IconExternalLink.vue'
import IconInfo from '@/components/ui/icons/IconInfo.vue'
import { useGreenlifeStore } from '@/stores/greenlife'
import {
  formatDateTime,
  formatMonth,
  formatRewardStatus,
  formatShortDate,
  formatUnitPrice,
  formatWon,
} from '@/utils/format'
import { isMonth } from '@/utils/month'

const route = useRoute()
const store = useGreenlifeStore()

const detail = computed(() => store.itemDetail)

const iconKey = computed(
  () => store.items?.items?.find((item) => item.itemId === detail.value?.itemId)?.iconKey ?? '',
)

/*
 * 목록에서 다른 항목으로 바로 넘어와도 다시 받도록 경로 파라미터를 지켜본다.
 *
 * ⚠️ **`month` 를 함께 넘긴다.** 목록(BN-02)이 8월을 보고 있는데 여기서 빼면 서버가 이번 달을
 * 골라 0건을 준다 — C-2-04 완료 조건("상세 건수가 목록 건수와 일치한다")이 깨진다.
 * 쿼리가 없거나 형식이 아니면 넘기지 않는다. 그때는 서버가 이번 달을 고르고, 목록도 같다.
 */
const month = computed(() => (isMonth(route.query.month) ? route.query.month : null))

/** 목록으로 돌아갈 때도 보던 달을 들고 간다 */
const backTo = computed(() => ({ path: '/benefit', query: month.value ? { month: month.value } : {} }))

function load() {
  const itemId = route.params.itemId
  if (itemId) store.fetchItemDetail(itemId, month.value ? { month: month.value } : {})
}

watch([() => route.params.itemId, month], load, { immediate: true })

function openExternal() {
  const url = detail.value?.externalUrl
  if (url) window.open(url, '_blank', 'noopener')
}
</script>

<template>
  <AppSubLayout title="실천항목 상세" :back="backTo">
    <GreenlifeState
      :loading="store.detailLoading && !detail"
      :error="detail ? null : store.detailError"
      @retry="load"
    >
      <div v-if="detail" class="space-y-4">
        <GpCard>
          <div class="flex items-center gap-3">
            <GreenlifeItemIcon :icon-key="iconKey" />
            <div class="min-w-0">
              <h2 class="text-section tracking-display text-ink m-0">{{ detail.name }}</h2>
              <p class="text-body-sm text-primary mt-0.5 mb-0">
                {{ formatUnitPrice(detail.unitPrice, detail.rewardUnit) }}
              </p>
            </div>
          </div>

          <div class="border-divider mt-4 flex items-baseline justify-between gap-2 border-t pt-4">
            <span class="text-body-sm text-ink-soft">{{ formatMonth(detail.month) }}</span>
            <span class="text-body-strong text-ink tabular-nums">
              {{ detail.validCount }}건 · {{ formatWon(detail.pendingAmount) }} 적립 예정
            </span>
          </div>

          <p v-if="detail.capReached" class="text-caption text-muted mt-2 mb-0 text-right">
            이번 달 상한에 도달했어요
          </p>
        </GpCard>

        <GpCard title="이렇게 실천해요">
          <GreenlifeStepList :steps="detail.practiceSteps" variant="check" />
        </GpCard>

        <GpCard title="최근 실천 내역">
          <ul v-if="detail.history?.length" class="divide-divider m-0 list-none divide-y p-0">
            <li
              v-for="entry in detail.history"
              :key="entry.activityId"
              class="flex items-center gap-3 py-3"
            >
              <span class="text-body-sm text-muted flex-none tabular-nums">
                {{ formatShortDate(entry.occurredAt) }}
              </span>
              <span class="text-body-sm text-ink-soft min-w-0 flex-1 truncate">
                {{ detail.name }}
              </span>
              <span class="text-body-strong text-ink flex-none tabular-nums">
                {{ formatWon(entry.rewardAmount) }}
              </span>
              <GpTag :tone="entry.rewardStatus === 'PAID' ? 'confirmed' : 'estimated'" small>
                {{ formatRewardStatus(entry.rewardStatus) }}
              </GpTag>
            </li>
          </ul>
          <p v-else class="text-body-sm text-muted m-0">
            아직 실천 내역이 없어요. 참여기업 앱에서 실천하면 여기에 쌓여요.
          </p>
        </GpCard>

        <GpButton variant="wide" size="wide" @click="openExternal">
          참여기업 확인하기
          <IconExternalLink :size="14" class="ml-1.5" />
        </GpButton>

        <p v-if="detail.syncedAt" class="text-caption text-muted m-0 text-center">
          최근 연동 {{ formatDateTime(detail.syncedAt) }}
        </p>

        <p
          v-if="detail.delayNotice"
          class="bg-surface-sub text-caption text-muted m-0 flex items-center gap-2 rounded-md px-3 py-2.5"
        >
          <IconInfo :size="14" class="flex-none" />
          {{ detail.delayNotice }}
        </p>
      </div>
    </GreenlifeState>
  </AppSubLayout>
</template>
