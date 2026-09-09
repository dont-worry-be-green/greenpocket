<script setup>
/*
 * ONB-01b 회원가입 ─ 휴대폰 본인확인 블록 (이슈 #121)
 *
 * ── 왜 바텀시트인가 ────────────────────────────────────────────────────
 * 가입 폼에는 이름·생년월일·성별·이메일·비밀번호가 모두 필요하다. 여기에 통신사·휴대폰 번호·
 * 인증번호까지 펼치면 핵심 입력을 훑기 어렵다. 기본 화면에는 인증 진입 행만 두고, 실제 인증
 * 단계는 공용 `GpModal`의 bottom 정렬로 분리한다. 누름 수와 검증 절차는 기존과 같다.
 *
 * 본인확인이 계정 입력보다 **위**에 있다. 이 서비스가 본인확인을 받는 명분이
 * 「본인 명의로 등록된 제도 실적을 불러온다」라, 이메일·비밀번호 밑에 묻히면 부가 절차로 보인다.
 * (카본페이·공공/금융권이 같은 순서다)
 *
 * ── 인증이 끝나면 **완료 카드만 남긴다** ────────────────────────────────
 * 통신사·번호·인증번호는 시트와 함께 닫고 완료 카드로 바꾼다. 이름·생년월일·성별은 인증된
 * 정보이므로 함께 잠근다. 「정보 변경」은 인증을 풀고 시트를 다시 열어 인증값과 가입 요청값이
 * 어긋나는 길을 막는다.
 *
 * ⚠️ **실제 문자를 보내지 않고 입력값도 서버로 가지 않는다.** 사정은 `api/auth.js` 주석에 있다.
 * 한동안 캡션이 데모 번호를 알려 줬는데 화면이 길어 뺐다 — placeholder 가 `000000` 이다.
 * **틀린 번호는 실제로 거부한다.** 아무 여섯 자리나 통과하면 검증이 없는 것과 같다.
 *
 * ── 타이머는 여기 있다 ──────────────────────────────────────────────────
 * 남은 시간은 화면에서만 의미가 있는 값이라 스토어에 두지 않는다. 스토어가 타이머를 들면
 * 테스트에서 시간을 흘려야 해 검증이 어려워진다(`GoalSettingView` 의 디바운스와 같은 이유).
 * 스토어는 만료 **시각**만 준다. 발송할 때마다 값이 달라져야 재전송·번호 변경 뒤에도
 * 타이머가 다시 돈다(초 단위 180 은 두 번째부터 같은 값이라 watch 가 깨지 않았다).
 */
import { computed, onUnmounted, ref, watch } from 'vue'

import GpButton from '@/components/ui/GpButton.vue'
import GpModal from '@/components/ui/GpModal.vue'
import IconCalendar from '@/components/ui/icons/IconCalendar.vue'
import IconChevronRight from '@/components/ui/icons/IconChevronRight.vue'
import IconSealCheck from '@/components/ui/icons/IconSealCheck.vue'
import IconShieldCheck from '@/components/ui/icons/IconShieldCheck.vue'
import IconUser from '@/components/ui/icons/IconUser.vue'

/** 이름은 가입에도 쓰는 값이라 화면이 갖는다. 본인확인의 대상이기도 해서 여기 있다 */
const name = defineModel('name', { type: String, default: '' })
const birthDate = defineModel('birthDate', { type: String, default: '' })
const gender = defineModel('gender', { type: String, default: '' })
const phoneNumber = defineModel('phoneNumber', { type: String, default: '' })

const props = defineProps({
  /** 이름이 `api-spec.md` 4.1 규칙을 통과했나. 판정과 문구는 화면이 갖는다 */
  nameValid: { type: Boolean, default: false },
  birthDateValid: { type: Boolean, default: false },
  maxBirthDate: { type: String, required: true },
  /** 인증번호를 보냈나. `false` 로 돌아오면 입력을 비운다(번호 변경·재입력) */
  sent: { type: Boolean, default: false },
  verified: { type: Boolean, default: false },
  /** 서버(모의)가 준 만료 시각(ms). 발송·재전송마다 새 값이 와서 타이머가 되살아난다 */
  expiresAt: { type: Number, default: 0 },
  /*
   * 로딩은 **버튼별로 받는다.** 하나로 두면 「확인」을 눌렀는데 「인증번호 받기」까지
   * 「보내는 중」이 된다(`stores/auth.js` 의 `pending`).
   */
  sending: { type: Boolean, default: false },
  verifying: { type: Boolean, default: false },
  /** 틀린 인증번호 안내. 에러 응답이 아니라 화면이 만든 문구다 */
  errorMessage: { type: String, default: '' },
  /** 이름 오류. 판정과 문구는 화면이 갖고, 여기는 이름칸 아래에 놓기만 한다 */
  nameError: { type: String, default: '' },
  birthDateError: { type: String, default: '' },
  genderError: { type: String, default: '' },
  /** 가입 요청이 번호 때문에 거절됐을 때(409 `phoneNumber`) 인증 진입 행 아래에 놓는 서버 문구 */
  phoneError: { type: String, default: '' },
})
const emit = defineEmits(['request', 'verify', 'resend', 'reset', 'name-blur'])

/* 통신사는 응답 필드가 아니라 화면 선택지다. ENUM 을 만들지 않는다 */
const CARRIERS = ['SKT', 'KT', 'LG U+', '알뜰폰']
const GENDERS = [
  { value: 'MALE', label: '남성' },
  { value: 'FEMALE', label: '여성' },
]
const CODE_LENGTH = 6

const carrier = ref(null)
const code = ref('')
const remaining = ref(0)
const sheetOpen = ref(false)

// 숫자만 남긴다. 붙여넣기로 하이픈이 섞여 들어오는 것을 막는다
const phoneInput = computed({
  get: () => phoneNumber.value,
  set: (value) => {
    phoneNumber.value = String(value ?? '')
      .replace(/\D/g, '')
      .slice(0, 11)
  },
})
const phoneDigits = computed(() => phoneNumber.value.replace(/\D/g, ''))
const phoneValid = computed(() => /^01[016789][0-9]{7,8}$/.test(phoneDigits.value))
const codeDigits = computed(() => code.value.replace(/\D/g, '').slice(0, CODE_LENGTH))
const expired = computed(() => remaining.value <= 0)
const identityComplete = computed(
  () => props.nameValid && props.birthDateValid && Boolean(gender.value),
)

const canRequest = computed(
  () => identityComplete.value && Boolean(carrier.value) && phoneValid.value && !props.sending,
)
const canVerify = computed(
  () => codeDigits.value.length === CODE_LENGTH && !expired.value && !props.verifying,
)

const clock = computed(() => {
  const total = Math.max(0, remaining.value)
  const minutes = String(Math.floor(total / 60)).padStart(2, '0')
  const seconds = String(total % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
})

/** 010-1234-5678. 인증이 끝난 번호를 확인만 하는 자리라 가리지 않는다 */
const phoneLabel = computed(() => {
  const raw = phoneDigits.value
  if (raw.length < 10) return phoneNumber.value
  return `${raw.slice(0, 3)}-${raw.slice(3, raw.length - 4)}-${raw.slice(-4)}`
})

const identityLabel = computed(() => {
  const genderLabel = GENDERS.find((item) => item.value === gender.value)?.label ?? ''
  return [name.value, birthDate.value.replaceAll('-', '.'), genderLabel, phoneLabel.value]
    .filter(Boolean)
    .join(' · ')
})

let timer = null

function stopTimer() {
  window.clearInterval(timer)
  timer = null
}

function secondsLeft() {
  return Math.max(0, Math.ceil((props.expiresAt - Date.now()) / 1000))
}

function startTimer() {
  stopTimer()
  timer = window.setInterval(() => {
    remaining.value = secondsLeft()
    if (remaining.value <= 0) stopTimer()
  }, 1000)
}

onUnmounted(stopTimer)

// 발송·재전송하면 만료 시각이 새로 내려온다. 입력도 비우고 타이머를 다시 돌린다
watch(
  () => props.expiresAt,
  () => {
    remaining.value = secondsLeft()
    code.value = ''
    if (remaining.value > 0) startTimer()
  },
)

// 인증이 끝나면 셈할 이유가 없다. 만료 문구가 뒤늦게 뜨는 것도 막는다
watch(
  () => props.verified,
  (value) => {
    if (value) {
      stopTimer()
      sheetOpen.value = false
    }
  },
)

// 번호 변경·재입력으로 발송 상태가 풀리면 인증번호 칸을 비운다
watch(
  () => props.sent,
  (value) => {
    if (!value) {
      code.value = ''
      remaining.value = 0
      stopTimer()
    }
  },
)

/** 인증을 풀고 입력칸을 되돌린다 */
function unlock() {
  emit('reset')
}

function openSheet() {
  sheetOpen.value = true
}

function changePhoneNumber() {
  unlock()
  sheetOpen.value = true
}
</script>

<template>
  <section class="space-y-3">
    <label class="block">
      <span class="text-body-strong text-muted mb-2 block">이름</span>
      <div class="relative">
        <IconUser
          :size="22"
          class="text-muted pointer-events-none absolute top-1/2 left-4 -translate-y-1/2"
        />
        <input
          v-model="name"
          type="text"
          autocomplete="name"
          maxlength="20"
          placeholder="이름을 입력하세요"
          class="bg-surface border-border text-body placeholder:text-disabled-text disabled:bg-disabled-bg disabled:text-disabled-text min-h-14 w-full rounded-lg border pr-4 pl-12 outline-hidden"
          :disabled="sent || verified"
          @blur="emit('name-blur')"
        />
      </div>
      <span v-if="nameError" class="text-body-sm text-negative mt-1.5 block">{{ nameError }}</span>
    </label>

    <!-- 좁은 화면에서도 생년월일과 성별을 한 행으로 유지한다. -->
    <div
      class="grid grid-cols-2 items-start gap-3"
      data-testid="identity-detail-row"
    >
      <label class="min-w-0 overflow-hidden">
        <span class="text-body-strong text-muted mb-2 block">생년월일</span>
        <div class="relative">
          <IconCalendar
            :size="20"
            class="text-muted pointer-events-none absolute top-1/2 left-3 -translate-y-1/2"
          />
          <input
            v-model="birthDate"
            type="date"
            autocomplete="bday"
            :max="maxBirthDate"
            :disabled="sent || verified"
            class="bg-surface border-border text-body disabled:bg-disabled-bg disabled:text-disabled-text min-h-14 w-full min-w-0 rounded-lg border pr-2 pl-10 tabular-nums outline-hidden"
          />
        </div>
        <span v-if="birthDateError" class="text-body-sm text-negative mt-1.5 block">
          {{ birthDateError }}
        </span>
      </label>

      <div class="min-w-0 overflow-hidden">
        <span class="text-body-strong text-muted mb-2 block" id="gender-label">성별</span>
        <div class="grid grid-cols-2 gap-1.5" role="radiogroup" aria-labelledby="gender-label">
          <button
            v-for="item in GENDERS"
            :key="item.value"
            type="button"
            role="radio"
            :aria-checked="item.value === gender"
            :disabled="sent || verified"
            class="ease-standard text-body-sm min-h-14 min-w-0 cursor-pointer rounded-lg border px-1 transition-colors duration-140 disabled:cursor-not-allowed"
            :class="
              item.value === gender
                ? 'bg-primary border-primary text-on-primary'
                : 'bg-surface border-border text-ink-soft'
            "
            @click="gender = item.value"
          >
            {{ item.label }}
          </button>
        </div>
        <span v-if="genderError" class="text-body-sm text-negative mt-1.5 block">
          {{ genderError }}
        </span>
      </div>
    </div>

    <div v-if="verified" class="bg-primary-bg rounded-lg px-4 py-3.5">
      <div class="flex items-center justify-between gap-3">
        <span class="text-body-strong text-primary-on-soft flex items-center gap-2">
          <IconSealCheck :size="20" />
          <span>휴대폰 본인인증 완료</span>
        </span>
        <button
          type="button"
          class="text-body-sm text-primary-on-soft shrink-0 cursor-pointer border-0 bg-transparent p-0 underline"
          @click="changePhoneNumber"
        >
          정보 변경
        </button>
      </div>
      <p class="text-body-sm text-ink-soft mt-1.5 mb-0">{{ identityLabel }}</p>
    </div>

    <div v-else>
      <span class="text-body-strong text-muted mb-2 block">휴대폰 본인인증</span>
      <button
        type="button"
        class="bg-surface border-border ease-standard flex min-h-14 w-full cursor-pointer items-center gap-3 rounded-lg border px-4 text-left transition-colors duration-140 active:bg-primary-bg"
        aria-label="휴대폰 본인인증 시작"
        aria-haspopup="dialog"
        :aria-expanded="sheetOpen"
        @click="openSheet"
      >
        <IconShieldCheck :size="26" class="text-muted shrink-0" />
        <span class="text-body text-ink-soft min-w-0 flex-1">안전하게 본인인증하기</span>
        <span class="text-label text-negative bg-negative-bg shrink-0 rounded-full px-2.5 py-1">미인증</span>
        <IconChevronRight :size="18" class="text-muted shrink-0" />
      </button>
      <span v-if="phoneError" class="text-body-sm text-negative mt-1.5 block">{{ phoneError }}</span>
    </div>

    <GpModal :open="sheetOpen" title="휴대폰 본인인증" @close="sheetOpen = false">
      <p class="text-body text-muted mt-0 mb-5">가입을 위해 본인 명의 휴대폰을 인증해 주세요.</p>

      <div class="space-y-4">
        <div>
          <span class="text-body-strong text-muted mb-2 block" id="carrier-label">통신사</span>
          <div class="grid grid-cols-4 gap-1.5" role="radiogroup" aria-labelledby="carrier-label">
            <button
              v-for="item in CARRIERS"
              :key="item"
              type="button"
              role="radio"
              :aria-checked="item === carrier"
              :disabled="sent"
              class="ease-standard text-body-sm min-h-11 min-w-0 cursor-pointer rounded-lg border px-1 transition-colors duration-140 disabled:cursor-not-allowed"
              :class="
                item === carrier
                  ? 'bg-primary border-primary text-on-primary'
                  : 'bg-surface border-border text-ink-soft'
              "
              @click="carrier = item"
            >
              {{ item }}
            </button>
          </div>
        </div>

        <div>
          <span class="text-body-strong text-muted mb-2 block" id="phone-label">휴대폰 번호</span>
          <div class="flex gap-2">
            <input
              v-model="phoneInput"
              type="tel"
              inputmode="numeric"
              autocomplete="tel"
              maxlength="11"
              placeholder="01012345678"
              aria-labelledby="phone-label"
              :disabled="sent"
              class="bg-surface border-border text-body placeholder:text-disabled-text disabled:bg-disabled-bg disabled:text-disabled-text min-h-14 min-w-0 flex-1 rounded-lg border px-3 tabular-nums outline-hidden"
            />
            <button
              v-if="!sent"
              type="button"
              :disabled="!canRequest"
              class="ease-standard bg-primary-bg text-primary-on-soft text-body-strong disabled:bg-disabled-bg disabled:text-disabled-text min-h-14 shrink-0 cursor-pointer rounded-lg border-0 px-3 transition-colors duration-140 disabled:cursor-not-allowed"
              @click="emit('request')"
            >
              {{ sending ? '보내는 중...' : '인증번호 받기' }}
            </button>
          </div>
          <p v-if="!identityComplete" class="text-body-sm text-negative mt-2 mb-0">
            바텀시트를 닫고 이름·생년월일·성별을 먼저 입력해 주세요.
          </p>
        </div>

        <div v-if="sent">
          <span class="text-body-strong text-muted mb-2 block" id="code-label">인증번호</span>
          <div class="relative">
            <input
              v-model="code"
              type="text"
              inputmode="numeric"
              autocomplete="one-time-code"
              :maxlength="CODE_LENGTH"
              placeholder="6자리 숫자"
              aria-labelledby="code-label"
              class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 pr-18 tabular-nums outline-hidden"
            />
            <span
              class="text-body-sm absolute top-1/2 right-4 -translate-y-1/2 tabular-nums"
              :class="expired ? 'text-negative' : 'text-primary-on-soft'"
            >
              {{ expired ? '만료' : clock }}
            </span>
          </div>

          <div class="mt-2 flex items-center justify-between gap-3">
            <span class="text-body-sm text-muted">인증번호는 3분 동안 유효해요.</span>
            <div class="flex shrink-0 gap-3">
              <button
                type="button"
                class="text-body-sm text-muted cursor-pointer border-0 bg-transparent p-0 underline"
                @click="unlock"
              >
                번호 변경
              </button>
              <button
                type="button"
                class="text-body-sm text-primary-on-soft cursor-pointer border-0 bg-transparent p-0 font-semibold"
                :disabled="sending"
                @click="emit('resend')"
              >
                재전송
              </button>
            </div>
          </div>

          <p v-if="errorMessage" class="text-body-sm text-negative mt-2 mb-0">{{ errorMessage }}</p>
        </div>

        <GpButton v-if="sent" :disabled="!canVerify" @click="emit('verify', codeDigits)">
          {{ verifying ? '확인 중...' : '인증 완료' }}
        </GpButton>
      </div>
    </GpModal>
  </section>
</template>
