<script setup>
/*
 * ONB-01b 회원가입 ① 휴대폰 본인인증 — 번호 입력 (이슈 #121)
 *
 * ── 여기서 받은 값이 서버로 가지 않는다 ─────────────────────────────────
 * 실제 문자를 보내지 않는다. 외부 SMS 는 MVP 제외 범위이고 `app_user` 에 전화번호 컬럼도
 * 없다(`api/auth.js` 주석). 화면 캡션에도 모의라고 그대로 밝힌다 — 발표에서 실제로 인증한
 * 것처럼 보이면 안 된다.
 *
 * ⚠️ **동의 없이는 CTA 를 열지 않는다**(핵심 비즈니스 규칙 4). 본인확인에 동의를 받는 자리다.
 *
 * ── 여기서 인증도, 가입도 끝나지 않는다 ─────────────────────────────────
 * 이 화면은 **인증번호 발송 요청까지**다. 확인은 `OnbSmsCodeForm`, 계정 만들기는 그다음
 * 단계다 — 한 번의 누름에 여러 개를 묶으면 사용자가 무엇에 동의해 무엇이 일어났는지
 * 구분할 수 없다.
 *
 * 원래 What-if 연동 앞(WF-01a)에 있던 화면이다. 서비스에 로그인이 들어오면서 본인확인의
 * 제자리가 회원가입이 되어 옮겼다(이슈 #121).
 */
import { computed, ref } from 'vue'

import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import IconInfo from '@/components/ui/icons/IconInfo.vue'

const props = defineProps({
  loading: { type: Boolean, default: false },
  errorMessage: { type: String, default: '' },
})
const emit = defineEmits(['request'])

/* 통신사는 응답 필드가 아니라 화면 선택지다. ENUM 을 만들지 않는다 */
const CARRIERS = ['SKT', 'KT', 'LG U+', '알뜰폰']

const carrier = ref(null)
const phone = ref('')
const agreed = ref(false)

// 숫자만 남긴다. 010 포함 10~11자리
const phoneDigits = computed(() => phone.value.replace(/\D/g, ''))
const canSubmit = computed(
  () =>
    Boolean(carrier.value) &&
    phoneDigits.value.length >= 10 &&
    phoneDigits.value.length <= 11 &&
    agreed.value &&
    !props.loading,
)
</script>

<template>
  <div class="space-y-5">
    <div>
      <h1 class="text-title tracking-display text-ink m-0">본인확인을 해주세요</h1>
      <p class="text-body text-muted mt-2 mb-0">
        내 명의로 등록된 제도 실적을 불러오는 데 써요
      </p>
    </div>

    <GpCard title="본인확인이 필요한 이유">
      <ul class="text-body-sm text-ink-soft m-0 list-none space-y-2 p-0">
        <li>에코마일리지 · 녹색생활실천은 <strong class="font-semibold">본인 명의</strong>로 참여해요</li>
        <li>다른 기기에서도 같은 계정으로 이어 볼 수 있어요</li>
        <li>그린포켓 계좌의 예금주가 돼요</li>
      </ul>
    </GpCard>

    <div class="space-y-2">
      <div>
        <span class="text-body-strong text-muted mb-2 block" id="carrier-label">통신사</span>
        <div class="flex flex-wrap gap-2" role="radiogroup" aria-labelledby="carrier-label">
          <button
            v-for="item in CARRIERS"
            :key="item"
            type="button"
            role="radio"
            :aria-checked="item === carrier"
            class="ease-standard text-body-strong min-h-11 cursor-pointer rounded-full border px-3.5 transition-colors duration-140"
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

      <label class="block">
        <span class="text-body-strong text-muted mb-2 block">휴대폰 번호</span>
        <input
          v-model="phone"
          type="tel"
          inputmode="numeric"
          autocomplete="tel"
          maxlength="13"
          placeholder="01012345678"
          class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 tabular-nums outline-hidden"
        />
      </label>
    </div>

    <!-- 동의 없이는 조회하지 않는다(핵심 비즈니스 규칙 4) -->
    <label class="bg-surface border-border flex min-h-14 cursor-pointer items-center gap-3 rounded-lg border px-4">
      <input
        v-model="agreed"
        type="checkbox"
        class="accent-primary size-(--gp-checkbox) shrink-0 cursor-pointer"
      />
      <span class="text-body text-ink-soft">본인확인과 제도 실적 조회에 동의해요</span>
    </label>

    <p v-if="errorMessage" class="text-body-sm text-negative m-0">{{ errorMessage }}</p>

    <div>
      <GpButton
        :disabled="!canSubmit"
        @click="emit('request', { phone: phoneDigits })"
      >
        {{ loading ? '보내는 중...' : '인증번호 받기' }}
      </GpButton>

      <p class="text-caption text-muted bg-surface-sub mt-3 mb-0 flex gap-2 rounded-md p-3">
        <IconInfo :size="16" class="text-icon-off mt-0.5 shrink-0" aria-hidden="true" />
        <span>발표용 데모라 실제 문자는 보내지 않아요. 입력한 값은 서버로 보내지 않아요.</span>
      </p>
    </div>
  </div>
</template>
