<script setup>
const email = defineModel('email', { type: String, default: '' })
const password = defineModel('password', { type: String, default: '' })
const passwordConfirm = defineModel('passwordConfirm', { type: String, default: '' })

defineProps({
  emailError: { type: String, default: '' },
})

const emit = defineEmits(['blur', 'email-blur'])
</script>

<template>
  <section class="space-y-3">
    <label class="block">
      <span class="text-body-strong text-muted mb-2 block">이메일</span>
      <input
        v-model="email"
        type="email"
        autocomplete="email"
        maxlength="255"
        placeholder="name@example.com"
        class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 outline-hidden"
        @blur="emit('email-blur')"
      />
      <span v-if="emailError" class="text-body-sm text-negative mt-1.5 block">{{
        emailError
      }}</span>
    </label>

    <label class="block">
      <span class="text-body-strong text-muted mb-2 block">비밀번호</span>
      <input
        v-model="password"
        type="password"
        autocomplete="new-password"
        maxlength="72"
        placeholder="8자 이상"
        class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 outline-hidden"
        @blur="emit('blur')"
      />
    </label>

    <label class="block">
      <span class="text-body-strong text-muted mb-2 block">비밀번호 확인</span>
      <input
        v-model="passwordConfirm"
        type="password"
        autocomplete="new-password"
        maxlength="72"
        placeholder="한 번 더 입력하세요"
        class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 outline-hidden"
        @blur="emit('blur')"
      />
    </label>
  </section>
</template>
