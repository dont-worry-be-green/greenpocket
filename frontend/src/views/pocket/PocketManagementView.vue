<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import PocketState from '@/components/pocket/PocketState.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconPocket from '@/components/ui/icons/IconPocket.vue'
import { usePocketStore } from '@/stores/pocket'
import { formatWon } from '@/utils/format'

const store = usePocketStore()
const router = useRouter()
const copyMessage = ref('')

const management = computed(
  () => store.management ?? { pocket: { accountNo: '', holder: '', balance: 0 }, accounts: [] },
)

onMounted(() => store.fetchManagement())

async function copyAccount() {
  try {
    await navigator.clipboard.writeText(management.value.pocket.accountNo)
    copyMessage.value = '계좌번호를 복사했어요.'
  } catch {
    copyMessage.value = '복사하지 못했어요. 다시 시도해 주세요.'
  }
  window.setTimeout(() => (copyMessage.value = ''), 2000)
}
</script>

<template>
  <AppSubLayout title="그린포켓 관리" back="/pocket" center-title>
    <PocketState :loading="store.isLoading" :error="store.error" @retry="store.fetchManagement">
      <div class="space-y-5 pt-5">
        <section>
          <h2 class="text-body-strong text-muted mb-3">그린포켓 정보</h2>
          <div class="bg-surface rounded-lg px-4">
            <div class="border-divider flex min-h-14 items-center justify-between gap-3 border-b">
              <span class="text-muted shrink-0">그린포켓 계좌</span>
              <button
                type="button"
                class="text-body-strong min-h-11 border-0 bg-transparent text-right"
                @click="copyAccount"
              >
                {{ management.pocket.accountNo }} <span class="text-primary">복사</span>
              </button>
            </div>
            <div class="border-divider flex min-h-14 items-center justify-between border-b">
              <span class="text-muted">예금주</span><strong>{{ management.pocket.holder }}</strong>
            </div>
            <div class="flex min-h-14 items-center justify-between">
              <span class="text-muted">포켓 잔액</span
              ><strong class="text-primary tabular-nums">{{
                formatWon(management.pocket.balance)
              }}</strong>
            </div>
          </div>
        </section>

        <section>
          <h2 class="text-body-strong text-muted mb-3">출금 계좌 관리</h2>
          <div class="space-y-3">
            <div
              v-for="account in store.accounts"
              :key="account.accountId"
              class="bg-surface flex min-h-16 items-center gap-3 rounded-lg px-4"
            >
              <span
                class="bg-primary-bg text-primary flex size-9 items-center justify-center rounded-md"
                ><IconPocket :size="20"
              /></span>
              <p class="text-body-strong m-0 flex-1">
                {{ account.bankName }} {{ account.accountNo }}
              </p>
              <GpTag v-if="account.isDefault" tone="estimated">기본 계좌</GpTag>
            </div>
            <div
              v-if="store.accountsLoaded && !store.accounts.length"
              class="bg-surface rounded-lg p-5 text-center"
            >
              <p class="text-body-sm text-muted m-0">등록된 출금 계좌가 없어요.</p>
            </div>
            <button
              type="button"
              class="border-control-border text-body-strong min-h-12 w-full rounded-full border bg-transparent"
              @click="router.push({ path: '/pocket/accounts/new', query: { from: 'management' } })"
            >
              + 출금 계좌 추가
            </button>
          </div>
        </section>

        <div class="bg-surface flex gap-3 rounded-lg p-4">
          <span
            class="bg-primary-bg text-primary flex size-9 shrink-0 items-center justify-center rounded-md"
            >!</span
          >
          <p class="text-body-sm text-muted m-0">
            출금 신청 시 등록된 계좌로 포인트가 입금됩니다. 안전한 이용을 위해 본인 명의 계좌만
            등록할 수 있어요.
          </p>
        </div>
      </div>
    </PocketState>

    <div
      v-if="copyMessage"
      class="bg-ink text-on-primary shadow-float fixed bottom-20 left-1/2 z-70 -translate-x-1/2 rounded-full px-4 py-3 text-caption"
    >
      {{ copyMessage }}
    </div>
  </AppSubLayout>
</template>
