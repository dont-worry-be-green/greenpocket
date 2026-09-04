<script setup>
/*
 * WF-01 에코마일리지 연동 전 (B-1-01 · B-1-09)
 *
 * linkable 은 GET /eco/status 의 필드다. 서울 밖이면 false + blockReason: 'NOT_SEOUL' 로
 * 내려와 버튼이 비활성된다(B-1-09). 사유 문장은 서버가 주지 않으므로 아래 안내 카드가 대신한다.
 */
import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import IconPlant from '@/components/ui/icons/IconPlant.vue'

defineProps({
  linkable: { type: Boolean, default: true },
})
defineEmits(['link'])
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

    <div>
      <GpButton :disabled="!linkable" @click="$emit('link')">에코마일리지 연동하기</GpButton>
      <p class="text-caption text-muted mt-3 mb-0 text-center">
        에코마일리지 회원이 아니어도 여기서 바로 가입할 수 있어요
      </p>
    </div>

    <GpCard tone="sub">
      <p class="text-caption text-muted m-0">
        작년에 이 집에 살지 않았어도 전입자 사용분이 기준이 되고, 신축이면 비슷한 가구가 기준이
        돼요.<br />
        서울시 제도라 지금은 서울 거주자만 쓸 수 있어요.
      </p>
    </GpCard>
  </div>
</template>
