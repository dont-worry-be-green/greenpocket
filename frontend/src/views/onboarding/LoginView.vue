<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import GpButton from '@/components/ui/GpButton.vue'
import IconEnvelope from '@/components/ui/icons/IconEnvelope.vue'
import IconEye from '@/components/ui/icons/IconEye.vue'
import IconEyeSlash from '@/components/ui/icons/IconEyeSlash.vue'
import IconLeaf from '@/components/ui/icons/IconLeaf.vue'
import IconLock from '@/components/ui/icons/IconLock.vue'
import signupHero from '@/assets/character/signup-hero.png'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const store = useAuthStore()
const email = ref('')
const password = ref('')
const showPassword = ref(false)
const keepSignedIn = ref(true)

const canSubmit = computed(() => Boolean(email.value.trim() && password.value) && !store.isLoading)

async function submit() {
  if (!canSubmit.value) return
  const result = await store.login({ email: email.value, password: password.value })
  if (!result) return
  router.replace('/analysis/eco-link')
}
</script>

<template>
  <main class="bg-canvas relative mx-auto min-h-dvh max-w-(--gp-viewport-w) overflow-hidden">
    <section class="relative h-72 overflow-hidden px-6 pt-9" aria-label="그린포켓 소개">
      <div class="bg-primary-bg absolute -top-24 -left-24 size-72 rounded-full opacity-75"></div>
      <div class="bg-primary-bg absolute top-30 -right-20 h-32 w-120 rotate-6 rounded-[50%] opacity-75"></div>

      <div class="relative z-10 flex items-center gap-3">
        <span
          class="bg-primary shadow-card text-on-primary flex size-12 items-center justify-center rounded-full"
          aria-hidden="true"
        >
          <IconLeaf :size="28" />
        </span>
        <strong class="text-title tracking-display text-ink">그린포켓</strong>
      </div>

      <p class="text-section text-muted absolute top-36 left-7 z-10 m-0 leading-relaxed">
        오늘도, 작은 실천이<br />
        더 좋은 지구를 만듭니다
      </p>

      <img
        :src="signupHero"
        alt="두 손을 모으고 반가워하는 그린포켓 캐릭터"
        class="absolute right-1 bottom-0 z-10 w-40 object-contain"
      />
    </section>

    <form class="bg-surface shadow-card relative z-20 mx-4 -mt-2 space-y-5 rounded-card p-5" @submit.prevent="submit">
      <div class="space-y-4">
        <label class="block">
          <span class="sr-only">이메일</span>
          <span class="border-border bg-surface-sub flex min-h-15 items-center gap-3 rounded-lg border px-4">
            <IconEnvelope class="text-icon-off shrink-0" :size="25" />
          <input
            v-model="email"
            type="email"
            autocomplete="email"
            maxlength="255"
            placeholder="이메일"
            class="text-button text-ink placeholder:text-disabled-text min-w-0 flex-1 bg-transparent outline-hidden"
          />
          </span>
        </label>

        <label class="block">
          <span class="sr-only">비밀번호</span>
          <span class="border-border bg-surface-sub flex min-h-15 items-center gap-3 rounded-lg border px-4">
            <IconLock class="text-icon-off shrink-0" :size="25" />
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              maxlength="72"
              placeholder="비밀번호"
              class="text-button text-ink placeholder:text-disabled-text min-w-0 flex-1 bg-transparent outline-hidden"
            />
            <button
              type="button"
              class="text-icon-off flex size-10 cursor-pointer items-center justify-center border-0 bg-transparent p-0"
              :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 보기'"
              @click="showPassword = !showPassword"
            >
              <IconEye v-if="showPassword" :size="25" />
              <IconEyeSlash v-else :size="25" />
            </button>
          </span>
        </label>
      </div>

      <p v-if="store.error" class="text-body-sm text-negative m-0">
        {{ store.error.message }}
      </p>

      <div class="flex items-center justify-between gap-3">
        <label class="text-body text-muted flex cursor-pointer items-center gap-2">
          <input
            v-model="keepSignedIn"
            type="checkbox"
            class="border-control-border text-primary focus:ring-primary size-5 rounded-sm"
          />
          로그인 상태 유지
        </label>
        <span class="text-body-strong text-disabled-text" title="비밀번호 재설정은 현재 제공하지 않아요">
          비밀번호 찾기
        </span>
      </div>

      <div class="space-y-3">
        <GpButton :disabled="!canSubmit" @click="submit">
          {{ store.isLoading ? '들어가는 중...' : '로그인' }}
        </GpButton>
        <button
          type="button"
          class="bg-primary-bg text-primary-on-soft h-(--gp-cta-h) text-button w-full cursor-pointer rounded-md border-0 font-semibold"
          @click="router.push('/onboarding/signup')"
        >
          회원가입
        </button>
      </div>
    </form>
    <div class="h-8" aria-hidden="true"></div>
  </main>
</template>
