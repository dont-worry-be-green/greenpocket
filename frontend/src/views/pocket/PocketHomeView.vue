<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppTabLayout from '@/components/layout/AppTabLayout.vue'
import PocketState from '@/components/pocket/PocketState.vue'
import IconLeaf from '@/components/ui/icons/IconLeaf.vue'
import { usePocketStore } from '@/stores/pocket'
import { formatDateTime, formatSignedWon, formatWon } from '@/utils/format'

const router = useRouter()
const store = usePocketStore()
const actionMessage = ref('')
const pocket = computed(
  () => store.home ?? { balance: 0, convertibleMileage: 0, recentTransactions: [] },
)

onMounted(() => {
  store.fetchHome()
  window.addEventListener('focus', completeConversionOnReturn)
})
onBeforeUnmount(() => window.removeEventListener('focus', completeConversionOnReturn))

function transactionTone(type) {
  return type === 'GREENLIFE' ? 'bg-water-bg text-water' : 'bg-primary-bg text-primary'
}

async function convertMileage() {
  const roundId = pocket.value.convertibleSource?.roundId
  if (!roundId) return
  if (!window.confirm('전환 후에는 취소할 수 없어요. 마일리지를 전환할까요?')) return
  const externalWindow = window.open('about:blank', '_blank')
  if (externalWindow) externalWindow.opener = null
  const started = await store.startConversion(roundId)
  if (started?.externalUrl) {
    if (externalWindow) externalWindow.location.replace(started.externalUrl)
    else window.location.assign(started.externalUrl)
    actionMessage.value = '누리집에서 전환한 뒤 돌아오면 포켓에 반영돼요.'
  } else {
    externalWindow?.close()
    actionMessage.value = store.conversionError?.message
  }
  window.setTimeout(() => (actionMessage.value = ''), 2200)
}

async function completeConversionOnReturn() {
  if (!store.pendingConversion || store.conversionLoading) return
  const result = await store.completeConversion()
  actionMessage.value = result
    ? `${formatWon(result.amount)} 전환이 완료됐어요.`
    : store.conversionError?.message
  window.setTimeout(() => (actionMessage.value = ''), 2200)
}
</script>

<template>
  <AppTabLayout tab="pocket" title="포켓">
    <template #headerAction>
      <button
        type="button"
        class="text-primary flex min-h-11 items-center border-0 bg-transparent p-0"
        @click="router.push('/pocket/management')"
      >
        <span class="bg-surface text-label rounded-full px-3 py-2">포켓 관리</span>
      </button>
    </template>

    <PocketState :loading="store.isLoading" :error="store.error" @retry="store.fetchHome">
      <div class="space-y-5">
        <section class="bg-primary text-on-primary relative rounded-xl px-6 pt-7 pb-5">
          <span
            class="bg-canvas text-primary absolute -top-3 right-5 flex size-12 items-center justify-center rounded-full"
            aria-hidden="true"
          >
            <IconLeaf :size="24" />
          </span>

          <p class="text-body m-0 opacity-80">잔액</p>
          <div class="mt-3 flex items-center justify-between gap-5">
            <p class="text-display tabular-nums m-0 min-w-0 whitespace-nowrap">
              {{ formatWon(pocket.balance) }}
            </p>
            <button
              type="button"
              class="flex min-h-11 shrink-0 items-center border-0 bg-transparent p-0 text-white"
              @click="router.push('/pocket/withdraw')"
            >
              <span class="bg-white/25 text-label rounded-md px-3 py-2">출금</span>
            </button>
          </div>

          <div class="mt-6 border-t border-white/35">
            <div class="flex min-h-16 items-center gap-3 pt-2">
              <span class="flex size-9 items-center justify-center rounded-md bg-white/20">
                <IconLeaf :size="20" />
              </span>
              <div class="min-w-0 flex-1">
                <p class="text-body-sm m-0 opacity-80">전환 가능한 마일리지</p>
                <p class="text-list-title tabular-nums m-0">
                  {{ formatWon(pocket.convertibleMileage) }}
                </p>
              </div>
              <button
                type="button"
                class="flex min-h-11 items-center border-0 bg-transparent p-0 text-white disabled:opacity-50"
                :disabled="pocket.convertibleMileage <= 0 || store.conversionLoading"
                @click="convertMileage"
              >
                <span class="bg-white/20 text-label rounded-md px-3 py-2">전환하기</span>
              </button>
            </div>
          </div>
        </section>

        <section>
          <div class="mb-3 flex items-center justify-between">
            <h2 class="text-section m-0">최근 적립 내역</h2>
            <button
              type="button"
              class="text-body-sm text-muted min-h-11 border-0 bg-transparent"
              @click="router.push('/pocket/transactions')"
            >
              전체 보기 &gt;
            </button>
          </div>
          <div v-if="pocket.recentTransactions.length" class="bg-surface rounded-lg px-4">
            <div
              v-for="(item, index) in pocket.recentTransactions"
              :key="item.transactionId"
              class="flex min-h-20 items-center gap-3"
              :class="{ 'border-divider border-t': index > 0 }"
            >
              <span
                class="flex size-10 shrink-0 items-center justify-center rounded-full"
                :class="transactionTone(item.transactionType)"
              >
                <IconLeaf :size="20" />
              </span>
              <div class="min-w-0 flex-1">
                <p class="text-body-strong m-0 truncate">{{ item.label }}</p>
                <p class="text-caption text-muted m-0">{{ formatDateTime(item.completedAt) }}</p>
              </div>
              <p class="text-list-title text-primary tabular-nums m-0">
                {{ formatSignedWon(item.amount) }}
              </p>
            </div>
          </div>
          <div v-else class="bg-surface rounded-lg px-5 py-8 text-center">
            <p class="text-body-strong mt-0 mb-1">아직 적립 내역이 없어요</p>
            <p class="text-caption text-muted m-0">
              다양한 친환경 활동을 실천하고 그린포켓을 채워보세요!
            </p>
          </div>
        </section>
      </div>
    </PocketState>
    <div
      v-if="actionMessage"
      class="bg-ink text-on-primary shadow-float fixed bottom-24 left-1/2 z-70 w-max max-w-[calc(100%-32px)] -translate-x-1/2 rounded-full px-4 py-3 text-caption"
    >
      {{ actionMessage }}
    </div>
  </AppTabLayout>
</template>
