<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import { usePocketStore } from '@/stores/pocket'

const BANKS = [
  { code: '088', name: '신한은행' },
  { code: '004', name: 'KB국민은행' },
  { code: '020', name: '우리은행' },
  { code: '081', name: '하나은행' },
  { code: '011', name: 'NH농협은행' },
  { code: '003', name: 'IBK기업은행' },
  { code: '090', name: '카카오뱅크' },
  { code: '092', name: '토스뱅크' },
]

const route = useRoute()
const router = useRouter()
const store = usePocketStore()

const bankCode = ref(BANKS[0].code)
const accountNo = ref('')
const holder = ref('')
const isDefault = ref(false)
const touched = ref(false)

const selectedBank = computed(() => BANKS.find((bank) => bank.code === bankCode.value))
const returnPath = computed(() =>
  route.query.from === 'withdraw' ? '/pocket/withdraw' : '/pocket/management',
)
const validationMessage = computed(() => {
  if (!accountNo.value.trim()) return '계좌번호를 입력해 주세요.'
  if (!holder.value.trim()) return '예금주명을 입력해 주세요.'
  return ''
})
const canSubmit = computed(() => !validationMessage.value && !store.accountCreateLoading)

function normalizeAccountNumber(event) {
  accountNo.value = event.target.value.replace(/[^0-9-]/g, '')
}

async function submit() {
  touched.value = true
  if (!canSubmit.value) return

  const created = await store.createAccount({
    bankCode: selectedBank.value.code,
    bankName: selectedBank.value.name,
    accountNo: accountNo.value.trim(),
    holder: holder.value.trim(),
    isDefault: isDefault.value,
  })

  if (created) router.replace(returnPath.value)
}
</script>

<template>
  <AppSubLayout title="출금 계좌 등록" :back="returnPath" has-footer>
    <form class="space-y-5 pt-5" @submit.prevent="submit">
      <label class="block">
        <span class="text-body-strong text-muted mb-3 block">은행</span>
        <span class="bg-surface relative flex min-h-14 items-center rounded-lg px-4">
          <select
            v-model="bankCode"
            class="text-body w-full appearance-none border-0 bg-transparent pr-8 outline-hidden"
            aria-label="은행 선택"
          >
            <option v-for="bank in BANKS" :key="bank.code" :value="bank.code">
              {{ bank.name }}
            </option>
          </select>
          <span class="text-icon-off pointer-events-none absolute right-4" aria-hidden="true"
            >›</span
          >
        </span>
      </label>

      <label class="block">
        <span class="text-body-strong text-muted mb-3 block">계좌번호</span>
        <input
          :value="accountNo"
          type="text"
          inputmode="numeric"
          autocomplete="off"
          placeholder="계좌번호를 입력해주세요"
          class="bg-surface text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border-0 px-4 outline-hidden"
          @input="normalizeAccountNumber"
        />
      </label>

      <label class="block">
        <span class="text-body-strong text-muted mb-3 block">예금주</span>
        <input
          v-model="holder"
          type="text"
          autocomplete="name"
          maxlength="30"
          placeholder="예금주명을 입력해주세요"
          class="bg-surface text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border-0 px-4 outline-hidden"
        />
      </label>

      <label class="flex min-h-11 cursor-pointer items-center gap-3">
        <input
          v-model="isDefault"
          type="checkbox"
          class="accent-primary size-(--gp-checkbox) shrink-0"
        />
        <span class="text-body">기본 출금 계좌로 설정</span>
      </label>

      <p v-if="touched && validationMessage" class="text-body-sm text-negative m-0">
        {{ validationMessage }}
      </p>
      <p v-if="store.accountCreateError" class="text-body-sm text-negative m-0">
        {{ store.accountCreateError.message }}
      </p>
    </form>

    <template #footer>
      <div
        class="bg-canvas fixed inset-x-0 bottom-0 mx-auto max-w-(--gp-viewport-w) px-(--gp-gutter) pt-3 pb-[max(12px,env(safe-area-inset-bottom))]"
      >
        <GpButton :disabled="!canSubmit" @click="submit">
          {{ store.accountCreateLoading ? '등록 중...' : '계좌 등록하기' }}
        </GpButton>
      </div>
    </template>
  </AppSubLayout>
</template>
