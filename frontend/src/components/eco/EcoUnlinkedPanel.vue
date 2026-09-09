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
import diagnosisGuide from '@/assets/character/diagnosis-guide.png'
import ecoMileageLogo from '@/assets/eco-mileage-logo.png'
import IconChart from '@/components/ui/icons/IconChart.vue'
import IconExternalLink from '@/components/ui/icons/IconExternalLink.vue'
import IconMapPin from '@/components/ui/icons/IconMapPin.vue'
import IconReceipt from '@/components/ui/icons/IconReceipt.vue'

defineProps({
  linkable: { type: Boolean, default: true },
  /** GET /eco/status 의 externalUrl (api-spec.md 8.1). 없으면 하단 버튼을 감춘다 */
  externalUrl: { type: String, default: '' },
  loading: { type: Boolean, default: false },
})
defineEmits(['link', 'openSite'])
</script>

<template>
  <div class="space-y-6">
    <section class="px-1 pt-10 pb-2">
      <h2
        class="-ml-4 flex items-center justify-start gap-0 text-[18px] leading-[1.4] font-semibold tracking-[-0.05em]"
      >
        <img :src="diagnosisGuide" alt="" class="size-20 shrink-0 rounded-full object-contain" />
        <span class="-ml-2">
          <span class="block whitespace-nowrap">진단을 위한 에코마일리지</span>
          <span class="block whitespace-nowrap">연동부터 진행할게요</span>
        </span>
      </h2>
    </section>

    <section>
      <div class="mb-3 flex items-center gap-2">
        <h3 class="text-list-title m-0 font-semibold">불러올 데이터</h3>
        <img
          :src="ecoMileageLogo"
          alt="에코마일리지"
          class="h-8 w-auto max-w-28 object-contain mix-blend-multiply"
        />
      </div>
      <ul class="m-0 grid list-none grid-cols-3 gap-2 p-0">
        <li
          class="bg-surface rounded-card flex min-h-40 flex-col items-center px-2 py-5 text-center"
        >
          <span
            class="bg-primary-bg text-primary-on-soft flex size-12 items-center justify-center rounded-full"
          >
            <IconReceipt :size="23" />
          </span>
          <span class="text-body mt-4 font-semibold">요금 종류</span>
          <span class="text-caption text-muted mt-1">전기·가스·수도</span>
        </li>
        <li
          class="bg-surface rounded-card flex min-h-40 flex-col items-center px-2 py-5 text-center"
        >
          <span
            class="bg-primary-bg text-primary-on-soft flex size-12 items-center justify-center rounded-full"
          >
            <IconChart :size="23" />
          </span>
          <span class="text-body mt-4 font-semibold">2년 사용량</span>
          <span class="text-caption text-muted mt-1">평가 기준</span>
        </li>
        <li
          class="bg-surface rounded-card flex min-h-40 flex-col items-center px-2 py-5 text-center"
        >
          <span
            class="bg-primary-bg text-primary-on-soft flex size-12 items-center justify-center rounded-full"
          >
            <IconMapPin :size="23" />
          </span>
          <span class="text-body mt-4 font-semibold">등록 주소</span>
          <span class="text-caption text-muted mt-1">에코마일리지</span>
        </li>
      </ul>
    </section>

    <div>
      <GpButton :disabled="!linkable || loading" @click="$emit('link')">
        {{ loading ? '불러오는 중...' : '에코마일리지 연동하기' }}
      </GpButton>
    </div>

    <!-- 미가입·등록 정보 없음. 화면이 판정하지 않고 길만 열어 둔다 (위 주석 참고) -->
    <section v-if="externalUrl">
      <h3 class="text-list-title text-ink mt-0 mb-2 font-semibold">에코마일리지가 처음인가요?</h3>
      <p class="text-body-sm text-muted mt-0 mb-3">
        에코마일리지는 서울시가 운영하는 제도예요. 누리집에서 가입하고 돌아와 위 버튼을 다시 누르면
        사용량을 불러올게요.
      </p>
      <button
        v-if="externalUrl"
        type="button"
        class="border-primary text-primary-on-soft bg-surface text-button flex h-(--gp-cta-h) w-full cursor-pointer items-center justify-center rounded-md border-2 font-semibold active:scale-[0.985]"
        @click="$emit('openSite')"
      >
        누리집에서 에코마일리지 시작하기
        <IconExternalLink :size="14" class="ml-1.5" />
      </button>
    </section>
  </div>
</template>
