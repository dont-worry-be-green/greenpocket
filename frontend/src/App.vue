<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <!-- 모바일 웹이 기본 타깃이다. 데스크톱에서도 시안 기준폭을 유지한다.
       393px = iPhone 14 Pro. 디자인 토큰의 --gp-viewport-w 와 같은 값이다 (frontend/AGENTS.md 7절) -->
  <div class="bg-canvas mx-auto min-h-dvh w-full max-w-[393px]">
    <RouterView />
  </div>
</template>
