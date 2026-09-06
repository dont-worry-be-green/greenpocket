<script setup>
import { RouterView } from 'vue-router'

import DemoToolsFab from '@/components/layout/DemoToolsFab.vue'

/*
 * 데모 도구는 개발 빌드에서만 뜬다. `npm run build` 결과물에는 들어가지 않는다
 * (Vite 가 `import.meta.env.DEV` 를 false 로 치환해 통째로 떨어져 나간다).
 */
const isDev = import.meta.env.DEV
</script>

<template>
  <!-- 모바일 웹이 기본 타깃이다. 데스크톱에서도 시안 기준폭을 유지한다.
       393px = iPhone 14 Pro. 디자인 토큰의 --gp-viewport-w 와 같은 값이다 (frontend/AGENTS.md 7절) -->
  <div class="bg-canvas mx-auto min-h-dvh w-full max-w-[393px]">
    <RouterView />
  </div>

  <DemoToolsFab v-if="isDev" />
</template>
