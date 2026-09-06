<script setup>
/*
 * WF-06 에코마일리지 참여 신청 안내 (B-4-05)
 *
 * `application` 은 GET /eco/home 의 `application` 그대로다.
 * **띄울지 말지는 서버의 `showBanner` 가 정한다** — `applicationStatus` 를 보고 화면이 판정하지 않는다.
 *
 * 실제 신청은 누리집에서 한다. 여기서 하는 일은 **신청했다고 표시**하는 것뿐이다.
 *
 * ⚠️ 상태 배지(신청 전·신청 중…)를 두지 않는다. 이 배너는 `showBanner` 가 true 일 때,
 * 곧 **미신청일 때만** 뜨므로 배지가 늘 같은 값이 되어 자리만 차지한다.
 * 명세 문구의 "아직 안 했어요" 가 그 자리를 대신한다(B-4-05).
 */
import GpButton from '@/components/ui/GpButton.vue'
import GpCard from '@/components/ui/GpCard.vue'
import IconSealCheck from '@/components/ui/icons/IconSealCheck.vue'

defineProps({
  application: { type: Object, required: true },
  loading: { type: Boolean, default: false },
})
defineEmits(['apply'])
</script>

<template>
  <GpCard>
    <div class="flex items-start gap-2.5">
      <IconSealCheck :size="20" class="text-primary-soft mt-0.5 shrink-0" aria-hidden="true" />

      <div class="min-w-0 flex-1">
        <p class="text-body-strong text-ink m-0">참여신청을 해야 받아요</p>
        <p class="text-caption text-muted mt-1 mb-0">
          평가 기간마다 한 번 · 아직 안 했어요 · 2년 연속 안 하면 휴면회원이 돼요
        </p>
      </div>

      <GpButton
        variant="pill"
        size="pill"
        :disabled="loading"
        class="mt-0.5"
        @click="$emit('apply', application.externalUrl)"
      >
        {{ loading ? '여는 중' : '신청' }}
      </GpButton>
    </div>
  </GpCard>
</template>
