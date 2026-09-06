<script setup>
/*
 * ONB-01a 로그인 (이슈 #121)
 *
 * ⚠️ **비밀번호를 검증하지 않는다.** BE 에 `POST /auth/login` 이 아직 없어 대조할 곳이 없다.
 * 이 기기에 가입해 둔 아이디와 맞춰 보는 수준이고, 화면이 그 사실을 캡션으로 밝힌다
 * (`api/auth.js` 주석). 계약이 붙으면 그 파일만 갈아끼운다 — 이 화면은 그대로 산다.
 *
 * 그래서 「없는 계정」은 에러가 아니라 안내다. `found: false` 를 받아 문구를 만든다 —
 * 공통 에러 코드를 새로 만들지 않는다(AGENTS.md 3절).
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import GpButton from '@/components/ui/GpButton.vue'
import IconInfo from '@/components/ui/icons/IconInfo.vue'
import { useAuthStore } from '@/stores/auth'
import { isOnboarded } from '@/router/guards'

const router = useRouter()
const store = useAuthStore()

// 이 기기에 가입한 아이디가 있으면 프리필한다. 데모에서 타이핑을 줄이는 자리다
const loginId = ref(store.savedLoginId)
const password = ref('')
const notFound = ref(false)

const canSubmit = computed(
  () => Boolean(loginId.value.trim() && password.value) && !store.isLoading,
)

async function submit() {
  if (!canSubmit.value) return
  notFound.value = false

  const result = await store.login({ loginId: loginId.value })
  if (!result) return
  if (!result.found) {
    notFound.value = true
    return
  }
  // 프로필까지 마친 사람은 홈으로, 아니면 이어서 프로필을 받는다
  router.replace(isOnboarded() ? '/whatif' : '/onboarding/profile')
}
</script>

<template>
  <AppSubLayout back="/onboarding/start">
    <form class="space-y-5 pt-2" @submit.prevent="submit">
      <div>
        <h1 class="text-title tracking-display text-ink m-0">다시 오셨네요</h1>
        <p class="text-body text-muted mt-2 mb-0">아이디와 비밀번호로 들어오세요</p>
      </div>

      <div class="space-y-3">
        <label class="block">
          <span class="text-body-strong text-muted mb-2 block">아이디</span>
          <input
            v-model="loginId"
            type="text"
            autocomplete="username"
            maxlength="20"
            placeholder="아이디를 입력하세요"
            class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 outline-hidden"
          />
        </label>

        <label class="block">
          <span class="text-body-strong text-muted mb-2 block">비밀번호</span>
          <input
            v-model="password"
            type="password"
            autocomplete="current-password"
            maxlength="32"
            placeholder="비밀번호를 입력하세요"
            class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 outline-hidden"
          />
        </label>
      </div>

      <!-- 서버가 아니라고 한 것이 아니라 이 기기에 기록이 없는 것이다 -->
      <p v-if="notFound" class="text-body-sm text-negative m-0">
        이 기기에 그 아이디로 가입한 기록이 없어요. 회원가입으로 시작해 주세요.
      </p>
      <p v-else-if="store.error" class="text-body-sm text-negative m-0">
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

        <p class="text-caption text-muted bg-surface-sub mt-3 mb-0 flex gap-2 rounded-md p-3">
          <IconInfo :size="16" class="text-icon-off mt-0.5 shrink-0" aria-hidden="true" />
          <span>
            발표용 데모라 비밀번호는 확인하지 않아요. 이 기기에서 가입한 아이디로만 들어올 수
            있어요.
          </span>
        </p>
      </div>
    </form>
  </AppSubLayout>
</template>
