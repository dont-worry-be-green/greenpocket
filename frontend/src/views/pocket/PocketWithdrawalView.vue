<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import PocketAccountSelectModal from '@/components/pocket/PocketAccountSelectModal.vue'
import GpButton from '@/components/ui/GpButton.vue'
import GpTag from '@/components/ui/GpTag.vue'
import IconPocket from '@/components/ui/icons/IconPocket.vue'
import { usePocketStore } from '@/stores/pocket'
import { formatNumber, formatWon } from '@/utils/format'

const router = useRouter()
const store = usePocketStore()
const amount = ref(0)
const isSubmitting = ref(false)
const selectedAccountId = ref(null)
const isAccountModalOpen = ref(false)

const balance = computed(() => store.balance?.balance ?? 0)
const selectedAccount = computed(
  () =>
    store.accounts.find((account) => account.accountId === selectedAccountId.value) ??
    store.defaultAccount,
)
const validationMessage = computed(() => {
  if (!Number.isInteger(amount.value) || amount.value <= 0) return '출금 금액을 입력해 주세요.'
  if (amount.value > balance.value) return '출금 가능 잔액보다 큰 금액은 신청할 수 없어요.'
  if (store.accountsLoaded && !selectedAccount.value) return '출금 계좌를 먼저 등록해 주세요.'
  return ''
})
const canSubmit = computed(
  () =>
    !validationMessage.value &&
    Boolean(selectedAccount.value) &&
    !store.accountsLoading &&
    !isSubmitting.value,
)

onMounted(async () => {
  await Promise.all([store.fetchBalance(), store.fetchWithdrawalAccounts()])
  amount.value = balance.value
  selectedAccountId.value = store.defaultAccount?.accountId ?? null
})

function selectAmount(value) {
  if (value > balance.value) return
  amount.value = value
}

function selectAccount(accountId) {
  selectedAccountId.value = accountId
  isAccountModalOpen.value = false
}

async function submit() {
  if (!canSubmit.value) return
  isSubmitting.value = true
  const result = await store.withdraw(amount.value, selectedAccount.value.accountId)
  isSubmitting.value = false
  if (result) router.push('/pocket/withdraw/complete')
}
</script>

<template>
  <AppSubLayout title="출금 신청" back="/pocket" center-title has-footer>
    <div class="space-y-6">
      <section class="bg-surface rounded-xl px-6 py-7">
        <p class="text-body text-muted mt-0 mb-2">출금 가능 잔액</p>
        <p class="text-display tabular-nums m-0">{{ formatWon(balance) }}</p>
      </section>

      <section>
        <h2 class="text-body-strong text-muted mb-3">출금 금액</h2>
        <div class="bg-surface flex min-h-16 items-center gap-3 rounded-lg px-4">
          <strong class="text-section tabular-nums flex-1">{{ formatNumber(amount) }}</strong>
          <span class="text-muted">원</span>
        </div>
        <div class="mt-3 grid grid-cols-4 gap-2">
          <button
            v-for="option in [10000, 30000, 50000]"
            :key="option"
            type="button"
            class="border-primary-bg-strong text-primary text-label min-h-11 rounded-full border bg-transparent disabled:border-control-border disabled:text-disabled-text"
            :disabled="option > balance"
            @click="selectAmount(option)"
          >
            +{{ option / 10000 }}만원
          </button>
          <button
            type="button"
            class="border-control-border text-label text-muted min-h-11 rounded-full border bg-transparent"
            @click="selectAmount(balance)"
          >
            전액
          </button>
        </div>
        <p v-if="validationMessage" class="text-caption text-negative mt-2 mb-0">
          {{ validationMessage }}
        </p>
      </section>

      <section>
        <h2 class="text-body-strong text-muted mb-3">출금 계좌</h2>
        <div
          v-if="store.accountsLoading"
          class="bg-surface flex min-h-16 items-center justify-center rounded-lg px-4"
        >
          <p class="text-body-sm text-muted m-0">출금 계좌를 불러오는 중이에요.</p>
        </div>
        <div
          v-else-if="store.accountsError"
          class="bg-surface flex min-h-16 items-center justify-between gap-3 rounded-lg px-4"
        >
          <p class="text-body-sm text-muted m-0">{{ store.accountsError.message }}</p>
          <button
            type="button"
            class="text-label text-primary shrink-0 border-0 bg-transparent"
            @click="store.fetchWithdrawalAccounts()"
          >
            다시 시도
          </button>
        </div>
        <div v-else-if="selectedAccount" class="space-y-3">
          <div class="bg-surface flex min-h-16 items-center gap-3 rounded-lg px-4">
            <span
              class="bg-primary-bg text-primary flex size-9 shrink-0 items-center justify-center rounded-md"
            >
              <IconPocket :size="20" />
            </span>
            <p class="text-body-strong m-0 min-w-0 flex-1">
              {{ selectedAccount.bankName }} {{ selectedAccount.accountNo }}
            </p>
            <GpTag v-if="selectedAccount.isDefault" tone="estimated" small>기본 계좌</GpTag>
          </div>

          <button
            type="button"
            class="border-control-border text-body-strong min-h-12 w-full rounded-full border bg-transparent"
            @click="isAccountModalOpen = true"
          >
            출금 계좌 변경
          </button>
        </div>
        <div v-else class="bg-surface rounded-lg p-5 text-center">
          <p class="text-body-sm text-muted m-0">등록된 출금 계좌가 없어요.</p>
        </div>
      </section>

      <div class="bg-confirmed-bg text-body-sm text-muted rounded-lg p-4">
        출금 신청은 평일 09:00 ~ 18:00에 가능하며, 신청 후 영업일 기준 1~2일 내 입금됩니다.
      </div>
      <p v-if="store.withdrawalError" class="text-body-sm text-negative m-0 text-center">
        {{ store.withdrawalError.message }}
      </p>
    </div>

    <template #footer>
      <div
        class="bg-canvas fixed inset-x-0 bottom-0 mx-auto max-w-(--gp-viewport-w) px-(--gp-gutter) pt-3 pb-[max(12px,env(safe-area-inset-bottom))]"
      >
        <GpButton :disabled="!canSubmit" @click="submit">{{ formatWon(amount) }} 출금하기</GpButton>
      </div>
    </template>

    <PocketAccountSelectModal
      :open="isAccountModalOpen"
      :accounts="store.accounts"
      :selected-id="selectedAccount?.accountId"
      @close="isAccountModalOpen = false"
      @save="selectAccount"
    />
  </AppSubLayout>
</template>
