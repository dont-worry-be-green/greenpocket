<script setup>
/*
 * 하위 화면 셸 (기능명세서 COM-02)
 * 뒤로가기 헤더 + 본문. **탭바를 붙이지 않는다.**
 *
 * back 을 주지 않으면 브라우저 히스토리로 돌아간다. 새로고침 직후처럼 히스토리가 없을 때를
 * 대비해 돌아갈 곳이 정해진 화면은 back 에 경로를 명시한다.
 *
 *   <AppSubLayout title="목표 정하기" back="/whatif"> ...본문... </AppSubLayout>
 */
import { useRouter } from 'vue-router'
import GpBackHeader from '@/components/ui/GpBackHeader.vue'

const props = defineProps({
  title: { type: String, default: '' },
  back: { type: [String, Object], default: null },
  centerTitle: { type: Boolean, default: false },
  // 하단에 CTA 를 고정하는 화면은 본문이 가리지 않도록 여백을 넓힌다
  hasFooter: { type: Boolean, default: false },
})

const router = useRouter()

function onBack() {
  if (props.back) router.push(props.back)
  else if (window.history.state?.back) router.back()
  else router.push('/whatif')
}
</script>

<template>
  <div class="bg-canvas min-h-dvh">
    <GpBackHeader :title="title" :center-title="centerTitle" @back="onBack">
      <template v-if="$slots.headerAction" #action>
        <slot name="headerAction" />
      </template>
    </GpBackHeader>

    <main class="px-(--gp-gutter)" :class="hasFooter ? 'pb-(--gp-safe-bottom)' : 'pb-8'">
      <slot />
    </main>

    <slot name="footer" />
  </div>
</template>
