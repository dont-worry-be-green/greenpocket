<script setup>
import { ref, watch } from 'vue'

import GpButton from '@/components/ui/GpButton.vue'
import GpModal from '@/components/ui/GpModal.vue'
import GpTag from '@/components/ui/GpTag.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '출금 계좌 변경' },
  accounts: { type: Array, default: () => [] },
  selectedId: { type: Number, default: null },
  saving: { type: Boolean, default: false },
  error: { type: Object, default: null },
})
const emit = defineEmits(['close', 'save'])

const draftId = ref(null)

watch(
  () => [props.open, props.selectedId],
  ([open, selectedId]) => {
    if (open) draftId.value = selectedId ?? props.accounts[0]?.accountId ?? null
  },
  { immediate: true },
)
</script>

<template>
  <GpModal :open="open" :title="title" @close="emit('close')">
    <div class="max-h-64 space-y-2 overflow-y-auto" role="radiogroup" aria-label="출금 계좌 목록">
      <button
        v-for="account in accounts"
        :key="account.accountId"
        type="button"
        role="radio"
        :aria-checked="draftId === account.accountId"
        class="border-divider flex min-h-14 w-full items-center gap-3 rounded-lg border bg-transparent px-3 text-left"
        @click="draftId = account.accountId"
      >
        <span
          class="flex size-5 shrink-0 items-center justify-center rounded-full border-2"
          :class="draftId === account.accountId ? 'border-primary' : 'border-control-border'"
          aria-hidden="true"
        >
          <span
            v-if="draftId === account.accountId"
            class="bg-primary size-2.5 rounded-full"
          ></span>
        </span>
        <span class="text-body-strong min-w-0 flex-1">
          {{ account.bankName }} {{ account.accountNo }}
        </span>
        <GpTag v-if="account.isDefault" tone="estimated" small>기본 계좌</GpTag>
      </button>
    </div>

    <p v-if="error" class="text-body-sm text-negative mt-3 mb-0">{{ error.message }}</p>

    <template #footer>
      <GpButton :disabled="draftId === null || saving" @click="emit('save', draftId)">
        {{ saving ? '저장 중...' : '저장' }}
      </GpButton>
    </template>
  </GpModal>
</template>
