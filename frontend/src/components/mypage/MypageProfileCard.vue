<script setup>
/*
 * 프로필 요약 카드 — MY-01 (E-1-01 · A-1-07)
 *
 * `profileSummary` 는 **서버가 조립한 문자열**이다(api-spec 5.2). 여기서 지역·주거 형태를
 * 다시 이어 붙이지 않는다 — A-1-07 완료 조건이 "동일 값이 화면마다 다르게 표시되지 않는다"고,
 * 같은 문장을 진단 헤더도 쓴다.
 */
import GpCard from '@/components/ui/GpCard.vue'
import IconUser from '@/components/ui/icons/IconUser.vue'

defineProps({
  profile: { type: Object, required: true },
})
</script>

<template>
  <GpCard>
    <div class="flex items-center gap-4">
      <span
        class="bg-primary-bg text-primary-soft flex size-14 shrink-0 items-center justify-center rounded-full"
        aria-hidden="true"
      >
        <IconUser :size="28" />
      </span>
      <div class="min-w-0">
        <p class="text-title tracking-display text-ink m-0 truncate">{{ profile.name }}</p>
        <p class="text-body-sm text-muted mt-1 mb-0 truncate">{{ profile.profileSummary }}</p>
      </div>
    </div>
  </GpCard>
</template>
