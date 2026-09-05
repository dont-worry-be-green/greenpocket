<script setup>
/*
 * 온보딩 진행 바 (ONB-02 상단)
 *
 * 칸 수는 온보딩 화면 수와 같다 — 지금은 ONB-01 · ONB-02 둘뿐이라 2칸이고,
 * ONB-02 에 오면 이름 등록이 끝난 상태라 **두 칸을 다 채운다.**
 * (시안은 왼쪽 한 칸만 찼지만 그 화면에서 남은 단계가 없다. 시안 오류로 본다)
 */
defineProps({
  step: { type: Number, required: true },
  total: { type: Number, required: true },
})
</script>

<template>
  <div
    class="flex gap-2"
    role="progressbar"
    :aria-valuenow="step"
    aria-valuemin="1"
    :aria-valuemax="total"
    :aria-label="`온보딩 ${total}단계 중 ${step}단계`"
  >
    <span
      v-for="index in total"
      :key="index"
      class="h-1 flex-1 rounded-full"
      :class="index <= step ? 'bg-primary' : 'bg-track'"
    />
  </div>
</template>
