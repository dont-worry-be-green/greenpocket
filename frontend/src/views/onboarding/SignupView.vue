<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import OnbAccountFields from '@/components/onboarding/OnbAccountFields.vue'
import OnbVerifyFields from '@/components/onboarding/OnbVerifyFields.vue'
import GpButton from '@/components/ui/GpButton.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const store = useAuthStore()

const name = ref('')
const birthDate = ref('')
const gender = ref('')
const phoneNumber = ref('')
const email = ref('')
const password = ref('')
const passwordConfirm = ref('')
const nameTouched = ref(false)
const emailTouched = ref(false)
const touched = ref(false)
const smsSent = ref(false)
const verified = ref(false)
const codeError = ref('')

const trimmedName = computed(() => name.value.trim())
const trimmedEmail = computed(() => email.value.trim())
const nameValid = computed(
  () =>
    trimmedName.value.length > 0 &&
    trimmedName.value.length <= 20 &&
    /[\p{L}\p{N}]/u.test(trimmedName.value),
)
const nameMessage = computed(() => {
  if (!trimmedName.value) return '이름을 입력해 주세요.'
  if (trimmedName.value.length > 20) return '이름은 20자까지 입력할 수 있어요.'
  if (!nameValid.value) return '이름에 글자나 숫자를 하나 이상 넣어 주세요.'
  return ''
})
const today = (() => {
  const now = new Date()
  const offset = now.getTimezoneOffset() * 60_000
  return new Date(now.getTime() - offset).toISOString().slice(0, 10)
})()
const birthDateMessage = computed(() => {
  if (!birthDate.value) return '생년월일을 입력해 주세요.'
  if (birthDate.value > today) return '생년월일은 오늘보다 이후일 수 없어요.'
  return ''
})
const genderMessage = computed(() => (gender.value ? '' : '성별을 선택해 주세요.'))
const emailMessage = computed(() => {
  if (!trimmedEmail.value) return '이메일을 입력해 주세요.'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail.value)) return '이메일 형식을 확인해 주세요.'
  return ''
})
const passwordMessage = computed(() => {
  if (password.value.length < 8) return '비밀번호는 8자 이상 입력해 주세요.'
  if (new TextEncoder().encode(password.value).length > 72)
    return '비밀번호는 영문 기준 72자까지 입력할 수 있어요.'
  if (password.value !== passwordConfirm.value) return '비밀번호가 서로 달라요.'
  return ''
})
const showValidation = computed(() => touched.value || Boolean(passwordConfirm.value))
const canSubmit = computed(
  () =>
    verified.value &&
    !nameMessage.value &&
    !birthDateMessage.value &&
    !genderMessage.value &&
    Boolean(phoneNumber.value) &&
    !emailMessage.value &&
    !passwordMessage.value &&
    !store.isSigningUp,
)
const ctaHint = computed(() => (verified.value ? '' : '휴대폰 본인확인을 마치면 가입할 수 있어요'))

async function requestCode() {
  const sent = await store.requestSmsCode()
  if (!sent) return
  codeError.value = ''
  smsSent.value = true
}

async function resendCode() {
  codeError.value = ''
  await store.requestSmsCode()
}

async function verifyCode(code) {
  const result = await store.verifyCode(code)
  if (!result) return
  if (!result.verified) {
    codeError.value = '인증번호가 맞지 않아요. 다시 확인해 주세요.'
    return
  }
  codeError.value = ''
  verified.value = true
}

function resetVerification() {
  smsSent.value = false
  verified.value = false
  codeError.value = ''
}

async function submit() {
  touched.value = true
  emailTouched.value = true
  if (!canSubmit.value) return

  const created = await store.signup({
    name: trimmedName.value,
    birthDate: birthDate.value,
    gender: gender.value,
    phoneNumber: phoneNumber.value,
    email: trimmedEmail.value,
    password: password.value,
  })
  if (created) router.replace('/whatif')
}
</script>

<template>
  <AppSubLayout title="회원가입" back="/onboarding/start">
    <form class="space-y-4 pt-2" @submit.prevent="submit">
      <OnbVerifyFields
        v-model:name="name"
        v-model:birth-date="birthDate"
        v-model:gender="gender"
        v-model:phone-number="phoneNumber"
        :name-valid="nameValid"
        :birth-date-valid="!birthDateMessage"
        :max-birth-date="today"
        :sent="smsSent"
        :verified="verified"
        :expires-in-seconds="store.smsExpiresInSeconds"
        :sending="store.isSendingSms"
        :verifying="store.isVerifyingCode"
        :error-message="codeError"
        :name-error="nameTouched ? nameMessage : ''"
        :birth-date-error="touched ? birthDateMessage : ''"
        :gender-error="touched ? genderMessage : ''"
        @request="requestCode"
        @verify="verifyCode"
        @resend="resendCode"
        @reset="resetVerification"
        @name-blur="nameTouched = true"
      />

      <hr class="border-border m-0 border-0 border-t" />

      <OnbAccountFields
        v-model:email="email"
        v-model:password="password"
        v-model:password-confirm="passwordConfirm"
        :email-error="emailTouched ? emailMessage : ''"
        @blur="touched = true"
        @email-blur="emailTouched = true"
      />

      <div>
        <p v-if="showValidation && passwordMessage" class="text-body-sm text-negative mt-0 mb-3">
          {{ passwordMessage }}
        </p>
        <p v-else-if="store.error" class="text-body-sm text-negative mt-0 mb-3">
          {{ store.error.message }}
        </p>

        <GpButton :disabled="!canSubmit" @click="submit">
          {{ store.isSigningUp ? '가입하는 중...' : '가입하고 시작하기' }}
        </GpButton>
        <p v-if="ctaHint" class="text-body-sm text-muted mt-2 mb-0 text-center">{{ ctaHint }}</p>
      </div>
    </form>
  </AppSubLayout>
</template>
