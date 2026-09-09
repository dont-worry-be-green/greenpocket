<script setup>
/*
 * WF-03 What-if 기준 사용량 전용 헤더.
 * 진단 탭에서도 같은 기준 사용량 카드를 쓰지만, 이 인사와 목표 안내는 What-if에만 보인다.
 */
import characterPush from '@/assets/character/push.png'
import gpLogo from '@/assets/그린포켓 로고 .png'

defineProps({
  name: { type: String, default: '' },
})
</script>

<template>
  <header class="relative z-0 pt-3">
    <div class="flex items-center gap-2">
      <img :src="gpLogo" alt="" aria-hidden="true" class="size-10 shrink-0 object-contain select-none" />
      <b class="text-list-title text-ink tracking-title">{{ name ? `${name}님` : '안녕하세요' }}</b>
    </div>

    <div class="relative mt-4 min-h-36">
      <div
        class="baseline-speech bg-surface shadow-card absolute top-5 left-20 z-[1] w-fit rounded-[24px] px-4 py-3.5"
      >
        <p class="tracking-title m-0 text-[16px] leading-[1.45] font-semibold">
          기준 사용량을 바탕으로<br />
          목표를 설정해봐요!
        </p>
      </div>
      <img
        :src="characterPush"
        alt=""
        aria-hidden="true"
        class="pointer-events-none absolute -right-7 -bottom-8 size-32 object-contain drop-shadow-[0_3px_7px_rgb(20_50_36/0.12)] select-none"
      />
    </div>
  </header>
</template>

<style scoped>
.baseline-speech::after {
  position: absolute;
  top: 68%;
  right: -13px;
  width: 16px;
  height: 18px;
  content: '';
  background: inherit;
  clip-path: polygon(0 0, 100% 50%, 0 100%);
  transform: translateY(-50%);
}
</style>
