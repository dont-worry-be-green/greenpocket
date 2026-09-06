<script setup>
/*
 * WF-01a 두 번째 단계 — 본인확인 뒤 「연동하기」 (B-1-01 · B-1-02 · COM-05)
 *
 * 인증을 먼저 받고 그 위에서 연동을 건다. 인증이 전제여야 「이 사람이 에코마일리지에
 * 등록돼 있는가」를 물을 수 있어서다. 그래서 인증과 연동을 한 번의 누름으로 묶지 않는다 —
 * 묶으면 사용자가 무엇에 동의해 무엇이 일어났는지 구분할 수 없다.
 *
 * ── 미가입 분기를 화면이 판정하지 않는다 ────────────────────────────────
 * ⚠️ **가입 여부를 알려주는 서버 값이 없다.** `POST /eco/link` 는 `X-Demo-Key` 밖에 모르고
 * 본인확인 엔드포인트도 없다(`api/eco.js` 의 `verifyEcoIdentity`). 그래서 미가입·등록 정보
 * 없음을 **별도 화면으로 가르지 않고 하단 안내로 상시 열어 둔다** — 서버가 아니라고 하지 않은
 * 것을 아니라고 말하지 않으려는 것이다(핵심 규칙 8).
 *
 * 누리집에서 가입하고 돌아오면 위의 「연동하기」를 그대로 누르면 된다. 그래서 재시도 버튼을
 * 따로 두지 않는다. 자동 재조회(`visibilitychange`)도 넣지 않는다 — 탭 전환·알림·화면 잠금에도
 * 걸려 발표 중 엉뚱한 시점에 연동이 시작된다(BN-01 과 같은 판단).
 */
import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import IconExternalLink from '@/components/ui/icons/IconExternalLink.vue'
import IconInfo from '@/components/ui/icons/IconInfo.vue'
import IconSealCheck from '@/components/ui/icons/IconSealCheck.vue'

const props = defineProps({
  /** 본인확인 폼에 입력한 이름. 없으면 이름 없이 문구를 만든다 */
  name: { type: String, default: '' },
  /** GET /eco/status 의 externalUrl (api-spec.md 8.1). 없으면 하단 버튼을 감춘다 */
  externalUrl: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  errorMessage: { type: String, default: '' },
})
const emit = defineEmits(['link', 'openSite'])

const greeting = () => (props.name ? `${props.name}님, 확인됐어요` : '본인확인이 끝났어요')
</script>

<template>
  <div class="space-y-5">
    <div>
      <span class="text-primary flex" aria-hidden="true"><IconSealCheck :size="32" /></span>
      <h1 class="text-title tracking-display text-ink mt-3 mb-0">{{ greeting() }}</h1>
      <p class="text-body text-muted mt-2 mb-0">
        이제 에코마일리지에 등록된 우리 집 사용량을 불러올 수 있어요
      </p>
    </div>

    <GpCard title="불러올 것">
      <ul class="text-body-sm text-ink-soft m-0 list-none space-y-2 p-0">
        <li>등록된 요금 종류 (전기 · 도시가스 · 수도)</li>
        <li>최근 2년 월별 사용량 — 평가 기준이 돼요</li>
        <li>에코마일리지에 등록된 주소</li>
      </ul>
      <p class="text-caption text-muted mt-3 mb-0">20초쯤 걸려요</p>
    </GpCard>

    <p v-if="errorMessage" class="text-body-sm text-negative m-0">{{ errorMessage }}</p>

    <div>
      <GpButton :disabled="loading" @click="emit('link')">
        {{ loading ? '불러오는 중...' : '에코마일리지 연동하기' }}
      </GpButton>

      <p class="text-caption text-muted bg-surface-sub mt-3 mb-0 flex gap-2 rounded-md p-3">
        <IconInfo :size="16" class="text-icon-off mt-0.5 shrink-0" aria-hidden="true" />
        <span>발표용 데모라 실제 본인확인은 하지 않아요. 사용량은 시드 데이터예요.</span>
      </p>
    </div>

    <!-- 미가입·등록 정보 없음. 화면이 판정하지 않고 길만 열어 둔다 (위 주석 참고) -->
    <GpCard tone="sub" title="아직 회원이 아니거나 등록 정보가 없나요?">
      <p class="text-body-sm text-ink-soft mt-0 mb-3">
        에코마일리지는 서울시가 운영하는 제도예요. 누리집에서 가입하고 돌아와 위 버튼을 다시 누르면
        사용량을 불러올게요.
      </p>

      <p class="text-caption text-muted mt-0 mb-1 font-semibold">가입할 때 필요한 것</p>
      <ul class="text-caption text-muted m-0 list-none space-y-1 p-0">
        <li>지금 살고 있는 집 주소</li>
        <li>전기 고객번호 (고지서에 적혀 있어요)</li>
        <li>도시가스 · 수도 고객번호 (있으면 함께 등록해요)</li>
      </ul>

      <GpButton v-if="externalUrl" variant="wide" size="wide" class="mt-4" @click="emit('openSite')">
        누리집에서 에코마일리지 시작하기
        <IconExternalLink :size="14" class="ml-1.5" />
      </GpButton>

      <p class="text-caption text-muted mt-3 mb-0">
        가입 직후에는 사용량이 아직 안 보일 수 있어요. 그럴 땐 잠시 뒤에 다시 눌러 주세요.
      </p>
    </GpCard>
  </div>
</template>
