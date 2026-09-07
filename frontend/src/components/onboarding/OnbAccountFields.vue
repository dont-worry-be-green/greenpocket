<script setup>
/*
 * ONB-01b 회원가입 ─ 계정 입력 블록 (이슈 #121)
 *
 * 아이디·비밀번호만 받는다. **이름은 본인확인 블록에 있다**(`OnbVerifyFields`) — 인증의 대상이고
 * 그 값이 그린포켓 예금주가 된다(결정 C-14).
 *
 * ── 인증 전에도 보인다 ──────────────────────────────────────────────────
 * 인증이 끝나야 나타나게 하지 않는다. 무엇을 적어야 하는지 처음에 다 보이는 편이 낫고,
 * **잠기는 것은 「가입하고 시작하기」 하나면 충분하다.** 그 판정은 화면이 한다(`SignupView`).
 *
 * ⚠️ **비밀번호는 아무 데도 보내지 않고 저장하지도 않는다.** BE 에 `POST /auth/signup` 이
 * 아직 없어 대조할 곳이 없고, 평문을 `localStorage` 에 두는 쪽이 더 나쁘다(`api/auth.js` 주석).
 * 한동안 화면 하단 캡션이 그 사실을 밝혔는데 **화면이 너무 길어 뺐다.** 지금 그 고지가 남아 있는
 * 곳은 로그인 화면(ONB-01a) 캡션뿐이다 — 계약이 붙기 전에 발표 문구를 손볼 일이 있으면 여기를 본다.
 *
 * 그래도 **입력 검증은 실제로 한다.** 비밀번호 확인 불일치·형식 미달을 통과시키면 나중에 계약이
 * 붙었을 때 처음 보는 에러가 된다(`StartView` 의 이름 검증과 같은 태도). 판정은 화면이 갖는다 —
 * 이 블록에는 CTA 가 없어서 무엇이 먼저 걸리는지 여기서 알 수 없다.
 */
const loginId = defineModel('loginId', { type: String, default: '' })
const password = defineModel('password', { type: String, default: '' })
const passwordConfirm = defineModel('passwordConfirm', { type: String, default: '' })

defineProps({
  /** 중복확인 결과. `''` 미확인 · `AVAILABLE` 쓸 수 있음 · `TAKEN` 이미 쓰는 중 */
  idStatus: { type: String, default: '' },
  /** 아이디 형식 오류. 판정과 문구는 화면이 갖고, 여기는 아이디칸 아래에 놓기만 한다 */
  idError: { type: String, default: '' },
  /** 형식이 맞아야 눌러 볼 수 있다. 판정은 화면이 한다 */
  canCheckId: { type: Boolean, default: false },
  /** **이 버튼만의 로딩이다.** 화면의 버튼 넷이 한 플래그를 나눠 쓰면 하나를 눌렀을 때 넷이 다 돈다 */
  checking: { type: Boolean, default: false },
})
/*
 * `blur` 는 비밀번호 칸만 낸다. **아이디 칸은 `id-blur` 로 따로 낸다** — 「중복확인」을 누르면
 * 아이디 칸이 blur 되는데, 그것이 비밀번호 오류까지 켜면 아직 손대지도 않은 칸을 나무라게 된다.
 */
const emit = defineEmits(['blur', 'id-blur', 'check-id'])
</script>

<template>
  <section class="space-y-3">
    <!-- 버튼이 붙어 label 로 감싸지 않는다 — label 안의 버튼은 눌러도 입력칸이 포커스를 가져간다 -->
    <div>
      <span class="text-body-strong text-muted mb-2 block" id="login-id-label">아이디</span>
      <div class="flex gap-2">
        <input
          v-model="loginId"
          type="text"
          autocomplete="username"
          maxlength="20"
          placeholder="4자 이상, 영문·숫자"
          aria-labelledby="login-id-label"
          class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 outline-hidden"
          @blur="emit('id-blur')"
        />
        <button
          type="button"
          :disabled="!canCheckId || checking"
          class="ease-standard bg-primary-bg text-primary-on-soft text-body-strong disabled:bg-disabled-bg disabled:text-disabled-text min-h-14 shrink-0 cursor-pointer rounded-lg border-0 px-4 transition-colors duration-140 disabled:cursor-not-allowed"
          @click="emit('check-id')"
        >
          {{ checking ? '확인 중...' : '중복확인' }}
        </button>
      </div>

      <!-- 형식 오류가 먼저다. 형식이 안 맞으면 중복확인을 눌러 볼 수조차 없다 -->
      <span v-if="idError" class="text-body-sm text-negative mt-1.5 block">{{ idError }}</span>
      <!-- 확인 결과. 아이디를 고치면 화면이 이 값을 비운다(`SignupView`) -->
      <span
        v-else-if="idStatus === 'AVAILABLE'"
        class="text-body-sm text-primary-on-soft mt-1.5 block"
      >
        사용할 수 있는 아이디예요.
      </span>
      <span v-else-if="idStatus === 'TAKEN'" class="text-body-sm text-negative mt-1.5 block">
        이미 사용 중인 아이디예요. 다른 아이디를 써 주세요.
      </span>
    </div>

    <label class="block">
      <span class="text-body-strong text-muted mb-2 block">비밀번호</span>
      <input
        v-model="password"
        type="password"
        autocomplete="new-password"
        maxlength="32"
        placeholder="8자 이상"
        class="bg-surface border-border text-body placeholder:text-disabled-text min-h-14 w-full rounded-lg border px-4 outline-hidden"
        @blur="emit('blur')"
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
        @blur="emit('blur')"
      />
    </label>
  </section>
</template>
