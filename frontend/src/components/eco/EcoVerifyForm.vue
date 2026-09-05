<script setup>
/*
 * WF-01a 본인확인 폼 — **기능명세서에 없는 화면이다.** 사정은 `api/eco.js` 의
 * `verifyEcoIdentity` 주석에 적어 두었다.
 *
 * ── 여기서 받은 값이 서버로 가지 않는다 ─────────────────────────────────
 * `POST /eco/link` 는 본문이 없다(api-spec.md 8.2). 계약을 바꾸지 않으려고 입력을 화면에만
 * 두었고, 화면 캡션에도 모의라고 그대로 밝힌다 — 발표에서 실제로 인증한 것처럼 보이면 안 된다.
 *
 * ⚠️ **동의 없이는 CTA 를 열지 않는다**(핵심 비즈니스 규칙 4). 조회에 동의를 받는 자리다.
 */
import { computed, ref } from 'vue'

import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import IconInfo from '@/components/ui/icons/IconInfo.vue'

const props = defineProps({
  /** 온보딩에서 받은 이름. 프리필만 하고 수정은 막지 않는다 */
  defaultName: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  errorMessage: { type: String, default: '' },
})
const emit = defineEmits(['verify', 'notMember'])

/* 통신사는 응답 필드가 아니라 화면 선택지다. ENUM 을 만들지 않는다 */
const CARRIERS = ['SKT', 'KT', 'LG U+', '알뜰폰']

const name = ref(props.defaultName)
const carrier = ref(null)
const phone = ref('')
const agreed = ref(false)

// 숫자만 남긴다. 010 포함 10~11자리
const phoneDigits = computed(() => phone.value.replace(/\D/g, ''))
const canSubmit = computed(
  () =>
    Boolean(name.value.trim() && carrier.value) &&
    phoneDigits.value.length >= 10 &&
    phoneDigits.value.length <= 11 &&
    agreed.value &&
    !props.loading,
)
</script>

<template>
  <div class="space-y-5">
    <div>
      <h1 class="text-title tracking-display text-ink m-0">본인확인이 필요해요</h1>
      <p class="text-body text-muted mt-2 mb-0">
        에코마일리지에 등록된 우리 집 사용량을 가져올게요
      </p>
    </div>

    <GpCard title="가져오는 정보">
      <ul class="text-body-sm text-ink-soft m-0 list-none space-y-2 p-0">
        <li>에코마일리지에 등록된 주소</li>
        <li>최근 2년 월별 사용량</li>
        <li>등록된 요금 종류 (전기 · 도시가스 · 수도)</li>
      </ul>
    </GpCard>

    <div class="space-y-2">
      <label class="block">
        <span class="text-body-strong text-muted mb-2 block">이름</span>
        <input
          v-model="name"
          type="text"
          autocomplete="name"
          maxlength="20"
          placeholder="이름을 입력하세요"
          class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 outline-hidden"
        />
      </label>

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
      <span class="text-body text-ink-soft">에코마일리지 사용량 조회에 동의해요</span>
    </label>

    <p v-if="errorMessage" class="text-body-sm text-negative m-0">{{ errorMessage }}</p>

    <div>
      <GpButton :disabled="!canSubmit" @click="emit('verify')">
        {{ loading ? '확인하는 중...' : '인증하고 사용량 불러오기' }}
      </GpButton>

      <p class="text-caption text-muted bg-surface-sub mt-3 mb-0 flex gap-2 rounded-md p-3">
        <IconInfo :size="16" class="text-icon-off mt-0.5 shrink-0" aria-hidden="true" />
        <span>발표용 데모라 실제 본인확인은 하지 않아요. 입력한 값은 서버로 보내지 않아요.</span>
      </p>
    </div>

    <button
      type="button"
      class="text-body-sm text-muted mx-auto block cursor-pointer border-0 bg-transparent underline"
      @click="emit('notMember')"
    >
      아직 에코마일리지 회원이 아니에요
    </button>
  </div>
</template>
