<script setup>
/*
 * 목표 구간 하향 제안 — WF-08 (B-4-09)
 *
 * `downgrade` 는 GET .../mission-adjust 의 `tierDowngrade` 그대로다.
 *
 * ⚠️ **제안만 한다.** 여기서 구간을 바꾸지 않는다(핵심 규칙 9 — 사용자 선택을 말없이 바꾸지 않는다).
 * 버튼은 목표 정하기 화면으로 보낼 뿐이고, 실제 변경은 거기서 사용자가 한다.
 *
 * `suggest` 는 **2회 연속 미달일 때만** true 다. 한 달 미끄러진 것으로 제안하면 잔소리가 된다 —
 * 그 판정은 서버가 하므로 `consecutiveMisses` 를 화면에서 다시 세지 않는다.
 */
import GpButton from '@/components/ui/GpButton.vue'
import IconWarning from '@/components/ui/icons/IconWarning.vue'

defineProps({
  downgrade: { type: Object, required: true },
})
defineEmits(['edit-goal'])
</script>

<template>
  <div v-if="downgrade.suggest" class="bg-negative-bg rounded-lg p-(--gp-card-pad)">
    <div class="flex items-start gap-2">
      <IconWarning :size="18" class="text-negative mt-px flex-none" />
      <p class="text-body text-ink-soft m-0">{{ downgrade.message }}</p>
    </div>
    <div class="mt-3">
      <GpButton variant="pill" size="pill" @click="$emit('edit-goal')">목표 구간 다시 보기</GpButton>
    </div>
  </div>
</template>
