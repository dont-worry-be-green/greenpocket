<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <!-- 모바일 웹이 기본 타깃이다. 데스크톱에서도 모바일 폭을 유지한다 (frontend/AGENTS.md 7절) -->
  <div class="mx-auto min-h-dvh w-full max-w-[430px] bg-white">
    <RouterView />
  </div>
</template>
