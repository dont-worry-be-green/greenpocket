<script setup>
import { useRouter } from 'vue-router'

import GpButton from '@/components/ui/GpButton.vue'
import { usePocketStore } from '@/stores/pocket'
import { formatDateTime, formatWon } from '@/utils/format'

const router = useRouter()
const store = usePocketStore()
</script>

<template>
  <div
    class="bg-canvas flex min-h-dvh flex-col px-(--gp-gutter) pt-16 pb-[max(12px,env(safe-area-inset-bottom))]"
  >
    <template v-if="store.withdrawalResult?.transactionStatus === 'COMPLETED'">
      <div class="text-center">
        <div
          class="bg-primary-bg text-primary mx-auto flex size-20 items-center justify-center rounded-xl text-5xl"
        >
          ✓
        </div>
        <h1 class="text-title mt-6 mb-2">출금 신청이 완료됐어요!</h1>
        <p class="text-body text-muted m-0">신청한 금액은 아래 계좌로<br />입금될 예정이에요.</p>
      </div>

      <section class="bg-surface mt-8 rounded-xl px-6 py-5">
        <div class="border-divider flex min-h-14 items-center justify-between border-b">
          <span class="text-muted">출금 금액</span
          ><strong class="tabular-nums">{{ formatWon(store.withdrawalResult.amount) }}</strong>
        </div>
        <div class="border-divider flex min-h-14 items-center justify-between gap-3 border-b">
          <span class="text-muted shrink-0">입금 계좌</span
          ><strong class="text-right"
            >{{ store.withdrawalResult.accountSnapshot.bankName }}
            {{ store.withdrawalResult.accountSnapshot.accountNo }}</strong
          >
        </div>
        <div class="flex min-h-14 items-center justify-between">
          <span class="text-muted">신청 일시</span
          ><strong>{{ formatDateTime(store.withdrawalResult.requestedAt) }}</strong>
        </div>
      </section>

      <p class="text-body-sm text-muted mt-5 text-center">{{ store.withdrawalResult.notice }}</p>
      <div class="mt-auto">
        <GpButton @click="router.replace('/pocket')">그린포켓으로 돌아가기</GpButton>
      </div>
    </template>

    <template v-else>
      <div class="my-auto text-center">
        <h1 class="text-section">완료된 출금 신청이 없어요</h1>
        <p class="text-body-sm text-muted">처리 중이거나 실패한 신청은 완료로 표시하지 않아요.</p>
        <GpButton variant="wide" size="wide" @click="router.replace('/pocket')"
          >그린포켓으로 돌아가기</GpButton
        >
      </div>
    </template>
  </div>
</template>
