<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const store = useAuthStore()
const email = ref('')
const password = ref('')

const canSubmit = computed(() => Boolean(email.value.trim() && password.value) && !store.isLoading)

async function submit() {
  if (!canSubmit.value) return
  const result = await store.login({ email: email.value, password: password.value })
  if (!result) return
  router.replace('/analysis/eco-link')
}
</script>

<template>
  <AppSubLayout back="/onboarding/start">
    <form class="space-y-5 pt-2" @submit.prevent="submit">
      <div>
        <h1 class="text-title tracking-display text-ink m-0">다시 오셨네요</h1>
        <p class="text-body text-muted mt-2 mb-0">이메일과 비밀번호로 들어오세요</p>
      </div>

      <div class="space-y-3">
        <label class="block">
          <span class="text-body-strong text-muted mb-2 block">이메일</span>
          <input
            v-model="email"
            type="email"
            autocomplete="email"
            maxlength="255"
            placeholder="name@example.com"
            class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 outline-hidden"
          />
        </label>

        <label class="block">
          <span class="text-body-strong text-muted mb-2 block">비밀번호</span>
          <input
            v-model="password"
            type="password"
            autocomplete="current-password"
            maxlength="72"
            placeholder="비밀번호를 입력하세요"
            class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 outline-hidden"
          />
        </label>
      </div>

      <p v-if="store.error" class="text-body-sm text-negative m-0">
        {{ store.error.message }}
      </p>

      <div>
        <GpButton :disabled="!canSubmit" @click="submit">
          {{ store.isLoading ? '들어가는 중...' : '로그인' }}
        </GpButton>
        <button
          type="button"
          class="text-label text-primary-on-soft mt-1 w-full cursor-pointer border-0 bg-transparent p-2 font-semibold"
          @click="router.push('/onboarding/signup')"
        >
          계정이 없어요 · 회원가입
        </button>
      </div>
    </form>
  </AppSubLayout>
</template>
