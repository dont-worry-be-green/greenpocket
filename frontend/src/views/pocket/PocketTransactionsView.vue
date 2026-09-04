<script setup>
import { computed, onMounted, ref } from 'vue'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconLeaf from '@/components/ui/icons/IconLeaf.vue'
import IconPocket from '@/components/ui/icons/IconPocket.vue'
import { usePocketStore } from '@/stores/pocket'
import { formatDateTime, formatMonth, formatSignedWon } from '@/utils/format'

const store = usePocketStore()
const activeTab = ref('credit')

const creditHistory = {
  groups: [
    {
      yearMonth: '2026-08',
      subtotal: 3900,
      items: [
        {
          transactionId: 1,
          transactionCode: 'GP-2808-0031',
          label: '녹색생활실천 8월 수령액',
          amount: 3900,
          completedAt: '2026-08-02T14:22:00+09:00',
        },
      ],
    },
    {
      yearMonth: '2026-07',
      subtotal: 8500,
      items: [
        {
          transactionId: 2,
          transactionCode: 'GP-2607-0018',
          label: '에코마일리지 수령액',
          amount: 8500,
          completedAt: '2026-07-15T09:41:00+09:00',
        },
      ],
    },
  ],
}

const statusLabels = {
  REQUESTED: '출금 요청',
  PROCESSING: '처리 중',
  COMPLETED: '출금 완료',
  FAILED: '출금 실패',
  CANCELED: '출금 취소',
}

const withdrawalHistory = computed(() => {
  const groups = new Map()
  for (const item of store.withdrawals?.content ?? []) {
    const dateTime = item.completedAt ?? item.requestedAt
    const yearMonth = dateTime?.slice(0, 7) ?? '날짜 없음'
    if (!groups.has(yearMonth)) groups.set(yearMonth, [])
    groups.get(yearMonth).push({
      ...item,
      completedAt: dateTime,
      label: item.accountSnapshot
        ? `${item.accountSnapshot.bankName} ${item.accountSnapshot.accountNo}`
        : '출금 계좌',
      statusLabel: statusLabels[item.transactionStatus] ?? item.transactionStatus,
    })
  }
  return {
    groups: [...groups].map(([yearMonth, items]) => ({ yearMonth, items })),
  }
})

const activeHistory = computed(() =>
  activeTab.value === 'credit' ? creditHistory : withdrawalHistory.value,
)

onMounted(() => store.fetchWithdrawals())
</script>

<template>
  <AppSubLayout title="내역" back="/pocket" center-title>
    <div class="space-y-5">
      <div class="border-divider flex rounded-lg border p-1">
        <button
          type="button"
          class="text-body-strong min-h-11 flex-1 rounded-md border-0 transition-colors"
          :class="
            activeTab === 'credit' ? 'bg-primary text-on-primary' : 'text-muted bg-transparent'
          "
          @click="activeTab = 'credit'"
        >
          적립 내역
        </button>
        <button
          type="button"
          class="text-body-strong min-h-11 flex-1 rounded-md border-0 transition-colors"
          :class="
            activeTab === 'withdrawal' ? 'bg-primary text-on-primary' : 'text-muted bg-transparent'
          "
          @click="activeTab = 'withdrawal'"
        >
          출금 내역
        </button>
      </div>

      <div
        v-if="activeTab === 'withdrawal' && store.withdrawalsLoading"
        class="bg-surface rounded-lg p-5 text-center"
      >
        <p class="text-body-sm text-muted m-0">출금 내역을 불러오는 중이에요.</p>
      </div>
      <div
        v-else-if="activeTab === 'withdrawal' && store.withdrawalsError"
        class="bg-surface rounded-lg p-5 text-center"
      >
        <p class="text-body-sm text-muted mt-0 mb-3">{{ store.withdrawalsError.message }}</p>
        <button
          type="button"
          class="text-label text-primary min-h-11 border-0 bg-transparent"
          @click="store.fetchWithdrawals()"
        >
          다시 시도
        </button>
      </div>
      <div
        v-else-if="activeTab === 'withdrawal' && !activeHistory.groups.length"
        class="bg-surface rounded-lg p-5 text-center"
      >
        <p class="text-body-sm text-muted m-0">아직 출금 내역이 없어요.</p>
      </div>

      <section
        v-for="group in activeHistory.groups"
        v-else
        :key="`${activeTab}-${group.yearMonth}`"
      >
        <div class="mb-3">
          <h2 class="text-list-title text-muted m-0">{{ formatMonth(group.yearMonth) }}</h2>
        </div>
        <div class="space-y-3">
          <article
            v-for="item in group.items"
            :key="item.transactionId"
            class="bg-surface flex min-h-24 items-center gap-3 rounded-lg p-5"
          >
            <span
              class="bg-primary-bg text-primary flex size-10 shrink-0 items-center justify-center rounded-full"
            >
              <IconLeaf v-if="activeTab === 'credit'" :size="20" />
              <IconPocket v-else :size="20" />
            </span>
            <div class="min-w-0 flex-1">
              <p class="text-body-strong m-0">{{ item.label }}</p>
              <p class="text-caption text-muted mt-1 mb-0">
                {{ formatDateTime(item.completedAt) }} · {{ item.transactionCode }}
              </p>
              <GpTag v-if="item.statusLabel" tone="positive" small class="mt-2">{{
                item.statusLabel
              }}</GpTag>
            </div>
            <p
              class="text-list-title tabular-nums m-0"
              :class="activeTab === 'credit' ? 'text-primary' : 'text-ink'"
            >
              {{ formatSignedWon(activeTab === 'credit' ? item.amount : -item.amount) }}
            </p>
          </article>
        </div>
      </section>
    </div>
  </AppSubLayout>
</template>
