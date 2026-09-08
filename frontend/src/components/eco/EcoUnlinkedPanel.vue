<script setup>
/*
 * WF-01 에코마일리지 연동 전 (B-1-01 · B-1-09)
 *
 * linkable 은 GET /eco/status 의 필드다. 서울 밖이면 false + blockReason: 'NOT_SEOUL' 로
 * 내려와 버튼이 비활성된다(B-1-09). 사유 문장은 서버가 주지 않으므로 아래 안내 카드가 대신한다.
 *
 * ── 본인확인 단계가 여기 없는 이유 ──────────────────────────────────────
 * 한동안 이 버튼이 본인확인 화면(WF-01a)으로 갔다. 서비스에 로그인·회원가입이 들어오면서
 * 본인확인은 **회원가입으로 옮겨갔고**(이슈 #121), 여기서는 곧바로 연동을 시작한다.
 * 그래서 화면이 하나 줄었고 시안(연동전 1.png)과도 다시 맞는다.
 *
 * ── 미가입 안내를 이 화면에 둔다 ────────────────────────────────────────
 * 인증된 사용자 상태는 JWT로 확인한다. 에코마일리지 자체의 가입·등록 여부는
 * `GET /eco/status`가 직접 판정하므로 이 화면은 서버 결과에 따라 연동 경로를 보여준다.
 */
import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import IconExternalLink from '@/components/ui/icons/IconExternalLink.vue'
import IconPlant from '@/components/ui/icons/IconPlant.vue'

defineProps({
  linkable: { type: Boolean, default: true },
  /** GET /eco/status 의 externalUrl (api-spec.md 8.1). 없으면 하단 버튼을 감춘다 */
  externalUrl: { type: String, default: '' },
  loading: { type: Boolean, default: false },
})
defineEmits(['link', 'openSite'])
</script>

<template>
  <div class="space-y-4">
    <GpCard>
      <div class="px-2 py-4 text-center">
        <span class="text-primary flex justify-center" aria-hidden="true">
          <IconPlant :size="32" />
        </span>
        <h2 class="text-section tracking-display mt-4 mb-2">얼마나 줄일지 정해볼까요</h2>
        <p class="text-body text-muted m-0">
          에코마일리지는 <strong class="text-ink-soft font-semibold">작년 같은 달</strong>과
          비교해요.<br />
          전기·도시가스·수도 사용량을 불러올게요.
        </p>
      </div>
    </GpCard>

    <GpCard title="불러올 것">
      <ul class="text-body-sm text-ink-soft m-0 list-none space-y-2 p-0">
        <li>등록된 요금 종류 (전기 · 도시가스 · 수도)</li>
        <li>최근 2년 월별 사용량 — 평가 기준이 돼요</li>
        <li>에코마일리지에 등록된 주소</li>
      </ul>
      <p class="text-caption text-muted mt-3 mb-0">20초쯤 걸려요</p>
    </GpCard>

    <div>
      <GpButton :disabled="!linkable || loading" @click="$emit('link')">
        {{ loading ? '불러오는 중...' : '에코마일리지 연동하기' }}
      </GpButton>
      <p class="text-caption text-muted mt-3 mb-0 text-center">
        가입할 때 확인한 본인 명의로 불러와요
      </p>
    </div>

    <GpCard tone="sub">
      <p class="text-caption text-muted m-0">
        작년에 이 집에 살지 않았어도 전입자 사용분이 기준이 되고, 신축이면 비슷한 가구가 기준이
        돼요.<br />
        서울시 제도라 지금은 서울 거주자만 쓸 수 있어요.
      </p>
    </GpCard>

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

      <GpButton
        v-if="externalUrl"
        variant="wide"
        size="wide"
        class="mt-4"
        @click="$emit('openSite')"
      >
        누리집에서 에코마일리지 시작하기
        <IconExternalLink :size="14" class="ml-1.5" />
      </GpButton>

      <p class="text-caption text-muted mt-3 mb-0">
        가입 직후에는 사용량이 아직 안 보일 수 있어요. 그럴 땐 잠시 뒤에 다시 눌러 주세요.
      </p>
    </GpCard>
  </div>
</template>
