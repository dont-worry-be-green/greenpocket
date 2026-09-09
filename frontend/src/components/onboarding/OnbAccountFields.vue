<script setup>
import { ref } from 'vue'

import IconEnvelope from '@/components/ui/icons/IconEnvelope.vue'
import IconEye from '@/components/ui/icons/IconEye.vue'
import IconEyeSlash from '@/components/ui/icons/IconEyeSlash.vue'
import IconLock from '@/components/ui/icons/IconLock.vue'

const email = defineModel('email', { type: String, default: '' })
const password = defineModel('password', { type: String, default: '' })
const passwordConfirm = defineModel('passwordConfirm', { type: String, default: '' })

defineProps({
  emailError: { type: String, default: '' },
})

const emit = defineEmits(['blur', 'email-blur'])

const showPassword = ref(false)
const showPasswordConfirm = ref(false)
</script>

<template>
  <section class="space-y-3">
    <label class="block">
      <span class="text-body-strong text-muted mb-2 block">이메일</span>
      <div class="relative">
        <IconEnvelope
          :size="22"
          class="text-muted pointer-events-none absolute top-1/2 left-4 -translate-y-1/2"
        />
        <input
          v-model="email"
          type="email"
          autocomplete="email"
          maxlength="255"
          placeholder="name@example.com"
          class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border pr-4 pl-12 outline-hidden"
          @blur="emit('email-blur')"
        />
      </div>
      <span v-if="emailError" class="text-body-sm text-negative mt-1.5 block">{{
        emailError
      }}</span>
    </label>

    <label class="block">
      <span class="text-body-strong text-muted mb-2 block">비밀번호</span>
      <div class="relative">
        <IconLock
          :size="22"
          class="text-muted pointer-events-none absolute top-1/2 left-4 -translate-y-1/2"
        />
        <input
          v-model="password"
          :type="showPassword ? 'text' : 'password'"
          autocomplete="new-password"
          maxlength="72"
          placeholder="8자 이상"
          class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border pr-12 pl-12 outline-hidden"
          @blur="emit('blur')"
        />
        <button
          type="button"
          class="text-muted absolute top-1/2 right-3 flex size-10 -translate-y-1/2 cursor-pointer items-center justify-center border-0 bg-transparent"
          :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 보기'"
          @click="showPassword = !showPassword"
        >
          <IconEyeSlash v-if="showPassword" :size="22" />
          <IconEye v-else :size="22" />
        </button>
      </div>
    </label>

    <label class="block">
      <span class="text-body-strong text-muted mb-2 block">비밀번호 확인</span>
      <div class="relative">
        <IconLock
          :size="22"
          class="text-muted pointer-events-none absolute top-1/2 left-4 -translate-y-1/2"
        />
        <input
          v-model="passwordConfirm"
          :type="showPasswordConfirm ? 'text' : 'password'"
          autocomplete="new-password"
          maxlength="72"
          placeholder="한 번 더 입력하세요"
          class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border pr-12 pl-12 outline-hidden"
          @blur="emit('blur')"
        />
        <button
          type="button"
          class="text-muted absolute top-1/2 right-3 flex size-10 -translate-y-1/2 cursor-pointer items-center justify-center border-0 bg-transparent"
          :aria-label="showPasswordConfirm ? '비밀번호 확인 숨기기' : '비밀번호 확인 보기'"
          @click="showPasswordConfirm = !showPasswordConfirm"
        >
          <IconEyeSlash v-if="showPasswordConfirm" :size="22" />
          <IconEye v-else :size="22" />
        </button>
      </div>
    </label>
  </section>
</template>
