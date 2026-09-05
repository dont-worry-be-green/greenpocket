<script setup>
/*
 * ONB-01 시작·이름 등록 (COM-01)
 *
 * **첫 화면이라 뒤로가기가 없다.** `AppSubLayout` 은 `GpBackHeader` 를 항상 그리므로 맞지 않고,
 * 헤더 없는 전체화면 셸은 `components/layout/` 에 없다. 소비자가 이 화면 하나뿐이라
 * 공용 폴더에 셸을 새로 만들지 않고 여기서 직접 쓴다.
 *
 * 이 화면의 저장이 **데모 키를 서버에 등록한다**(`POST /users`). 그전에는 나머지 API 가 전부
 * 401 이다 — 자세한 것은 `api/onboarding.js` 의 `startUser` 주석.
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import GpButton from '@/components/ui/GpButton.vue'
import { useOnboardingStore } from '@/stores/onboarding'

const router = useRouter()
const store = useOnboardingStore()

const name = ref('')
const touched = ref(false)

// api-spec.md 4.1 — trim 후 1~20자. 공백·특수문자만이면 NAME_INVALID.
// shim 도 같은 조건으로 던진다(이중 방어). 여기서만 막으면 연동 후 처음 보는 에러가 된다
const trimmed = computed(() => name.value.trim())
const validationMessage = computed(() => {
  if (!trimmed.value) return '이름을 입력해 주세요.'
  if (trimmed.value.length > 20) return '이름은 20자까지 입력할 수 있어요.'
  if (!/[\p{L}\p{N}]/u.test(trimmed.value)) return '글자나 숫자를 하나 이상 넣어 주세요.'
  return ''
})
const canSubmit = computed(() => !validationMessage.value && !store.isLoading)

async function submit() {
  touched.value = true
  if (!canSubmit.value) return

  const started = await store.startUser(trimmed.value)
  if (started) router.push('/onboarding/profile')
}
</script>

<template>
  <div class="bg-canvas flex min-h-dvh flex-col px-(--gp-gutter)">
    <main class="flex flex-1 flex-col pt-16">
      <h1 class="text-title tracking-display text-ink m-0">그린포켓</h1>
      <p class="text-body text-ink-soft mt-3 mb-0">
        관리비 고지서로 새는 돈을 찾고, 아낀 만큼 마일리지로 모아요.
      </p>

      <p class="text-caption text-muted bg-surface mt-6 mb-0 rounded-lg p-4">
        발표용 데모예요. 회원가입 없이 바로 쓰고, 입력한 정보는 이 기기에만 저장돼요.
      </p>

      <form class="mt-8" @submit.prevent="submit">
        <label class="block">
          <span class="text-body-strong text-muted mb-3 block">이름</span>
          <input
            v-model="name"
            type="text"
            autocomplete="name"
            maxlength="20"
            placeholder="이름을 입력해주세요"
            class="bg-surface text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border-0 px-4 outline-hidden"
            @blur="touched = true"
          />
        </label>

        <p v-if="touched && validationMessage" class="text-body-sm text-negative mt-3 mb-0">
          {{ validationMessage }}
        </p>
        <p v-else-if="store.error" class="text-body-sm text-negative mt-3 mb-0">
          {{ store.error.message }}
        </p>
      </form>
    </main>

    <div class="pt-3 pb-[max(12px,env(safe-area-inset-bottom))]">
      <GpButton :disabled="!canSubmit" @click="submit">
        {{ store.isLoading ? '시작하는 중...' : '시작하기' }}
      </GpButton>
    </div>
  </div>
</template>
