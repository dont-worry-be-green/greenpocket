<script setup>
/*
 * ONB-01b 회원가입 ② 휴대폰 본인인증 — 인증번호 확인 (이슈 #121)
 *
 * ⚠️ **실제 문자를 보내지 않는다.** 사정은 `api/auth.js` 의 `requestSmsCode` 주석에 있다.
 * 데모 코드는 `demoCode` 로 내려오고 아래 캡션이 그대로 밝힌다 — 발표자가 맞힐 수 있어야 하고,
 * **틀린 번호는 실제로 거부돼야** 인증처럼 보인다. 아무 여섯 자리나 통과하면 검증이 없는 것과 같다.
 *
 * ── 타이머는 여기 있다 ──────────────────────────────────────────────────
 * 남은 시간은 화면에서만 의미가 있는 값이라 스토어에 두지 않는다. 스토어가 타이머를 들면
 * 테스트에서 시간을 흘려야 해 검증이 어려워진다(`GoalSettingView` 의 디바운스와 같은 이유).
 * 만료되면 확인을 잠그고 재전송으로 유도한다 — 만료된 번호가 통과하면 타이머가 장식이 된다.
 */
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'

import GpButton from '@/components/ui/GpButton.vue'
import IconInfo from '@/components/ui/icons/IconInfo.vue'

const props = defineProps({
  /** 입력한 번호. 보낸 곳을 보여주기만 한다 */
  phone: { type: String, default: '' },
  /** 서버(모의)가 준 만료 시간(초). 재전송하면 이 값이 다시 내려와 타이머가 되살아난다 */
  expiresInSeconds: { type: Number, default: 0 },
  /** 모의 응답에만 있는 데모 코드. 실제 엔드포인트가 생기면 빈 값이 되어 캡션이 사라진다 */
  demoCode: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  /** 틀린 인증번호 안내. 에러 응답이 아니라 화면이 만든 문구다 */
  errorMessage: { type: String, default: '' },
})
const emit = defineEmits(['verify', 'resend', 'edit'])

const CODE_LENGTH = 6

const code = ref('')
const remaining = ref(props.expiresInSeconds)

// 숫자만 남긴다. 붙여넣기로 하이픈이 섞여 들어오는 것을 막는다
const digits = computed(() => code.value.replace(/\D/g, '').slice(0, CODE_LENGTH))
const expired = computed(() => remaining.value <= 0)
const canSubmit = computed(
  () => digits.value.length === CODE_LENGTH && !expired.value && !props.loading,
)

const clock = computed(() => {
  const total = Math.max(0, remaining.value)
  const minutes = String(Math.floor(total / 60)).padStart(2, '0')
  const seconds = String(total % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
})

/** 010-1234-5678. 보낸 곳을 확인만 하는 자리라 가리지 않는다 */
const phoneLabel = computed(() => {
  const raw = props.phone.replace(/\D/g, '')
  if (raw.length < 10) return props.phone
  const head = raw.slice(0, 3)
  const tail = raw.slice(-4)
  return `${head}-${raw.slice(3, raw.length - 4)}-${tail}`
})

let timer = null

function startTimer() {
  window.clearInterval(timer)
  timer = window.setInterval(() => {
    remaining.value -= 1
    if (remaining.value <= 0) window.clearInterval(timer)
  }, 1000)
}

onMounted(startTimer)
onUnmounted(() => window.clearInterval(timer))

// 재전송하면 만료 시간이 새로 내려온다. 입력도 비우고 타이머를 다시 돌린다
watch(
  () => props.expiresInSeconds,
  (value) => {
    remaining.value = value
    code.value = ''
    if (value > 0) startTimer()
  },
)
</script>

<template>
  <div class="space-y-5">
    <div>
      <h1 class="text-title tracking-display text-ink m-0">인증번호를 입력해 주세요</h1>
      <p class="text-body text-muted mt-2 mb-0">
        <strong class="text-ink-soft font-semibold tabular-nums">{{ phoneLabel }}</strong> 로
        보냈어요
      </p>
      <button
        type="button"
        class="text-body-sm text-primary-on-soft mt-2 cursor-pointer border-0 bg-transparent p-0 underline"
        @click="emit('edit')"
      >
        번호가 틀렸어요
      </button>
    </div>

    <div>
      <label class="block">
        <span class="text-body-strong text-muted mb-2 block">인증번호 6자리</span>
        <input
          v-model="code"
          type="text"
          inputmode="numeric"
          autocomplete="one-time-code"
          :maxlength="CODE_LENGTH"
          placeholder="000000"
          class="bg-surface border-border text-amount placeholder:text-disabled-text min-h-16 w-full rounded-lg border px-4 text-center tracking-[0.4em] tabular-nums outline-hidden"
        />
      </label>

      <div class="mt-2 flex items-center justify-between gap-3">
        <span
          class="text-body-sm tabular-nums"
          :class="expired ? 'text-negative' : 'text-primary-on-soft'"
        >
          {{ expired ? '인증번호가 만료됐어요' : `${clock} 남음` }}
        </span>
        <button
          type="button"
          class="text-body-sm text-muted cursor-pointer border-0 bg-transparent p-0 underline"
          :disabled="loading"
          @click="emit('resend')"
        >
          인증번호 재전송
        </button>
      </div>
    </div>

    <p v-if="errorMessage" class="text-body-sm text-negative m-0">{{ errorMessage }}</p>

    <div>
      <GpButton :disabled="!canSubmit" @click="emit('verify', digits)">
        {{ loading ? '확인하는 중...' : '확인' }}
      </GpButton>

      <p class="text-caption text-muted bg-surface-sub mt-3 mb-0 flex gap-2 rounded-md p-3">
        <IconInfo :size="16" class="text-icon-off mt-0.5 shrink-0" aria-hidden="true" />
        <span v-if="demoCode">
          발표용 데모라 실제 문자는 보내지 않아요. 인증번호는
          <strong class="text-ink-soft font-semibold tabular-nums">{{ demoCode }}</strong> 예요.
        </span>
        <span v-else>발표용 데모라 실제 문자는 보내지 않아요.</span>
      </p>
    </div>
  </div>
</template>
