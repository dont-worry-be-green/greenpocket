<script setup>
/*
 * 아직 화면이 붙지 않은 탭의 자리표시자.
 *
 * 탭바(COM-02)를 먼저 세우려면 탭 5개가 모두 갈 곳이 있어야 하는데, 화면은 담당자별로 나중에
 * 붙는다. 그동안 빈 화면을 남기지 않기 위한 것이다(COM-08 "핵심 흐름에 무반응 또는 빈 화면이 없다").
 *
 * **담당자가 실제 화면을 만들면 routes/<도메인>.js 의 component 한 줄만 바꾸면 된다.**
 * 이 파일은 어느 도메인에도 속하지 않으므로 views/ 바로 아래에 둔다.
 *
 * 제목·설명은 라우트 meta 에서 읽는다. meta 형태는 routes/eco.js 주석 참고.
 */
import { useRoute } from 'vue-router'
import AppTabLayout from '@/components/layout/AppTabLayout.vue'
import GpCard from '@/components/ui/GpCard.vue'

const route = useRoute()
</script>

<template>
  <AppTabLayout
    :tab="route.meta.tab"
    :title="route.meta.title"
    :subtitle="route.meta.subtitle ?? ''"
  >
    <GpCard>
      <p class="text-body text-muted m-0">화면을 준비하고 있어요. 아래 탭으로 이동할 수 있어요.</p>
    </GpCard>
  </AppTabLayout>
</template>
