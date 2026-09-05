<script setup>
import { computed, onMounted, ref, watch } from 'vue'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconLeaf from '@/components/ui/icons/IconLeaf.vue'
import IconPocket from '@/components/ui/icons/IconPocket.vue'
import { usePocketStore } from '@/stores/pocket'
import { formatDateTime, formatMonth, formatSignedWon } from '@/utils/format'

const store = usePocketStore()
const activeTab = ref('credit')

const statusLabels = {
  REQUESTED: '출금 요청',
  PROCESSING: '처리 중',
  COMPLETED: '출금 완료',
  FAILED: '출금 실패',
  CANCELED: '출금 취소',
}

const activeHistory = computed(() => store.transactions ?? { groups: [] })

function loadTransactions() {
  return store.fetchTransactions(activeTab.value === 'credit' ? 'CREDIT' : 'DEBIT')
}

function statusLabel(status) {
  return statusLabels[status] ?? status
}

onMounted(loadTransactions)
watch(activeTab, loadTransactions)
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

      <div v-if="store.isLoading" class="bg-surface rounded-lg p-5 text-center">
        <p class="text-body-sm text-muted m-0">출금 내역을 불러오는 중이에요.</p>
      </div>
      <div v-else-if="store.error" class="bg-surface rounded-lg p-5 text-center">
        <p class="text-body-sm text-muted mt-0 mb-3">{{ store.error.message }}</p>
        <button
          type="button"
          class="text-label text-primary min-h-11 border-0 bg-transparent"
          @click="loadTransactions"
        >
          다시 시도
        </button>
      </div>
      <div v-else-if="!activeHistory.groups.length" class="bg-surface rounded-lg p-5 text-center">
        <p class="text-body-sm text-muted m-0">
          아직 {{ activeTab === 'credit' ? '적립' : '출금' }} 내역이 없어요.
        </p>
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
              <GpTag v-if="item.transactionStatus" tone="positive" small class="mt-2">{{
                statusLabel(item.transactionStatus)
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
