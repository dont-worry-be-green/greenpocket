<script setup>
/*
 * 탭 루트 화면 셸 (기능명세서 COM-02)
 * 헤더 + 본문 + 하단 탭바. 탭 5개의 최상위 화면(AN-01 · BN-01 · WF-06 · PK-01 · MY-01)이 쓴다.
 *
 * 하위 화면(목표 정하기 · 고지서 상세 · 출금 등)은 탭바 대신 AppSubLayout 을 쓴다.
 *
 *   <AppTabLayout tab="whatif" title="Green What-if" subtitle="...">
 *     ...본문...
 *   </AppTabLayout>
 */
import { useRouter } from 'vue-router'
import GpTabBar from '@/components/ui/GpTabBar.vue'
import GpPageHeader from '@/components/ui/GpPageHeader.vue'
import { TABS, findTab } from './tabs'

const props = defineProps({
  tab: { type: String, required: true }, // tabs.js 의 key
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  eyebrow: { type: String, default: '' },
})

const router = useRouter()

function onChange(key) {
  if (key === props.tab) return
  const next = findTab(key)
  if (next) router.push(next.path)
}
</script>

<template>
  <div class="bg-canvas min-h-dvh">
    <GpPageHeader v-if="title" :title="title" :subtitle="subtitle" :eyebrow="eyebrow">
      <template v-if="$slots.headerAction" #action>
        <slot name="headerAction" />
      </template>
    </GpPageHeader>

    <!-- 탭바가 떠 있는 만큼 아래 여백을 둔다. 없으면 마지막 카드가 탭바에 가린다 -->
    <main class="px-(--gp-gutter) pb-(--gp-safe-bottom)">
      <slot />
    </main>

    <GpTabBar :active="tab" :tabs="TABS" @change="onChange" />
  </div>
</template>
