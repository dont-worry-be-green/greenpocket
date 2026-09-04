<script setup>
/*
 * 그린포켓 · 하단 탭바 (기능명세서 COM-02)
 * 진단 · 혜택 · [Green What-if] · 포켓 · 마이페이지
 * 가운데 What-if 는 FAB 형태이고 서비스의 메인 탭이자 진입 화면을 겸한다.
 *
 * 시안은 아트보드 안에서 position:absolute 였지만 실제 화면에서는 fixed 라야
 * 스크롤과 무관하게 붙어 있는다. 하단 노치는 safe-area-inset 으로 피한다.
 */
defineProps({
  active: { type: String, default: 'whatif' },
  // [{ key, label, icon }] · 순서 고정: 진단 · 혜택 · whatif · 포켓 · 마이페이지
  tabs: { type: Array, required: true },
})
defineEmits(['change'])
</script>

<template>
  <nav
    class="bg-surface shadow-float fixed inset-x-0 bottom-0 z-20 mx-auto flex h-(--gp-tabbar-h) max-w-[393px] items-center pb-[env(safe-area-inset-bottom)]"
    role="tablist"
  >
    <button
      v-for="t in tabs"
      :key="t.key"
      type="button"
      role="tab"
      :aria-selected="active === t.key"
      class="flex min-h-(--gp-min-touch) flex-1 cursor-pointer flex-col items-center gap-[3px] border-0 bg-transparent"
      :class="active === t.key ? 'text-primary' : 'text-icon-off'"
      @click="$emit('change', t.key)"
    >
      <span
        v-if="t.key === 'whatif'"
        class="text-on-primary shadow-float z-30 -mt-[calc(var(--gp-fab)/2+4px)] flex size-(--gp-fab) items-center justify-center rounded-full bg-(image:--gp-grad-hero)"
      >
        <component :is="t.icon" />
      </span>
      <component :is="t.icon" v-else />
      <span class="text-nav" :class="{ 'font-bold': active === t.key }">{{ t.label }}</span>
    </button>
  </nav>
</template>
