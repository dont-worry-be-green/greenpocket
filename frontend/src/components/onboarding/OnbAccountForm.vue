<script setup>
/*
 * ONB-01b 회원가입 ③ 계정 만들기 — 이름 · 아이디 · 비밀번호 (이슈 #121)
 *
 * ⚠️ **비밀번호는 아무 데도 보내지 않고 저장하지도 않는다.** BE 에 `POST /auth/signup` 이
 * 아직 없어 대조할 곳이 없고, 평문을 `localStorage` 에 두는 쪽이 더 나쁘다(`api/auth.js` 주석).
 * 그래서 화면이 캡션으로 그 사실을 밝힌다 — 발표에서 실제로 계정이 보호되는 것처럼 보이면 안 된다.
 *
 * 그래도 **입력 검증은 실제로 한다.** 비밀번호 확인 불일치·형식 미달을 통과시키면 나중에 계약이
 * 붙었을 때 처음 보는 에러가 된다(`StartView` 의 이름 검증과 같은 태도).
 *
 * 이름 규칙은 `api-spec.md` 4.1 을 그대로 따른다 — trim 후 1~20자, 글자나 숫자 하나 이상.
 * 그 값이 그린포켓 예금주가 된다(결정 C-14).
 */
import { computed, ref } from 'vue'

import GpButton from '@/components/ui/GpButton.vue'
import IconInfo from '@/components/ui/icons/IconInfo.vue'

const props = defineProps({
  /** 본인확인 단계에서 이미 아는 값이 있으면 프리필한다 */
  defaultName: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  errorMessage: { type: String, default: '' },
})
const emit = defineEmits(['submit'])

const name = ref(props.defaultName)
const loginId = ref('')
const password = ref('')
const passwordConfirm = ref('')
const touched = ref(false)

const trimmedName = computed(() => name.value.trim())
const trimmedId = computed(() => loginId.value.trim())

/** 첫 번째로 걸리는 것만 보여준다. 네 줄을 한꺼번에 띄우면 무엇부터 고칠지 알 수 없다 */
const validationMessage = computed(() => {
  if (!trimmedName.value) return '이름을 입력해 주세요.'
  if (trimmedName.value.length > 20) return '이름은 20자까지 입력할 수 있어요.'
  if (!/[\p{L}\p{N}]/u.test(trimmedName.value)) return '이름에 글자나 숫자를 하나 이상 넣어 주세요.'
  if (trimmedId.value.length < 4) return '아이디는 4자 이상 입력해 주세요.'
  if (!/^[a-zA-Z0-9._-]+$/.test(trimmedId.value))
    return '아이디는 영문·숫자와 . _ - 만 쓸 수 있어요.'
  if (password.value.length < 8) return '비밀번호는 8자 이상 입력해 주세요.'
  if (password.value !== passwordConfirm.value) return '비밀번호가 서로 달라요.'
  return ''
})

const canSubmit = computed(() => !validationMessage.value && !props.loading)

/*
 * CTA 가 비활성이면 클릭 이벤트가 아예 오지 않아 `touched` 가 켜지지 않는다. 그래서 무엇이
 * 잘못됐는지 **보여줄 기회가 없다.** 비밀번호 확인을 입력하기 시작하면 그때부터 알려준다 —
 * 불일치는 사용자가 화면만 보고는 알 수 없는 유일한 항목이다.
 */
const showValidation = computed(() => touched.value || Boolean(passwordConfirm.value))

function submit() {
  touched.value = true
  if (!canSubmit.value) return
  emit('submit', { name: trimmedName.value, loginId: trimmedId.value, password: password.value })
}
</script>

<template>
  <form class="space-y-5" @submit.prevent="submit">
    <div>
      <h1 class="text-title tracking-display text-ink m-0">계정을 만들어요</h1>
      <p class="text-body text-muted mt-2 mb-0">
        다음에는 아이디와 비밀번호로 바로 들어올 수 있어요
      </p>
    </div>

    <div class="space-y-3">
      <label class="block">
        <span class="text-body-strong text-muted mb-2 block">이름</span>
        <input
          v-model="name"
          type="text"
          autocomplete="name"
          maxlength="20"
          placeholder="이름을 입력하세요"
          class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 outline-hidden"
          @blur="touched = true"
        />
        <span class="text-caption text-muted mt-1.5 block">그린포켓 계좌의 예금주가 돼요</span>
      </label>

      <label class="block">
        <span class="text-body-strong text-muted mb-2 block">아이디</span>
        <input
          v-model="loginId"
          type="text"
          autocomplete="username"
          maxlength="20"
          placeholder="4자 이상, 영문·숫자"
          class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 outline-hidden"
          @blur="touched = true"
        />
      </label>

      <label class="block">
        <span class="text-body-strong text-muted mb-2 block">비밀번호</span>
        <input
          v-model="password"
          type="password"
          autocomplete="new-password"
          maxlength="32"
          placeholder="8자 이상"
          class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 outline-hidden"
          @blur="touched = true"
        />
      </label>

      <label class="block">
        <span class="text-body-strong text-muted mb-2 block">비밀번호 확인</span>
        <input
          v-model="passwordConfirm"
          type="password"
          autocomplete="new-password"
          maxlength="32"
          placeholder="한 번 더 입력하세요"
          class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 outline-hidden"
          @blur="touched = true"
        />
      </label>
    </div>

    <p v-if="showValidation && validationMessage" class="text-body-sm text-negative m-0">
      {{ validationMessage }}
    </p>
    <p v-else-if="errorMessage" class="text-body-sm text-negative m-0">{{ errorMessage }}</p>

    <div>
      <GpButton :disabled="!canSubmit" @click="submit">
        {{ loading ? '가입하는 중...' : '가입하고 시작하기' }}
      </GpButton>

      <p class="text-caption text-muted bg-surface-sub mt-3 mb-0 flex gap-2 rounded-md p-3">
        <IconInfo :size="16" class="text-icon-off mt-0.5 shrink-0" aria-hidden="true" />
        <span>
          발표용 데모라 비밀번호는 서버로 보내지 않고 저장하지도 않아요. 같은 기기에서는 아이디만
          맞으면 다시 들어올 수 있어요.
        </span>
      </p>
    </div>
  </form>
</template>
