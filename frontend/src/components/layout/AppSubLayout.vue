<script setup>
/*
 * 하위 화면 셸 (기능명세서 COM-02)
 * 뒤로가기 헤더 + 본문. **탭바를 붙이지 않는다.**
 *
 * back 을 주지 않으면 브라우저 히스토리로 돌아간다. 새로고침 직후처럼 히스토리가 없을 때를
 * 대비해 돌아갈 곳이 정해진 화면은 back 에 경로를 명시한다.
 *
 *   <AppSubLayout title="목표 정하기" back="/whatif"> ...본문... </AppSubLayout>
 *
 * ── subtitle 을 주면 제목이 커진다 ────────────────────────────────────────
 * 기본은 뒤로가기 옆 작은 제목 한 줄이다. `subtitle` 을 넘기면 헤더에는 화살표만 남고
 * 본문 위에 탭 화면과 같은 **큰 제목 + 설명 한 줄**이 온다(WF-07 · WF-10 시안).
 * 부제가 없는 화면은 아무것도 달라지지 않는다.
 */
import { useRouter } from 'vue-router'
import GpBackHeader from '@/components/ui/GpBackHeader.vue'
import GpPageHeader from '@/components/ui/GpPageHeader.vue'

const props = defineProps({
  title: { type: String, default: '' },
  // 주면 큰 제목 블록으로 바뀐다. 위 주석 참고
  subtitle: { type: String, default: '' },
  back: { type: [String, Object], default: null },
  centerTitle: { type: Boolean, default: false },
  // 뒤로가기(←) 대신 닫기(X). 한 번 보고 닫는 결과 화면용 (WF-11)
  dismiss: { type: Boolean, default: false },
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
    <GpBackHeader
      :title="subtitle ? '' : title"
      :center-title="centerTitle"
      :dismiss="dismiss"
      @back="onBack"
    >
      <template v-if="$slots.headerAction" #action>
        <slot name="headerAction" />
      </template>
    </GpBackHeader>

    <GpPageHeader v-if="subtitle" :title="title" :subtitle="subtitle" class="pt-1" />

    <main class="px-(--gp-gutter)" :class="hasFooter ? 'pb-(--gp-safe-bottom)' : 'pb-8'">
      <slot />
    </main>

    <slot name="footer" />
  </div>
</template>
