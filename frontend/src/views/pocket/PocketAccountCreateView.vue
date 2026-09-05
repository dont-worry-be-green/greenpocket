<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import IconChevronRight from '@/components/ui/icons/IconChevronRight.vue'
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
const bankPicker = ref(null)
const isBankPickerOpen = ref(false)
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
  accountNo.value = event.target.value.replace(/\D/g, '')
}

function selectBank(code) {
  bankCode.value = code
  isBankPickerOpen.value = false
}

function closeBankPicker(event) {
  if (!bankPicker.value?.contains(event.target)) isBankPickerOpen.value = false
}

function closeBankPickerWithEscape(event) {
  if (event.key === 'Escape') isBankPickerOpen.value = false
}

onMounted(() => {
  document.addEventListener('click', closeBankPicker)
  document.addEventListener('keydown', closeBankPickerWithEscape)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeBankPicker)
  document.removeEventListener('keydown', closeBankPickerWithEscape)
})

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
      <div ref="bankPicker" class="relative block">
        <span class="text-body-strong text-muted mb-3 block">은행</span>
        <button
          type="button"
          class="bg-surface text-body flex min-h-14 w-full items-center rounded-lg border-0 px-4 text-left"
          aria-haspopup="listbox"
          :aria-expanded="isBankPickerOpen"
          @click="isBankPickerOpen = !isBankPickerOpen"
        >
          <span class="flex-1">{{ selectedBank.name }}</span>
          <IconChevronRight :size="18" class="text-icon-off rotate-90" />
        </button>
        <div
          v-if="isBankPickerOpen"
          class="bg-surface shadow-card absolute inset-x-0 top-full z-20 mt-2 max-h-64 overflow-y-auto rounded-lg p-1"
          role="listbox"
          aria-label="은행 선택"
        >
          <button
            v-for="bank in BANKS"
            :key="bank.code"
            type="button"
            role="option"
            :aria-selected="bank.code === bankCode"
            class="text-body hover:bg-primary-bg flex min-h-12 w-full items-center rounded-md border-0 bg-transparent px-3 text-left"
            :class="bank.code === bankCode ? 'text-primary font-semibold' : 'text-ink'"
            @click="selectBank(bank.code)"
          >
            {{ bank.name }}
          </button>
        </div>
      </div>

      <label class="block">
        <span class="text-body-strong text-muted mb-3 block">계좌번호</span>
        <input
          :value="accountNo"
          type="text"
          inputmode="numeric"
          pattern="[0-9]*"
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
