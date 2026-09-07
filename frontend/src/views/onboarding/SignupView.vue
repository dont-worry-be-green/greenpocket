<script setup>
/*
 * ONB-01b 회원가입 (이슈 #121) — **한 화면이다.**
 *
 * ── 위에서 아래로 ───────────────────────────────────────────────────────
 *   본인확인  이름 · 통신사 · 번호 · 동의 → 「인증번호 받기」 → 6자리 → 「확인」
 *   계정      아이디 · 비밀번호 → 「가입하고 시작하기」
 *
 * 예전에는 이 셋이 한 화면 안의 **세 단계**였다. 합치면서 바뀐 것은 **화면 수이지 누름 수가
 * 아니다** — 세 버튼은 그대로 남는다. 번호만 적으면 인증이 끝나거나 가입 한 번에 인증까지
 * 묶이면 그건 본인인증이 아니라 번호 수집이고, 사용자가 무엇에 동의해 무엇이 일어났는지
 * 구분할 수 없다. **그러니 이 화면의 버튼을 더 줄이지 않는다.**
 *
 * 본인확인이 계정보다 위다. 인증에서 받아오는 값이 없어 기술적으로는 순서가 자유롭지만,
 * 이 서비스가 본인확인을 받는 명분이 「본인 명의로 등록된 제도 실적을 불러온다」라
 * 아이디·비밀번호 밑에 묻히면 부가 절차로 보인다(카본페이·공공/금융권과 같은 순서).
 *
 * ── 잠기는 것은 CTA 하나다 ──────────────────────────────────────────────
 * 계정 입력칸은 인증 전에도 보인다. 무엇을 적어야 하는지 처음에 다 보이는 편이 낫고,
 * 인증을 마쳐야 한다는 사실은 **잠긴 CTA 와 그 아래 안내**가 말한다.
 *
 * ⚠️ 실제 문자를 보내지 않고 비밀번호도 저장하지 않는다. 사정은 `api/auth.js` 주석에 있고,
 * 화면 하단 캡션이 그대로 밝힌다.
 *
 * 가입이 성공하면 `POST /users` 가 데모 키를 서버에 등록해 나머지 API 가 열리고, 곧바로
 * ONB-02 프로필로 이어진다(`stores/auth.js`).
 */
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import OnbAccountFields from '@/components/onboarding/OnbAccountFields.vue'
import OnbProgress from '@/components/onboarding/OnbProgress.vue'
import OnbVerifyFields from '@/components/onboarding/OnbVerifyFields.vue'
import GpButton from '@/components/ui/GpButton.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const store = useAuthStore()

const name = ref('')
const loginId = ref('')
const password = ref('')
const passwordConfirm = ref('')

/*
 * 오류를 보여줄 때가 됐나. **두 자리로 나눠 든다** — 한 화면이 되면서 이름칸에서 포커스가
 * 빠지는 것만으로 아직 손대지도 않은 아이디 칸의 오류가 뜨는 길이 생겼다.
 * 이름 오류는 이름칸 아래, 계정 오류는 CTA 위다.
 */
const nameTouched = ref(false)
const idTouched = ref(false)
const touched = ref(false)

/** 인증번호를 보냈나 · 인증이 끝났나. 단계가 아니라 이 두 플래그가 화면을 정한다 */
const smsSent = ref(false)
const verified = ref(false)

/** 틀린 인증번호 안내. 서버 에러가 아니라 화면이 만든 문구다(`api/auth.js` 주석) */
const codeError = ref('')

/**
 * 아이디 중복확인 결과. `''` 미확인 · `AVAILABLE` · `TAKEN`
 *
 * **아이디를 고치면 풀린다** — 확인한 아이디와 제출되는 아이디가 어긋나면 안 되는 건
 * 휴대폰 번호와 같다. 다만 번호처럼 **잠그지는 않는다**: 문자인증은 다시 받는 비용이 있지만
 * 중복확인은 버튼 한 번이라, 고쳐 쓰다가 다시 누르는 쪽이 자연스럽다.
 */
const idStatus = ref('')

/** 온보딩 진행 표시. 회원가입 → 프로필 두 칸 중 첫 칸이다 */
const PROGRESS_TOTAL = 2

const trimmedName = computed(() => name.value.trim())
const trimmedId = computed(() => loginId.value.trim())

/** 이름 규칙은 `api-spec.md` 4.1 그대로 — trim 후 1~20자, 글자나 숫자 하나 이상 */
const nameValid = computed(
  () =>
    trimmedName.value.length > 0 &&
    trimmedName.value.length <= 20 &&
    /[\p{L}\p{N}]/u.test(trimmedName.value),
)

/** 이름 오류. 이름칸 바로 아래에 뜬다 — 「인증번호 받기」가 왜 잠겼는지 알려 주는 자리다 */
const nameMessage = computed(() => {
  if (!trimmedName.value) return '이름을 입력해 주세요.'
  if (trimmedName.value.length > 20) return '이름은 20자까지 입력할 수 있어요.'
  if (!nameValid.value) return '이름에 글자나 숫자를 하나 이상 넣어 주세요.'
  return ''
})

/*
 * 아이디 형식. **중복확인을 눌러 볼 수 있는 조건이기도 하다** — 형식도 안 맞는 값을
 * 서버에 물어보면 「이미 쓰는 아이디」와 「쓸 수 없는 아이디」가 한 문구로 섞인다.
 */
const idFormatMessage = computed(() => {
  if (trimmedId.value.length < 4) return '아이디는 4자 이상 입력해 주세요.'
  if (!/^[a-zA-Z0-9._-]+$/.test(trimmedId.value))
    return '아이디는 영문·숫자와 . _ - 만 쓸 수 있어요.'
  return ''
})

/*
 * 비밀번호 오류. 첫 번째로 걸리는 것만 보여준다 — 두 줄을 한꺼번에 띄우면 무엇부터 고칠지 알 수 없다.
 *
 * **이름도 아이디도 여기 없다.** 둘은 각자의 칸 아래에서 말한다. 한 자리에 모아 두면
 * 앞 칸에서 포커스가 빠지는 것만으로 **아직 손대지 않은 뒤 칸의 오류가 뜬다** — 이름에서 한 번,
 * 중복확인 버튼에서 또 한 번 그랬다(버튼을 누르면 아이디 칸이 blur 된다).
 * 인증을 마쳤다면 이름은 이미 통과한 값이고(`nameValid` 없이는 인증번호를 못 받는다),
 * 아이디도 마찬가지다(형식이 맞아야 중복확인을 누를 수 있다).
 */
const validationMessage = computed(() => {
  if (password.value.length < 8) return '비밀번호는 8자 이상 입력해 주세요.'
  if (password.value !== passwordConfirm.value) return '비밀번호가 서로 달라요.'
  return ''
})

const canCheckId = computed(() => !idFormatMessage.value && !store.isCheckingLoginId)

/** 아이디 오류. 아이디칸 바로 아래 — 「중복확인」이 왜 잠겼는지 알려 주는 자리다 */
const idMessage = computed(() => (idTouched.value ? idFormatMessage.value : ''))

// 아이디가 바뀌면 앞서 확인한 결과는 그 아이디의 것이 아니다
watch(trimmedId, () => {
  idStatus.value = ''
})

const canSubmit = computed(
  () =>
    verified.value &&
    idStatus.value === 'AVAILABLE' &&
    !nameMessage.value &&
    !validationMessage.value &&
    !store.isSigningUp,
)

/*
 * CTA 가 왜 잠겼는지 상시로 말한다. 비활성 버튼은 클릭 이벤트를 주지 않아 `touched` 가
 * 켜지지 않고, 그러면 검증 문구를 띄울 기회 자체가 없다.
 * `TAKEN` 은 여기서 말하지 않는다 — 아이디 칸 바로 아래가 이미 말하고 있다.
 */
const ctaHint = computed(() => {
  if (!verified.value) return '휴대폰 본인확인을 마치면 가입할 수 있어요'
  if (idStatus.value === '' && !idFormatMessage.value) return '아이디 중복확인을 눌러 주세요'
  return ''
})

/*
 * CTA 가 비활성이면 클릭 이벤트가 아예 오지 않아 `touched` 가 켜지지 않는다. 그래서 무엇이
 * 잘못됐는지 **보여줄 기회가 없다.** 입력칸에서 포커스가 빠지거나 비밀번호 확인을 적기
 * 시작하면 그때부터 알려준다 — 불일치는 사용자가 화면만 보고는 알 수 없는 유일한 항목이다.
 * (본인확인 미완료는 문구가 아니라 CTA 아래 안내가 상시로 말한다)
 */
const showValidation = computed(() => touched.value || Boolean(passwordConfirm.value))

const errorMessage = computed(() => store.error?.message ?? '')

// ── 본인확인 ──────────────────────────────────────────────────────────

/** 인증번호 발송 요청. **여기서 인증이 끝나지 않는다** */
async function requestCode() {
  const sent = await store.requestSmsCode()
  if (!sent) return
  codeError.value = ''
  smsSent.value = true
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
  verified.value = true
}

/**
 * 번호 변경·재입력. **인증을 함께 푼다** — 한 화면이 되면서 인증한 번호와 제출되는 번호가
 * 어긋날 수 있는 길이 새로 생겼고, 여기가 그 길을 막는 자리다.
 */
function resetVerification() {
  smsSent.value = false
  verified.value = false
  codeError.value = ''
}

// ── 아이디 ────────────────────────────────────────────────────────────

/** 중복확인. **「이미 쓴다」는 에러 응답이 아니라 `available: false` 다** */
async function checkId() {
  const result = await store.checkLoginId(trimmedId.value)
  if (!result) return
  idStatus.value = result.available ? 'AVAILABLE' : 'TAKEN'
}

// ── 가입 ──────────────────────────────────────────────────────────────

/** 성공하면 로그인 상태가 되고 프로필(ONB-02)로 이어진다 */
async function submit() {
  touched.value = true
  if (!canSubmit.value) return

  const created = await store.signup({ name: trimmedName.value, loginId: trimmedId.value })
  if (created) router.replace('/onboarding/profile')
}
</script>

<template>
  <!-- 제목은 헤더가 든다. 화면이 길어 본문 위 큰 제목까지 두면 인증 블록이 접힌 아래로 밀린다 -->
  <AppSubLayout title="회원가입" back="/onboarding/start">
    <OnbProgress :step="1" :total="PROGRESS_TOTAL" />

    <form class="space-y-6 pt-5" @submit.prevent="submit">
      <OnbVerifyFields
        v-model:name="name"
        :name-valid="nameValid"
        :sent="smsSent"
        :verified="verified"
        :expires-in-seconds="store.smsExpiresInSeconds"
        :sending="store.isSendingSms"
        :verifying="store.isVerifyingCode"
        :error-message="codeError"
        :name-error="nameTouched ? nameMessage : ''"
        @request="requestCode"
        @verify="verifyCode"
        @resend="resendCode"
        @reset="resetVerification"
        @name-blur="nameTouched = true"
      />

      <hr class="border-border m-0 border-0 border-t" />

      <OnbAccountFields
        v-model:login-id="loginId"
        v-model:password="password"
        v-model:password-confirm="passwordConfirm"
        :id-status="idStatus"
        :id-error="idMessage"
        :can-check-id="canCheckId"
        :checking="store.isCheckingLoginId"
        @blur="touched = true"
        @id-blur="idTouched = true"
        @check-id="checkId"
      />

      <div>
        <p v-if="showValidation && validationMessage" class="text-body-sm text-negative mt-0 mb-3">
          {{ validationMessage }}
        </p>
        <p v-else-if="errorMessage" class="text-body-sm text-negative mt-0 mb-3">
          {{ errorMessage }}
        </p>

        <GpButton :disabled="!canSubmit" @click="submit">
          {{ store.isSigningUp ? '가입하는 중...' : '가입하고 시작하기' }}
        </GpButton>

        <p v-if="ctaHint" class="text-body-sm text-muted mt-2 mb-0 text-center">{{ ctaHint }}</p>
      </div>
    </form>
  </AppSubLayout>
</template>
