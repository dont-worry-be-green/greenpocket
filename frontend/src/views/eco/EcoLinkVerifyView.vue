<script setup>
/*
 * WF-01a 에코마일리지 본인확인 · 연동 (B-1-01 · B-1-02 · COM-05)
 *
 * ⚠️ **기능명세서에 없는 화면이다.** WF-01 의 「연동하기」가 `POST /eco/link` 를 곧바로 불렀는데,
 * 서버는 `X-Demo-Key` 밖에 모르면서 「작년 우리 집 사용량」을 내려준다. 신원을 잇는 단계가
 * 흐름에 통째로 빠져 있어 그 자리를 화면으로 채운다. **서버 계약은 바꾸지 않는다.**
 *
 * ── 단계 세 개 ──────────────────────────────────────────────────────────
 *   PHONE  이름·통신사·번호·동의 → 「인증번호 받기」
 *   CODE   인증번호 6자리 → 「확인」 (문자인증)
 *   LINK   「연동하기」 → `POST /eco/link` → 홈이 WF-02 를 그리고 폴링을 이어받는다
 *
 * **셋을 한 누름으로 묶지 않는다.** 번호만 적고 인증이 끝나면 그건 본인인증이 아니라 번호
 * 수집이고, 인증과 연동을 붙이면 사용자가 무엇에 동의해 무엇이 일어났는지 구분할 수 없다.
 * 인증이 먼저여야 「이 사람이 에코마일리지에 등록돼 있는가」를 물을 수 있다는 것이 이 흐름의
 * 전제이기도 하다.
 *
 * ⚠️ **실제 문자를 보내지 않는다.** 외부 SMS 는 MVP 제외 범위(「로그인·회원가입·소셜 인증」·
 * 「실제 외부 연동」)이고 `app_user` 에 전화번호 컬럼도 없다. 사정은 `api/eco.js` 주석.
 *
 * ── 미가입은 화면을 가르지 않는다 ───────────────────────────────────────
 * 가입 여부를 알려주는 서버 값이 없다. 그래서 별도 화면(옛 WF-01b) 대신 **LINK 단계 하단에
 * 누리집으로 가는 길을 상시 열어 둔다.** 가입하고 돌아오면 같은 「연동하기」를 다시 누르면 된다.
 * 서버가 아니라고 하지 않은 것을 화면이 아니라고 말하지 않는다(핵심 규칙 8).
 *
 * 세 단계는 라우트가 아니다. 뒤로가기는 어느 쪽에서든 WF-01 로 돌아가야 한다 —
 * 인증 도중에 브라우저 히스토리가 늘면 뒤로가기를 세 번 눌러야 탭을 벗어난다.
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import EcoLinkReadyPanel from '@/components/eco/EcoLinkReadyPanel.vue'
import EcoSmsCodeForm from '@/components/eco/EcoSmsCodeForm.vue'
import EcoVerifyForm from '@/components/eco/EcoVerifyForm.vue'
import AppSubLayout from '@/components/layout/AppSubLayout.vue'
import { useEcoStore } from '@/stores/eco'
import { useOnboardingStore } from '@/stores/onboarding'

const router = useRouter()
const store = useEcoStore()
const onboarding = useOnboardingStore()

const step = ref('PHONE')

/** 입력한 이름·번호. 인사와 「어디로 보냈는지」에만 쓴다 — 서버로 가지 않는다 */
const verifiedName = ref('')
const phone = ref('')

/** 틀린 인증번호 안내. 서버 에러가 아니라 화면이 만든 문구다(`api/eco.js` 주석) */
const codeError = ref('')

// 온보딩에서 받은 이름을 프리필한다. 새로고침하면 비어 있고, 그때는 직접 입력한다
const defaultName = computed(() => onboarding.user?.name ?? '')

// 누리집 주소는 GET /eco/status 가 준다(api-spec.md 8.1). 화면이 만들지 않는다
const externalUrl = computed(() => store.status?.externalUrl ?? '')

const errorMessage = computed(() => store.error?.message ?? '')

// 홈을 거치지 않고 주소창으로 들어오면 status 가 비어 누리집 주소를 모른다
onMounted(() => {
  if (!store.status) store.fetchStatus()
})

/** 인증번호 발송 요청. 여기서 인증이 끝나지 않는다 */
async function requestCode({ name, phone: inputPhone }) {
  const sent = await store.requestSmsCode()
  if (!sent) return
  verifiedName.value = name ?? ''
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
 * `store.error` 가 아니라 이 화면의 문구로 알린다(공통 에러 코드를 만들지 않는다).
 */
async function verifyCode(code) {
  const result = await store.verifySmsCode(code)
  if (!result) return
  if (!result.verified) {
    codeError.value = '인증번호가 맞지 않아요. 다시 확인해 주세요.'
    return
  }
  codeError.value = ''
  step.value = 'LINK'
}

/** 「연동하기」. 홈이 WF-02 를 그리고 폴링을 이어받는다 */
async function startLink() {
  const linkJobId = await store.startLink()
  if (!linkJobId) return
  router.replace('/whatif')
}

function openSite() {
  if (externalUrl.value) window.open(externalUrl.value, '_blank', 'noopener')
}
</script>

<template>
  <AppSubLayout back="/whatif">
    <div class="pt-2">
      <EcoVerifyForm
        v-if="step === 'PHONE'"
        :default-name="defaultName"
        :loading="store.isLoading"
        :error-message="errorMessage"
        @request="requestCode"
      />

      <EcoSmsCodeForm
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

      <EcoLinkReadyPanel
        v-else
        :name="verifiedName"
        :external-url="externalUrl"
        :loading="store.isLoading"
        :error-message="errorMessage"
        @link="startLink"
        @open-site="openSite"
      />
    </div>
  </AppSubLayout>
</template>
