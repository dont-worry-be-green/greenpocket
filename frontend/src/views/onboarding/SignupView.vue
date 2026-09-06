<script setup>
/*
 * ONB-01b 회원가입 (이슈 #121)
 *
 * ── 단계 세 개 ──────────────────────────────────────────────────────────
 *   PHONE    이름 없이 번호·통신사·동의 → 「인증번호 받기」
 *   CODE     인증번호 6자리 → 「확인」 (문자인증)
 *   ACCOUNT  이름 · 아이디 · 비밀번호 → 「가입하고 시작하기」
 *
 * **본인확인을 먼저 받고 계정을 만든다.** 카본페이(탄소중립포인트 공식 앱)도 휴대폰 본인인증
 * 한 번으로 3개 분야 계정을 통합 가입한다 — 같은 순서다. 그리고 셋을 한 누름으로 묶지 않는다:
 * 번호만 적고 인증이 끝나면 그건 본인인증이 아니라 번호 수집이고, 인증과 가입을 붙이면
 * 사용자가 무엇에 동의해 무엇이 일어났는지 구분할 수 없다.
 *
 * ⚠️ **실제 문자를 보내지 않고 비밀번호도 저장하지 않는다.** 사정은 `api/auth.js` 주석에 있다.
 * 각 화면이 캡션으로 그 사실을 밝힌다.
 *
 * 가입이 성공하면 `POST /users` 가 데모 키를 서버에 등록해 나머지 API 가 열리고, 곧바로
 * ONB-02 프로필로 이어진다(`stores/auth.js`).
 *
 * 세 단계는 라우트가 아니다 — 인증 도중에 히스토리가 늘면 뒤로가기를 세 번 눌러야 랜딩을
 * 벗어난다. 대신 CODE·ACCOUNT 단계에서 앞 단계로 돌아갈 길을 각 화면이 준다.
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import OnbAccountForm from '@/components/onboarding/OnbAccountForm.vue'
import OnbPhoneForm from '@/components/onboarding/OnbPhoneForm.vue'
import OnbProgress from '@/components/onboarding/OnbProgress.vue'
import OnbSmsCodeForm from '@/components/onboarding/OnbSmsCodeForm.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const store = useAuthStore()

const step = ref('PHONE')

/** 입력한 번호. 「어디로 보냈는지」에만 쓴다 — 서버로 가지 않는다 */
const phone = ref('')

/** 틀린 인증번호 안내. 서버 에러가 아니라 화면이 만든 문구다(`api/auth.js` 주석) */
const codeError = ref('')

const errorMessage = computed(() => store.error?.message ?? '')

/** 온보딩 진행 표시. 회원가입 → 프로필 두 칸 중 첫 칸이다 */
const PROGRESS_TOTAL = 2

/** 인증번호 발송 요청. 여기서 인증이 끝나지 않는다 */
async function requestCode({ phone: inputPhone }) {
  const sent = await store.requestSmsCode()
  if (!sent) return
  phone.value = inputPhone ?? ''
  codeError.value = ''
  step.value = 'CODE'
}

/** 재전송. 만료 시간이 새로 내려와 타이머가 되살아난다 */
async function resendCode() {
  codeError.value = ''
  await store.requestSmsCode()
}

/**
 * 인증번호 확인. **틀린 번호는 에러 응답이 아니라 `verified: false` 다** — 그래서
 * `store.error` 가 아니라 이 화면의 문구로 알린다.
 */
async function verifyCode(code) {
  const result = await store.verifyCode(code)
  if (!result) return
  if (!result.verified) {
    codeError.value = '인증번호가 맞지 않아요. 다시 확인해 주세요.'
    return
  }
  codeError.value = ''
  step.value = 'ACCOUNT'
}

/** 가입. 성공하면 로그인 상태가 되고 프로필(ONB-02)로 이어진다 */
async function signup({ name, loginId }) {
  const created = await store.signup({ name, loginId })
  if (created) router.replace('/onboarding/profile')
}
</script>

<template>
  <AppSubLayout back="/onboarding/start">
    <OnbProgress :step="1" :total="PROGRESS_TOTAL" />

    <div class="pt-5">
      <OnbPhoneForm
        v-if="step === 'PHONE'"
        :loading="store.isLoading"
        :error-message="errorMessage"
        @request="requestCode"
      />

      <OnbSmsCodeForm
        v-else-if="step === 'CODE'"
        :phone="phone"
        :expires-in-seconds="store.smsExpiresInSeconds"
        :demo-code="store.smsDemoCode"
        :loading="store.isLoading"
        :error-message="codeError || errorMessage"
        @verify="verifyCode"
        @resend="resendCode"
        @edit="step = 'PHONE'"
      />

      <OnbAccountForm
        v-else
        :loading="store.isLoading"
        :error-message="errorMessage"
        @submit="signup"
      />
    </div>
  </AppSubLayout>
</template>
