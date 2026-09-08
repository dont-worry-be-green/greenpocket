<script setup>
/* 프로필 이름 카드 — MY-01 (E-1-01) */
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
      </div>
    </div>
  </GpCard>
</template>
