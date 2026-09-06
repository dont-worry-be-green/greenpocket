<script setup>
import { computed, onMounted, ref, watch } from 'vue'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconBank from '@/components/ui/icons/IconBank.vue'
import IconLeaf from '@/components/ui/icons/IconLeaf.vue'
import { usePocketStore } from '@/stores/pocket'
import { formatDate, formatDateTime, formatMonth, formatSignedWon } from '@/utils/format'

const store = usePocketStore()
const activeTab = ref('all')

const isWithdrawalTab = computed(() => activeTab.value === 'withdrawal')

const creditHistory = computed(() => store.transactions ?? { groups: [] })
const withdrawalItems = computed(() => store.withdrawals?.content ?? [])
const withdrawalMap = computed(() => {
  const map = {}
  for (const w of store.withdrawals?.content ?? []) map[w.transactionId] = w
  return map
})

const isLoading = computed(() =>
  isWithdrawalTab.value ? store.withdrawalsLoading : store.isLoading,
)
const loadError = computed(() =>
  isWithdrawalTab.value ? store.withdrawalsError : store.error,
)
const isEmpty = computed(() =>
  isWithdrawalTab.value ? !withdrawalItems.value.length : !creditHistory.value.groups.length,
)

const withdrawalStatusLabels = {
  REQUESTED: '출금 요청',
  PROCESSING: '처리 중',
  COMPLETED: '출금',
  FAILED: '출금 실패',
  CANCELED: '출금 취소',
}

function creditStatusLabel(item) {
  if (item.direction === 'DEBIT') {
    return withdrawalStatusLabels[item.transactionStatus] ?? item.transactionStatus
  }
  if (item.transactionStatus !== 'COMPLETED') return item.transactionStatus
  return item.transactionType === 'GREENLIFE' ? '지급 완료' : '입금'
}

function loadData() {
  if (isWithdrawalTab.value) return store.fetchWithdrawals()
  const direction = activeTab.value === 'credit' ? 'CREDIT' : undefined
  const calls = [store.fetchTransactions(direction)]
  if (activeTab.value === 'all') calls.push(store.fetchWithdrawals())
  return Promise.all(calls)
}

onMounted(loadData)
watch(activeTab, loadData)
</script>

<template>
  <AppSubLayout title="내역" back="/pocket" center-title>
    <div class="space-y-5">
      <div class="border-divider grid grid-cols-3 rounded-lg border p-1">
        <button
          type="button"
          class="text-body-strong min-h-11 rounded-md border-0 transition-colors"
          :class="activeTab === 'all' ? 'bg-primary text-on-primary' : 'text-muted bg-transparent'"
          @click="activeTab = 'all'"
        >
          전체
        </button>
        <button
          type="button"
          class="text-body-strong min-h-11 rounded-md border-0 transition-colors"
          :class="activeTab === 'credit' ? 'bg-primary text-on-primary' : 'text-muted bg-transparent'"
          @click="activeTab = 'credit'"
        >
          적립 내역
        </button>
        <button
          type="button"
          class="text-body-strong min-h-11 rounded-md border-0 transition-colors"
          :class="activeTab === 'withdrawal' ? 'bg-primary text-on-primary' : 'text-muted bg-transparent'"
          @click="activeTab = 'withdrawal'"
        >
          출금 내역
        </button>
      </div>

      <div v-if="isLoading" class="bg-surface rounded-lg p-5 text-center">
        <p class="text-body-sm text-muted m-0">내역을 불러오는 중이에요.</p>
      </div>
      <div v-else-if="loadError" class="bg-surface rounded-lg p-5 text-center">
        <p class="text-body-sm text-muted mt-0 mb-3">{{ loadError.message }}</p>
        <button
          type="button"
          class="text-label text-primary min-h-11 border-0 bg-transparent"
          @click="loadData"
        >
          다시 시도
        </button>
      </div>
      <div v-else-if="isEmpty" class="bg-surface rounded-lg p-5 text-center">
        <p class="text-body-sm text-muted m-0">
          아직 {{ activeTab === 'all' ? '' : activeTab === 'credit' ? '적립 ' : '출금 ' }}내역이
          없어요.
        </p>
      </div>

      <!-- 출금 내역 탭: GET /pocket/withdrawals -->
      <template v-else-if="isWithdrawalTab">
        <div class="space-y-3">
          <article
            v-for="item in withdrawalItems"
            :key="item.transactionId"
            class="bg-surface flex min-h-24 items-center gap-3 rounded-lg p-5"
          >
            <span class="bg-primary-bg text-primary flex size-10 shrink-0 items-center justify-center rounded-full">
              <IconBank :size="20" />
            </span>
            <div class="min-w-0 flex-1">
              <p class="text-body-strong m-0">
                {{ item.accountSnapshot?.bankName }} {{ item.accountSnapshot?.accountNo }}
              </p>
              <p class="text-caption text-muted mt-1 mb-0">
                {{ formatDateTime(item.requestedAt) }}
              </p>
              <p v-if="item.expectedDate && item.transactionStatus === 'PROCESSING'" class="text-caption text-muted mt-0.5 mb-0">
                입금 예정일 {{ formatDate(item.expectedDate) }}
              </p>
              <p v-if="item.failureReason" class="text-caption text-negative mt-0.5 mb-0">
                {{ item.failureReason }}
              </p>
            </div>
            <div class="flex shrink-0 flex-col items-end gap-1">
              <p class="text-list-title text-negative tabular-nums m-0">
                {{ formatSignedWon(-item.amount) }}
              </p>
              <GpTag v-if="item.transactionStatus" tone="negative" small>
                {{ withdrawalStatusLabels[item.transactionStatus] ?? item.transactionStatus }}
              </GpTag>
            </div>
          </article>
        </div>
      </template>

      <!-- 전체·적립 탭: GET /pocket/transactions -->
      <template v-else>
        <section
          v-for="group in creditHistory.groups"
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
              <!-- 아이콘 -->
              <span
                class="bg-primary-bg text-primary flex size-10 shrink-0 items-center justify-center rounded-full"
              >
                <IconBank v-if="item.direction === 'DEBIT'" :size="20" />
                <IconLeaf v-else :size="20" />
              </span>

              <!-- 본문 -->
              <div class="min-w-0 flex-1">
                <p class="text-body-strong m-0">
                  <template v-if="item.direction === 'DEBIT' && withdrawalMap[item.transactionId]?.accountSnapshot">
                    {{ withdrawalMap[item.transactionId].accountSnapshot.bankName }}
                    {{ withdrawalMap[item.transactionId].accountSnapshot.accountNo }}
                  </template>
                  <template v-else>{{ item.label }}</template>
                </p>
                <p class="text-caption text-muted mt-1 mb-0">
                  {{ formatDateTime(item.completedAt) }}
                </p>
              </div>

              <!-- 금액 + 태그 (오른쪽) -->
              <div class="flex shrink-0 flex-col items-end gap-1">
                <p
                  class="text-list-title tabular-nums m-0"
                  :class="item.direction === 'CREDIT' ? 'text-primary' : 'text-negative'"
                >
                  {{ formatSignedWon(item.direction === 'CREDIT' ? item.amount : -item.amount) }}
                </p>
                <GpTag
                  v-if="item.transactionStatus"
                  :tone="item.direction === 'DEBIT' ? 'negative' : 'positive'"
                  small
                >{{ creditStatusLabel(item) }}</GpTag>
              </div>
            </article>
          </div>
        </section>
      </template>
    </div>
  </AppSubLayout>
</template>
